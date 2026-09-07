package com.chocho.airpodsglance;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/** Stores the complete snapshot as one JSON value and one commit boundary. */
public final class BatteryStateStore {
    private static final String PREFS = "airpods_snapshot";
    private static final String KEY = "snapshot_v1";
    private final SharedPreferences prefs;

    public BatteryStateStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void save(AirPodsSnapshot snapshot) {
        prefs.edit().putString(KEY, encode(snapshot)).commit();
    }

    public synchronized AirPodsSnapshot load() {
        String value = prefs.getString(KEY, null);
        if (value == null) return AirPodsSnapshot.empty(null);
        try {
            JSONObject json = new JSONObject(value);
            long observed = json.optLong("observed", 0L);
            return new AirPodsSnapshot(
                    nullableString(json, "name"),
                    json.optBoolean("connected", false),
                    confidence(json.optString("confidence", "NONE")),
                    nullableInt(json, "model"),
                    component(json.optJSONObject("left")),
                    component(json.optJSONObject("right")),
                    component(json.optJSONObject("case")),
                    observed,
                    source(json.optString("source", observed > 0L
                            ? "BLE_PUBLIC_DECILE" : "NONE")));
        } catch (JSONException | IllegalArgumentException error) {
            return AirPodsSnapshot.empty(null);
        }
    }

    public synchronized AirPodsSnapshot markConnected(boolean connected, String selectedName) {
        AirPodsSnapshot previous = load();
        AirPodsSnapshot next = new AirPodsSnapshot(
                selectedName != null ? selectedName : previous.selectedDeviceName,
                connected,
                connected ? previous.identityConfidence : AirPodsSnapshot.IdentityConfidence.NONE,
                previous.modelId, previous.left, previous.right, previous.caseBattery,
                previous.observedAtEpochMillis, previous.source);
        save(next);
        return next;
    }

    public synchronized AirPodsSnapshot markNeedsCalibration(String selectedName) {
        AirPodsSnapshot previous = load();
        AirPodsSnapshot next = new AirPodsSnapshot(
                selectedName != null ? selectedName : previous.selectedDeviceName,
                true, AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION,
                previous.modelId, previous.left, previous.right, previous.caseBattery,
                previous.observedAtEpochMillis, previous.source);
        save(next);
        return next;
    }

    private static String encode(AirPodsSnapshot snapshot) {
        try {
            JSONObject json = new JSONObject();
            json.put("name", snapshot.selectedDeviceName == null
                    ? JSONObject.NULL : snapshot.selectedDeviceName);
            json.put("connected", snapshot.connected);
            json.put("confidence", snapshot.identityConfidence.name());
            json.put("model", snapshot.modelId == null ? JSONObject.NULL : snapshot.modelId);
            json.put("left", componentJson(snapshot.left));
            json.put("right", componentJson(snapshot.right));
            json.put("case", componentJson(snapshot.caseBattery));
            json.put("observed", snapshot.observedAtEpochMillis);
            json.put("source", snapshot.source.name());
            return json.toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Object componentJson(BatteryComponent component) throws JSONException {
        if (component == null) return JSONObject.NULL;
        JSONObject json = new JSONObject();
        json.put("percent", component.percent == null ? JSONObject.NULL : component.percent);
        json.put("charging", component.charging == null ? JSONObject.NULL : component.charging);
        return json;
    }

    private static BatteryComponent component(JSONObject json) throws JSONException {
        if (json == null) return null;
        Integer percent = nullableInt(json, "percent");
        Boolean charging = json.isNull("charging") ? null : json.getBoolean("charging");
        return new BatteryComponent(percent, charging);
    }

    private static Integer nullableInt(JSONObject json, String key) throws JSONException {
        return json.isNull(key) || !json.has(key) ? null : json.getInt(key);
    }

    private static String nullableString(JSONObject json, String key) throws JSONException {
        return json.isNull(key) || !json.has(key) ? null : json.getString(key);
    }

    private static AirPodsSnapshot.IdentityConfidence confidence(String value) {
        try {
            return AirPodsSnapshot.IdentityConfidence.valueOf(value);
        } catch (IllegalArgumentException error) {
            return AirPodsSnapshot.IdentityConfidence.NONE;
        }
    }

    private static AirPodsSnapshot.Source source(String value) {
        try {
            return AirPodsSnapshot.Source.valueOf(value);
        } catch (IllegalArgumentException error) {
            return AirPodsSnapshot.Source.NONE;
        }
    }
}
