/*
 * PlusGram for Android.
 * "PlusGram" settings section: dual time, mint color, performance.
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.plusgram.messenger.PlusConfig;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class PlusSettingsActivity extends UniversalFragment {

    private static final int ID_DUAL_TIME = 1;
    private static final int ID_FROM_BUSINESS = 2;
    private static final int ID_IN_GROUPS = 3;
    private static final int ID_SHOW_OFFSET = 4;
    private static final int ID_PEER_ZONES = 5;
    private static final int ID_APPLY_MINT = 6;
    private static final int ID_LIGHT_MODE = 7;

    private static final String LITE_BACKUP_KEY = "liteModeBackup";

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.PlusSettingsPlus);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.PlusHeaderDualTime)));
        items.add(UItem.asCheck(ID_DUAL_TIME, getString(R.string.PlusDualTime)).setChecked(PlusConfig.dualTime));
        if (PlusConfig.dualTime) {
            items.add(UItem.asCheck(ID_FROM_BUSINESS, getString(R.string.PlusDualTimeFromBusiness)).setChecked(PlusConfig.dualTimeFromBusiness));
            items.add(UItem.asCheck(ID_IN_GROUPS, getString(R.string.PlusDualTimeInGroups)).setChecked(PlusConfig.dualTimeInGroups));
            items.add(UItem.asCheck(ID_SHOW_OFFSET, getString(R.string.PlusDualTimeShowOffset)).setChecked(PlusConfig.dualTimeShowOffset));
            int count = PlusConfig.getPeerZones().size();
            items.add(UItem.asButton(ID_PEER_ZONES, getString(R.string.PlusPeerZones), count > 0 ? String.valueOf(count) : ""));
        }
        items.add(UItem.asShadow(getString(R.string.PlusDualTimeInfo)));

        items.add(UItem.asHeader(getString(R.string.PlusHeaderAppearance)));
        items.add(UItem.asButton(ID_APPLY_MINT, getString(R.string.PlusApplyMint)).accent());
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.PlusHeaderPerformance)));
        items.add(UItem.asCheck(ID_LIGHT_MODE, getString(R.string.PlusLightMode)).setChecked(PlusConfig.lightMode));
        items.add(UItem.asShadow(getString(R.string.PlusLightModeInfo)));

        items.add(UItem.asShadow(formatString(R.string.PlusVersion, PlusConfig.getVersion(), BuildVars.BUILD_VERSION_STRING)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_DUAL_TIME:
                PlusConfig.setDualTime(!PlusConfig.dualTime);
                setChecked(view, PlusConfig.dualTime);
                break;
            case ID_FROM_BUSINESS:
                PlusConfig.setDualTimeFromBusiness(!PlusConfig.dualTimeFromBusiness);
                setChecked(view, PlusConfig.dualTimeFromBusiness);
                break;
            case ID_IN_GROUPS:
                PlusConfig.setDualTimeInGroups(!PlusConfig.dualTimeInGroups);
                setChecked(view, PlusConfig.dualTimeInGroups);
                break;
            case ID_SHOW_OFFSET:
                PlusConfig.setDualTimeShowOffset(!PlusConfig.dualTimeShowOffset);
                setChecked(view, PlusConfig.dualTimeShowOffset);
                break;
            case ID_PEER_ZONES:
                presentFragment(new PeerTimezonesActivity());
                return;
            case ID_APPLY_MINT:
                applyMint();
                return;
            case ID_LIGHT_MODE:
                setLightMode(!PlusConfig.lightMode);
                setChecked(view, PlusConfig.lightMode);
                break;
            default:
                return;
        }
        getNotificationCenter().postNotificationName(NotificationCenter.plusMessagesUpdated);
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void setChecked(View view, boolean value) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    private void applyMint() {
        Theme.ThemeInfo theme = Theme.getActiveTheme();
        Theme.ThemeAccent accent = theme != null && theme.themeAccentsMap != null ? theme.themeAccentsMap.get(Theme.PLUS_MINT_ACCENT_ID) : null;
        if (accent == null) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, getString(R.string.PlusMintUnavailable)).show();
            return;
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, theme, Theme.isCurrentThemeNight(), null, Theme.PLUS_MINT_ACCENT_ID);
        BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, getString(R.string.PlusMintApplied)).show();
    }

    private void setLightMode(boolean enabled) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("plusgram_config", Context.MODE_PRIVATE);
        if (enabled) {
            prefs.edit().putInt(LITE_BACKUP_KEY, LiteMode.getValue(true)).apply();
            int lite = LiteMode.PRESET_LOW & ~(LiteMode.FLAG_PARTICLES | LiteMode.FLAG_CHAT_THANOS | LiteMode.FLAG_CHAT_BLUR | LiteMode.FLAG_LIQUID_GLASS);
            LiteMode.setAllFlags(lite);
        } else {
            int backup = prefs.getInt(LITE_BACKUP_KEY, LiteMode.PRESET_HIGH);
            LiteMode.setAllFlags(backup);
            // LiteMode.setAllFlags does not notify; redraw what got turned back on
            org.telegram.ui.Components.AnimatedEmojiDrawable.updateAll();
            org.telegram.messenger.SvgHelper.SvgDrawable.updateLiteValues();
            Theme.reloadWallpaper(true);
        }
        PlusConfig.setLightMode(enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
