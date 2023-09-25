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
import android.app.Fragment;
import android.app.FragmentManager;

import java.util.List;

public class HotspotRemindFragment extends GuidedStepFragment {
   
    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getString(R.string.hotspot_settings_open_hotspot_title),
                getString(R.string.hotspot_settings_open_hotspot_message),
                null,
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_YES)
                .title(getString(R.string.hotspot_comfirm_yes)).build());
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_NO)
                .title(getString(R.string.hotspot_comfirm_no)).build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        FragmentManager fm = getFragmentManager();
        GuidedStepFragment next; 
        if (action.getId() == GuidedAction.ACTION_ID_YES) {
                ((HotspotSettingActivity)getActivity()).closeOption();
                next = new HotspotSecurityFragment();
                GuidedStepFragment.add(fm, next);
        } else {
             getActivity().finish();
        }
       
    }
}
