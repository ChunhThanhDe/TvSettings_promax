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

package com.android.tv.settings.about;

import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_CLASSIC;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_TWO_PANEL;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_VENDOR;
import static com.android.tv.settings.overlay.OverlayUtils.FLAVOR_X;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.R;
import com.android.tv.settings.overlay.OverlayUtils;

import java.util.List;

@Keep
public class RebootConfirmFragment extends GuidedStepFragment {

    private static final String ARG_SAFE_MODE = "RebootConfirmFragment.safe_mode";

    public static RebootConfirmFragment newInstance(boolean safeMode) {

        Bundle args = new Bundle(1);
        args.putBoolean(ARG_SAFE_MODE, safeMode);

        RebootConfirmFragment fragment = new RebootConfirmFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSelectedActionPosition(1);
    }

    @Override
    public @NonNull
    GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        if (getArguments().getBoolean(ARG_SAFE_MODE, false)) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.reboot_safemode_confirm),
                    getString(R.string.reboot_safemode_desc),
                    null,
                    getActivity().getDrawable(R.drawable.ic_warning_132dp)
            );
        } else {
            return new GuidanceStylist.Guidance(
                    getString(R.string.system_reboot_confirm),
                    null,
                    null,
                    getActivity().getDrawable(R.drawable.ic_warning_132dp)
            );
        }
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions,
            Bundle savedInstanceState) {
        final Context context = getActivity();
        if (getArguments().getBoolean(ARG_SAFE_MODE, false)) {
            actions.add(new GuidedAction.Builder(context)
                    .id(GuidedAction.ACTION_ID_OK)
                    .title(R.string.reboot_safemode_action)
                    .build());
        } else {
            actions.add(new GuidedAction.Builder(context)
                    .id(GuidedAction.ACTION_ID_OK)
                    .title(R.string.restart_button_label)
                    .build());
        }
        actions.add(new GuidedAction.Builder(context)
                .clickAction(GuidedAction.ACTION_ID_CANCEL)
                .build());
    }

    @Override
    public GuidanceStylist onCreateGuidanceStylist() {
        return new GuidanceStylist() {
            @Override
            public int onProvideLayoutId() {
                switch (OverlayUtils.getFlavor(getContext())) {
                    case FLAVOR_CLASSIC:
                    case FLAVOR_TWO_PANEL:
                        return R.layout.confirm_guidance;
                    case FLAVOR_X:
                    case FLAVOR_VENDOR:
                        return R.layout.confirm_guidance_x;
                    default:
                        return R.layout.confirm_guidance;
                }
            }
        };
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == GuidedAction.ACTION_ID_OK) {
            final boolean toSafeMode = getArguments().getBoolean(ARG_SAFE_MODE, false);
            final PowerManager pm =
                    (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    if (toSafeMode) {
                        pm.rebootSafeMode();
                    } else {
                        pm.reboot(null);
                    }
                    return null;
                }
            }.execute();
        } else {
            getFragmentManager().popBackStack();
        }
    }
}
