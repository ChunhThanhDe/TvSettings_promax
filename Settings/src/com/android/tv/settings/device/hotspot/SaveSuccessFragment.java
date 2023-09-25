package com.android.tv.settings.device.hotspot;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.FragmentManager;

/**
 * Fragment that shows network is successfully connected.
 */
public class SaveSuccessFragment extends MessageFragment {
    private static final int MSG_TIME_OUT = 1;
    private static final int TIME_OUT_MS = 3 * 1000;
    private static final String KEY_TIME_OUT_DURATION = "time_out_duration";
    private Handler mTimeoutHandler;

    /**
     * Get the fragment based on the title.
     *
     * @param title title of the fragment.
     * @return the fragment.
     */
    public static SaveSuccessFragment newInstance(String title) {
        SaveSuccessFragment fragment = new SaveSuccessFragment();
        Bundle args = new Bundle();
        addArguments(args, title, false);
        fragment.setArguments(args);
        return fragment;
    }


    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTimeoutHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_TIME_OUT:
                        Log.d("SDMC", "handleMessage: finish");
                        removeSelf();
                        break;
                    default:
                        break;
                }
            }
        };
        super.onCreate(savedInstanceState);
    }


    private void removeSelf() {
        ((HotspotResultActivity)getActivity()).finishSelf(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimeoutHandler.sendEmptyMessageDelayed(MSG_TIME_OUT,
                getArguments().getInt(KEY_TIME_OUT_DURATION, TIME_OUT_MS));
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimeoutHandler.removeMessages(MSG_TIME_OUT);
    }
}
