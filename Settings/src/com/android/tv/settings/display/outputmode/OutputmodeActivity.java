/*
 * Copyright (c) 2014 Amlogic, Inc. All rights reserved.
 *
 * This source code is subject to the terms and conditions defined in the
 * file 'LICENSE' which is part of this source code package.
 *
 * Description:
 *     AMLOGIC OutputmodeActivity
 */

package com.android.tv.settings.display.outputmode;

import com.android.tv.settings.BaseSettingsFragment;
import com.android.tv.settings.DroidLogicTvSettingsActivity;

import android.app.Fragment;

/**
 * Activity to control Output mode settings.
 */
public class OutputmodeActivity extends DroidLogicTvSettingsActivity {

    @Override
    protected Fragment createSettingsFragment() {
        return SettingsFragment.newInstance();
    }

    public static class SettingsFragment extends BaseSettingsFragment {

        public static SettingsFragment newInstance() {
            return new SettingsFragment();
        }

        @Override
        public void onPreferenceStartInitialScreen() {
            final OutputmodeFragment fragment = OutputmodeFragment.newInstance();
            startPreferenceFragment(fragment);
        }
    }
}


