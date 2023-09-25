/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tv.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.transition.Scene;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import android.provider.Settings;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.ComponentName;
import java.lang.reflect.Method;

import com.android.tv.settings.tvoption.TvOptionSettingManager;
import com.android.tv.settings.tvoption.SoundParameterSettingManager;
import com.android.tv.settings.soundeffect.OptionParameterManager;
import com.droidlogic.app.DataProviderManager;
import com.droidlogic.app.tv.AudioEffectManager;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.SharedPreferencesLogger;
import com.android.tv.settings.overlay.FeatureFactory;

public abstract class TvSettingsActivity extends Activity {
    private static final String TAG = "TvSettingsActivity";

    private static final String SETTINGS_FRAGMENT_TAG =
            "com.android.tv.settings.MainSettings.SETTINGS_FRAGMENT";

    private static final int REQUEST_CODE_STARTUP_VERIFICATION = 1;

    public static final String INTENT_ACTION_FINISH_FRAGMENT = "action.finish.droidsettingsmodefragment";
    public static final int MODE_LAUNCHER = 0;
    public static final int MODE_LIVE_TV = 1;
    private int mStartMode = MODE_LAUNCHER;
    private SoundParameterSettingManager mSoundParameterSettingManager = null;
    private OptionParameterManager mOptionParameterManager = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {

            final Fragment fragment = createSettingsFragment();
            if (fragment == null) {
                return;
            }
            if (FeatureFactory.getFactory(this).isTwoPanelLayout()) {
                if (isStartupVerificationRequired()) {
                    if (FeatureFactory.getFactory(this)
                            .getStartupVerificationFeatureProvider()
                            .startStartupVerificationActivityForResult(
                                    this, REQUEST_CODE_STARTUP_VERIFICATION)) {
                        return;
                    }
                }
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in,
                                android.R.animator.fade_out)
                        .add(android.R.id.content, fragment, SETTINGS_FRAGMENT_TAG)
                        .commitNow();
                return;
            }

            final ViewGroup root = findViewById(android.R.id.content);
            root.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            root.getViewTreeObserver().removeOnPreDrawListener(this);
                            final Scene scene = new Scene(root);
                            scene.setEnterAction(() -> {
                                if (getFragmentManager().isStateSaved()
                                        || getFragmentManager().isDestroyed()) {
                                    Log.d(TAG, "Got torn down before adding fragment");
                                    return;
                                }
                                getFragmentManager().beginTransaction()
                                        .add(android.R.id.content, fragment,
                                                SETTINGS_FRAGMENT_TAG)
                                        .commitNow();
                            });

                            final Slide slide = new Slide(Gravity.END);
                            slide.setSlideFraction(
                                    getResources().getDimension(R.dimen.lb_settings_pane_width)
                                            / root.getWidth());
                            TransitionManager.go(scene, slide);

