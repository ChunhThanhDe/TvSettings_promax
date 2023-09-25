package com.android.tv.settings.device.hotspot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.provider.Settings;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.content.Intent;
import android.net.ConnectivityManager;
import static android.net.ConnectivityManager.TETHERING_WIFI;

public class HotspotSettingActivity extends Activity {
    private static final String TAG = "HotspotSettingActivity";
    private String ssid;
    private String password;
    private boolean needPassword = true;
    private ConnectivityManager mConnectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SDMC","HotspotSettingActivity  onCreate");
        ssid = getIntent().getStringExtra(HotspotFragment.KEY_HOTSPOT_SSID);
        password = getIntent().getStringExtra(HotspotFragment.KEY_HOTSPOT_PASSWORD);
        GuidedStepFragment fragment = getStartDialog();
        GuidedStepFragment.addAsRoot(this, fragment, android.R.id.content);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void showResult() {
        startActivityForResult(new Intent(this,HotspotResultActivity.class),100);   
    }

    public String getSsid() {
        return ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public void setNeedPassword(boolean needPassword) {
        this.needPassword = needPassword;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SDMC"," requestCode = " + requestCode + "   resultCode = " + resultCode);
        if (requestCode == 100 && resultCode == 1) {
           setResult(1,getResult());
           GuidedStepFragment.getCurrentGuidedStepFragment(getFragmentManager()).finishGuidedStepFragments();
        }
    }

    public void closeOption() {
        writeOption(HotspotFragment.PERSIST_HOTSPOT_OPEN, false);
        if (mConnectivityManager == null) {
                mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        mConnectivityManager.stopTethering(TETHERING_WIFI);
        Log.d("SDMC","Colse hotspot ");
    }

    private GuidedStepFragment getStartDialog() {
        if (readOption(HotspotFragment.PERSIST_HOTSPOT_OPEN)) {
            return new HotspotRemindFragment();
        }

        return new HotspotSecurityFragment();
    }

    private Intent getResult() {
        Intent intent = new Intent();
        intent.putExtra(HotspotFragment.KEY_HOTSPOT_SSID, ssid);
        if (needPassword) {    
            intent.putExtra(HotspotFragment.KEY_HOTSPOT_PASSWORD, password);
        }
        Log.d("SDMC","set result = ssid = " + ssid + "   password = " + password);
        return intent;
    }

    private boolean readOption(String key) {
        return Settings.Global.getInt(getContentResolver(), key, 0) == 1;
    }

    private void writeOption(String key, boolean value) {
        Settings.Global.putInt(getContentResolver(), key, value ? 1 : 0);
    }
}
