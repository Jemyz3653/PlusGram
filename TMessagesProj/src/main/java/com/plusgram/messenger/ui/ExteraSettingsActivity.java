/*
 * PlusGram for Android.
 * "exteraGram" settings section.
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import com.plusgram.messenger.extera.ExteraConfig;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class ExteraSettingsActivity extends UniversalFragment {

    private static final int ID_HIDE_KEYBOARD = 1;
    private static final int ID_NO_NEXT_CHANNEL = 2;
    private static final int ID_TIME_SECONDS = 3;
    private static final int ID_HIDE_STICKER_TIME = 4;
    private static final int ID_HIDE_PHONE = 5;
    private static final int ID_SHOW_ID = 6;
    private static final int ID_NO_ROUNDING = 7;
    private static final int ID_DOWNLOAD_BOOST = 8;
    private static final int ID_UPLOAD_BOOST = 9;

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.PlusSettingsExtera);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.ExteraHeaderChats)));
        items.add(UItem.asCheck(ID_HIDE_KEYBOARD, getString(R.string.ExteraHideKeyboardOnScroll)).setChecked(ExteraConfig.hideKeyboardOnScroll));
        items.add(UItem.asCheck(ID_NO_NEXT_CHANNEL, getString(R.string.ExteraDisableJumpToNextChannel)).setChecked(ExteraConfig.disableJumpToNextChannel));
        items.add(UItem.asCheck(ID_TIME_SECONDS, getString(R.string.ExteraTimeWithSeconds)).setChecked(ExteraConfig.formatTimeWithSeconds));
        items.add(UItem.asCheck(ID_HIDE_STICKER_TIME, getString(R.string.ExteraHideStickerTime)).setChecked(ExteraConfig.hideStickerTime));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.ExteraHeaderGeneral)));
        items.add(UItem.asCheck(ID_HIDE_PHONE, getString(R.string.ExteraHidePhoneNumber)).setChecked(ExteraConfig.hidePhoneNumber));
        items.add(UItem.asCheck(ID_SHOW_ID, getString(R.string.ExteraShowId)).setChecked(ExteraConfig.showIdInProfile));
        items.add(UItem.asCheck(ID_NO_ROUNDING, getString(R.string.ExteraDisableNumberRounding)).setChecked(ExteraConfig.disableNumberRounding));
        items.add(UItem.asShadow(getString(R.string.ExteraDisableNumberRoundingInfo)));

        items.add(UItem.asHeader(getString(R.string.ExteraHeaderSpeed)));
        items.add(UItem.asCheck(ID_DOWNLOAD_BOOST, getString(R.string.ExteraDownloadBoost)).setChecked(ExteraConfig.downloadSpeedBoost));
        items.add(UItem.asCheck(ID_UPLOAD_BOOST, getString(R.string.ExteraUploadBoost)).setChecked(ExteraConfig.uploadSpeedBoost));
        items.add(UItem.asShadow(getString(R.string.ExteraSpeedInfo)));

        items.add(UItem.asShadow(getString(R.string.ExteraCredits)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        final boolean value;
        switch (item.id) {
            case ID_HIDE_KEYBOARD:
                value = !ExteraConfig.hideKeyboardOnScroll;
                ExteraConfig.setHideKeyboardOnScroll(value);
                break;
            case ID_NO_NEXT_CHANNEL:
                value = !ExteraConfig.disableJumpToNextChannel;
                ExteraConfig.setDisableJumpToNextChannel(value);
                break;
            case ID_TIME_SECONDS:
                value = !ExteraConfig.formatTimeWithSeconds;
                ExteraConfig.setFormatTimeWithSeconds(value);
                break;
            case ID_HIDE_STICKER_TIME:
                value = !ExteraConfig.hideStickerTime;
                ExteraConfig.setHideStickerTime(value);
                break;
            case ID_HIDE_PHONE:
                value = !ExteraConfig.hidePhoneNumber;
                ExteraConfig.setHidePhoneNumber(value);
                break;
            case ID_SHOW_ID:
                value = !ExteraConfig.showIdInProfile;
                ExteraConfig.setShowIdInProfile(value);
                break;
            case ID_NO_ROUNDING:
                value = !ExteraConfig.disableNumberRounding;
                ExteraConfig.setDisableNumberRounding(value);
                break;
            case ID_DOWNLOAD_BOOST:
                value = !ExteraConfig.downloadSpeedBoost;
                ExteraConfig.setDownloadSpeedBoost(value);
                break;
            case ID_UPLOAD_BOOST:
                value = !ExteraConfig.uploadSpeedBoost;
                ExteraConfig.setUploadSpeedBoost(value);
                break;
            default:
                return;
        }
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
        if (item.id == ID_TIME_SECONDS || item.id == ID_HIDE_STICKER_TIME || item.id == ID_NO_ROUNDING) {
            getNotificationCenter().postNotificationName(NotificationCenter.plusMessagesUpdated);
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
