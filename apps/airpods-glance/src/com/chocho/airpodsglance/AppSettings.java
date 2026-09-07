package com.chocho.airpodsglance;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    private static final String PREFS = "airpods_settings";
    private static final String MONITOR = "monitor_enabled";
    private static final String POPUP = "popup_enabled";
    private static final String LOW = "low_battery_enabled";
    private static final String ADDRESS = "selected_address";
    private static final String NAME = "selected_name";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean monitorEnabled() { return prefs.getBoolean(MONITOR, true); }
    public void setMonitorEnabled(boolean value) { prefs.edit().putBoolean(MONITOR, value).apply(); }
    public boolean popupEnabled() { return prefs.getBoolean(POPUP, true); }
    public void setPopupEnabled(boolean value) { prefs.edit().putBoolean(POPUP, value).apply(); }
    public boolean lowBatteryEnabled() { return prefs.getBoolean(LOW, true); }
    public void setLowBatteryEnabled(boolean value) { prefs.edit().putBoolean(LOW, value).apply(); }
    public String selectedAddress() { return prefs.getString(ADDRESS, null); }
    public String selectedName() { return prefs.getString(NAME, null); }

    public void selectDevice(String address, String name) {
        prefs.edit().putString(ADDRESS, address).putString(NAME, name).commit();
    }
}
