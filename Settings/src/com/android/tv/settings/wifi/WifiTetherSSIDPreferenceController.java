/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.tv.settings.wifi;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.support.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import android.util.Log;
//import com.droidlogic.tv.settings.wifi.WifiTetherBasePreferenceController.OnTetherConfigUpdateListener;
public class WifiTetherSSIDPreferenceController extends WifiTetherBasePreferenceController
        implements ValidatedEditTextPreference.Validator {

    private static final String TAG = "WifiTetherSsidPref";
    private static final String PREF_KEY = "wifi_tether_network_name";
    @VisibleForTesting
    static final String DEFAULT_SSID = "AndroidAP";

    private String mSSID;
    private WifiDeviceNameTextValidator mWifiDeviceNameTextValidator;

    public WifiTetherSSIDPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config != null) {
            mSSID = config.getSsid();
            Log.d(TAG, "Updating SSID in Preference, " + mSSID);
        } else {
            mSSID = DEFAULT_SSID;
            Log.d(TAG, "Updating to default SSID in Preference, " + mSSID);
        }
        Log.d(TAG,"ValidatedEditTextPreference"+mPreference.getKey()+"/"+(mPreference instanceof  ValidatedEditTextPreference));
        ((ValidatedEditTextPreference) mPreference).setValidator(this);
        updateSsidDisplay((EditTextPreference) mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSSID = (String) newValue;
        updateSsidDisplay((EditTextPreference) preference);
        mListener.onTetherConfigUpdated();
        return true;
    }

    @Override
    public boolean isTextValid(String value) {
        return mWifiDeviceNameTextValidator.isTextValid(value);
    }

    public String getSSID() {
        return mSSID;
    }

    private void updateSsidDisplay(EditTextPreference preference) {
        preference.setText(mSSID);
        preference.setSummary(mSSID);
    }
}
