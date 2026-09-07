package com.chocho.airpodsglance;

import java.util.Objects;

/** The one state contract shared by app, widget and notifications. */
public final class AirPodsSnapshot {
    public enum IdentityConfidence { NONE, EXACT, NEEDS_CALIBRATION }
    public enum Source { NONE, BLE_PUBLIC_DECILE, AAP_EXACT_PERCENT }

    public final String selectedDeviceName;
    public final boolean connected;
    public final IdentityConfidence identityConfidence;
    public final Integer modelId;
    public final BatteryComponent left;
    public final BatteryComponent right;
    public final BatteryComponent caseBattery;
    public final long observedAtEpochMillis;
    public final Source source;

    public AirPodsSnapshot(String selectedDeviceName, boolean connected,
                           IdentityConfidence identityConfidence, Integer modelId,
                           BatteryComponent left, BatteryComponent right,
                           BatteryComponent caseBattery, long observedAtEpochMillis,
                           Source source) {
        this.selectedDeviceName = selectedDeviceName;
        this.connected = connected;
        this.identityConfidence = identityConfidence == null
                ? IdentityConfidence.NONE : identityConfidence;
        this.modelId = modelId;
        this.left = left;
        this.right = right;
        this.caseBattery = caseBattery;
        this.observedAtEpochMillis = observedAtEpochMillis;
        this.source = source == null ? Source.NONE : source;
    }

    public static AirPodsSnapshot empty(String selectedDeviceName) {
        return new AirPodsSnapshot(selectedDeviceName, false, IdentityConfidence.NONE,
                null, null, null, null, 0L, Source.NONE);
    }

    public AirPodsSnapshot withConnected(boolean value) {
        return new AirPodsSnapshot(selectedDeviceName, value,
                value ? identityConfidence : IdentityConfidence.NONE,
                modelId, left, right, caseBattery, observedAtEpochMillis, source);
    }

    /** Timestamp changes alone do not require a full widget/notification redraw. */
    public boolean sameVisibleState(AirPodsSnapshot that) {
        return that != null
                && connected == that.connected
                && Objects.equals(selectedDeviceName, that.selectedDeviceName)
                && identityConfidence == that.identityConfidence
                && Objects.equals(modelId, that.modelId)
                && Objects.equals(left, that.left)
                && Objects.equals(right, that.right)
                && Objects.equals(caseBattery, that.caseBattery)
                && source == that.source;
    }
}
