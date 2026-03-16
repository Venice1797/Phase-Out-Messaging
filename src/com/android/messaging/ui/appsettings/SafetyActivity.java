/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

        // Show the confirmation dialog when the user presses Done on the keyboard.
        mAutoDeleteDaysField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                hideKeyboard();
                maybeConfirmDaysChange();
                return true;
            }
            return false;
        });

        // Fallback: also trigger when focus leaves the field (e.g. user taps elsewhere).
        mAutoDeleteDaysField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                maybeConfirmDaysChange();
            }
        });

        findViewById(R.id.auto_delete_info_button).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.auto_delete_info_text)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
    }

    /**
     * If the toggle is ON and the days value has changed, prompt the user to confirm before
     * persisting and executing. Guards against double-triggering from the editor action +
     * subsequent focus-loss by checking whether the value actually differs from what's saved.
     */
    private void maybeConfirmDaysChange() {
        if (!mAutoDeleteSwitch.isChecked()) return;

        final int newDays = parseDays(mAutoDeleteDaysField.getText().toString());
        final int savedDays = mPrefs.getInt(AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS,
                AutoDeleteScheduler.DEFAULT_DAYS);

        if (newDays == savedDays) return; // Nothing changed.

        // Normalise the field to the parsed value before the dialog opens.
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
    }

    private void hideKeyboard() {
        final InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(mAutoDeleteDaysField.getWindowToken(), 0);
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
