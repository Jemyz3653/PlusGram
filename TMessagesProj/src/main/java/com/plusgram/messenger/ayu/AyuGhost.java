/*
 * PlusGram for Android.
 * Ghost mode request hook, based on the request hook of AyuGram for Android
 * (Radolyn Labs, https://github.com/AyuGram/AyuGram4A).
 */

package com.plusgram.messenger.ayu;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.RequestDelegateTimestamp;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

import java.util.concurrent.atomic.AtomicInteger;

public final class AyuGhost {

    // Read packets that are allowed to pass even in ghost mode (explicit user actions)
    private static final AtomicInteger allowedReadPackets = new AtomicInteger();

    private AyuGhost() {
    }

    public static void allowNextReadPacket() {
        allowedReadPackets.incrementAndGet();
    }

    private static boolean consumeReadAllowance() {
        while (true) {
            int value = allowedReadPackets.get();
            if (value <= 0) {
                return false;
            }
            if (allowedReadPackets.compareAndSet(value, value - 1)) {
                return true;
            }
        }
    }

    private static boolean isReadPacket(TLObject object) {
        return object instanceof TLRPC.TL_messages_readHistory
            || object instanceof TLRPC.TL_channels_readHistory
            || object instanceof TLRPC.TL_messages_readDiscussion
            || object instanceof TLRPC.TL_messages_readSavedHistory
            || object instanceof TLRPC.TL_messages_readMessageContents
            || object instanceof TLRPC.TL_channels_readMessageContents
            || object instanceof TLRPC.TL_messages_readEncryptedHistory;
    }

    /**
     * Called before a request is serialized. Returns true when the request must not
     * reach the server; in that case the caller gets a harmless fake answer.
     */
    public static boolean intercept(TLObject object, RequestDelegate onComplete, RequestDelegateTimestamp onCompleteTimestamp) {
        if (object == null) {
            return false;
        }
        if (!AyuConfig.sendUploadProgress && (object instanceof TLRPC.TL_messages_setTyping || object instanceof TLRPC.TL_messages_setEncryptedTyping)) {
            deliver(onComplete, onCompleteTimestamp, new TLRPC.TL_boolTrue());
            return true;
        }
        if (!AyuConfig.sendOnlinePackets && object instanceof TL_account.updateStatus) {
            ((TL_account.updateStatus) object).offline = true;
            return false;
        }
        if (!AyuConfig.sendReadStories) {
            if (object instanceof TL_stories.TL_stories_readStories) {
                deliver(onComplete, onCompleteTimestamp, new Vector<Vector.Int>(Vector.Int::TLDeserialize));
                return true;
            }
            if (object instanceof TL_stories.TL_stories_incrementStoryViews) {
                deliver(onComplete, onCompleteTimestamp, new TLRPC.TL_boolTrue());
                return true;
            }
        }
        if (!AyuConfig.sendReadPackets && isReadPacket(object) && !consumeReadAllowance()) {
            deliver(onComplete, onCompleteTimestamp, fakeReadResponse(object));
            return true;
        }
        return false;
    }

    private static TLObject fakeReadResponse(TLObject object) {
        if (object instanceof TLRPC.TL_messages_readHistory || object instanceof TLRPC.TL_messages_readMessageContents) {
            TLRPC.TL_messages_affectedMessages res = new TLRPC.TL_messages_affectedMessages();
            // pts == -1 is ignored by processNewDifferenceParams
            res.pts = -1;
            res.pts_count = 0;
            return res;
        }
        return new TLRPC.TL_boolTrue();
    }

    private static void deliver(RequestDelegate onComplete, RequestDelegateTimestamp onCompleteTimestamp, TLObject response) {
        if (onComplete == null && onCompleteTimestamp == null) {
            return;
        }
        Utilities.stageQueue.postRunnable(() -> {
            try {
                if (onComplete != null) {
                    onComplete.run(response, null);
                } else {
                    onCompleteTimestamp.run(response, null, 0);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    /**
     * With "read after sending" on, a message we send marks the chat as read
     * on the server, like a regular client would do.
     */
    public static RequestDelegate wrap(int account, TLObject object, RequestDelegate onComplete) {
        if (AyuConfig.sendReadPackets || !AyuConfig.markReadAfterSend || onComplete == null) {
            return onComplete;
        }
        final TLRPC.InputPeer peer;
        if (object instanceof TLRPC.TL_messages_sendMessage) {
            peer = ((TLRPC.TL_messages_sendMessage) object).peer;
        } else if (object instanceof TLRPC.TL_messages_sendMedia) {
            peer = ((TLRPC.TL_messages_sendMedia) object).peer;
        } else if (object instanceof TLRPC.TL_messages_sendMultiMedia) {
            peer = ((TLRPC.TL_messages_sendMultiMedia) object).peer;
        } else {
            peer = null;
        }
        if (peer == null) {
            return onComplete;
        }
        return (response, error) -> {
            onComplete.run(response, error);
            if (error == null) {
                markReadOnServer(account, peer);
            }
        };
    }

    private static void markReadOnServer(int account, TLRPC.InputPeer peer) {
        final long dialogId = DialogObject.getPeerDialogId(peer);
        if (dialogId == 0) {
            return;
        }
        MessagesStorage.getInstance(account).getDialogMaxMessageId(dialogId, maxId -> {
            if (maxId <= 0) {
                return;
            }
            final TLObject req;
            if (peer instanceof TLRPC.TL_inputPeerChannel) {
                TLRPC.TL_channels_readHistory request = new TLRPC.TL_channels_readHistory();
                request.channel = MessagesController.getInputChannel(peer);
                request.max_id = maxId;
                req = request;
            } else {
                TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
                request.peer = peer;
                request.max_id = maxId;
                req = request;
            }
            allowNextReadPacket();
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
                if (err == null && res instanceof TLRPC.TL_messages_affectedMessages) {
                    TLRPC.TL_messages_affectedMessages affected = (TLRPC.TL_messages_affectedMessages) res;
                    MessagesController.getInstance(account).processNewDifferenceParams(-1, affected.pts, -1, affected.pts_count);
                }
            });
        });
    }
}
