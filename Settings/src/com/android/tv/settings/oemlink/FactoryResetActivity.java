/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tv.settings.oemlink;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.tv.settings.overlay.FeatureFactory;

/**
 * Oemlink of Factory Reset that takes special mode into consideration before launching the real
 * ResetActivity.
 */
public class FactoryResetActivity extends Activity {

    private static final String TAG = "OemFDRLink";

    private static final int REQUEST_CODE_STARTUP_VERIFICATION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (!FeatureFactory.getFactory(this)
                .getStartupVerificationFeatureProvider()
                .startStartupVerificationActivityForResult(
                        this, REQUEST_CODE_STARTUP_VERIFICATION)) {
            startFactoryResetActivityAndFinish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_STARTUP_VERIFICATION) {
            if (resultCode == RESULT_OK) {
                startFactoryResetActivityAndFinish();
            } else {
                Log.e(TAG, "Reset activity startup verification cancelled or failed.");
                finish();
            }
        }
    }

    private void startFactoryResetActivityAndFinish() {
        Log.v(TAG, "Starting reset activity.");
        Intent i = new Intent();
        // ChunhThanhde change
        //i.setClassName(this, "com.android.tv.settings.device.storage.ResetActivity");
        i.setClassName(this, "com.android.tv.settings.vnptt.VnpttFactoryResetActivity");
        startActivity(i);
        finish();
    }
}