                            // Skip the current draw, there's nothing in it
                            return false;
                        }
                    });
        }

        mStartMode = getIntent().getIntExtra("from_live_tv", MODE_LAUNCHER);
        Log.d(TAG, "mStartMode : " + mStartMode);
        if (SettingsConstant.needDroidlogicCustomization(this)) {
            init(this);
            if (mStartMode == MODE_LIVE_TV) {
                startShowActivityTimer();
            }
        }
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "intent = " + intent);
            switch (intent.getAction()) {
                case INTENT_ACTION_FINISH_FRAGMENT:
                    startShowActivityTimer();
                    break;
                case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                    finish();
                    break;
            }
        }
    };
    public void registerSomeReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_ACTION_FINISH_FRAGMENT);
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, intentFilter);
    }

    public void unregisterSomeReceivers() {
        unregisterReceiver(mReceiver);
    }
    public void startShowActivityTimer () {
        handler.removeMessages(0);

        int seconds = DataProviderManager.getIntValue(this, TvOptionSettingManager.KEY_MENU_TIME,
                TvOptionSettingManager.DEFAULT_MENU_TIME);
        if (seconds == 1) {
            seconds = 15;
        } else if (seconds == 2) {
            seconds = 30;
        } else if (seconds == 3) {
            seconds = 60;
        } else if (seconds == 4) {
            seconds = 120;
        } else if (seconds == 5) {
            seconds = 240;
        } else {
            seconds = 0;
        }
        if (seconds > 0) {
            handler.sendEmptyMessageDelayed(0, seconds * 1000);
        } else {
            handler.removeMessages(0);
        }
    }
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            finish();
        }
    };
    @Override
    public void onResume() {
        registerSomeReceivers();
        if (SettingsConstant.needDroidlogicCustomization(this)) {
            if (mStartMode == MODE_LIVE_TV) {
                startShowActivityTimer();
            }
        }
        super.onResume();
    }
    @Override
    public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_BACK:
                    if (mStartMode == MODE_LIVE_TV) {
                        Log.d(TAG, "dispatchKeyEvent");
                        startShowActivityTimer();
                    }
                    break;
                default:
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }
    public void finish() {
        final Fragment fragment = getFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_TAG);
        if (FeatureFactory.getFactory(this).isTwoPanelLayout()) {
            super.finish();
            return;
        }

        if (isResumed() && fragment != null) {
            final ViewGroup root = findViewById(android.R.id.content);
            final Scene scene = new Scene(root);
            scene.setEnterAction(() -> getFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitNow());
            final Slide slide = new Slide(Gravity.END);
            slide.setSlideFraction(
                    getResources().getDimension(R.dimen.lb_settings_pane_width) / root.getWidth());
            slide.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    getWindow().setDimAmount(0);
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    transition.removeListener(this);
                    TvSettingsActivity.super.finish();
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });
            TransitionManager.go(scene, slide);
        } else {
            super.finish();
        }
    }

    private void init(Context context) {
        mSoundParameterSettingManager = new SoundParameterSettingManager(context);
        mOptionParameterManager = new OptionParameterManager(context);
        getAudioEffectManager();
    }
    public AudioEffectManager getAudioEffectManager() {
        return AudioEffectManager.getInstance(getApplicationContext());
    }
    public SoundParameterSettingManager getSoundParameterSettingManager() {
        if (mSoundParameterSettingManager == null) {
            mSoundParameterSettingManager = new SoundParameterSettingManager(this);
        }
        return mSoundParameterSettingManager;
    }
    public OptionParameterManager getOptionParameterManager() {
        if (mOptionParameterManager == null) {
            mOptionParameterManager = new OptionParameterManager(this);
        }
        return mOptionParameterManager;
    }

    protected abstract Fragment createSettingsFragment();

    /**
     * Subclass may override this to return true to indicate that the Activity may only be started
     * after some verification. Example: in kids mode, we need to challenge the user with adult
     * re-auth before launching account settings.
     *
     * This only works in two panel style as we do not have features requiring the startup
     * verification in classic one panel style.
     * TODO: make this more explicit to TvSettings' "flavor" instead of 1 panel vs 2 panels.
     */
    protected boolean isStartupVerificationRequired() {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_STARTUP_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                Log.v(TAG, "Startup verification succeeded.");
                if (FeatureFactory.getFactory(this).isTwoPanelLayout()) {
                    if (createSettingsFragment() == null) {
                        Log.e(TAG, "Fragment is null.");
                        finish();
                        return;
                    }
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(
                                    android.R.animator.fade_in, android.R.animator.fade_out)
                            .add(
                                    android.R.id.content,
                                    createSettingsFragment(),
                                    SETTINGS_FRAGMENT_TAG)
                            .commitNow();
                }
            } else {
                Log.v(TAG, "Startup verification cancelled or failed.");
                finish();
            }
        }
    }

    private String getMetricsTag() {
        String tag = getClass().getName();
        if (tag.startsWith("com.android.tv.settings.")) {
            tag = tag.replace("com.android.tv.settings.", "");
        }
        return tag;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag(),
                    new MetricsFeatureProvider());
        }
        return super.getSharedPreferences(name, mode);
    }
}
