/*
 * PlusGram for Android.
 * Options ported from exteraGram (exteraSquad), re-implemented on top of
 * Telegram for Android 13.x.
 */

package com.plusgram.messenger.extera;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public final class ExteraConfig {

    private static final String PREFS = "plusgram_extera";

    private static final Object sync = new Object();
    private static volatile boolean loaded;
    private static SharedPreferences preferences;

    // Chats
    public static boolean hideKeyboardOnScroll;
    public static boolean disableJumpToNextChannel;
    public static boolean formatTimeWithSeconds;
    public static boolean hideStickerTime;

    // General
    public static boolean hidePhoneNumber;
    public static boolean showIdInProfile;
    public static boolean disableNumberRounding;

    // Speed
    public static boolean downloadSpeedBoost;
    public static boolean uploadSpeedBoost;

    private ExteraConfig() {
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

            hideKeyboardOnScroll = preferences.getBoolean("hideKeyboardOnScroll", true);
            disableJumpToNextChannel = preferences.getBoolean("disableJumpToNextChannel", false);
            formatTimeWithSeconds = preferences.getBoolean("formatTimeWithSeconds", false);
            hideStickerTime = preferences.getBoolean("hideStickerTime", false);

            hidePhoneNumber = preferences.getBoolean("hidePhoneNumber", false);
            showIdInProfile = preferences.getBoolean("showIdInProfile", true);
            disableNumberRounding = preferences.getBoolean("disableNumberRounding", false);

            downloadSpeedBoost = preferences.getBoolean("downloadSpeedBoost", true);
            uploadSpeedBoost = preferences.getBoolean("uploadSpeedBoost", true);

            loaded = true;
        }
    }

    private static void put(String key, boolean value) {
        load();
        preferences.edit().putBoolean(key, value).apply();
    }

    public static void setHideKeyboardOnScroll(boolean value) {
        hideKeyboardOnScroll = value;
        put("hideKeyboardOnScroll", value);
    }

    public static void setDisableJumpToNextChannel(boolean value) {
        disableJumpToNextChannel = value;
        put("disableJumpToNextChannel", value);
    }

    public static void setFormatTimeWithSeconds(boolean value) {
        formatTimeWithSeconds = value;
        put("formatTimeWithSeconds", value);
    }

    public static void setHideStickerTime(boolean value) {
        hideStickerTime = value;
        put("hideStickerTime", value);
    }

    public static void setHidePhoneNumber(boolean value) {
        hidePhoneNumber = value;
        put("hidePhoneNumber", value);
    }

    public static void setShowIdInProfile(boolean value) {
        showIdInProfile = value;
        put("showIdInProfile", value);
    }

    public static void setDisableNumberRounding(boolean value) {
        disableNumberRounding = value;
        put("disableNumberRounding", value);
    }

    public static void setDownloadSpeedBoost(boolean value) {
        downloadSpeedBoost = value;
        put("downloadSpeedBoost", value);
    }

    public static void setUploadSpeedBoost(boolean value) {
        uploadSpeedBoost = value;
        put("uploadSpeedBoost", value);
    }
}
