package com.chocho.airpodsglance;

public final class BatteryPresentation {
    public static final long FRESH_FOR_MILLIS = 2 * 60 * 1000L;

    private BatteryPresentation() {}

    public static boolean isFresh(AirPodsSnapshot snapshot, long now) {
        return snapshot != null
                && snapshot.connected
                && snapshot.identityConfidence == AirPodsSnapshot.IdentityConfidence.EXACT
                && snapshot.observedAtEpochMillis > 0
                && now >= snapshot.observedAtEpochMillis
                && now - snapshot.observedAtEpochMillis <= FRESH_FOR_MILLIS;
    }

    public static String percent(BatteryComponent component) {
        return component == null || component.percent == null ? "—" : component.percent + "%";
    }

    public static boolean showCharging(AirPodsSnapshot snapshot,
                                       BatteryComponent component, long now) {
        return isFresh(snapshot, now)
                && component != null
                && Boolean.TRUE.equals(component.charging);
    }
}
