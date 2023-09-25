package com.android.tv.settings.vnptt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VnpttUntils {
    private static final String LOG_TAG = "VNPTTSetting";
    private static final String TAG = "Untils";
    private static final boolean DEBUG = true;
    public static final String EXPIRATION_PASSWORD_DEVICE_PREF = "expiration_password_device_pref";
    public static final String EXPIRATION_PASSWORD_APP_SYSTEM = "expiration_password_app_system";
    public static final String DEVELOPER_STATUS = "expiration_password_app_system";

    // Password of VNPTT Settings bu ChunhThanhDe
    public static final String DEVICE_PREFS_PASSWORD = "22042001";
    // it means "C"
    public static final String FACTORY_PASSWORD = "3214789";
    // it means "T"
    public static final String APP_SYSTEM_PASSWORD = "12358";
    //  it means "D"
    public static final String DEVELOPMENT_PASSWORD = "147268";
    //  it means "O"
    public static final String SETTINGS_PASSWORD = "32147896";

    private static final String KEY_SECURITY_SETTINGS = "system_functionality";


    static SharedPreferences mPrefs;
    private static VnpttUntils untils = null;

    /**
     * Override Log
     * <p>
     * Log.v(); // Verbose (Big Info like detail message)
     * Log.d(); // Debug
     * Log.i(); // Info
     * Log.w(); // Warning
     * Log.e(); // Error
     * Log.wtf(); // appear when they are wtf errors
     */
    public static void LogDebug(String classNameTag, String log) {
        if (DEBUG) Log.d(LOG_TAG, "class " + classNameTag + ": " + log);
    }

    public static void LogError(String classNameTag, String log) {
        Log.e(LOG_TAG, "class " + classNameTag + ": " + log);
    }

    public static void LogInfor(String classNameTag, String log) {
        Log.i(LOG_TAG, "class " + classNameTag + ": " + log);
    }

    public static void LogWarning(String classNameTag, String log) {
        Log.w(LOG_TAG, "class " + classNameTag + ": " + log);
    }

    public static void LogVerbose(String classNameTag, String log) {
        Log.v(LOG_TAG, "class " + classNameTag + ": " + log);
    }

    public static void LogWtf(String classNameTag, String log) {
        Log.wtf(LOG_TAG, "class " + classNameTag + ": " + log);
    }


    private VnpttUntils(Context context) {
        mPrefs = context.getSharedPreferences(EXPIRATION_PASSWORD_DEVICE_PREF, 0);
        mPrefs = context.getSharedPreferences(EXPIRATION_PASSWORD_APP_SYSTEM, 0);
        mPrefs = context.getSharedPreferences(DEVELOPER_STATUS, 0);
        mPrefs = context.getSharedPreferences(KEY_SECURITY_SETTINGS, 0);
    }

    public static synchronized VnpttUntils getInstance(Context context) {
        if (untils == null) {
            untils = new VnpttUntils(context);
        }
        return untils;
    }

    public boolean getDevicePrefsSecurityStatus() {
        synchronized (mPrefs) {
            boolean securitySettingsEnabled = mPrefs.getBoolean(KEY_SECURITY_SETTINGS, true);
            if (!securitySettingsEnabled) {
                return true;
            }

            String expirationString = mPrefs.getString(EXPIRATION_PASSWORD_DEVICE_PREF, "");
            if (expirationString == null || expirationString.equals("")) {
                return false;
            }

            LocalDateTime expiration;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                expiration = LocalDateTime.parse(expirationString, formatter);
            } catch (Exception e) {
                LogDebug(TAG, "expiration error: " + e.toString());
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.plusMinutes(-5);
            LogDebug(TAG, "now: " + now.toString() + " expiration tDevicePrefsSecurity: " + expiration.toString());
            Boolean a = expiration.isAfter(threshold);
            Boolean b = expiration.isBefore(now);
            LogDebug(TAG, "expiration.isBefore(now); " + b.toString() + " expiration.isAfter(threshold) " + a.toString());
            return a && b;
        }
    }

    public static void setDevicePrefsOpenSecurity() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        String nowString = now.format(formatter);
        synchronized (mPrefs) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(EXPIRATION_PASSWORD_DEVICE_PREF, nowString);
            editor.commit();
        }
    }

    public static void setAppSystemOpenSecurity() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        String nowString = now.format(formatter);
        synchronized (mPrefs) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(EXPIRATION_PASSWORD_APP_SYSTEM, nowString);
            editor.commit();
        }
    }

    public boolean getAppSystemSecurityStatus() {
        synchronized (mPrefs) {
            boolean securitySettingsEnabled = mPrefs.getBoolean(KEY_SECURITY_SETTINGS, true);
            if (!securitySettingsEnabled) {
                return true;
            }

            String expirationString = mPrefs.getString(EXPIRATION_PASSWORD_APP_SYSTEM, "");
            if (expirationString == null || expirationString.equals("")) {
                return false;
            }

            LocalDateTime expiration;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                expiration = LocalDateTime.parse(expirationString, formatter);
            } catch (Exception e) {
                LogDebug(TAG, "expiration error: " + e.toString());
                return false;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.plusMinutes(-5);
            LogDebug(TAG, "now: " + now.toString() + " expiration AppSystemSecurity: " + expiration.toString());
            return expiration.isAfter(threshold) && expiration.isBefore(now);
        }
    }

    public static boolean isSystemApp(String packageName) {
        if (packageName.equals("vn.mytvnet.b2cott") ||
                packageName.equals("com.android.tv.settings") ||
                packageName.equals("com.vnpt.tr069") ||
                packageName.equals("com.vnptt.update.application") ||
                packageName.equals("com.vnptt.ota") ||
                packageName.equals("com.vnptt.packageinstaller")) {
            return true;
        }
        return false;
    }

    public static void setSettingsSecurity(Boolean status) {
        synchronized (mPrefs) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(KEY_SECURITY_SETTINGS, status);
            editor.commit();
        }
    }

    public boolean getSettingsSecurityStatus() {
        synchronized (mPrefs) {
            boolean securitySettingsEnabled = mPrefs.getBoolean(KEY_SECURITY_SETTINGS, true);
            return securitySettingsEnabled;
        }
    }
}
