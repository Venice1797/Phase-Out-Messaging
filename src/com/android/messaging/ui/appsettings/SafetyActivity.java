/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.util.AutoDeleteScheduler;
import com.android.messaging.util.BuglePrefs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;

public class SafetyActivity extends BugleActionBarActivity {

    private SwitchCompat mAutoDeleteSwitch;
    private EditText mAutoDeleteDaysField;
    private SharedPreferences mPrefs;

    // Guard against re-entrant setChecked() calls triggering the listener.
    private boolean mSettingSwitch = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.safety_title));

        mPrefs = getSharedPreferences(BuglePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mAutoDeleteSwitch = findViewById(R.id.auto_delete_switch);
        mAutoDeleteDaysField = findViewById(R.id.auto_delete_days_field);

        final boolean enabled = mPrefs.getBoolean(AutoDeleteScheduler.PREF_AUTO_DELETE_ENABLED, false);
        final int days = mPrefs.getInt(AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS,
                AutoDeleteScheduler.DEFAULT_DAYS);

        mAutoDeleteSwitch.setChecked(enabled);
        mAutoDeleteDaysField.setText(String.valueOf(days));
        mAutoDeleteDaysField.setEnabled(enabled);

        mAutoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSettingSwitch) return;

            if (isChecked) {
                final int currentDays = parseDays(mAutoDeleteDaysField.getText().toString());
                showConfirmation(currentDays,
                        /* onConfirm */ () -> {
                            mAutoDeleteDaysField.setEnabled(true);
                            mPrefs.edit()
                                    .putBoolean(AutoDeleteScheduler.PREF_AUTO_DELETE_ENABLED, true)
                                    .putInt(AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS, currentDays)
                                    .apply();
                            AutoDeleteScheduler.runNow(currentDays);
                            AutoDeleteScheduler.scheduleNext(this);
                        },
                        /* onCancel */ () -> {
                            mSettingSwitch = true;
                            mAutoDeleteSwitch.setChecked(false);
                            mSettingSwitch = false;
                        });
            } else {
                mAutoDeleteDaysField.setEnabled(false);
                mPrefs.edit()
                        .putBoolean(AutoDeleteScheduler.PREF_AUTO_DELETE_ENABLED, false)
                        .apply();
                AutoDeleteScheduler.cancel(this);
            }
        });

        mAutoDeleteDaysField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) return;
            if (!mAutoDeleteSwitch.isChecked()) return;

            final String text = mAutoDeleteDaysField.getText().toString();
            final int newDays = parseDays(text);
            final int savedDays = mPrefs.getInt(AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS,
                    AutoDeleteScheduler.DEFAULT_DAYS);

            if (newDays == savedDays) return; // No change — nothing to confirm.

            // Ensure the field shows a valid value while the dialog is open.
            mAutoDeleteDaysField.setText(String.valueOf(newDays));

            showConfirmation(newDays,
                    /* onConfirm */ () -> {
                        mPrefs.edit()
                                .putInt(AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS, newDays)
                                .apply();
                        AutoDeleteScheduler.runNow(newDays);
                        AutoDeleteScheduler.scheduleNext(this);
                    },
                    /* onCancel */ () ->
                            mAutoDeleteDaysField.setText(String.valueOf(savedDays)));
        });

        findViewById(R.id.auto_delete_info_button).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.auto_delete_info_text)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
    }

    /**
     * Show a confirmation dialog describing what will be deleted. Calls {@code onConfirm} if the
     * user accepts, {@code onCancel} if they dismiss.
     */
    private void showConfirmation(final int days, final Runnable onConfirm,
            final Runnable onCancel) {
        final String message = getResources().getQuantityString(
                R.plurals.auto_delete_confirm_message, days, days);
        new AlertDialog.Builder(this)
                .setTitle(R.string.auto_delete_section_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> onConfirm.run())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> onCancel.run())
                .setOnCancelListener(dialog -> onCancel.run())
                .show();
    }

    /** Parse the days field; falls back to DEFAULT_DAYS if empty or zero. */
    private int parseDays(final String text) {
        try {
            final int value = Integer.parseInt(text.trim());
            return value > 0 ? value : AutoDeleteScheduler.DEFAULT_DAYS;
        } catch (NumberFormatException e) {
            return AutoDeleteScheduler.DEFAULT_DAYS;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
