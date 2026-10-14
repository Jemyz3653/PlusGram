/*
 * PlusGram for Android.
 * Ghost mode and message saving ported from AyuGram for Android
 * (Radolyn Labs, https://github.com/AyuGram/AyuGram4A).
 */

package com.plusgram.messenger.ayu;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

public final class AyuConfig {

    private static final String PREFS = "plusgram_ayu";
    public static final String DEFAULT_DELETED_MARK = "🗑";

    private static final Object sync = new Object();
    private static volatile boolean loaded;
    private static SharedPreferences preferences;

    // Ghost mode: "true" means the packet IS sent (same semantics as AyuGram)
    public static boolean sendReadPackets;
    public static boolean sendOnlinePackets;
    public static boolean sendUploadProgress;
    public static boolean sendReadStories;
    public static boolean markReadAfterSend;

    // Message saving
    public static boolean saveDeletedMessages;
    public static boolean saveMessagesHistory;
    public static String deletedMark;

    // Other
    public static boolean hideSponsoredMessages;

    private AyuConfig() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        synchronized (sync) {
            if (loaded) {
                return;
            }
            preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            sendReadPackets = preferences.getBoolean("sendReadPackets", true);
            sendOnlinePackets = preferences.getBoolean("sendOnlinePackets", true);
            sendUploadProgress = preferences.getBoolean("sendUploadProgress", true);
            sendReadStories = preferences.getBoolean("sendReadStories", true);
            markReadAfterSend = preferences.getBoolean("markReadAfterSend", true);

            saveDeletedMessages = preferences.getBoolean("saveDeletedMessages", true);
            saveMessagesHistory = preferences.getBoolean("saveMessagesHistory", true);
            deletedMark = preferences.getString("deletedMark", DEFAULT_DELETED_MARK);

            hideSponsoredMessages = preferences.getBoolean("hideSponsoredMessages", false);

            loaded = true;
        }
    }

    private static void put(String key, boolean value) {
        load();
        preferences.edit().putBoolean(key, value).apply();
    }

    public static boolean isGhostModeActive() {
        return !sendReadPackets && !sendOnlinePackets && !sendUploadProgress && !sendReadStories;
    }

    public static void setGhostMode(boolean enabled) {
        sendReadPackets = !enabled;
        sendOnlinePackets = !enabled;
        sendUploadProgress = !enabled;
        sendReadStories = !enabled;
        load();
        preferences.edit()
            .putBoolean("sendReadPackets", sendReadPackets)
            .putBoolean("sendOnlinePackets", sendOnlinePackets)
            .putBoolean("sendUploadProgress", sendUploadProgress)
            .putBoolean("sendReadStories", sendReadStories)
            .apply();
    }

    public static void setSendReadPackets(boolean value) {
        sendReadPackets = value;
        put("sendReadPackets", value);
    }

    public static void setSendOnlinePackets(boolean value) {
        sendOnlinePackets = value;
        put("sendOnlinePackets", value);
    }

    public static void setSendUploadProgress(boolean value) {
        sendUploadProgress = value;
        put("sendUploadProgress", value);
    }

    public static void setSendReadStories(boolean value) {
        sendReadStories = value;
        put("sendReadStories", value);
    }

    public static void setMarkReadAfterSend(boolean value) {
        markReadAfterSend = value;
        put("markReadAfterSend", value);
    }

    public static void setSaveDeletedMessages(boolean value) {
        saveDeletedMessages = value;
        put("saveDeletedMessages", value);
    }

    public static void setSaveMessagesHistory(boolean value) {
        saveMessagesHistory = value;
        put("saveMessagesHistory", value);
    }

    public static void setHideSponsoredMessages(boolean value) {
        hideSponsoredMessages = value;
        put("hideSponsoredMessages", value);
    }

    public static void setDeletedMark(String value) {
        load();
        deletedMark = TextUtils.isEmpty(value) ? DEFAULT_DELETED_MARK : value;
        preferences.edit().putString("deletedMark", deletedMark).apply();
    }
}
