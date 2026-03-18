/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.text.method.KeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.util.BuglePrefs;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NavUtils;

import java.util.ArrayList;
import java.util.List;

public class NudgeFriendsActivity extends BugleActionBarActivity {

    public static final String PREF_SHORT_NUDGE_MESSAGE  = "short_nudge_message";
    public static final String PREF_LONG_NUDGE_MESSAGE   = "long_nudge_message";
    public static final String PREF_AUTO_REPLY_ENABLED   = "auto_reply_enabled";
    public static final String PREF_AUTO_REPLY_SELECTION = "auto_reply_selection";
    public static final String PREF_AUTO_REPLY_CUSTOM    = "auto_reply_custom_message";
    /** Fully-resolved message text — read by the receive actions to send the reply. */
    public static final String PREF_AUTO_REPLY_TEXT      = "auto_reply_text";

    private static final String[] APPROVED_APPS = {
        "Signal", "Session", "SimpleX", "Briar", "Element"
    };

    private static final String[] AUTO_REPLY_TEMPLATES = {
        "<Auto Reply: I am not using SMS or MMS, please email me or contact me on %s>",
        "<Auto Reply: Messaging on my phone is disabled. Please email me or contact me on %s>",
        "<Auto Reply: Please do not send me messages on SMS, MMS, or RCS due to security"
                + " and privacy risks. Email me, or contact me on %s >"
    };
    private static final int AUTO_REPLY_CUSTOM_INDEX = 3;

    private EditText             mPrivacyAppField;
    private EditText             mNudgeMessageField;
    private EditText             mLongNudgeMessageField;
    private Switch               mAutoReplyToggle;
    private LinearLayout         mAutoReplyContent;
    private Spinner              mAutoReplySpinner;
    private EditText             mAutoReplyMessageField;
    private ArrayAdapter<String> mAutoReplyAdapter;
    private KeyListener          mAutoReplyFieldKeyListener;
    private int                  mAutoReplyFieldInputType;

