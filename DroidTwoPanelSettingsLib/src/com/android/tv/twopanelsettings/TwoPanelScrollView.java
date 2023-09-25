/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tv.twopanelsettings;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

/**
 * A horizontal scroll view that ignores left/right key events for scrolling.
 */
public class TwoPanelScrollView extends HorizontalScrollView {

    public TwoPanelScrollView(Context context) {
        super(context);
    }

    public TwoPanelScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean arrowScroll(int direction) {
        return false;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
        // Workaround b/161359022: ScrollView is the only ViewGroup that calls
        // children's addFocusables() in requestFocus(), this reveals a
        // leanback addFocusables() cash when clearFocus() calls getRootView()
        // .requestFocus(). The bug is fixed in aosp/1472549, but due to the
        // difficulty of backport androidx library changes to Q prebuilt:
        // avoid using FocusFinder to workaround.
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                if (child.requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
        }
        return false;
    }
}
