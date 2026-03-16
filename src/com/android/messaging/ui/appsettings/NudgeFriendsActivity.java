/*
 * PO Messaging
 */

package com.android.messaging.ui.appsettings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NavUtils;

public class NudgeFriendsActivity extends BugleActionBarActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nudge_friends);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.nudge_friends_title));

        findViewById(R.id.nudge_tell_me_more_button).setOnClickListener(v -> showHelpDialog());
    }

    /**
     * Shows the help dialog using a custom TextView so that SpannableStringBuilder formatting
     * (bold, size) is guaranteed to render — AlertDialog.setMessage() can strip spans internally.
     */
    private void showHelpDialog() {
        final int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        final TextView textView = new TextView(this);
        textView.setText(buildHelpText());
        textView.setTextSize(15);
        textView.setPadding(dp16, dp16, dp16, dp16);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);

        new AlertDialog.Builder(this)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Builds the formatted help text from the privacy warning document, preserving bold and
     * size formatting as it appeared in the original ODT source.
     *
     * Structure (matching the ODT):
     *   Heading  – ⚠️ PRIVACY ALERT: ABANDON SMS, MMS, & RCS  (bold, larger)
     *   Paragraph – intro with inline bold "RCS"
     *   Bullets  – bold label + normal body for each item
     *   Closing  – entirely bold call-to-action
     */
    private SpannableStringBuilder buildHelpText() {
        final SpannableStringBuilder sb = new SpannableStringBuilder();

        // ── Heading ──────────────────────────────────────────────────────────
        appendBold(sb, "\u26a0\ufe0f PRIVACY ALERT: ABANDON SMS, MMS, & RCS", 1.25f);
        sb.append("\n\n");

        // ── Intro paragraph ───────────────────────────────────────────────────
        sb.append("Standard messaging is a surveillance trap. SMS and MMS are fundamentally broken, while ");
        appendBold(sb, "RCS", 1f);
        sb.append(" remains a proprietary \u201cblack box\u201d controlled by big tech and carriers.");
        sb.append("\n\n");

        // ── Bullet list ───────────────────────────────────────────────────────
        appendBullet(sb, "Cybercriminal Target:",
                " Legacy protocols like SMS/MMS are playground for hackers, enabling SIM swapping,"
                + " phishing, and interception of your private codes and conversations.");

        appendBullet(sb, "Closed Infrastructure:",
                " RCS is a \u201cwalled garden\u201d with zero transparency, making it prone to"
                + " provider-side exploits that you can neither see nor fix.");

        appendBullet(sb, "Data Exploitation:",
                " Corporations harvest your metadata to fuel their own profit margins, turning your"
                + " private habits into a commodity.");

        appendBullet(sb, "Government Compliance:",
                " These centralized entities are engineered to be compliant; they routinely crack"
                + " under government pressure, handing over your data via secret subpoenas without"
                + " a fight.");

        sb.append("\n");

        // ── Closing call-to-action (entirely bold) ────────────────────────────
        appendBold(sb,
                "Stop being the product and the victim. Switch to open-source, encrypted alternatives.",
                1f);

        return sb;
    }

    /** Appends bold text, optionally at a relative size. */
    private static void appendBold(final SpannableStringBuilder sb, final String text,
            final float relativeSize) {
        final int start = sb.length();
        sb.append(text);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (relativeSize != 1f) {
            sb.setSpan(new RelativeSizeSpan(relativeSize), start, sb.length(),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** Appends a bullet item with a bold label and normal-weight body. */
    private static void appendBullet(final SpannableStringBuilder sb, final String label,
            final String body) {
        sb.append("\u2022 ");
        appendBold(sb, label, 1f);
        sb.append(body);
        sb.append("\n\n");
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
