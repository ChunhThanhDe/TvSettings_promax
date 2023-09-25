package com.android.tv.settings.device.hotspot;

import android.app.Activity;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.util.Log;
import android.app.FragmentManager;
import com.android.tv.settings.R;

public class HotspotResultActivity extends Activity {
	private static final String TAG = "HotspotResultActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_result);
        SaveSuccessFragment fragment = SaveSuccessFragment
                .newInstance(getString(R.string.hotspot_settings_success_message));
        updateView(fragment,true);
    }

      @Override
    public void onBackPressed() {
        Log.d("SDMC", "onBackPressed: ");
        if(getFragmentManager().getBackStackEntryCount() == 0){  
             super.onBackPressed();  
        }else{  
             finishSelf(0);
        } 
        
    }
    

    public void finishSelf(int result) {
    	getFragmentManager().popBackStack();  
    	setResult(result);
        finish();
    }


    private void updateView(Fragment fragment, boolean movingForward) {
        if (fragment != null) {
            FragmentTransaction updateTransaction = getFragmentManager().beginTransaction();
            if (movingForward) {
                updateTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            } else {
                updateTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            }
            updateTransaction.replace(R.id.network_settings_container, fragment, TAG);
            updateTransaction.commit();
        }
    }
}
