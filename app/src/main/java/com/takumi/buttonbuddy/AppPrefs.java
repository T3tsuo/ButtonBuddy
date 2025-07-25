package com.takumi.buttonbuddy;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
    private static final String PREFS_NAME = "ButtonBuddyPrefs";
    private static final String KEY_TARGET_PACKAGE = "target_package";
    static final String DEFAULT_TARGET_PACKAGE = "com.google.android.apps.walletnfcrel"; // Default to Google Wallet

    public static void saveTargetPackage(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TARGET_PACKAGE, packageName);
        editor.apply();
    }

    public static String getTargetPackage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE);
    }
}