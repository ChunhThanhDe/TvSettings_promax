package com.android.tv.settings.vnptt;


import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Log;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import com.android.tv.settings.R;
import com.android.tv.settings.device.storage.ResetActivity;

import java.util.List;

public class VnpttFactoryResetActivity extends Activity {

    private static final String TAG = "VnpttFactoryResetActivity";

    private static VnpttUntils securitySettings;

    /**
     * Support for shutdown-after-reset. If our launch intent has a true value for
     * the boolean extra under the following key, then include it in the intent we
     * use to trigger a factory reset. This will cause us to shut down instead of
     * restart after the reset.
     */
    private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        VnpttUntils.LogDebug(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        securitySettings = VnpttUntils.getInstance(this);

        if (savedInstanceState == null) {
            GuidedStepFragment.addAsRoot(this, VnpttFactoryResetActivity.ResetFragment.newInstance(), android.R.id.content);
        }
    }

    public static class ResetFragment extends GuidedStepFragment {

        public static VnpttFactoryResetActivity.ResetFragment newInstance() {

            Bundle args = new Bundle();

            VnpttFactoryResetActivity.ResetFragment fragment = new VnpttFactoryResetActivity.ResetFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            VnpttUntils.LogDebug(TAG, "onCreateGuidance");
            return new GuidanceStylist.Guidance(
                    getString(R.string.device_reset),
                    getString(R.string.factory_reset_description),
                    null,
                    getContext().getDrawable(R.drawable.ic_settings_backup_restore_132dp));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            VnpttUntils.LogDebug(TAG, "onCreateActions");
            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .build());
            if (securitySettings.getSettingsSecurityStatus()) {
                GuidedAction.Builder passwordActionBuilder = new GuidedAction.Builder(getContext())
                        .id(GuidedAction.ACTION_ID_OK)
                        .title(R.string.device_reset)
                        .descriptionEditable(true)
                        .descriptionInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                        .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                actions.add(passwordActionBuilder.build());
            } else {
                actions.add(new GuidedAction.Builder(getContext())
                        .clickAction(GuidedAction.ACTION_ID_OK)
                        .title(getString(R.string.device_reset))
                        .build());
            }

        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            VnpttUntils.LogDebug(TAG, "onGuidedActionClicked " + action.getTitle());
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                if (securitySettings.getSettingsSecurityStatus()) {
                    String password = action.getDescription().toString();
                    VnpttUntils.LogWtf(TAG, "Password " + password);
                    if (password.equals(VnpttUntils.FACTORY_PASSWORD)) {
                        add(getFragmentManager(), VnpttFactoryResetActivity.ResetConfirmFragment.newInstance());
                    }
                } else {
                    add(getFragmentManager(), ResetActivity.ResetConfirmFragment.newInstance());
                }
            } else if (action.getId() == GuidedAction.ACTION_ID_CANCEL) {
                getActivity().finish();
            } else {
                VnpttUntils.LogWtf(TAG, "Unknown action clicked");
            }
        }
    }

    public static class ResetConfirmFragment extends GuidedStepFragment {

        public static VnpttFactoryResetActivity.ResetConfirmFragment newInstance() {

            Bundle args = new Bundle();

            VnpttFactoryResetActivity.ResetConfirmFragment fragment = new VnpttFactoryResetActivity.ResetConfirmFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.device_reset),
                    getString(R.string.confirm_factory_reset_description),
                    null,
                    getContext().getDrawable(R.drawable.ic_settings_backup_restore_132dp));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions,
                                    Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_CANCEL)
                    .build());
            actions.add(new GuidedAction.Builder(getContext())
                    .clickAction(GuidedAction.ACTION_ID_OK)
                    .title(getString(R.string.confirm_factory_reset_device))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == GuidedAction.ACTION_ID_OK) {
                if (ActivityManager.isUserAMonkey()) {
                    VnpttUntils.LogVerbose(TAG, "Monkey tried to erase the device. Bad monkey, bad!");
                    getActivity().finish();
                } else {
                    performFactoryReset();
                }
            } else if (action.getId() == GuidedAction.ACTION_ID_CANCEL) {
                getActivity().finish();
            } else {
                VnpttUntils.LogWtf(TAG, "Unknown action clicked");
            }
        }

        private void performFactoryReset() {
            final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager) getContext().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);

            if (shouldWipePersistentDataBlock(pdbManager)) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        pdbManager.wipe();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        doMainClear();
                    }
                }.execute();
            } else {
                doMainClear();
            }
        }

        private boolean shouldWipePersistentDataBlock(PersistentDataBlockManager pdbManager) {
            if (pdbManager == null) {
                return false;
            }
            // If OEM unlock is allowed, the persistent data block will be wiped during FR.
            // If disabled, it will be wiped here instead.
            if (((OemLockManager) getActivity().getSystemService(Context.OEM_LOCK_SERVICE))
                    .isOemUnlockAllowed()) {
                return false;
            }
            return true;
        }

        private void doMainClear() {
            if (getActivity() == null) {
                return;
            }
            Intent resetIntent = new Intent(Intent.ACTION_FACTORY_RESET);
            resetIntent.setPackage("android");
            resetIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            resetIntent.putExtra(Intent.EXTRA_REASON, "ResetConfirmFragment");
            if (getActivity().getIntent().getBooleanExtra(SHUTDOWN_INTENT_EXTRA, false)) {
                resetIntent.putExtra(SHUTDOWN_INTENT_EXTRA, true);
            }
            getActivity().sendBroadcastAsUser(resetIntent, UserHandle.SYSTEM);
        }
    }
}
