package com.chocho.airpodsglance;

import java.util.Objects;

/** A battery reading. Source-specific precision is enforced by its decoder. */
public final class BatteryComponent {
    public final Integer percent;
    public final Boolean charging;

    public BatteryComponent(Integer percent, Boolean charging) {
        if (percent != null && (percent < 0 || percent > 100)) {
            throw new IllegalArgumentException("Battery percent must be between 0 and 100");
        }
        this.percent = percent;
        this.charging = charging;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BatteryComponent)) return false;
        BatteryComponent that = (BatteryComponent) other;
        return Objects.equals(percent, that.percent)
                && Objects.equals(charging, that.charging);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percent, charging);
    }
}
