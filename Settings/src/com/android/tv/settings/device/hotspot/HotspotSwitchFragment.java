/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tv.settings.device.hotspot;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import android.util.Log;
import com.android.tv.settings.R;

import java.util.List;

public class HotspotSwitchFragment extends GuidedStepFragment {

    private Callback mCallback;
    
    public interface Callback {
        void onEnableConfirm(int result);
    }


    public void setCallback(Callback callback) {
        mCallback = callback;
    }


    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getString(R.string.hotspot_switch_open_hotspot_title),
                getString(R.string.hotspot_switch_open_hotspot_message),
                null,
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_YES).build());
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_NO).build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == GuidedAction.ACTION_ID_YES) {
             if (mCallback != null) {
                 mCallback.onEnableConfirm(HotspotFragment.RESULT_OK);
             }
            getFragmentManager().popBackStack();
        } else {
             if (mCallback != null) {
                 mCallback.onEnableConfirm(HotspotFragment.RESULT_CANCEL);
             }
            getFragmentManager().popBackStack();
        }
    }
}
