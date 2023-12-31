/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tv.settings;

import android.app.Fragment;

import com.android.tv.settings.overlay.FeatureFactory;
import android.view.KeyEvent;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Main settings which loads up the top level headers.
 */
public class MainSettings extends TvSettingsActivity {

    @Override
    protected Fragment createSettingsFragment() {
        return FeatureFactory.getFactory(this).getSettingsFragmentProvider()
            .newSettingsFragment(MainFragment.class.getName(), null);
    }

    @Override
    protected boolean isStartupVerificationRequired() {
        return true;
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
			Intent intent=new Intent();
			intent.setClassName("com.android.tv.settings","com.android.tv.settings.more.MorePrefFragmentActivity");
			startActivity(intent);
		}

		return super.onKeyDown(keyCode, event);
	}
}
