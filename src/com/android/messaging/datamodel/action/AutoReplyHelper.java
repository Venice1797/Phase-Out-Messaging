/*
 * PO Messaging
 */

package com.android.messaging.datamodel.action;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.ui.appsettings.NudgeFriendsActivity;
import com.android.messaging.util.BuglePrefs;

/**
 * Evaluates all guards and fires an auto-reply SMS when appropriate.
 * Called from ReceiveSmsMessageAction and ReceiveMmsMessageAction.
 *
 * Guards (any failure → no reply):
 *   1. Auto-reply is enabled and resolved text is set
 *   2. Incoming text does not start with "<Auto Reply:" (loop prevention)
 *   3. Sender is not the unknown-sender placeholder
 *   4. Sender is not our own number (self-send across SIMs)
 *   5. Sender address is a standard dialable phone number (not a short code or alpha sender ID)
 */
final class AutoReplyHelper {

    /**
     * @param subId         SIM subscription ID that received the message.
     * @param senderAddress Raw sender address from the incoming message.
     * @param self          Self participant for the receiving SIM.
     * @param incomingText  Body text of the incoming message, or null if unavailable (MMS).
     */
    static void maybeSendAutoReply(
            final int subId,
            final String senderAddress,
            final ParticipantData self,
            final String incomingText) {

        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();

        // Guard 1: feature enabled and text configured
        if (!prefs.getBoolean("auto_reply_enabled", false)) return;
        final String replyText = prefs.getString("auto_reply_text", "");
        if (TextUtils.isEmpty(replyText)) return;

        // Guard 2: don't reply to auto-replies (breaks mutual-loop between two devices)
        if (incomingText != null && incomingText.startsWith("<Auto Reply:")) return;
        if (incomingText != null && incomingText.startsWith("[AI Automated]")) return;

        // Guard 3: don't reply to unknown sender
        if (senderAddress.equals(ParticipantData.getUnknownSenderDestination())) return;

        // Guard 4: don't reply to our own number
        if (isSelfAddress(senderAddress, self)) return;

        // Guard 5: don't reply to short codes or alpha sender IDs
        if (!isDialablePhoneNumber(senderAddress)) return;

        // Guard 6: audience filter (everyone / contacts only / unknowns only)
        final int audience = prefs.getInt(
                NudgeFriendsActivity.PREF_AUTO_REPLY_AUDIENCE,
                NudgeFriendsActivity.AUDIENCE_EVERYONE);
        if (audience != NudgeFriendsActivity.AUDIENCE_EVERYONE) {
            final Context ctx = Factory.get().getApplicationContext();
            final boolean inContacts = isInContacts(ctx, senderAddress);
            if (audience == NudgeFriendsActivity.AUDIENCE_CONTACTS && !inContacts) return;
            if (audience == NudgeFriendsActivity.AUDIENCE_UNKNOWNS  &&  inContacts) return;
        }

        InsertNewMessageAction.insertNewMessage(subId, senderAddress, replyText, null);
    }

    /**
     * Returns true if the sender normalises to the same number as the receiving SIM.
     */
    private static boolean isSelfAddress(final String address, final ParticipantData self) {
        final String selfDest = self.getNormalizedDestination();
        if (TextUtils.isEmpty(selfDest)) return false;
        final ParticipantData senderData =
                ParticipantData.getFromRawPhoneBySimLocale(address, self.getSubId());
        return selfDest.equals(senderData.getNormalizedDestination());
    }

    /**
     * Returns true if the given phone number matches any entry in the device contacts.
     * Uses ContactsContract.PhoneLookup for normalised matching.
     */
    private static boolean isInContacts(final Context ctx, final String address) {
        final Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
        try (Cursor c = ctx.getContentResolver().query(
                lookupUri,
                new String[]{ ContactsContract.PhoneLookup._ID },
                null, null, null)) {
            return c != null && c.moveToFirst();
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Returns true only for standard dialable phone numbers.
     * Rejects alpha sender IDs (contain letters) and short codes (fewer than 7 digits).
     */
    private static boolean isDialablePhoneNumber(final String address) {
        if (TextUtils.isEmpty(address)) return false;
        // Alpha sender IDs contain at least one letter
        for (int i = 0; i < address.length(); i++) {
            if (Character.isLetter(address.charAt(i))) return false;
        }
        // Count digits only; short codes have fewer than 7
        int digits = 0;
        for (int i = 0; i < address.length(); i++) {
            if (Character.isDigit(address.charAt(i))) digits++;
        }
        return digits >= 7;
    }

    private AutoReplyHelper() {}
}
