package com.chocho.taptotop;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    final SharedPreferences prefs;
    public AppSettings(Context c) { prefs=c.getSharedPreferences("tap_settings",Context.MODE_PRIVATE); }
    public boolean enabled() { return prefs.getBoolean("enabled",true); }
    public boolean setEnabled(boolean value) {
        boolean old=enabled();
        if(prefs.edit().putBoolean("enabled",value).commit()) return true;
        prefs.edit().putBoolean("enabled",old).apply(); return false;
    }
    public ScrollOptions options() {
        java.util.Map<String,?> p=prefs.getAll();
        return ScrollOptions.restore(p.get("taps"),p.get("strength"),p.get("distance"));
    }
    public boolean saveOptions(ScrollOptions value) {
        ScrollOptions old=options();
        if(write(value).commit()) return true;
        write(old).apply(); return false;
    }
    private SharedPreferences.Editor write(ScrollOptions v) {
        return prefs.edit().putInt("taps",v.taps).putInt("strength",v.strength).putInt("distance",v.distance);
    }
}
