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

package com.android.tv.settings.display;

import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.tv.settings.util.DroidUtils;
import com.android.tv.settings.SettingsConstant;
import com.android.tv.settings.R;
import com.droidlogic.app.SystemControlManager;
import android.util.Log;

public class DisplayFragment extends LeanbackPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "DisplayFragment";

    private static final String KEY_POSITION = "position";
    private static final String KEY_OUTPUTMODE = "outputmode";
    private static final String KEY_HDR = "hdr";
    private static final String KEY_SDR = "sdr";
    private static final String KEY_DOLBY_VISION    = "dolby_vision";
    private static final String KEY_ALLM_MODE    = "allm_mode";
    private static final String KEY_GAME_CONTENT_TYPE    = "game_content_type";

    private ListPreference mAllmPref;
    private SystemControlManager mSystemControlManager;

    public static DisplayFragment newInstance() {
        return new DisplayFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.display, null);
        boolean tvFlag = SettingsConstant.needDroidlogicTvFeature(getContext())
            && (SystemProperties.getBoolean("tv.soc.as.mbox", false) == false);
        final Preference outputmodePref = findPreference(KEY_OUTPUTMODE);
        outputmodePref.setVisible(SettingsConstant.needScreenResolutionFeture(getContext()) && !tvFlag);

        final Preference screenPositionPref = findPreference(KEY_POSITION);
        screenPositionPref.setVisible(!tvFlag);

        final Preference sdrPref = findPreference(KEY_SDR);
        sdrPref.setVisible(false);

        final Preference hdrPref = findPreference(KEY_HDR);
        hdrPref.setVisible(false);

        final Preference dvPref =(Preference) findPreference(KEY_DOLBY_VISION);
        dvPref.setVisible((SystemProperties.getBoolean("ro.vendor.platform.support.dolbyvision", false) == true)
            && tvFlag);

        mSystemControlManager = SystemControlManager.getInstance();
        mAllmPref = (ListPreference) findPreference(KEY_ALLM_MODE);
        mAllmPref.setOnPreferenceChangeListener(this);
        mAllmPref.setVisible(SystemProperties.getBoolean("ro.vendor.debug.allm", false));

        mSystemControlManager = SystemControlManager.getInstance();
        mAllmPref = (ListPreference) findPreference(KEY_GAME_CONTENT_TYPE);
        mAllmPref.setOnPreferenceChangeListener(this);
        mAllmPref.setVisible(SystemProperties.getBoolean("ro.vendor.debug.allm", false));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (TextUtils.equals(preference.getKey(), KEY_ALLM_MODE)) {
            int allmmode = Integer.parseInt((String)newValue);
            if (allmmode == 0) {
                /* set allmmode to 0 is not really disable the allm mode
                   the VSIF still contain the allm mode info (0), and it still
                   will conflit with Dobly Vision.
                   so amlogic add a new value -1 to readlly disable disable allm
                   mode, not only driver info, but also VSIF info
                */
                allmmode = -1;
            }
            mSystemControlManager.setALLMMode(allmmode);
        }

        if (TextUtils.equals(preference.getKey(), KEY_GAME_CONTENT_TYPE)) {
            mSystemControlManager.sendHDMIContentType(Integer.parseInt((String)newValue));
        }
        return true;
    }
}
