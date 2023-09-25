package com.android.tv.settings.device.hotspot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

public class HotspotSwitchActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HotspotSwitchFragment fragment = new HotspotSwitchFragment();
        fragment.setCallback(new HotspotSwitchFragment.Callback() {
            @Override
            public void onEnableConfirm(int result) {
                if (result == HotspotFragment.RESULT_OK) {
                    setResult(HotspotFragment.RESULT_OK);
                }
                finish();
            }
        });
        GuidedStepFragment.addAsRoot(this, fragment, android.R.id.content);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
