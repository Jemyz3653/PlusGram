/*
 * PlusGram for Android.
 * Dual time in message bubbles: first my local time, then the local time
 * of the person I am talking to (time zone set by hand or taken from
 * their Telegram Business opening hours).
 */

package com.plusgram.messenger;

import android.text.TextUtils;

import com.plusgram.messenger.extera.ExteraConfig;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.time.FastDateFormat;
import org.telegram.tgnet.TLRPC;

import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public final class PlusTime {

    private static final ConcurrentHashMap<String, FastDateFormat> formatters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TimeZone> zones = new ConcurrentHashMap<>();

    private PlusTime() {
    }

    /** Time of a message as shown in the bubble, with the peer's time appended when known. */
    public static String formatMessageTime(int account, MessageObject messageObject, int date) {
        final long millis = date * 1000L;
        final String mine = ExteraConfig.formatTimeWithSeconds
            ? LocaleController.getInstance().getFormatterDayWithSeconds().format(millis)
            : LocaleController.getInstance().getFormatterDay().format(millis);
        final String peer = formatPeerTime(account, messageObject, millis);
        if (peer == null) {
            return mine;
        }
        return mine + " / " + peer;
    }

    private static String formatPeerTime(int account, MessageObject messageObject, long millis) {
        if (!PlusConfig.dualTime || messageObject == null || messageObject.messageOwner == null) {
            return null;
        }
        final long peerUserId = getPeerUserId(account, messageObject);
        if (peerUserId <= 0) {
            return null;
        }
        final String zoneId = getZoneId(account, peerUserId);
        if (zoneId == null) {
            return null;
        }
        final TimeZone zone = getZone(zoneId);
        if (zone == null || zone.getOffset(millis) == TimeZone.getDefault().getOffset(millis)) {
            return null;
        }
        String text = getFormatter(zone).format(millis);
        if (PlusConfig.dualTimeShowOffset) {
            text = text + " " + formatOffset(zone.getOffset(millis));
        }
        return text;
    }

    private static long getPeerUserId(int account, MessageObject messageObject) {
        final long dialogId = messageObject.getDialogId();
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        if (dialogId > 0) {
            if (dialogId == selfId) {
                return 0;
            }
            return dialogId;
        }
        if (!PlusConfig.dualTimeInGroups || messageObject.isOutOwner()) {
            return 0;
        }
        final long fromId = messageObject.getFromChatId();
        return fromId > 0 && fromId != selfId ? fromId : 0;
    }

    /** Manually chosen zone first, then the one from Telegram Business opening hours. */
    public static String getZoneId(int account, long userId) {
        String zoneId = PlusConfig.getPeerZone(userId);
        if (TextUtils.isEmpty(zoneId) && PlusConfig.dualTimeFromBusiness) {
            TLRPC.UserFull userFull = MessagesController.getInstance(account).getUserFull(userId);
            if (userFull != null && userFull.business_work_hours != null) {
                zoneId = userFull.business_work_hours.timezone_id;
            }
        }
        return TextUtils.isEmpty(zoneId) ? null : zoneId;
    }

    public static boolean isZoneFromBusiness(int account, long userId) {
        return TextUtils.isEmpty(PlusConfig.getPeerZone(userId)) && getZoneId(account, userId) != null;
    }

    private static TimeZone getZone(String zoneId) {
        TimeZone zone = zones.get(zoneId);
        if (zone == null) {
            zone = TimeZone.getTimeZone(zoneId);
            zones.put(zoneId, zone);
        }
        return zone;
    }

    private static FastDateFormat getFormatter(TimeZone zone) {
        final String pattern = LocaleController.is24HourFormat
            ? (ExteraConfig.formatTimeWithSeconds ? "HH:mm:ss" : "HH:mm")
            : (ExteraConfig.formatTimeWithSeconds ? "h:mm:ss a" : "h:mm a");
        final String key = pattern + "|" + zone.getID();
        FastDateFormat formatter = formatters.get(key);
        if (formatter == null) {
            formatter = FastDateFormat.getInstance(pattern, zone, Locale.US);
            formatters.put(key, formatter);
        }
        return formatter;
    }

    public static String formatOffset(int offsetMillis) {
        final int totalMinutes = Math.abs(offsetMillis) / 60000;
        final int hours = totalMinutes / 60;
        final int minutes = totalMinutes % 60;
        final String sign = offsetMillis < 0 ? "-" : "+";
        if (minutes == 0) {
            return "UTC" + sign + hours;
        }
        return String.format(Locale.US, "UTC%s%d:%02d", sign, hours, minutes);
    }

    /** "15:42 (UTC+3)" — current time in the given zone, for the menus. */
    public static String describeZoneNow(String zoneId) {
        if (TextUtils.isEmpty(zoneId)) {
            return "";
        }
        final TimeZone zone = getZone(zoneId);
        final long now = System.currentTimeMillis();
        return getFormatter(zone).format(now) + " (" + formatOffset(zone.getOffset(now)) + ")";
    }
}
