package com.chocho.airpodsglance;

/** Last-received readings only; never promises a live measurement or current charging state. */
public final class LockWidgetState {
    private static final String[] LABELS = {"왼쪽", "오른쪽", "케이스"};
    public final String label;
    public final int part;
    public final Integer percent;
    public final long observed;

    private LockWidgetState(int part, String label, Integer percent, long observed) {
        this.part = part;
        this.label = label;
        this.percent = percent;
        this.observed = observed;
    }

    public static LockWidgetState from(AirPodsSnapshot snapshot, long now, int part) {
        if (part < 0 || part > 2) throw new IllegalArgumentException("Unknown battery part");
        boolean usable = snapshot != null && snapshot.observedAtEpochMillis > 0
                && snapshot.observedAtEpochMillis <= now
                && snapshot.identityConfidence != AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION;
        BatteryComponent component = !usable ? null : part == 0 ? snapshot.left
                : part == 1 ? snapshot.right : snapshot.caseBattery;
        Integer value = component == null ? null : component.percent;
        if (value != null && (value < 0 || value > 100)) value = null;
        return new LockWidgetState(part, LABELS[part], value, value == null ? 0 : snapshot.observedAtEpochMillis);
    }

    public String value() { return percent == null ? "—" : percent + "%"; }
    public String caption() { return label + (percent == null ? "" : " 기록"); }
    public String nextLabel() { return LABELS[LockWidgetSelection.next(part)]; }
    public String description() {
        return "에어팟 " + label + (percent == null ? ", 잔량 알 수 없음" : ", 마지막 수신 잔량 " + value());
    }
}
