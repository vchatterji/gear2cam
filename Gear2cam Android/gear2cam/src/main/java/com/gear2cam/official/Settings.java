package com.gear2cam.official;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by varun on 10/5/14.
 */
public class Settings {
    //The shared preference file
    private static final String PREF_FILE_NAME = "gear2cam";

    //The keys
    private static final String KEY_USER_EMAIL = "KEY_USER_EMAIL";
    private static final String KEY_IS_PUBLISH_DIALOG_DISABLED = "KEY_IS_PUBLISH_DIALOG_DISABLED";
    private static final String KEY_IS_PUBLISHING_ENABLED = "KEY_IS_PUBLISHING_ENABLED";
    private static final String KEY_IS_GEAR_ABSENT = "KEY_IS_GEAR_ABSENT";
    private static final String KEY_IS_EULA_ACCEPTED = "KEY_IS_EULA_ACCEPTED";
    private static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";
    private static final String KEY_IS_BACKGROUND_SUPPORTED = "KEY_IS_BACKGROUND_SUPPORTED_V_1_3_1";
    private static final String KEY_SUPPORT_EMAIL = "KEY_SUPPORT_EMAIL";
    private static final String KEY_IS_BACKGROUND_CHECK_COMPLETED = "KEY_IS_BACKGROUND_CHECK_COMPLETED";

    private static SharedPreferences prefLocal = null;

    private static SharedPreferences getPrefs(Context context)
    {
        if(prefLocal == null) {
            prefLocal = context.getSharedPreferences(PREF_FILE_NAME, context.MODE_PRIVATE);
        }
        return prefLocal;
    }

    public synchronized static boolean isBackgroundCheckCompleted(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_BACKGROUND_CHECK_COMPLETED, false);
    }

    public synchronized static void setBackgroundCheckCompleted(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_BACKGROUND_CHECK_COMPLETED, val);
        editor.commit();
    }

    public synchronized static boolean isBackgroundSupported(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_BACKGROUND_SUPPORTED, true);
    }

    public synchronized static void setBackgroundSupported(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_BACKGROUND_SUPPORTED, val);
        editor.commit();
    }

    public synchronized static boolean isPublishDialogDisabled(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_PUBLISH_DIALOG_DISABLED, false);
    }

    public synchronized static void setPublishDialogDisabled(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_PUBLISH_DIALOG_DISABLED, val);
        editor.commit();
    }

    public synchronized static boolean isGearAbsent(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_GEAR_ABSENT, true);
    }

    public synchronized static void setGearAbsent(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_GEAR_ABSENT, val);
        editor.commit();
    }

    public synchronized static boolean isEulaAccepted(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_EULA_ACCEPTED, false);
    }

    public synchronized static int getNotificationId(Context context) {
        SharedPreferences pref = getPrefs(context);
        int id = pref.getInt(KEY_NOTIFICATION_ID, 1);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_NOTIFICATION_ID, id+1);
        editor.commit();
        return id;
    }

    public synchronized static void setEulaAccepted(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_EULA_ACCEPTED, val);
        editor.commit();
    }

    public synchronized static String getUserEmail(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public synchronized static void setUserEmail(Context context, String val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_USER_EMAIL, val);
        editor.commit();
    }

    public synchronized static String getSupportEmail(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getString(KEY_SUPPORT_EMAIL, "");
    }

    public synchronized static void setSupportEmail(Context context, String val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_SUPPORT_EMAIL, val);
        editor.commit();
    }

    public synchronized static boolean isPublishingEnabled(Context context) {
        SharedPreferences pref = getPrefs(context);
        return pref.getBoolean(KEY_IS_PUBLISHING_ENABLED, false);
    }

    public synchronized static void setPublishingEnabled(Context context, boolean val) {
        SharedPreferences pref = getPrefs(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_PUBLISHING_ENABLED, val);
        editor.commit();
    }
}
