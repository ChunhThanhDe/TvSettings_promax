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

package com.android.tv.settings.device.display.daydream;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_CLASSIC;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_TWO_PANEL;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_VENDOR;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_X;
import static com.android.tv.settings.util.InstrumentationUtils.logEntrySelected;

import android.app.tvsettings.TvSettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.BatteryManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.dream.DreamBackend;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.overlay.OverlayUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import com.droidlogic.app.SystemControlManager;

/**
 * The screen saver screen in TV settings.
 */
@Keep
public class DaydreamFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "DaydreamFragment";

    private static final String KEY_ACTIVE_DREAM = "activeDream";
    private static final String KEY_DREAM_TIME = "dreamTime";
    private static final String KEY_DREAM_NOW = "dreamNow";

    private static final String KEY_SCREEN_SAVER = "screen_saver";
    private static final String KEY_SCREEN_SAVER_DURING_PLAY_VIDEO = "screen_saver_during_play_video";
    private static final String MODE_ALWAY_SCREEN_SAVER = "alway_screen_saver";

    private static final String DREAM_COMPONENT_NONE = "NONE";
    private static final String PACKAGE_SCHEME = "package";
    private static final String defaultDreamComponent = "{com.google.android.deskclock/com.android.deskclock.Screensaver}";
    private static final String defaultValue = "{com.google.android.deskclock/com.android.deskclock.Screensaver}";

    private static final int DEFAULT_DREAM_TIME_MS = (int) (30 * DateUtils.MINUTE_IN_MILLIS);

    private final PackageReceiver mPackageReceiver = new PackageReceiver();

    private DreamBackend mBackend;
    private final Map<String, DreamBackend.DreamInfo> mDreamInfos = new ArrayMap<>();

    public static DaydreamFragment newInstance() {
        return new DaydreamFragment();
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int stayOnWhilePluggedIn = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    3);
            if (stayOnWhilePluggedIn != 0) {
               Settings.Global.putInt(context.getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                        (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB)); // == 3
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBackend = new DreamBackend(getActivity());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromBackend();

        // listen for package changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(PACKAGE_SCHEME);
        getActivity().registerReceiver(mPackageReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mPackageReceiver);
    }


    private int getPreferenceScreenResId() {
	String country = Locale.getDefault().getCountry();

        switch (OverlayUtils.getFlavor(getContext())) {
            case FLAVOR_CLASSIC:
            case FLAVOR_TWO_PANEL:
		if (country.equals("US") || country.equals("VN")){
			return R.xml.daydream;
		} else 
			return R.xml.daydream_other_language;
            case FLAVOR_X:
            case FLAVOR_VENDOR:
                return R.xml.daydream_x;
            default:
                if (country.equals("US") || country.equals("VN")){
			return R.xml.daydream;
		} else 
			return R.xml.daydream_other_language;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(getPreferenceScreenResId(), null);

        boolean screenSaverEnabled = (3 != Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                0));
        final TwoStatePreference screenSaverPref = (TwoStatePreference) findPreference(KEY_SCREEN_SAVER);
        screenSaverPref.setChecked(screenSaverEnabled);

	// Add Switch for enable or disable play video mode
        final TwoStatePreference duringPlayVideoPref = (TwoStatePreference) findPreference(KEY_SCREEN_SAVER_DURING_PLAY_VIDEO);
        duringPlayVideoPref.setChecked(getDreamModeForPlayVideo() == 1 ? true : false);
        duringPlayVideoPref.setEnabled(screenSaverEnabled);

        final ListPreference activeDreamPref = (ListPreference) findPreference(KEY_ACTIVE_DREAM);
        refreshActiveDreamPref(activeDreamPref);
        if (activeDreamPref != null) {
            activeDreamPref.setOnPreferenceChangeListener(this);
        }

        if (screenSaverEnabled){
            if (getDreamModeForPlayVideo() == 1){
                setActiveDream(activeDreamPref.getValue());
                activeDreamPref.setEnabled(false);
            }
            else {
                activeDreamPref.setEnabled(true);
            }
        }
        else {
            activeDreamPref.setEnabled(false);
        }

        final ListPreference dreamTimePref = (ListPreference) findPreference(KEY_DREAM_TIME);
        if (dreamTimePref != null) {
            dreamTimePref.setValue(Integer.toString(getDreamTime()));
            dreamTimePref.setOnPreferenceChangeListener(this);
        }
        //final Preference dreamNowPref = findPreference(KEY_DREAM_NOW);
        //dreamNowPref.setEnabled(mBackend.isEnabled());
    }

    private void refreshActiveDreamPref(ListPreference activeDreamPref) {
        final List<DreamBackend.DreamInfo> infos = mBackend.getDreamInfos();
        final CharSequence[] dreamEntries = new CharSequence[infos.size() + 1];
        final CharSequence[] dreamEntryValues = new CharSequence[infos.size() + 1];
        refreshDreamInfoMap(infos, dreamEntries, dreamEntryValues);
        if (activeDreamPref != null) {
            activeDreamPref.setEntries(dreamEntries);
            activeDreamPref.setEntryValues(dreamEntryValues);
        }
        final ComponentName currentDreamComponent = mBackend.getActiveDream();

        if ((currentDreamComponent.toShortString().equals(defaultDreamComponent)) &&
               ((activeDreamPref.getValue() == null ) || activeDreamPref.getValue().equals(defaultValue))) {
            setActiveDream(DREAM_COMPONENT_NONE);
            activeDreamPref.setValue(DREAM_COMPONENT_NONE);
        }

        if (activeDreamPref != null) {
            activeDreamPref.setValue(mBackend.isEnabled() && currentDreamComponent != null
                    ? currentDreamComponent.toShortString() : DREAM_COMPONENT_NONE);
        }
    }

    private void refreshDreamInfoMap(List<DreamBackend.DreamInfo> infos,
            CharSequence[] listEntries, CharSequence[] listEntryValues) {
        mDreamInfos.clear();
        listEntries[0] = getString(R.string.device_daydreams_none);
        listEntryValues[0] = DREAM_COMPONENT_NONE;
        int index = 1;
        for (final DreamBackend.DreamInfo info : infos) {
            final String componentNameString = info.componentName.toShortString();
            mDreamInfos.put(componentNameString, info);
            listEntries[index] = info.caption;
            listEntryValues[index] = componentNameString;
            index++;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_ACTIVE_DREAM:
                logEntrySelected(TvSettingsEnums.PREFERENCES_SCREENSAVER_CHOOSER);
                setActiveDream((String) newValue);
                break;
            case KEY_DREAM_TIME:
                final int sleepTimeout = Integer.parseInt((String) newValue);
                if (getSleepTimeoutEntryId(sleepTimeout) != -1) {
                    logEntrySelected(getSleepTimeoutEntryId(sleepTimeout));
                }
                setDreamTime(sleepTimeout);
                break;
        }
        return true;
    }

    private void setActiveDream(String componentNameString) {
        final DreamBackend.DreamInfo dreamInfo = mDreamInfos.get(componentNameString);
        if (dreamInfo != null) {
            if (dreamInfo.settingsComponentName != null) {
                startActivity(new Intent().setComponent(dreamInfo.settingsComponentName));
            }
            if (!mBackend.isEnabled()) {
                mBackend.setEnabled(true);
            }
            if (!Objects.equals(mBackend.getActiveDream(), dreamInfo.componentName)) {
                mBackend.setActiveDream(dreamInfo.componentName);
            }
        } else {
            if (mBackend.isEnabled()) {
                mBackend.setActiveDream(null);
                mBackend.setEnabled(false);
            }
        }
    }

    private int getDreamTime() {
        return Settings.System.getInt(getActivity().getContentResolver(), SCREEN_OFF_TIMEOUT,
                DEFAULT_DREAM_TIME_MS);
    }

    private void setDreamTime(int ms) {
        Settings.System.putInt(getActivity().getContentResolver(), SCREEN_OFF_TIMEOUT, ms);
    }

    private int getDreamModeForPlayVideo() {
        return Settings.System.getInt(getActivity().getContentResolver(), MODE_ALWAY_SCREEN_SAVER,
                0);
    }

    private void setDreamModeForPlayVideo(int mode) {
        Settings.System.putInt(getActivity().getContentResolver(), MODE_ALWAY_SCREEN_SAVER, mode);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final TwoStatePreference duringPlayVideoPref = (TwoStatePreference) findPreference(KEY_SCREEN_SAVER_DURING_PLAY_VIDEO);
        final ListPreference activeDreamPref = (ListPreference) findPreference(KEY_ACTIVE_DREAM);
        boolean mode = (getDreamModeForPlayVideo() == 1 ? true : false);

        switch (preference.getKey()) {
//            case KEY_DREAM_NOW:
//                logEntrySelected(TvSettingsEnums.PREFERENCES_SCREENSAVER_START_NOW);
//                mBackend.startDreaming();
//                return true;

            case KEY_SCREEN_SAVER:
                final ListPreference dreamTimePref = (ListPreference) findPreference(KEY_DREAM_TIME);
                boolean isEnabled = (3 != Settings.Global.getInt(getActivity().getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                        0));
                activeDreamPref.setEnabled(!isEnabled);
                dreamTimePref.setEnabled(!isEnabled);

                duringPlayVideoPref.setEnabled(!isEnabled);
                if (mode && !isEnabled){
                    activeDreamPref.setEnabled(false);
                    setActiveDream(activeDreamPref.getValue());
                }

                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                        isEnabled ? (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB) : 0);
		return true;
            case KEY_SCREEN_SAVER_DURING_PLAY_VIDEO:
                duringPlayVideoPref.setChecked(!mode);
                activeDreamPref.setValueIndex(0);
                setActiveDream(activeDreamPref.getValue());
                activeDreamPref.setEnabled(mode);
                setDreamModeForPlayVideo(mode ? 0 : 1);
            default:
                return super.onPreferenceTreeClick(preference);
        }
    }

    private void refreshFromBackend() {
        if (getActivity() == null) {
            Log.d(TAG, "No activity, not refreshing");
            return;
        }

        final ListPreference activeDreamPref = (ListPreference) findPreference(KEY_ACTIVE_DREAM);
        if (activeDreamPref != null) {
            refreshActiveDreamPref(activeDreamPref);
        }

        final ListPreference dreamTimePref = (ListPreference) findPreference(KEY_DREAM_TIME);
        if (dreamTimePref != null) {
            dreamTimePref.setValue(Integer.toString(getDreamTime()));
        }

//        final Preference dreamNowPref = findPreference(KEY_DREAM_NOW);
//        if (dreamNowPref != null) {
//            dreamNowPref.setEnabled(mBackend.isEnabled());
//        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DREAM;
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshFromBackend();
        }
    }

    // Map @array/sleep_timeout_entries to defined log enum
    private int getSleepTimeoutEntryId(int sleepTimeout) {
        switch (sleepTimeout) {
            case 300000:
                return TvSettingsEnums.PREFERENCES_SCREENSAVER_START_DELAY_5M;
            case 900000:
                return TvSettingsEnums.PREFERENCES_SCREENSAVER_START_DELAY_15M;
            case 1800000:
                return TvSettingsEnums.PREFERENCES_SCREENSAVER_START_DELAY_30M;
            case 3600000:
                return TvSettingsEnums.PREFERENCES_SCREENSAVER_START_DELAY_1H;
            case 7200000:
                return TvSettingsEnums.PREFERENCES_SCREENSAVER_START_DELAY_2H;
            default:
                return -1;
        }
    }

    @Override
    protected int getPageId() {
        return TvSettingsEnums.PREFERENCES_SCREENSAVER;
    }
}
