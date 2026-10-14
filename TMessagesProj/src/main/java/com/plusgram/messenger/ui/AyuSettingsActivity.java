/*
 * PlusGram for Android.
 * "AyuGram" settings section: ghost mode and message saving, ported from
 * AyuGram for Android (Radolyn Labs).
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import com.plusgram.messenger.ayu.AyuConfig;
import com.plusgram.messenger.ayu.AyuMessages;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class AyuSettingsActivity extends UniversalFragment {

    private static final int ID_GHOST = 1;
    private static final int ID_SEND_READ = 2;
    private static final int ID_SEND_ONLINE = 3;
    private static final int ID_SEND_TYPING = 4;
    private static final int ID_SEND_STORIES = 5;
    private static final int ID_READ_AFTER_SEND = 6;
    private static final int ID_SAVE_DELETED = 7;
    private static final int ID_SAVE_HISTORY = 8;
    private static final int ID_DELETED_MARK = 9;
    private static final int ID_DELETED_LIST = 10;
    private static final int ID_CLEAR = 11;
    private static final int ID_HIDE_SPONSORED = 12;

    private static final String[] MARKS = {"🗑", "❌", "🚫", "(del)"};

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.PlusSettingsAyu);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asRippleCheck(ID_GHOST, getString(R.string.AyuGhostMode)).setChecked(AyuConfig.isGhostModeActive()));
        items.add(UItem.asShadow(getString(R.string.AyuGhostModeInfo)));

        items.add(UItem.asHeader(getString(R.string.AyuHeaderGhost)));
        items.add(UItem.asCheck(ID_SEND_READ, getString(R.string.AyuSendReadPackets)).setChecked(AyuConfig.sendReadPackets));
        items.add(UItem.asCheck(ID_SEND_ONLINE, getString(R.string.AyuSendOnlinePackets)).setChecked(AyuConfig.sendOnlinePackets));
        items.add(UItem.asCheck(ID_SEND_TYPING, getString(R.string.AyuSendUploadProgress)).setChecked(AyuConfig.sendUploadProgress));
        items.add(UItem.asCheck(ID_SEND_STORIES, getString(R.string.AyuSendReadStories)).setChecked(AyuConfig.sendReadStories));
        items.add(UItem.asCheck(ID_READ_AFTER_SEND, getString(R.string.AyuMarkReadAfterSend)).setChecked(AyuConfig.markReadAfterSend));
        items.add(UItem.asShadow(getString(R.string.AyuMarkReadAfterSendInfo)));

        items.add(UItem.asHeader(getString(R.string.AyuHeaderSaving)));
        items.add(UItem.asCheck(ID_SAVE_DELETED, getString(R.string.AyuSaveDeleted)).setChecked(AyuConfig.saveDeletedMessages));
        items.add(UItem.asCheck(ID_SAVE_HISTORY, getString(R.string.AyuSaveHistory)).setChecked(AyuConfig.saveMessagesHistory));
        items.add(UItem.asButton(ID_DELETED_MARK, getString(R.string.AyuDeletedMark), AyuConfig.deletedMark));
        items.add(UItem.asButton(ID_DELETED_LIST, getString(R.string.AyuDeletedList)));
        items.add(UItem.asButton(ID_CLEAR, getString(R.string.AyuClearSaved)).red());
        items.add(UItem.asShadow(getString(R.string.AyuSavingInfo)));

        items.add(UItem.asHeader(getString(R.string.AyuHeaderOther)));
        items.add(UItem.asCheck(ID_HIDE_SPONSORED, getString(R.string.AyuHideSponsored)).setChecked(AyuConfig.hideSponsoredMessages));
        items.add(UItem.asShadow(getString(R.string.AyuCredits)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_GHOST: {
                final boolean enable = !AyuConfig.isGhostModeActive();
                AyuConfig.setGhostMode(enable);
                if (view instanceof TextCheckCell) {
                    TextCheckCell cell = (TextCheckCell) view;
                    cell.setChecked(enable);
                    cell.setBackgroundColorAnimated(enable, Theme.getColor(enable ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, getString(enable ? R.string.AyuGhostOn : R.string.AyuGhostOff)).show();
                break;
            }
            case ID_SEND_READ:
                AyuConfig.setSendReadPackets(!AyuConfig.sendReadPackets);
                setChecked(view, AyuConfig.sendReadPackets);
                break;
            case ID_SEND_ONLINE:
                AyuConfig.setSendOnlinePackets(!AyuConfig.sendOnlinePackets);
                setChecked(view, AyuConfig.sendOnlinePackets);
                break;
            case ID_SEND_TYPING:
                AyuConfig.setSendUploadProgress(!AyuConfig.sendUploadProgress);
                setChecked(view, AyuConfig.sendUploadProgress);
                break;
            case ID_SEND_STORIES:
                AyuConfig.setSendReadStories(!AyuConfig.sendReadStories);
                setChecked(view, AyuConfig.sendReadStories);
                break;
            case ID_READ_AFTER_SEND:
                AyuConfig.setMarkReadAfterSend(!AyuConfig.markReadAfterSend);
                setChecked(view, AyuConfig.markReadAfterSend);
                break;
            case ID_SAVE_DELETED:
                AyuConfig.setSaveDeletedMessages(!AyuConfig.saveDeletedMessages);
                setChecked(view, AyuConfig.saveDeletedMessages);
                break;
            case ID_SAVE_HISTORY:
                AyuConfig.setSaveMessagesHistory(!AyuConfig.saveMessagesHistory);
                setChecked(view, AyuConfig.saveMessagesHistory);
                break;
            case ID_HIDE_SPONSORED:
                AyuConfig.setHideSponsoredMessages(!AyuConfig.hideSponsoredMessages);
                setChecked(view, AyuConfig.hideSponsoredMessages);
                break;
            case ID_DELETED_MARK:
                showMarkPicker();
                return;
            case ID_DELETED_LIST:
                presentFragment(new AyuDeletedMessagesActivity());
                return;
            case ID_CLEAR:
                confirmClear();
                return;
            default:
                return;
        }
        updateList();
    }

    private void setChecked(View view, boolean value) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    private void updateList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showMarkPicker() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyuDeletedMark));
        builder.setItems(MARKS, (dialog, which) -> {
            AyuConfig.setDeletedMark(MARKS[which]);
            getNotificationCenter().postNotificationName(NotificationCenter.plusMessagesUpdated);
            updateList();
        });
        showDialog(builder.create());
    }

    private void confirmClear() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getString(R.string.AyuClearSaved));
        builder.setMessage(getString(R.string.AyuClearSavedConfirm));
        builder.setPositiveButton(getString(R.string.Delete), (dialog, which) -> AyuMessages.getInstance().clearAll(() ->
            BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_delete, getString(R.string.AyuCleared)).show()
        ));
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        android.widget.TextView button = (android.widget.TextView) dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
