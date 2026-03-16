/*
 * PO Messaging
 */

package com.android.messaging.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.android.messaging.util.AutoDeleteScheduler;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.LogUtil;

/**
 * Receives the daily auto-delete alarm, executes deletion if the feature is enabled,
 * and schedules the next day's alarm.
 */
public class AutoDeleteReceiver extends BroadcastReceiver {

    public static final String ACTION_AUTO_DELETE = "com.po.messaging.action.AUTO_DELETE";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!ACTION_AUTO_DELETE.equals(intent.getAction())) return;

        final SharedPreferences prefs = context.getSharedPreferences(
                BuglePrefs.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final boolean enabled = prefs.getBoolean(
                AutoDeleteScheduler.PREF_AUTO_DELETE_ENABLED, false);
        final int days = prefs.getInt(
                AutoDeleteScheduler.PREF_AUTO_DELETE_DAYS, AutoDeleteScheduler.DEFAULT_DAYS);

        if (enabled && days > 0) {
            LogUtil.i(LogUtil.BUGLE_TAG,
                    "AutoDeleteReceiver: deleting messages older than " + days + " day(s)");
            AutoDeleteScheduler.runNow(days);
        }

        // Always re-schedule so the alarm survives even if the feature is temporarily disabled.
        AutoDeleteScheduler.scheduleNext(context);
    }
}
