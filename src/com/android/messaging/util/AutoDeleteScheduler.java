/*
 * PO Messaging
 */

package com.android.messaging.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.android.messaging.datamodel.action.AutoDeleteAction;
import com.android.messaging.receiver.AutoDeleteReceiver;

/**
 * Manages scheduling and immediate execution of the auto-delete feature.
 * Pref keys are defined here so they can be shared between SafetyActivity and AutoDeleteReceiver
 * without creating a circular dependency.
 */
public class AutoDeleteScheduler {

    public static final String PREF_AUTO_DELETE_ENABLED = "auto_delete_enabled";
    public static final String PREF_AUTO_DELETE_DAYS = "auto_delete_days";
    public static final int DEFAULT_DAYS = 14;

    private static final int ALARM_REQUEST_CODE = 9001;

    /** Schedule the next daily auto-delete alarm (one-shot; receiver re-schedules after firing). */
    public static void scheduleNext(final Context context) {
        final PendingIntent pi = buildPendingIntent(context);
        context.getSystemService(AlarmManager.class).set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY,
                pi);
    }

    /** Cancel any pending daily auto-delete alarm. */
    public static void cancel(final Context context) {
        context.getSystemService(AlarmManager.class)
                .cancel(buildPendingIntent(context));
    }

    /** Run auto-deletion immediately on the action-service thread. */
    public static void runNow(final int days) {
        final long cutoff = System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000L;
        AutoDeleteAction.run(cutoff);
    }

    private static PendingIntent buildPendingIntent(final Context context) {
        final Intent intent = new Intent(AutoDeleteReceiver.ACTION_AUTO_DELETE)
                .setClass(context, AutoDeleteReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
