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
package com.android.tv.settings.about;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

/** An {@InfoFragment} that hosts preview pane of factory reset. */
@Keep
public class FactoryResetInfoFragment extends InfoFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("ChunhThanhde", "onCreateView: chung thhanhde");
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ((ImageView) view.findViewById(R.id.info_title_icon))
                .setImageResource(R.drawable.ic_info_outline_base);
        view.findViewById(R.id.info_title_icon).setVisibility(View.VISIBLE);
        ((TextView) view.findViewById(R.id.info_summary))
                .setText(R.string.factory_reset_info_description);
        view.findViewById(R.id.info_summary).setVisibility(View.VISIBLE);

        return view;
    }
    @Override
    public void updateInfoFragment() {
        // No-op as this is hosting a static info preview panel
    }
}
