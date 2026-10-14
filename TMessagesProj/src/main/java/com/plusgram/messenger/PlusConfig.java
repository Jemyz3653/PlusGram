/*
 * PlusGram for Android.
 * Telegram client based on the official Telegram for Android sources,
 * with features ported from exteraGram and AyuGram (Radolyn Labs).
 */

package com.plusgram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.plusgram.messenger.ayu.AyuConfig;
import com.plusgram.messenger.extera.ExteraConfig;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public final class PlusConfig {

    private static final String PREFS = "plusgram_config";
    private static final String PREFS_TIMEZONES = "plusgram_timezones";
    private static final String TZ_PREFIX = "tz_";

    private static final Object sync = new Object();
    private static volatile boolean loaded;

    private static SharedPreferences preferences;
    private static SharedPreferences timezonePreferences;

    // Dual time: my time zone first, then the time zone of the person I talk to
    public static boolean dualTime;
    public static boolean dualTimeFromBusiness;
    public static boolean dualTimeInGroups;
    public static boolean dualTimeShowOffset;

    // Performance
    public static boolean lightMode;

    private static final ConcurrentHashMap<Long, String> peerZones = new ConcurrentHashMap<>();

    private PlusConfig() {
    }

    public static String getVersion() {
        return BuildConfig.PLUS_VERSION;
    }

    public static void load() {
        if (loaded) {
            return;
        }
        synchronized (sync) {
            if (loaded) {
                return;
            }
            Context context = ApplicationLoader.applicationContext;
            preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            timezonePreferences = context.getSharedPreferences(PREFS_TIMEZONES, Context.MODE_PRIVATE);

            dualTime = preferences.getBoolean("dualTime", true);
            dualTimeFromBusiness = preferences.getBoolean("dualTimeFromBusiness", true);
            dualTimeInGroups = preferences.getBoolean("dualTimeInGroups", false);
            dualTimeShowOffset = preferences.getBoolean("dualTimeShowOffset", false);
            lightMode = preferences.getBoolean("lightMode", false);

            peerZones.clear();
            for (Map.Entry<String, ?> entry : timezonePreferences.getAll().entrySet()) {
                if (!entry.getKey().startsWith(TZ_PREFIX) || !(entry.getValue() instanceof String)) {
                    continue;
                }
                try {
                    long userId = Long.parseLong(entry.getKey().substring(TZ_PREFIX.length()));
                    peerZones.put(userId, (String) entry.getValue());
                } catch (NumberFormatException ignore) {
                }
            }
            loaded = true;
        }
        ExteraConfig.load();
        AyuConfig.load();
    }

    /**
     * Called once on application start. Without Telegram's FCM project the app
     * cannot receive pushes, so the keep-alive service and the background
     * connection are switched on unless the user already chose otherwise.
     */
    public static void applyFirstLaunchDefaults() {
        load();
        if (preferences.getBoolean("firstLaunchDefaultsApplied", false)) {
            return;
        }
        // same file as MessagesController.getGlobalNotificationsSettings(), read directly
        // because this runs before Telegram controllers are created
        SharedPreferences notifications = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = notifications.edit();
        if (!notifications.contains("pushService")) {
            editor.putBoolean("pushService", true);
        }
        if (!notifications.contains("pushConnection")) {
            editor.putBoolean("pushConnection", true);
        }
        editor.apply();
        preferences.edit().putBoolean("firstLaunchDefaultsApplied", true).apply();
    }

    public static void putBoolean(String key, boolean value) {
        load();
        preferences.edit().putBoolean(key, value).apply();
    }

    public static void setDualTime(boolean value) {
        dualTime = value;
        putBoolean("dualTime", value);
    }

    public static void setDualTimeFromBusiness(boolean value) {
        dualTimeFromBusiness = value;
        putBoolean("dualTimeFromBusiness", value);
    }

    public static void setDualTimeInGroups(boolean value) {
        dualTimeInGroups = value;
        putBoolean("dualTimeInGroups", value);
    }

    public static void setDualTimeShowOffset(boolean value) {
        dualTimeShowOffset = value;
        putBoolean("dualTimeShowOffset", value);
    }

    public static void setLightMode(boolean value) {
        lightMode = value;
        putBoolean("lightMode", value);
    }

    /** Manually chosen IANA time zone of a user, or null. */
    public static String getPeerZone(long userId) {
        load();
        return peerZones.get(userId);
    }

    public static void setPeerZone(long userId, String zoneId) {
        load();
        if (TextUtils.isEmpty(zoneId)) {
            peerZones.remove(userId);
            timezonePreferences.edit().remove(TZ_PREFIX + userId).apply();
        } else {
            peerZones.put(userId, zoneId);
            timezonePreferences.edit().putString(TZ_PREFIX + userId, zoneId).apply();
        }
    }

    public static HashMap<Long, String> getPeerZones() {
        load();
        return new HashMap<>(peerZones);
    }

    public static boolean isValidZone(String zoneId) {
        if (TextUtils.isEmpty(zoneId)) {
            return false;
        }
        TimeZone zone = TimeZone.getTimeZone(zoneId);
        return zone != null && (zone.getID().equals(zoneId) || !"GMT".equals(zone.getID()));
    }
}
