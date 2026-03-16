/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NavUtils;

public class NudgeFriendsActivity extends BugleActionBarActivity {

    private EditText mPrivacyAppField;
    private EditText mNudgeMessageField;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_friends);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.nudge_friends_title));

        mPrivacyAppField = findViewById(R.id.nudge_privacy_app_field);
        mNudgeMessageField = findViewById(R.id.nudge_message_field);

        // Set initial nudge message using the default app name
        mNudgeMessageField.setText(buildNudgeMessage(mPrivacyAppField.getText().toString().trim()));

        // Reset button restores the default message using the current app name
        final Button resetButton = findViewById(R.id.nudge_reset_message_button);
        resetButton.setOnClickListener(v -> {
            final String appName = mPrivacyAppField.getText().toString().trim();
            mNudgeMessageField.setText(buildNudgeMessage(appName.isEmpty()
                    ? getString(R.string.nudge_privacy_app_default) : appName));
        });

        findViewById(R.id.nudge_tell_me_more_button).setOnClickListener(v -> showHelpDialog());
    }

    private String buildNudgeMessage(final String appName) {
        return "Reminder to contact me on " + appName
                + ", I will try to reply back briefly.";
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
        addBullet(container, "Cybercriminal Target:",
                "Legacy protocols like SMS/MMS are playground for hackers, enabling SIM swapping,"
                + " phishing, and interception of your private codes and conversations.",
                dp8, dp24);

        addBullet(container, "Closed Infrastructure:",
                "RCS is a \u201cwalled garden\u201d with zero transparency, making it prone to"
                + " provider-side exploits that you can neither see nor fix.",
                dp8, dp24);

        addBullet(container, "Data Exploitation:",
                "Corporations harvest your metadata to fuel their own profit margins, turning your"
                + " private habits into a commodity.",
                dp8, dp24);

        addBullet(container, "Government Compliance:",
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
