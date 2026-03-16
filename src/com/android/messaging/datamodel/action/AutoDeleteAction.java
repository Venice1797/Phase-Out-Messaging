/*
 * PO Messaging
 */

package com.android.messaging.datamodel.action;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DataModelException;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Action that deletes all messages older than a given cutoff timestamp from both the
 * local database and the system telephony provider. Empty conversations/threads are
 * removed automatically after their messages are gone.
 */
public class AutoDeleteAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    private static final String KEY_CUTOFF_TIMESTAMP = "auto_delete_cutoff_ms";

    /** Trigger auto-deletion of messages older than {@code cutoffTimestampMs}. */
    public static void run(final long cutoffTimestampMs) {
        new AutoDeleteAction(cutoffTimestampMs).start();
    }

    private AutoDeleteAction(final long cutoffTimestampMs) {
        super();
        actionParameters.putLong(KEY_CUTOFF_TIMESTAMP, cutoffTimestampMs);
    }

    @Override
    protected Object executeAction() {
        requestBackgroundWork();
        return null;
    }

    @Override
    protected Bundle doBackgroundWork() throws DataModelException {
        final long cutoff = actionParameters.getLong(KEY_CUTOFF_TIMESTAMP);
        final DatabaseWrapper db = DataModel.get().getDatabase();

        // Snapshot all conversations (id + telephony thread id) before we start deleting.
        final List<String> conversationIds = new ArrayList<>();
        final List<Long> threadIds = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.CONVERSATIONS_TABLE,
                    new String[]{ConversationColumns._ID, ConversationColumns.SMS_THREAD_ID},
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                conversationIds.add(cursor.getString(0));
                threadIds.add(cursor.getLong(1));
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        boolean anyChanges = false;
        for (int i = 0; i < conversationIds.size(); i++) {
            if (deleteOldMessages(db, conversationIds.get(i), threadIds.get(i), cutoff)) {
                anyChanges = true;
            }
        }

        if (anyChanges) {
            MessagingContentProvider.notifyConversationListChanged();
        }
        return null;
    }

    /**
     * Deletes messages older than {@code cutoff} from one conversation in both the local DB and
     * the telephony provider. Removes the conversation itself if it becomes empty.
     *
     * @return true if any messages were deleted
     */
    private boolean deleteOldMessages(final DatabaseWrapper db, final String conversationId,
            final long threadId, final long cutoff) {

        // Collect telephony URIs of old messages now, before we delete them locally.
        // These are needed as a fallback when the telephony thread ID is unavailable.
        final List<Uri> oldMessageUris = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.MESSAGES_TABLE,
                    new String[]{MessageColumns.SMS_MESSAGE_URI},
                    MessageColumns.CONVERSATION_ID + "=? AND "
                            + MessageColumns.RECEIVED_TIMESTAMP + "<=?",
                    new String[]{conversationId, Long.toString(cutoff)},
                    null, null, null);
            while (cursor.moveToNext()) {
                final String uriStr = cursor.getString(0);
                if (uriStr != null && !uriStr.isEmpty()) {
                    try {
                        oldMessageUris.add(Uri.parse(uriStr));
                    } catch (Exception e) {
                        LogUtil.w(TAG, "AutoDeleteAction: could not parse message URI: " + uriStr);
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (oldMessageUris.isEmpty()) {
            return false; // Nothing old enough in this conversation.
        }

        // Delete old messages from the local DB; remove the conversation row if it becomes empty.
        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.MESSAGES_TABLE,
                    MessageColumns.CONVERSATION_ID + "=? AND "
                            + MessageColumns.RECEIVED_TIMESTAMP + "<=?",
                    new String[]{conversationId, Long.toString(cutoff)});

            BugleDatabaseOperations.deleteConversationIfEmptyInTransaction(db, conversationId);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        MessagingContentProvider.notifyMessagesChanged(conversationId);

        // Mirror deletion in the system telephony provider.
        if (threadId > 0) {
            MmsUtils.deleteThread(threadId, cutoff);
        } else {
            // Fallback: delete each message individually by its telephony URI.
            for (final Uri uri : oldMessageUris) {
                MmsUtils.deleteMessage(uri);
            }
        }

        return true;
    }

    // ---- Parcelable ----

    private AutoDeleteAction(final Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }

    public static final Parcelable.Creator<AutoDeleteAction> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public AutoDeleteAction createFromParcel(final Parcel in) {
                    return new AutoDeleteAction(in);
                }

                @Override
                public AutoDeleteAction[] newArray(final int size) {
                    return new AutoDeleteAction[size];
                }
            };
}
