package com.chocho.airpodsglance;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists session-only UI edges across a service recreation. */
public final class ConnectionSessionStore {
    private static final String PREFS = "airpods_session";
    private static final String AUTO_AAP_ATTEMPTED = "automatic_aap_attempted";
    private final SharedPreferences prefs;

    public ConnectionSessionStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized long beginIfNeeded() {
        if (prefs.getBoolean("active", false)) return prefs.getLong("token", 0L);
        long token = System.currentTimeMillis();
        prefs.edit().putBoolean("active", true).putLong("token", token)
                .putBoolean("popup_shown", false)
                .putBoolean(AUTO_AAP_ATTEMPTED, false).commit();
        return token;
    }

    /** Claims the one automatic direct-battery attempt allowed for this ACL session. */
    public synchronized boolean claimAutomaticAapAttempt() {
        if (!prefs.getBoolean("active", false)
                || prefs.getBoolean(AUTO_AAP_ATTEMPTED, false)) {
            return false;
        }
        prefs.edit().putBoolean(AUTO_AAP_ATTEMPTED, true).commit();
        return true;
    }

    public synchronized boolean claimPopup() {
        if (!prefs.getBoolean("active", false) || prefs.getBoolean("popup_shown", false)) {
            return false;
        }
        prefs.edit().putBoolean("popup_shown", true).commit();
        return true;
    }

    public synchronized void end() {
        prefs.edit().putBoolean("active", false)
                .putBoolean("popup_shown", false)
                .putBoolean(AUTO_AAP_ATTEMPTED, false).commit();
    }
}
