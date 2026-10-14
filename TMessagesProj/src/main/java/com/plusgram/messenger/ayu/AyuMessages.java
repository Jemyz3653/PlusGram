/*
 * PlusGram for Android.
 * Saving deleted messages and edit history, idea and behaviour from
 * AyuGram for Android (Radolyn Labs, https://github.com/AyuGram/AyuGram4A).
 *
 * Deleted messages are not removed from Telegram's local cache, so they stay
 * in the chat with a mark. In addition, their text is copied into our own
 * database, so it survives cache cleanup and can be viewed in settings.
 */

package com.plusgram.messenger.ayu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.collection.LongSparseArray;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AyuMessages {

    public static final class DeletedMessage {
        public int account;
        public long dialogId;
        public int messageId;
        public int date;
        public int deletedDate;
        public long fromId;
        public String text;
    }

    public static final class EditedVersion {
        public int editDate;
        public String text;
    }

    private static final String DB_NAME = "plusgram_messages.db";
    private static final int DB_VERSION = 1;
    private static final int MAX_EDITS_PER_MESSAGE = 50;

    private static volatile AyuMessages instance;

    public static AyuMessages getInstance() {
        AyuMessages local = instance;
        if (local == null) {
            synchronized (AyuMessages.class) {
                local = instance;
                if (local == null) {
                    instance = local = new AyuMessages(ApplicationLoader.applicationContext);
                }
            }
        }
        return local;
    }

    private final DispatchQueue queue = new DispatchQueue("plusgramMessagesQueue");
    private final Helper helper;
    private final Set<String> deletedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> editedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AyuMessages(Context context) {
        helper = new Helper(context);
        queue.postRunnable(this::loadKeys);
    }

    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE deleted(account INTEGER, dialog_id INTEGER, msg_id INTEGER, is_channel INTEGER, date INTEGER, deleted_date INTEGER, from_id INTEGER, text TEXT, PRIMARY KEY(account, dialog_id, msg_id))");
            db.execSQL("CREATE INDEX deleted_by_date ON deleted(deleted_date)");
            db.execSQL("CREATE TABLE edits(id INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER, dialog_id INTEGER, msg_id INTEGER, edit_date INTEGER, text TEXT)");
            db.execSQL("CREATE INDEX edits_by_message ON edits(account, dialog_id, msg_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    private static String key(int account, long dialogKey, int messageId) {
        return account + ":" + dialogKey + ":" + messageId;
    }

    private static boolean isChannelMessage(TLRPC.Message message) {
        return message != null && message.peer_id != null && message.peer_id.channel_id != 0;
    }

    private void loadKeys() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT account, dialog_id, msg_id, is_channel FROM deleted", null);
            while (cursor.moveToNext()) {
                long dialogKey = cursor.getInt(3) != 0 ? cursor.getLong(1) : 0;
                deletedKeys.add(key(cursor.getInt(0), dialogKey, cursor.getInt(2)));
            }
            cursor.close();
            cursor = db.rawQuery("SELECT DISTINCT account, dialog_id, msg_id FROM edits", null);
            while (cursor.moveToNext()) {
                editedKeys.add(key(cursor.getInt(0), cursor.getLong(1), cursor.getInt(2)));
            }
            cursor.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        AndroidUtilities.runOnUIThread(this::notifyChanged);
    }

    private void notifyChanged() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.plusMessagesUpdated);
            }
        }
    }

    public boolean isDeleted(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || deletedKeys.isEmpty()) {
            return false;
        }
        long dialogKey = isChannelMessage(messageObject.messageOwner) ? messageObject.getDialogId() : 0;
        return deletedKeys.contains(key(account, dialogKey, messageObject.getId()));
    }

    public boolean hasEditHistory(int account, MessageObject messageObject) {
        if (messageObject == null || editedKeys.isEmpty()) {
            return false;
        }
        return editedKeys.contains(key(account, messageObject.getDialogId(), messageObject.getId()));
    }

    /**
     * Takes the deletions from an update batch. Returns what still has to be deleted
     * by Telegram (null when everything is kept).
     */
    public LongSparseArray<ArrayList<Integer>> onMessagesDeleted(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null || !AyuConfig.saveDeletedMessages) {
            return deleted;
        }
        for (int a = 0, size = deleted.size(); a < size; a++) {
            final long dialogKey = deleted.keyAt(a);
            final ArrayList<Integer> ids = new ArrayList<>(deleted.valueAt(a));
            for (int i = 0; i < ids.size(); i++) {
                deletedKeys.add(key(account, dialogKey, ids.get(i)));
            }
            captureDeleted(account, dialogKey, ids);
        }
        AndroidUtilities.runOnUIThread(this::notifyChanged);
        return null;
    }

    private void captureDeleted(int account, long dialogKey, ArrayList<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        final MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            final ArrayList<DeletedMessage> found = new ArrayList<>();
            SQLiteCursor cursor = null;
            try {
                String idsString = TextUtils.join(",", ids);
                if (dialogKey == 0) {
                    cursor = storage.getDatabase().queryFinalized("SELECT uid, data FROM messages_v2 WHERE mid IN (" + idsString + ") AND is_channel = 0");
                } else {
                    cursor = storage.getDatabase().queryFinalized("SELECT uid, data FROM messages_v2 WHERE uid = " + dialogKey + " AND mid IN (" + idsString + ")");
                }
                int now = (int) (System.currentTimeMillis() / 1000);
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(1);
                    if (data == null) {
                        continue;
                    }
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    data.reuse();
                    if (message == null) {
                        continue;
                    }
                    DeletedMessage item = new DeletedMessage();
                    item.account = account;
                    item.dialogId = cursor.longValue(0);
                    item.messageId = message.id;
                    item.date = message.date;
                    item.deletedDate = now;
                    item.fromId = message.from_id != null ? DialogObject.getPeerDialogId(message.from_id) : item.dialogId;
                    item.text = describe(message);
                    found.add(item);
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            if (!found.isEmpty()) {
                final boolean channel = dialogKey != 0;
                queue.postRunnable(() -> saveDeleted(found, channel));
            }
        });
    }

    private static String describe(TLRPC.Message message) {
        String text = message.message;
        boolean hasMedia = message.media != null && !(message.media instanceof TLRPC.TL_messageMediaEmpty);
        if (hasMedia) {
            String mediaMark = "[" + (message.media instanceof TLRPC.TL_messageMediaPhoto ? "photo" : message.media instanceof TLRPC.TL_messageMediaDocument ? "file" : "media") + "]";
            return TextUtils.isEmpty(text) ? mediaMark : mediaMark + " " + text;
        }
        return text == null ? "" : text;
    }

    private void saveDeleted(ArrayList<DeletedMessage> items, boolean channel) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < items.size(); i++) {
                    DeletedMessage item = items.get(i);
                    ContentValues values = new ContentValues();
                    values.put("account", item.account);
                    values.put("dialog_id", item.dialogId);
                    values.put("msg_id", item.messageId);
                    values.put("is_channel", channel ? 1 : 0);
                    values.put("date", item.date);
                    values.put("deleted_date", item.deletedDate);
                    values.put("from_id", item.fromId);
                    values.put("text", item.text);
                    db.insertWithOnConflict("deleted", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /**
     * Called from the update loop before Telegram writes the edited message to its cache,
     * so the cached row still holds the previous version.
     */
    public void onMessageEdited(int account, TLRPC.Message newMessage) {
        if (!AyuConfig.saveMessagesHistory || newMessage == null || newMessage.edit_hide) {
            return;
        }
        final long dialogId = newMessage.dialog_id != 0 ? newMessage.dialog_id : MessageObject.getDialogId(newMessage);
        final int messageId = newMessage.id;
        final String newText = newMessage.message == null ? "" : newMessage.message;
        final MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            TLRPC.Message oldMessage = null;
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized("SELECT data FROM messages_v2 WHERE uid = " + dialogId + " AND mid = " + messageId + " LIMIT 1");
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        oldMessage = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        data.reuse();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            if (oldMessage == null) {
                return;
            }
            final String oldText = describe(oldMessage);
            if (TextUtils.equals(oldText, newText) || TextUtils.equals(oldMessage.message, newText)) {
                return;
            }
            final int oldDate = oldMessage.edit_date != 0 ? oldMessage.edit_date : oldMessage.date;
            queue.postRunnable(() -> {
                saveEdit(account, dialogId, messageId, oldDate, oldText);
                editedKeys.add(key(account, dialogId, messageId));
                AndroidUtilities.runOnUIThread(this::notifyChanged);
            });
        });
    }

    private void saveEdit(int account, long dialogId, int messageId, int editDate, String text) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("account", account);
            values.put("dialog_id", dialogId);
            values.put("msg_id", messageId);
            values.put("edit_date", editDate);
            values.put("text", text);
            db.insert("edits", null, values);
            db.execSQL("DELETE FROM edits WHERE account = ? AND dialog_id = ? AND msg_id = ? AND id NOT IN (SELECT id FROM edits WHERE account = ? AND dialog_id = ? AND msg_id = ? ORDER BY id DESC LIMIT " + MAX_EDITS_PER_MESSAGE + ")",
                new Object[]{account, dialogId, messageId, account, dialogId, messageId});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public interface Callback<T> {
        void run(T result);
    }

    public void loadEditHistory(int account, long dialogId, int messageId, Callback<ArrayList<EditedVersion>> callback) {
        queue.postRunnable(() -> {
            ArrayList<EditedVersion> result = new ArrayList<>();
            try {
                SQLiteDatabase db = helper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT edit_date, text FROM edits WHERE account = ? AND dialog_id = ? AND msg_id = ? ORDER BY id ASC",
                    new String[]{String.valueOf(account), String.valueOf(dialogId), String.valueOf(messageId)});
                while (cursor.moveToNext()) {
                    EditedVersion version = new EditedVersion();
                    version.editDate = cursor.getInt(0);
                    version.text = cursor.getString(1);
                    result.add(version);
                }
                cursor.close();
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    public void loadDeleted(int account, int limit, Callback<ArrayList<DeletedMessage>> callback) {
        queue.postRunnable(() -> {
            ArrayList<DeletedMessage> result = new ArrayList<>();
            try {
                SQLiteDatabase db = helper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT dialog_id, msg_id, date, deleted_date, from_id, text FROM deleted WHERE account = ? ORDER BY deleted_date DESC, msg_id DESC LIMIT " + limit,
                    new String[]{String.valueOf(account)});
                while (cursor.moveToNext()) {
                    DeletedMessage item = new DeletedMessage();
                    item.account = account;
                    item.dialogId = cursor.getLong(0);
                    item.messageId = cursor.getInt(1);
                    item.date = cursor.getInt(2);
                    item.deletedDate = cursor.getInt(3);
                    item.fromId = cursor.getLong(4);
                    item.text = cursor.getString(5);
                    result.add(item);
                }
                cursor.close();
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(result));
        });
    }

    public void clearAll(Runnable onDone) {
        queue.postRunnable(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("DELETE FROM deleted");
                db.execSQL("DELETE FROM edits");
            } catch (Exception e) {
                FileLog.e(e);
            }
            deletedKeys.clear();
            editedKeys.clear();
            AndroidUtilities.runOnUIThread(() -> {
                notifyChanged();
                if (onDone != null) {
                    onDone.run();
                }
            });
        });
    }
}
