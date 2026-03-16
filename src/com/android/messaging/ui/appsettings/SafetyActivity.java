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
import com.android.messaging.util.BuglePrefs;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NavUtils;

public class SafetyActivity extends BugleActionBarActivity {

    private static final String PREF_AUTO_DELETE_ENABLED = "auto_delete_enabled";
    private static final String PREF_AUTO_DELETE_DAYS = "auto_delete_days";
    private static final int DEFAULT_AUTO_DELETE_DAYS = 14;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.safety_title));

        final SharedPreferences prefs = getSharedPreferences(
                BuglePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        final SwitchCompat autoDeleteSwitch = findViewById(R.id.auto_delete_switch);
        final EditText autoDeleteDaysField = findViewById(R.id.auto_delete_days_field);

        final boolean autoDeleteEnabled = prefs.getBoolean(PREF_AUTO_DELETE_ENABLED, false);
        final int autoDeleteDays = prefs.getInt(PREF_AUTO_DELETE_DAYS, DEFAULT_AUTO_DELETE_DAYS);

        autoDeleteSwitch.setChecked(autoDeleteEnabled);
        autoDeleteDaysField.setText(String.valueOf(autoDeleteDays));
        autoDeleteDaysField.setEnabled(autoDeleteEnabled);

        autoDeleteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoDeleteDaysField.setEnabled(isChecked);
            prefs.edit().putBoolean(PREF_AUTO_DELETE_ENABLED, isChecked).apply();
        });

        autoDeleteDaysField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                final String text = autoDeleteDaysField.getText().toString();
                final int days = text.isEmpty() ? DEFAULT_AUTO_DELETE_DAYS
                        : Integer.parseInt(text);
                if (text.isEmpty()) {
                    autoDeleteDaysField.setText(String.valueOf(DEFAULT_AUTO_DELETE_DAYS));
                }
                prefs.edit().putInt(PREF_AUTO_DELETE_DAYS, days).apply();
            }
        });

        findViewById(R.id.auto_delete_info_button).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.auto_delete_info_text)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
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
