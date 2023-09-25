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
import androidx.leanback.widget.GuidedActionsStylist;
import android.util.Log;
import android.view.View;
import com.android.tv.settings.R;
import android.app.Fragment;
import android.app.FragmentManager;
import android.text.InputType;

import java.util.List;

public class HotspotPasswordFragment extends GuidedStepFragment {
    private GuidedAction mAction;
   
    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getString(R.string.hotspot_settings_password_title),
                getString(R.string.hotspot_settings_password_message),
                null,
                null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final Context context = getContext();
        mAction = new GuidedAction.Builder(context)
                .title(((HotspotSettingActivity)getActivity()).getPassword())
                .editInputType(InputType.TYPE_CLASS_TEXT)
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .editable(true)
                .build();
        actions.add(mAction);
    }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            openInEditMode(mAction);
        }


    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action){

        if (action.getId() == GuidedAction.ACTION_ID_CONTINUE) {
            String password = action.getTitle().toString();
            Log.d("SDMC", " password = " + password);
            if (password.length() >= 8) {
                ((HotspotSettingActivity)getActivity()).setPassword(password);
                ((HotspotSettingActivity)getActivity()).showResult();
            }
            
        }
    
        return action.getId();
    }
}
