/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.util.BuglePrefs;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NavUtils;

public class NudgeFriendsActivity extends BugleActionBarActivity {

    public static final String PREF_SHORT_NUDGE_MESSAGE = "short_nudge_message";
    public static final String PREF_LONG_NUDGE_MESSAGE  = "long_nudge_message";

    private static final String[] APPROVED_APPS = {
        "Signal", "Session", "SimpleX", "Briar", "Element"
    };

    private EditText mPrivacyAppField;
    private EditText mNudgeMessageField;
    private EditText mLongNudgeMessageField;
    private SharedPreferences mPrefs;

    private boolean mUpdatingMessage     = false;
    private boolean mNudgeMessageEdited  = false;
    private boolean mUpdatingLongMessage    = false;
    private boolean mLongMessageEdited      = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_friends);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.nudge_friends_title));

        mPrefs = getSharedPreferences(BuglePrefs.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        mPrivacyAppField      = findViewById(R.id.nudge_privacy_app_field);
        mNudgeMessageField    = findViewById(R.id.nudge_message_field);
        mLongNudgeMessageField = findViewById(R.id.nudge_long_message_field);

        // ── Short message: restore or generate default ───────────────────────
        final String savedShort = mPrefs.getString(PREF_SHORT_NUDGE_MESSAGE, null);
        if (savedShort != null) {
            mUpdatingMessage = true;
            mNudgeMessageField.setText(savedShort);
            mUpdatingMessage = false;
            mNudgeMessageEdited = true;
        } else {
            setShortNudgeMessage(mPrivacyAppField.getText().toString().trim());
        }

        // ── Long message: restore or generate default ────────────────────────
        final String savedLong = mPrefs.getString(PREF_LONG_NUDGE_MESSAGE, null);
        if (savedLong != null) {
            mUpdatingLongMessage = true;
            mLongNudgeMessageField.setText(savedLong);
            mUpdatingLongMessage = false;
            mLongMessageEdited = true;
        } else {
            setLongNudgeMessage(mPrivacyAppField.getText().toString().trim());
        }

        // ── App name field: warn if not on approved list (on Done or focus loss) ──
        mPrivacyAppField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard(mPrivacyAppField);
                maybeWarnUnapprovedApp();
                return true;
            }
            return false;
        });
        mPrivacyAppField.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                maybeWarnUnapprovedApp();
            }
        });

        // ── App name changes → sync both messages (if not manually edited) ───
        mPrivacyAppField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(final Editable s) {
                final String appName = s.toString().trim().isEmpty()
                        ? getString(R.string.nudge_privacy_app_default) : s.toString().trim();
                if (!mNudgeMessageEdited) {
                    setShortNudgeMessage(appName);
                }
                if (!mLongMessageEdited) {
                    setLongNudgeMessage(appName);
                }
            }
        });

        // ── Short message: track edits and persist ───────────────────────────
        mNudgeMessageField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(final Editable s) {
                if (!mUpdatingMessage) {
                    mNudgeMessageEdited = true;
                }
                mPrefs.edit().putString(PREF_SHORT_NUDGE_MESSAGE, s.toString()).apply();
            }
        });

        // ── Long message: track edits and persist ────────────────────────────
        mLongNudgeMessageField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(final Editable s) {
                if (!mUpdatingLongMessage) {
                    mLongMessageEdited = true;
                }
                mPrefs.edit().putString(PREF_LONG_NUDGE_MESSAGE, s.toString()).apply();
            }
        });

        // ── Short reset button ───────────────────────────────────────────────
        findViewById(R.id.nudge_reset_message_button).setOnClickListener(v -> {
            mNudgeMessageEdited = false;
            final String appName = resolvedAppName();
            setShortNudgeMessage(appName);
            mPrefs.edit().putString(PREF_SHORT_NUDGE_MESSAGE, buildShortNudgeMessage(appName)).apply();
        });

        // ── Long reset button ────────────────────────────────────────────────
        findViewById(R.id.nudge_reset_long_message_button).setOnClickListener(v -> {
            mLongMessageEdited = false;
            final String appName = resolvedAppName();
            setLongNudgeMessage(appName);
            mPrefs.edit().putString(PREF_LONG_NUDGE_MESSAGE, buildLongNudgeMessage(appName)).apply();
        });

        findViewById(R.id.nudge_tell_me_more_button).setOnClickListener(v -> showHelpDialog());
    }

    private boolean isApprovedApp(final String name) {
        final String normalized = name.trim();
        for (final String approved : APPROVED_APPS) {
            if (approved.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void maybeWarnUnapprovedApp() {
        final String entered = mPrivacyAppField.getText().toString().trim();
        if (!entered.isEmpty() && !isApprovedApp(entered)) {
            new AlertDialog.Builder(this)
                    .setMessage("Use open-source or nothing. Proprietary apps will flip on your "
                            + "privacy the moment a subpoena or profit motive hits their desk!")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void hideKeyboard(final View view) {
        final InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private String resolvedAppName() {
        final String name = mPrivacyAppField.getText().toString().trim();
        return name.isEmpty() ? getString(R.string.nudge_privacy_app_default) : name;
    }

    private void setShortNudgeMessage(final String appName) {
        mUpdatingMessage = true;
        mNudgeMessageField.setText(buildShortNudgeMessage(appName));
        mUpdatingMessage = false;
    }

    private void setLongNudgeMessage(final String appName) {
        mUpdatingLongMessage = true;
        mLongNudgeMessageField.setText(buildLongNudgeMessage(appName));
        mUpdatingLongMessage = false;
    }

    private String buildShortNudgeMessage(final String appName) {
        return "Reminder to contact me on " + appName
                + ", I will try to reply back briefly.";
    }

    private String buildLongNudgeMessage(final String appName) {
        return "SMS/MMS/RCS are insecure and exposed to Big Tech, carriers, and hackers.\n"
                + "Let\u2019s switch to " + appName + " for real privacy.\n"
                + "I\u2019ll keep replies here brief until we move our chat to " + appName + ".";
    }

    /**
     * Shows the privacy-warning help dialog. Bold is applied via setTypeface() at the View level
     * rather than via spans, which is guaranteed to render on all devices and themes.
     */
    private void showHelpDialog() {
        final float density = getResources().getDisplayMetrics().density;
        final int dp8  = (int) (8  * density);
        final int dp16 = (int) (16 * density);
        final int dp24 = (int) (24 * density);

        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);

        // ── Title (bold, larger) ────────────────────────────────────────────
        final TextView titleTv = makeTv(18, Typeface.DEFAULT_BOLD);
        titleTv.setText("\u26a0\ufe0f PRIVACY ALERT: ABANDON SMS, MMS, & RCS");
        container.addView(titleTv);
        container.addView(spacer(dp8));

        // ── Intro paragraph (inline bold on "RCS") ──────────────────────────
        final TextView introTv = makeTv(15, Typeface.DEFAULT);
        introTv.setText(buildIntroText());
        container.addView(introTv);
        container.addView(spacer(dp8));

        // ── Bullet items ────────────────────────────────────────────────────
        addBullet(container, "Cybercriminal target:",
                "Legacy protocols like SMS/MMS are playground for hackers, enabling SIM swapping,"
                + " phishing, and interception of your private codes and conversations.",
                dp8, dp24);

        addBullet(container, "Closed infrastructure:",
                "RCS is a \u201cwalled garden\u201d with zero transparency, making it prone to"
                + " provider-side exploits that you can neither see nor fix.",
                dp8, dp24);

        addBullet(container, "Data exploitation:",
                "Corporations harvest your metadata to fuel their own profit margins, turning your"
                + " private habits into a commodity.",
                dp8, dp24);

        addBullet(container, "Government compliance:",
                "These centralized entities are engineered to be compliant; they routinely crack"
                + " under government pressure, handing over your data via secret subpoenas without"
                + " a fight.",
                dp8, dp24);

        container.addView(spacer(dp8));

        // ── Closing sentence (all bold) ─────────────────────────────────────
        final TextView lastTv = makeTv(15, Typeface.DEFAULT_BOLD);
        lastTv.setText("Stop being the product and the victim."
                + " Switch to open-source, encrypted alternatives.");
        container.addView(lastTv);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        new AlertDialog.Builder(this)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Intro paragraph with "RCS" in bold inline.
     */
    private SpannableStringBuilder buildIntroText() {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("Standard messaging is a surveillance trap. SMS and MMS are fundamentally"
                + " broken, while ");
        final int start = sb.length();
        sb.append("RCS");
        sb.setSpan(new TypefaceSpan(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)),
                start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(1.0f), start, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" remains a proprietary \u201cblack box\u201d controlled by big tech and"
                + " carriers.");
        return sb;
    }

    /**
     * Adds a bullet item as two stacked TextViews: a bold label line and an indented normal body.
     */
    private void addBullet(final LinearLayout container, final String label, final String body,
            final int spacingPx, final int indentPx) {
        final TextView labelTv = makeTv(15, Typeface.DEFAULT_BOLD);
        labelTv.setText("\u2022 " + label);
        container.addView(labelTv);

        final LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.setMarginStart(indentPx);
        final TextView bodyTv = makeTv(15, Typeface.DEFAULT);
        bodyTv.setText(body);
        container.addView(bodyTv, bodyParams);
        container.addView(spacer(spacingPx));
    }

    private TextView makeTv(final int textSizeSp, final Typeface typeface) {
        final TextView tv = new TextView(this);
        tv.setTextSize(textSizeSp);
        tv.setTypeface(typeface);
        tv.setTextIsSelectable(true);
        return tv;
    }

    private View spacer(final int heightPx) {
        final View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightPx));
        return v;
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