    private boolean mUpdatingMessage     = false;
    private boolean mNudgeMessageEdited  = false;
    private boolean mUpdatingLongMessage = false;
    private boolean mLongMessageEdited   = false;
    private boolean mUpdatingSpinner     = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_friends);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.nudge_friends_title));

        mPrivacyAppField       = findViewById(R.id.nudge_privacy_app_field);
        mNudgeMessageField     = findViewById(R.id.nudge_message_field);
        mLongNudgeMessageField = findViewById(R.id.nudge_long_message_field);

        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();

        // ── Short message: restore or generate default ───────────────────────
        final String savedShort = prefs.getString(PREF_SHORT_NUDGE_MESSAGE, null);
        if (savedShort != null) {
            mUpdatingMessage = true;
            mNudgeMessageField.setText(savedShort);
            mUpdatingMessage = false;
            mNudgeMessageEdited = true;
        } else {
            setShortNudgeMessage(mPrivacyAppField.getText().toString().trim());
        }

        // ── Long message: restore or generate default ────────────────────────
        final String savedLong = prefs.getString(PREF_LONG_NUDGE_MESSAGE, null);
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

        // ── App name changes → sync nudge messages, auto-reply spinner, and resolved text ──
        mPrivacyAppField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(final Editable s) {
                // For nudge messages: empty → "Signal"
                final String nudgeAppName = s.toString().trim().isEmpty()
                        ? getString(R.string.nudge_privacy_app_default) : s.toString().trim();
                if (!mNudgeMessageEdited) {
                    setShortNudgeMessage(nudgeAppName);
                }
                if (!mLongMessageEdited) {
                    setLongNudgeMessage(nudgeAppName);
                }
                // For auto-reply: empty → literal placeholder
                if (mAutoReplyAdapter != null) {
                    final int currentSel = mAutoReplySpinner.getSelectedItemPosition();
                    final String autoReplyAppName = resolvedAutoReplyAppName();
                    mUpdatingSpinner = true;
                    mAutoReplyAdapter.clear();
                    mAutoReplyAdapter.addAll(buildAutoReplySpinnerItems(autoReplyAppName));
                    mAutoReplySpinner.setSelection(currentSel >= 0 ? currentSel : 0);
                    mUpdatingSpinner = false;
                    if (currentSel != AUTO_REPLY_CUSTOM_INDEX) {
                        updateAutoReplyMessageField(currentSel, autoReplyAppName,
                                BuglePrefs.getApplicationPrefs().getString(PREF_AUTO_REPLY_CUSTOM, ""));
                    }
                    saveResolvedAutoReplyText();
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
                BuglePrefs.getApplicationPrefs().putString(PREF_SHORT_NUDGE_MESSAGE, s.toString());
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
                BuglePrefs.getApplicationPrefs().putString(PREF_LONG_NUDGE_MESSAGE, s.toString());
            }
        });

        // ── Short reset button ───────────────────────────────────────────────
        findViewById(R.id.nudge_reset_message_button).setOnClickListener(v -> {
            mNudgeMessageEdited = false;
            final String appName = resolvedAppName();
            setShortNudgeMessage(appName);
            BuglePrefs.getApplicationPrefs().putString(
                    PREF_SHORT_NUDGE_MESSAGE, buildShortNudgeMessage(appName));
        });

        // ── Long reset button ────────────────────────────────────────────────
        findViewById(R.id.nudge_reset_long_message_button).setOnClickListener(v -> {
            mLongMessageEdited = false;
            final String appName = resolvedAppName();
            setLongNudgeMessage(appName);
            BuglePrefs.getApplicationPrefs().putString(
                    PREF_LONG_NUDGE_MESSAGE, buildLongNudgeMessage(appName));
        });

        findViewById(R.id.nudge_tell_me_more_button).setOnClickListener(v -> showHelpDialog());

        // ── Auto Reply section ───────────────────────────────────────────────
        setupAutoReply();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Auto Reply
    // ────────────────────────────────────────────────────────────────────────

    private void setupAutoReply() {
        mAutoReplyToggle       = findViewById(R.id.auto_reply_toggle);
        mAutoReplyContent      = findViewById(R.id.auto_reply_content);
        mAutoReplySpinner      = findViewById(R.id.auto_reply_spinner);
        mAutoReplyMessageField = findViewById(R.id.auto_reply_message_field);

        // Save original key listener and input type so we can restore them for Custom mode
        mAutoReplyFieldKeyListener = mAutoReplyMessageField.getKeyListener();
        mAutoReplyFieldInputType   = mAutoReplyMessageField.getInputType();

        // Blue toggle: checked=blue, unchecked=gray
        final int[][] switchStates = { { android.R.attr.state_checked }, {} };
        mAutoReplyToggle.setThumbTintList(
                new ColorStateList(switchStates, new int[]{ 0xFF006FFF, 0xFFBBBBBB }));
        mAutoReplyToggle.setTrackTintList(
                new ColorStateList(switchStates, new int[]{ 0x80006FFF, 0x80AAAAAA }));

        // Build and attach spinner adapter
        mAutoReplyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                buildAutoReplySpinnerItems(resolvedAutoReplyAppName()));
        mAutoReplyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mAutoReplySpinner.setAdapter(mAutoReplyAdapter);

        // Restore persisted state (set before attaching listeners)
        final BuglePrefs prefs   = BuglePrefs.getApplicationPrefs();
        final boolean enabled    = prefs.getBoolean(PREF_AUTO_REPLY_ENABLED, false);
        final int     selection  = prefs.getInt(PREF_AUTO_REPLY_SELECTION, 0);
        final String  custom     = prefs.getString(PREF_AUTO_REPLY_CUSTOM, "");

        mUpdatingSpinner = true;
        mAutoReplyToggle.setChecked(enabled);
        mAutoReplySpinner.setSelection(selection);
        mUpdatingSpinner = false;

        updateAutoReplyMessageField(selection, resolvedAutoReplyAppName(), custom);
        mAutoReplyContent.setAlpha(enabled ? 1.0f : 0.4f);
        mAutoReplySpinner.setEnabled(enabled);
        applyAutoReplyFieldEditState(selection, enabled);

        // Ensure the resolved text is always current when the screen is opened
        saveResolvedAutoReplyText();

        // Toggle listener
        mAutoReplyToggle.setOnCheckedChangeListener((btn, isChecked) -> {
            BuglePrefs.getApplicationPrefs().putBoolean(PREF_AUTO_REPLY_ENABLED, isChecked);
            mAutoReplyContent.setAlpha(isChecked ? 1.0f : 0.4f);
            mAutoReplySpinner.setEnabled(isChecked);
            applyAutoReplyFieldEditState(
                    mAutoReplySpinner.getSelectedItemPosition(), isChecked);
        });

        // Spinner listener
        mAutoReplySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (mUpdatingSpinner) return;
                BuglePrefs.getApplicationPrefs().putInt(PREF_AUTO_REPLY_SELECTION, pos);
                updateAutoReplyMessageField(pos, resolvedAutoReplyAppName(),
                        BuglePrefs.getApplicationPrefs().getString(PREF_AUTO_REPLY_CUSTOM, ""));
                applyAutoReplyFieldEditState(pos, mAutoReplyToggle.isChecked());
                saveResolvedAutoReplyText();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Custom message text watcher — persists and saves resolved text when Custom is selected
        mAutoReplyMessageField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(final Editable s) {
                if (mAutoReplySpinner.getSelectedItemPosition() == AUTO_REPLY_CUSTOM_INDEX) {
                    BuglePrefs.getApplicationPrefs().putString(PREF_AUTO_REPLY_CUSTOM, s.toString());
                    saveResolvedAutoReplyText();
                }
            }
        });
    }

    private List<String> buildAutoReplySpinnerItems(final String appName) {
        final List<String> items = new ArrayList<>();
        for (final String template : AUTO_REPLY_TEMPLATES) {
            items.add(String.format(template, appName));
        }
        items.add(getString(R.string.auto_reply_custom));
        return items;
    }

    private void updateAutoReplyMessageField(final int selection, final String appName,
            final String customText) {
        if (selection == AUTO_REPLY_CUSTOM_INDEX) {
            mAutoReplyMessageField.setText(customText);
        } else if (selection >= 0 && selection < AUTO_REPLY_TEMPLATES.length) {
            mAutoReplyMessageField.setText(
                    String.format(AUTO_REPLY_TEMPLATES[selection], appName));
        }
    }

    /**
     * Computes the effective auto-reply text from the current UI state and writes it to
     * PREF_AUTO_REPLY_TEXT so the receive actions can read it without knowing about templates.
     */
    private void saveResolvedAutoReplyText() {
        final int sel = mAutoReplySpinner.getSelectedItemPosition();
        final String text;
        if (sel == AUTO_REPLY_CUSTOM_INDEX) {
            // Wrap so the receiver can detect it as an auto-reply and break mutual loops
            text = "<Auto Reply: " + mAutoReplyMessageField.getText().toString() + ">";
        } else if (sel >= 0 && sel < AUTO_REPLY_TEMPLATES.length) {
            text = String.format(AUTO_REPLY_TEMPLATES[sel], resolvedAutoReplyAppName());
        } else {
            text = "";
        }
        BuglePrefs.getApplicationPrefs().putString(PREF_AUTO_REPLY_TEXT, text);
    }

    private void applyAutoReplyFieldEditState(final int selection, final boolean toggleEnabled) {
        if (!toggleEnabled) {
            // Toggle OFF: grey out entirely, no interaction
            mAutoReplyMessageField.setEnabled(false);
            mAutoReplyMessageField.setTextIsSelectable(false);
            mAutoReplyMessageField.setFocusable(false);
            mAutoReplyMessageField.setFocusableInTouchMode(false);
            mAutoReplyMessageField.setCursorVisible(false);
            return;
        }
        mAutoReplyMessageField.setEnabled(true);
        if (selection == AUTO_REPLY_CUSTOM_INDEX) {
            // Custom: fully editable — restore original key listener and input type
            mAutoReplyMessageField.setKeyListener(mAutoReplyFieldKeyListener);
            mAutoReplyMessageField.setInputType(mAutoReplyFieldInputType);
            mAutoReplyMessageField.setTextIsSelectable(false);
            mAutoReplyMessageField.setFocusable(true);
            mAutoReplyMessageField.setFocusableInTouchMode(true);
            mAutoReplyMessageField.setCursorVisible(true);
        } else {
            // Default template: read-only but selectable so user can copy the text
            mAutoReplyMessageField.setKeyListener(null);
            mAutoReplyMessageField.setTextIsSelectable(true);
            mAutoReplyMessageField.setFocusable(true);
            mAutoReplyMessageField.setFocusableInTouchMode(true);
            mAutoReplyMessageField.setCursorVisible(false);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Existing helpers
    // ────────────────────────────────────────────────────────────────────────

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

    /** For nudge messages: empty field falls back to the default app name ("Signal"). */
    private String resolvedAppName() {
        final String name = mPrivacyAppField.getText().toString().trim();
        return name.isEmpty() ? getString(R.string.nudge_privacy_app_default) : name;
    }

    /**
     * For auto-reply templates: empty field falls back to "Signal" so the message is always
     * complete and sendable.
     */
    private String resolvedAutoReplyAppName() {
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
