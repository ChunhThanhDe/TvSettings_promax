package com.android.tv.settings.vnptt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.R;
import com.android.tv.settings.MainSettings;

import java.util.List;

public class VnpttDevicePrefsActivity extends Activity {

    private static final String TAG = "VnpttDevicePrefsActivity";

    static VnpttUntils securityStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            GuidedStepFragment.addAsRoot(this, PasswordFragment.newInstance(), android.R.id.content);
        }
    }

    public static class PasswordFragment extends GuidedStepFragment {

        public static PasswordFragment newInstance() {
            return new PasswordFragment();
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.device_pref_security_title),
                    getString(R.string.device_pref_security_description),
                    null,
                    getContext().getDrawable(R.drawable.vnptt_shield_lock));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction.Builder passwordActionBuilder = new GuidedAction.Builder(getContext())
                    .id(GuidedAction.ACTION_ID_OK)
                    .title(R.string.device_pref_password_title)
                    .descriptionEditable(true)
                    .descriptionInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                    .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            actions.add(passwordActionBuilder.build());

            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                String password = action.getDescription().toString();
                if (password.equals(VnpttUntils.DEVICE_PREFS_PASSWORD)) {
                    Toast.makeText(getContext(), "Welcome ....", Toast.LENGTH_LONG).show();
                    securityStatus.setDevicePrefsOpenSecurity();
                    getActivity().finish();
                } else {
                    action.setDescription(null);
                    Toast.makeText(getContext(), "Sorry, password is incorrect ....", Toast.LENGTH_LONG).show();
                }
            } else if (action.getId() == GuidedAction.ACTION_ID_CANCEL) {
                getActivity().finish();
            } else {
                VnpttUntils.LogWtf(TAG, "Unknown action clicked");
            }
        }
    }
}