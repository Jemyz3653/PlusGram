/*
 * PlusGram for Android.
 * List of people with a manually set time zone.
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import com.plusgram.messenger.PlusConfig;
import com.plusgram.messenger.PlusTime;

import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PeerTimezonesActivity extends UniversalFragment {

    private final ArrayList<Long> userIds = new ArrayList<>();

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.PlusPeerZones);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        userIds.clear();
        HashMap<Long, String> zones = PlusConfig.getPeerZones();
        for (Map.Entry<Long, String> entry : zones.entrySet()) {
            userIds.add(entry.getKey());
        }
        Collections.sort(userIds);
        if (userIds.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.PlusPeerZonesEmpty)));
            return;
        }
        for (int i = 0; i < userIds.size(); i++) {
            long userId = userIds.get(i);
            TLRPC.User user = getMessagesController().getUser(userId);
            String name = user != null ? UserObject.getUserName(user) : "ID " + userId;
            String zoneId = zones.get(userId);
            String value = PlusTimezonePicker.getZoneName(currentAccount, zoneId) + " · " + PlusTime.describeZoneNow(zoneId);
            items.add(UItem.asButton(i, name, value));
        }
        items.add(UItem.asShadow(getString(R.string.PlusDualTimeInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id < 0 || item.id >= userIds.size()) {
            return;
        }
        PlusTimezonePicker.show(this, userIds.get(item.id), () -> {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
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
