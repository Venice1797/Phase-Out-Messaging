/*
 * PO Messaging
 */

package com.android.messaging.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores per-conversation auto-reply overrides.
 * When a conversation has an override, auto-reply is suppressed for all participants
 * in that conversation regardless of the global audience filter.
 * Override entries are removed when the conversation thread is deleted.
 */
public final class AutoReplyOverrideStore {

    private static final String PREFS_NAME = "auto_reply_overrides";
    private static final String KEY_OVERRIDDEN = "overridden_conversations";

    public static boolean hasOverride(final Context ctx, final String conversationId) {
        return getOverridden(ctx).contains(conversationId);
    }

    public static void setOverride(final Context ctx, final String conversationId,
            final boolean active) {
        final Set<String> current = new HashSet<>(getOverridden(ctx));
        if (active) {
            current.add(conversationId);
        } else {
            current.remove(conversationId);
        }
        prefs(ctx).edit().putStringSet(KEY_OVERRIDDEN, current).apply();
    }

    public static void removeOverride(final Context ctx, final String conversationId) {
        setOverride(ctx, conversationId, false);
    }

    public static void clearAll(final Context ctx) {
        prefs(ctx).edit().remove(KEY_OVERRIDDEN).apply();
    }

    public static int getCount(final Context ctx) {
        return getOverridden(ctx).size();
    }

    private static Set<String> getOverridden(final Context ctx) {
        final Set<String> stored = prefs(ctx).getStringSet(KEY_OVERRIDDEN, null);
        return stored != null ? stored : Collections.emptySet();
    }

    private static SharedPreferences prefs(final Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private AutoReplyOverrideStore() {}
}
