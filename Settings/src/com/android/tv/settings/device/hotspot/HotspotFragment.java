/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC HotspotFragment
 */

package com.android.tv.settings.device.hotspot;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.tv.settings.SettingsPreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment ;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import android.text.TextUtils;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import com.android.tv.settings.R;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import java.util.Map;
import java.util.Set;
import android.os.SystemProperties;
import android.widget.Toast;
import java.util.Random;
import com.android.internal.logging.nano.MetricsProto;

/**
 * Fragment to control HDMI Cec settings.
 */
public class HotspotFragment extends SettingsPreferenceFragment {

	private static final String TAG = "HotspotFragment";

	private static final String KEY_HOTSPOT_OPEN = "hotspot_open";
	private static final String KEY_NETWORK_SETTINGS = "hotspot_settings";

	public static final String KEY_HOTSPOT_SSID = "KEY_HOTSPOT_SSID";
	public static final String KEY_HOTSPOT_PASSWORD = "KEY_HOTSPOT_PASSWORD";


	private static final int REQUEST_CODE = 100;
	public static final int RESULT_OK = 1;
	public static final int RESULT_CANCEL = 0;

    public static final String PERSIST_HOTSPOT_OPEN = "persist.vendor.sys.hotspot.open";

	private TwoStatePreference mHotspotOpenPref;
	private ConnectivityManager mConnectivityManager;

	public static HotspotFragment newInstance() {
		return new HotspotFragment();
	}


	 public static class BootReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            Settings.Global.putInt(context.getContentResolver(),
            HotspotFragment.PERSIST_HOTSPOT_OPEN,0);
        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.hotspot, null);
		mHotspotOpenPref = (TwoStatePreference) findPreference(KEY_HOTSPOT_OPEN);
	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		final String key = preference.getKey();
		if (key == null) {
			return super.onPreferenceTreeClick(preference);
		}
		switch (key) {
			case KEY_HOTSPOT_OPEN:
				if (mHotspotOpenPref.isChecked()) {
					mHotspotOpenPref.setChecked(false);
	 				startActivityForResult(new Intent(getContext(),HotspotSwitchActivity.class),100);		
				} else {
					if (mConnectivityManager == null) {
                		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                	}
					mConnectivityManager.stopTethering(TETHERING_WIFI);
					writeOption(PERSIST_HOTSPOT_OPEN, false);
					Log.d("SDMC","Colse hotspot");
				}

				return true;

	        case KEY_NETWORK_SETTINGS:
	            Log.d("SDMC","get KEY_NETWORK_SETTINGS");
	            startSetHotspot();
	            return true;
		}
		return super.onPreferenceTreeClick(preference);
	}


	private void startSetHotspot() {
		WifiManager mWifiManager = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);;
		Intent intent = new Intent(getContext(),HotspotSettingActivity.class);
		String ssid = mWifiManager.getWifiApConfiguration().SSID;
        String password = mWifiManager.getWifiApConfiguration().preSharedKey;
        Log.d("SDMC","read ssid = " + ssid + "   password = " + password);

        if (ssid == null) {
        	String sn = SystemProperties.get("ro.serialno",null);
        	if (sn == null) {
        		Random r = new Random();
        		int random = r.nextInt(9000) + 1000;
        		sn = String.valueOf(random);
        	} else {
        		sn = sn.substring(sn.length() - 4);
        	}
        	ssid = "AndroidAP" + sn;
        } 
        if (password == null) {
        	password = "123456789";
        }
	    intent.putExtra(KEY_HOTSPOT_SSID, ssid);
	    intent.putExtra(KEY_HOTSPOT_PASSWORD, password);
	    startActivityForResult(intent,200);   

	}


	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d("SDMC","HotspotFragment requestCode = " + requestCode 
    		+ "   resultCode = " + resultCode);
        if (requestCode == 100) {
        	if (resultCode == 1) {
        		writeOption(PERSIST_HOTSPOT_OPEN, true);
        		
                if (mConnectivityManager == null) {
                	mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                }
        		mConnectivityManager.startTethering(TETHERING_WIFI, true /* showProvisioningUi */,
                    mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
        	} 
        	
        } else if (requestCode == 200 && resultCode == 1) {
        	String ssid = data.getStringExtra(KEY_HOTSPOT_SSID);
        	String password = data.getStringExtra(KEY_HOTSPOT_PASSWORD);

        	WifiManager mWifiManager =(WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        	boolean result = mWifiManager.setWifiApConfiguration(buildNewConfig(ssid,password));
        	Log.d("SDMC","setWifiApConfiguration " + result);
        }
    }

	private void refresh() {
		mHotspotOpenPref.setChecked(readOption(PERSIST_HOTSPOT_OPEN));
	}

	private boolean readOption(String key) {
		return Settings.Global.getInt(getContext().getContentResolver(), key, 0) == 1;
	}

	private void writeOption(String key, boolean value) {
		Settings.Global.putInt(getContext().getContentResolver(), key, value ? 1 : 0);
	}

	 private WifiConfiguration buildNewConfig(String name,String password) {
         WifiConfiguration config = new WifiConfiguration();

        config.SSID = name == null ? "AndoirdAP" : name;

        if (password != null) {
        	config.preSharedKey = password;
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK);
        } else {
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
       
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        //config.apBand = WifiConfiguration.AP_BAND_2GHZ;
        Log.d("SDMC","buildNewConfig name = " + config.SSID + "   password = " + config.preSharedKey);
        return config;
    }

      private ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    Log.d("SDMC", "onTetheringFailed");
                    writeOption(PERSIST_HOTSPOT_OPEN, false);
                }

                @Override
                public void onTetheringStarted() {
                    super.onTetheringStarted();
                    Log.d("SDMC", "onTetheringStarted");
                }
            };
			
	@Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_CONDITION_HOTSPOT;
    }
}
