/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.settings.device.apps;

import static android.content.pm.ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;

import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.tvsettings.TvSettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.vnptt.VnpttUntils;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing a single app
 */
public class AppManagementFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AppManagementFragment";

    private static final String ARG_PACKAGE_NAME = "packageName";

    private static final String KEY_VERSION = "version";
    private static final String KEY_OPEN = "open";
    private static final String KEY_FORCE_STOP = "forceStop";
    private static final String KEY_UNINSTALL = "uninstall";
    private static final String KEY_ENABLE_DISABLE = "enableDisable";
    private static final String KEY_APP_STORAGE = "appStorage";
    private static final String KEY_CLEAR_DATA = "clearData";
    private static final String KEY_CLEAR_CACHE = "clearCache";
    private static final String KEY_CLEAR_DEFAULTS = "clearDefaults";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_LICENSES = "licenses";

    // Intent action implemented by apps that have open source licenses to display under settings
    private static final String VIEW_LICENSES_ACTION = "com.android.tv.settings.VIEW_LICENSES";

    // Result code identifiers
    private static final int REQUEST_UNINSTALL = 1;
    private static final int REQUEST_MANAGE_SPACE = 2;
    private static final int REQUEST_UNINSTALL_UPDATES = 3;

    private PackageManager mPackageManager;
    private String mPackageName;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private ApplicationsState.AppEntry mEntry;
    private final ApplicationsState.Callbacks mCallbacks = new ApplicationsStateCallbacks();

    private ForceStopPreference mForceStopPreference;
    private UninstallPreference mUninstallPreference;
    private EnableDisablePreference mEnableDisablePreference;
    private AppStoragePreference mAppStoragePreference;
    private ClearDataPreference mClearDataPreference;
    private ClearCachePreference mClearCachePreference;
    private ClearDefaultsPreference mClearDefaultsPreference;
    private NotificationsPreference mNotificationsPreference;

    //ChunhThanhde
    VnpttUntils securityStatus = null;
    private static final String KEY_PASSWORD = "key_password";
    private static final String KEY_CANCEL = "key_cancel";
    private static final String KEY_SECURITY = "key_security";

    private final Handler mHandler = new Handler();
    private Runnable mBailoutRunnable = () -> {
        if (isResumed() && !getFragmentManager().popBackStackImmediate()) {
            getActivity().onBackPressed();
        }
    };

    public static void prepareArgs(@NonNull Bundle args, String packageName) {
        args.putString(ARG_PACKAGE_NAME, packageName);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_INSTALLED_APP_DETAILS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPackageName = getArguments().getString(ARG_PACKAGE_NAME);
        VnpttUntils.LogDebug(TAG, "onCreate with PackageName " + mPackageName);
        securityStatus = VnpttUntils.getInstance(getContext());

        final Activity activity = getActivity();
        mPackageManager = activity.getPackageManager();
        mApplicationsState = ApplicationsState.getInstance(activity.getApplication());
        mSession = mApplicationsState.newSession(mCallbacks, getLifecycle());
        mEntry = mApplicationsState.getEntry(mPackageName, UserHandle.myUserId());

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mEntry == null) {
            Log.w(TAG, "App not found, trying to bail out");
            navigateBack();
        }

        if (mClearDefaultsPreference != null) {
            mClearDefaultsPreference.refresh();
        }
        if (mEnableDisablePreference != null) {
            mEnableDisablePreference.refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mBailoutRunnable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VnpttUntils.LogDebug(TAG, "onActivityResult ");
        if (mEntry == null) {
            return;
        }
        switch (requestCode) {
            case REQUEST_UNINSTALL:
                final int deleteResult = data != null
                        ? data.getIntExtra(Intent.EXTRA_INSTALL_RESULT, 0) : 0;
                if (deleteResult == PackageManager.DELETE_SUCCEEDED) {
                    final int userId = UserHandle.getUserId(mEntry.info.uid);
                    mApplicationsState.removePackage(mPackageName, userId);
                    navigateBack();
                } else {
                    Log.e(TAG, "Uninstall failed with result " + deleteResult);
                }
                break;
            case REQUEST_MANAGE_SPACE:
                mClearDataPreference.setClearingData(false);
                if (resultCode == Activity.RESULT_OK) {
                    final int userId = UserHandle.getUserId(mEntry.info.uid);
                    mApplicationsState.requestSize(mPackageName, userId);
                } else {
                    Log.w(TAG, "Failed to clear data!");
                }
                break;
            case REQUEST_UNINSTALL_UPDATES:
                mUninstallPreference.refresh();
                break;
        }
    }

    private void navigateBack() {
        if (getCallbackFragment() instanceof TwoPanelSettingsFragment) {
            TwoPanelSettingsFragment parentFragment =
                    (TwoPanelSettingsFragment) getCallbackFragment();
            if (parentFragment.isFragmentInTheMainPanel(this)) {
                parentFragment.navigateBack();
            }
        } else {
            // need to post this to avoid recursing in the fragment manager.
            mHandler.removeCallbacks(mBailoutRunnable);
            mHandler.post(mBailoutRunnable);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        VnpttUntils.LogDebug(TAG, "With PackageName " + mPackageName + " click preference " + preference.getKey());
        if (preference.equals(mEnableDisablePreference)) {
            // disable the preference to prevent double clicking
            mHandler.post(() -> {
                mEnableDisablePreference.setEnabled(false);
            });
        }
        final Intent intent = preference.getIntent();
        if (intent != null) {
            try {
                if (preference.equals(mUninstallPreference)) {
                    mMetricsFeatureProvider.action(getContext(),
                            MetricsEvent.ACTION_SETTINGS_UNINSTALL_APP);
                    startActivityForResult(intent, mUninstallPreference.canUninstall()
                            ? REQUEST_UNINSTALL : REQUEST_UNINSTALL_UPDATES);
                } else {
                    startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Could not find activity to launch", e);
                Toast.makeText(getContext(), R.string.device_apps_app_management_not_available,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context themedContext = getPreferenceManager().getContext();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(themedContext);
        VnpttUntils.LogDebug(TAG, "With PackageName " + mPackageName + " click preference");
        screen.setTitle(getAppName());
        setPreferenceScreen(screen);


        if (!securityStatus.getAppSystemSecurityStatus() && securityStatus.isSystemApp(mPackageName)) {
            appSecurityPrefs();
        } else {
            VnpttUntils.LogDebug(TAG, " start updatePrefs " + securityStatus.getAppSystemSecurityStatus());
            updatePrefs();
            if (Intent.ACTION_AUTO_REVOKE_PERMISSIONS.equals(getActivity().getIntent().getAction())) {
                scrollToPreference(findPreference(KEY_PERMISSIONS));
            }
        }

    }

    private void updatePrefs() {
        VnpttUntils.LogDebug(TAG, "updatePrefs");
        if (mEntry == null) {
            final PreferenceScreen screen = getPreferenceScreen();
            screen.removeAll();
            return;
        }

        final Context themedContext = getPreferenceManager().getContext();

        // Version
        Preference versionPreference = findPreference(KEY_VERSION);
        if (versionPreference == null) {
            versionPreference = new Preference(themedContext);
            versionPreference.setKey(KEY_VERSION);
            replacePreference(versionPreference);
            versionPreference.setSelectable(false);
        }
        versionPreference.setTitle(getString(R.string.device_apps_app_management_version,
                mEntry.getVersion(getActivity())));
        versionPreference.setSummary(mPackageName);

        // Open
        Preference openPreference = findPreference(KEY_OPEN);
        if (openPreference == null) {
            openPreference = new Preference(themedContext);
            openPreference.setKey(KEY_OPEN);
            replacePreference(openPreference);
        }
        Intent appLaunchIntent =
                mPackageManager.getLeanbackLaunchIntentForPackage(mEntry.info.packageName);
        if (appLaunchIntent == null) {
            appLaunchIntent = mPackageManager.getLaunchIntentForPackage(mEntry.info.packageName);
        }
        if (appLaunchIntent != null) {
            openPreference.setIntent(appLaunchIntent);
            openPreference.setTitle(R.string.device_apps_app_management_open);
            openPreference.setVisible(true);
            openPreference.setOnPreferenceClickListener(
                    preference -> {
                        logEntrySelected(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_OPEN);
                        return false;
                    });
        } else {
            openPreference.setVisible(false);
        }

        // Force stop
        if (mForceStopPreference == null) {
            mForceStopPreference = new ForceStopPreference(themedContext, mEntry);
            mForceStopPreference.setKey(KEY_FORCE_STOP);
            replacePreference(mForceStopPreference);
        } else {
            mForceStopPreference.setEntry(mEntry);
        }

        // Uninstall
        if (mUninstallPreference == null) {
            mUninstallPreference = new UninstallPreference(themedContext, mEntry);
            mUninstallPreference.setKey(KEY_UNINSTALL);
            replacePreference(mUninstallPreference);
        } else {
            mUninstallPreference.setEntry(mEntry);
        }

        // Disable/Enable
        if (mEnableDisablePreference == null) {
            mEnableDisablePreference = new EnableDisablePreference(themedContext, mEntry);
            mEnableDisablePreference.setKey(KEY_ENABLE_DISABLE);
            replacePreference(mEnableDisablePreference);
        } else {
            mEnableDisablePreference.setEntry(mEntry);
            mEnableDisablePreference.setEnabled(true);
        }

        // Storage used
        if (mAppStoragePreference == null) {
            mAppStoragePreference = new AppStoragePreference(themedContext, mEntry);
            mAppStoragePreference.setKey(KEY_APP_STORAGE);
            replacePreference(mAppStoragePreference);
        } else {
            mAppStoragePreference.setEntry(mEntry);
        }

        // Clear data
        if (clearDataAllowed()) {
            if (mClearDataPreference == null) {
                mClearDataPreference = new ClearDataPreference(themedContext, mEntry);
                mClearDataPreference.setKey(KEY_CLEAR_DATA);
                replacePreference(mClearDataPreference);
            } else {
                mClearDataPreference.setEntry(mEntry);
            }
        }

        // Clear cache
        if (mClearCachePreference == null) {
            mClearCachePreference = new ClearCachePreference(themedContext, mEntry);
            mClearCachePreference.setKey(KEY_CLEAR_CACHE);
            replacePreference(mClearCachePreference);
        } else {
            mClearCachePreference.setEntry(mEntry);
        }

        // Clear defaults
        if (mClearDefaultsPreference == null) {
            mClearDefaultsPreference = new ClearDefaultsPreference(themedContext, mEntry);
            mClearDefaultsPreference.setKey(KEY_CLEAR_DEFAULTS);
            replacePreference(mClearDefaultsPreference);
        } else {
            mClearDefaultsPreference.setEntry(mEntry);
        }

        // Notifications
        if (mNotificationsPreference == null) {
            mNotificationsPreference = new NotificationsPreference(themedContext, mEntry);
            mNotificationsPreference.setKey(KEY_NOTIFICATIONS);
            replacePreference(mNotificationsPreference);
        } else {
            mNotificationsPreference.setEntry(mEntry);
        }

        // Open Source Licenses
        Preference licensesPreference = findPreference(KEY_LICENSES);
        if (licensesPreference == null) {
            licensesPreference = new Preference(themedContext);
            licensesPreference.setKey(KEY_LICENSES);
            replacePreference(licensesPreference);
        }
        // Check if app has open source licenses to display
        Intent licenseIntent = new Intent(VIEW_LICENSES_ACTION);
        licenseIntent.setPackage(mEntry.info.packageName);
        ResolveInfo resolveInfo = resolveIntent(licenseIntent);
        if (resolveInfo == null) {
            licensesPreference.setVisible(false);
        } else {
            Intent intent = new Intent(licenseIntent);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            licensesPreference.setIntent(intent);
            licensesPreference.setTitle(R.string.device_apps_app_management_licenses);
            licensesPreference.setOnPreferenceClickListener(
                    preference -> {
                        logEntrySelected(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_LICENSES);
                        return false;
                    });
            licensesPreference.setVisible(true);
        }

        // Permissions
        Preference permissionsPreference = findPreference(KEY_PERMISSIONS);
        if (permissionsPreference == null) {
            permissionsPreference = new Preference(themedContext);
            permissionsPreference.setKey(KEY_PERMISSIONS);
            permissionsPreference.setTitle(R.string.device_apps_app_management_permissions);
            replacePreference(permissionsPreference);
        }
        permissionsPreference.setOnPreferenceClickListener(
                preference -> {
                    logEntrySelected(TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY_PERMISSIONS);
                    return false;
                });
        permissionsPreference.setIntent(new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName));
    }

    private void appSecurityPrefs() {
        VnpttUntils.LogDebug(TAG, "appSecurityPrefs");
        final Context themedContext = getPreferenceManager().getContext();

        // Version
        Preference versionPreference = findPreference(KEY_VERSION);
        if (versionPreference == null) {
            versionPreference = new Preference(themedContext);
            versionPreference.setKey(KEY_VERSION);
            replacePreference(versionPreference);
            versionPreference.setSelectable(false);
        }
        versionPreference.setTitle(getString(R.string.device_apps_app_management_version, mEntry.getVersion(getActivity())));
        versionPreference.setSummary(mPackageName);


        // security
        Preference securityPreference = findPreference(KEY_SECURITY);
        if (securityPreference == null) {
            securityPreference = new Preference(themedContext);
            securityPreference.setKey(KEY_SECURITY);
            replacePreference(securityPreference);
            securityPreference.setSelectable(false);
        }
        securityPreference.setIcon(R.drawable.vnptt_shield_lock);
        securityPreference.setTitle(R.string.app_security_title);
        securityPreference.setSummary(R.string.app_security_description);


        // Password input
        EditTextPreference passwordPref = findPreference(KEY_PASSWORD);
        if (passwordPref == null) {
            passwordPref = new EditTextPreference(themedContext);
            passwordPref.setKey(KEY_PASSWORD);
            passwordPref.setTitle(R.string.app_security_title_button);
            passwordPref.setDialogTitle(R.string.app_security_dialog_title);
            getPreferenceScreen().addPreference(passwordPref);
        }

        passwordPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String password = (String) newValue;
                if (password.equals(VnpttUntils.APP_SYSTEM_PASSWORD)) {
                    securityStatus.setAppSystemOpenSecurity();
                    Toast.makeText(getActivity(), R.string.app_security_success, Toast.LENGTH_SHORT).show();
                    passwordSuccess();
                } else {
                    Toast.makeText(getActivity(), R.string.app_security_fail, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        passwordPref.setSummaryProvider(new Preference.SummaryProvider<EditTextPreference>() {
            @Override
            public CharSequence provideSummary(EditTextPreference preference) {
                return null;
            }
        });


        final PreferenceScreen screen = getPreferenceScreen();

        Preference open = findPreference(KEY_OPEN);
        if (open != null) {
            screen.removePreference(open);
        }

        Preference forceStop = findPreference(KEY_FORCE_STOP);
        if (forceStop != null) {
            screen.removePreference(forceStop);
        }
        Preference uninstall = findPreference(KEY_UNINSTALL);
        if (uninstall != null) {
            screen.removePreference(uninstall);
        }
        Preference enable = findPreference(KEY_ENABLE_DISABLE);
        if (enable != null) {
            screen.removePreference(enable);
        }
        Preference storage = findPreference(KEY_APP_STORAGE);
        if (storage != null) {
            screen.removePreference(storage);
        }
        Preference clearData = findPreference(KEY_CLEAR_DATA);
        if (clearData != null) {
            screen.removePreference(clearData);
        }
        Preference clearCache = findPreference(KEY_CLEAR_CACHE);
        if (clearCache != null) {
            screen.removePreference(clearCache);
        }
        Preference clear = findPreference(KEY_CLEAR_DEFAULTS);
        if (clear != null) {
            screen.removePreference(clear);
        }
        Preference noti = findPreference(KEY_NOTIFICATIONS);
        if (noti != null) {
            screen.removePreference(noti);
        }
        Preference per = findPreference(KEY_PERMISSIONS);
        if (per != null) {
            screen.removePreference(per);
        }
    }

    private void replacePreference(Preference preference) {
        VnpttUntils.LogDebug(TAG, "replacePreference " + preference.getKey());
        final String key = preference.getKey();
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Can't replace a preference without a key");
        }
        final Preference old = findPreference(key);
        if (old != null) {
            getPreferenceScreen().removePreference(old);
        }
        getPreferenceScreen().addPreference(preference);
    }

    private ResolveInfo resolveIntent(Intent intent) {
        VnpttUntils.LogDebug(TAG, "resolveIntent ");
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, 0);
        return (resolveInfos == null || resolveInfos.size() <= 0) ? null : resolveInfos.get(0);
    }

    public String getAppName() {
        if (mEntry == null) {
            return null;
        }
        mEntry.ensureLabel(getActivity());
        return mEntry.label;
    }

    public Drawable getAppIcon() {
        if (mEntry == null) {
            return null;
        }
        mApplicationsState.ensureIcon(mEntry);
        return mEntry.icon;
    }

    public void clearData() {
        VnpttUntils.LogDebug(TAG, "clearData ");
        if (!clearDataAllowed()) {
            Log.e(TAG, "Attempt to clear data failed. Clear data is disabled for " + mPackageName);
            return;
        }

        mMetricsFeatureProvider.action(getContext(), MetricsEvent.ACTION_SETTINGS_CLEAR_APP_DATA);
        mClearDataPreference.setClearingData(true);
        String spaceManagementActivityName = mEntry.info.manageSpaceActivityName;
        if (spaceManagementActivityName != null) {
            if (!ActivityManager.isUserAMonkey()) {
                Intent intent = new Intent(Intent.ACTION_DEFAULT);
                intent.setClassName(mEntry.info.packageName, spaceManagementActivityName);
                startActivityForResult(intent, REQUEST_MANAGE_SPACE);
            }
        } else {
            // Disabling clear cache preference while clearing data is in progress. See b/77815256
            // for details.
            mClearCachePreference.setClearingCache(true);
            ActivityManager am = (ActivityManager) getActivity().getSystemService(
                    Context.ACTIVITY_SERVICE);
            boolean success = am.clearApplicationUserData(
                    mEntry.info.packageName, new IPackageDataObserver.Stub() {
                        public void onRemoveCompleted(
                                final String packageName, final boolean succeeded) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mClearDataPreference.setClearingData(false);
                                    mClearCachePreference.setClearingCache(false);
                                    if (succeeded) {
                                        dataCleared(true);
                                    } else {
                                        dataCleared(false);
                                    }
                                }
                            });
                        }
                    });
            if (!success) {
                mClearDataPreference.setClearingData(false);
                dataCleared(false);
            }
        }
        mClearDataPreference.refresh();
    }

    private void dataCleared(boolean succeeded) {
        VnpttUntils.LogDebug(TAG, "dataCleared ");
        if (succeeded) {
            final int userId = UserHandle.getUserId(mEntry.info.uid);
            mApplicationsState.requestSize(mPackageName, userId);
        } else {
            Log.w(TAG, "Failed to clear data!");
            mClearDataPreference.refresh();
        }
    }

    public void clearCache() {
        VnpttUntils.LogDebug(TAG, "clearCache ");
        mMetricsFeatureProvider.action(getContext(), MetricsEvent.ACTION_SETTINGS_CLEAR_APP_CACHE);
        mClearCachePreference.setClearingCache(true);
        mPackageManager.deleteApplicationCacheFiles(mEntry.info.packageName,
                new IPackageDataObserver.Stub() {
                    public void onRemoveCompleted(final String packageName,
                                                  final boolean succeeded) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mClearCachePreference.setClearingCache(false);
                                cacheCleared(succeeded);
                            }
                        });
                    }
                });
        mClearCachePreference.refresh();
    }

    private void cacheCleared(boolean succeeded) {
        VnpttUntils.LogDebug(TAG, "cacheCleared ");
        if (succeeded) {
            final int userId = UserHandle.getUserId(mEntry.info.uid);
            mApplicationsState.requestSize(mPackageName, userId);
        } else {
            Log.w(TAG, "Failed to clear cache!");
            mClearCachePreference.refresh();
        }
    }

    /**
     * Clearing data can only be disabled for system apps. For all non-system apps it is enabled.
     * System apps disable it explicitly via the android:allowClearUserData tag.
     **/
    private boolean clearDataAllowed() {
        VnpttUntils.LogDebug(TAG, "clearDataAllowed ");
        boolean sysApp = (mEntry.info.flags & FLAG_SYSTEM) == FLAG_SYSTEM;
        boolean allowClearData =
                (mEntry.info.flags & FLAG_ALLOW_CLEAR_USER_DATA) == FLAG_ALLOW_CLEAR_USER_DATA;
        return !sysApp || allowClearData;
    }

    @Override
    protected int getPageId() {
        VnpttUntils.LogDebug(TAG, "getPageId ");
        return TvSettingsEnums.APPS_ALL_APPS_APP_ENTRY;
    }

    private class ApplicationsStateCallbacks implements ApplicationsState.Callbacks {

        @Override
        public void onRunningStateChanged(boolean running) {
            VnpttUntils.LogDebug(TAG, "onRunningStateChanged ");
            if (mForceStopPreference != null) {
                mForceStopPreference.refresh();
            }
        }

        @Override
        public void onPackageListChanged() {
            VnpttUntils.LogDebug(TAG, "onPackageListChanged ");
            if (mEntry == null || mEntry.info == null) {
                return;
            }
            final int userId = UserHandle.getUserId(mEntry.info.uid);
            mEntry = mApplicationsState.getEntry(mPackageName, userId);
            if (mEntry == null) {
                navigateBack();
            }
            updatePrefsByVnptt();
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            if (mAppStoragePreference == null) {
                // Nothing to do here.
                return;
            }
            mAppStoragePreference.refresh();
            mClearCachePreference.refresh();

            if (mClearDataPreference != null) {
                mClearDataPreference.refresh();
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mAppStoragePreference == null) {
                // Nothing to do here.
                return;
            }
            mAppStoragePreference.refresh();
            mClearCachePreference.refresh();

            if (mClearDataPreference != null) {
                mClearDataPreference.refresh();
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            VnpttUntils.LogDebug(TAG, "onLauncherInfoChanged");
            updatePrefsByVnptt();
        }

        @Override
        public void onLoadEntriesCompleted() {
            VnpttUntils.LogDebug(TAG, "onLoadEntriesCompleted");
            mEntry = mApplicationsState.getEntry(mPackageName, UserHandle.myUserId());
            updatePrefsByVnptt();
            if (mAppStoragePreference == null) {
                // Nothing to do here.
                return;
            }
            mAppStoragePreference.refresh();
            mClearCachePreference.refresh();

            if (mClearDataPreference != null) {
                mClearDataPreference.refresh();
            }
        }
    }

    private void updatePrefsByVnptt() {
        if (!securityStatus.getAppSystemSecurityStatus() && securityStatus.isSystemApp(mPackageName)) {
            appSecurityPrefs();
        } else {
            updatePrefs();
        }
    }

    private void passwordSuccess() {
        final PreferenceScreen screen = getPreferenceScreen();
        Preference security = findPreference(KEY_SECURITY);
        if (security != null) {
            screen.removePreference(security);
        }
        Preference password = findPreference(KEY_PASSWORD);
        if (password != null) {
            screen.removePreference(password);
        }
        Preference cancel = findPreference(KEY_CANCEL);
        if (cancel != null) {
            screen.removePreference(cancel);
        }
        updatePrefs();
    }
}
