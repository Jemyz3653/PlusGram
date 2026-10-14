/*
 * PlusGram for Android.
 * Menu for choosing the time zone of a person (for dual time in messages).
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;

import com.plusgram.messenger.PlusConfig;
import com.plusgram.messenger.PlusTime;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Business.TimezoneSelector;
import org.telegram.ui.Business.TimezonesController;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public final class PlusTimezonePicker {

    private PlusTimezonePicker() {
    }

    public static String getZoneName(int account, String zoneId) {
        if (TextUtils.isEmpty(zoneId)) {
            return "";
        }
        try {
            return TimezonesController.getInstance(account).getTimezoneName(zoneId, false);
        } catch (Exception e) {
            FileLog.e(e);
            return zoneId;
        }
    }

    public static void show(BaseFragment fragment, long userId, Runnable onChanged) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        final TLRPC.User user = fragment.getMessagesController().getUser(userId);
        final String manual = PlusConfig.getPeerZone(userId);
        final String effective = PlusTime.getZoneId(account, userId);

        final StringBuilder message = new StringBuilder();
        if (user != null) {
            message.append(UserObject.getUserName(user)).append("\n");
        }
        if (effective == null) {
            message.append(getString(R.string.PlusPeerTimezoneNone));
        } else {
            message.append(getZoneName(account, effective));
            if (TextUtils.isEmpty(manual)) {
                message.append(" · ").append(getString(R.string.PlusPeerTimezoneFromBusiness));
            }
            message.append("\n").append(formatString(R.string.PlusPeerTimezoneNow, PlusTime.describeZoneNow(effective)));
        }

        final ArrayList<CharSequence> items = new ArrayList<>();
        final ArrayList<Integer> actions = new ArrayList<>();
        items.add(getString(R.string.PlusPeerTimezoneSet));
        actions.add(0);
        if (!TextUtils.isEmpty(manual)) {
            items.add(getString(R.string.PlusPeerTimezoneRemove));
            actions.add(1);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(getString(R.string.PlusPeerTimezone));
        builder.setMessage(message.toString());
        builder.setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
            if (which < 0 || which >= actions.size()) {
                return;
            }
            if (actions.get(which) == 0) {
                openSelector(fragment, userId, effective, onChanged);
            } else {
                PlusConfig.setPeerZone(userId, null);
                notifyChanged(fragment);
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, getString(R.string.PlusTimezoneRemoved)).show();
                if (onChanged != null) {
                    onChanged.run();
                }
            }
        });
        fragment.showDialog(builder.create());
    }

    private static void openSelector(BaseFragment fragment, long userId, String current, Runnable onChanged) {
        final TimezoneSelector[] selector = new TimezoneSelector[1];
        selector[0] = new TimezoneSelector()
            .setValue(current != null ? current : TimezonesController.getInstance(fragment.getCurrentAccount()).getSystemTimezoneId())
            .whenSelected(zoneId -> {
                PlusConfig.setPeerZone(userId, zoneId);
                notifyChanged(fragment);
                if (onChanged != null) {
                    onChanged.run();
                }
                if (selector[0] != null) {
                    selector[0].finishFragment();
                }
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.contact_check, getString(R.string.PlusTimezoneSaved)).show();
            });
        fragment.presentFragment(selector[0]);
    }

    private static void notifyChanged(BaseFragment fragment) {
        fragment.getNotificationCenter().postNotificationName(NotificationCenter.plusMessagesUpdated);
    }
}
