package com.chocho.airpodsglance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Session-scoped low-battery edge detector. Repeated advertisements do not repeat alerts. */
public final class LowBatteryPolicy {
    public enum Part { LEFT, RIGHT, CASE }

    private int budThreshold;
    private int caseThreshold;
    private boolean leftLatched;
    private boolean rightLatched;
    private boolean caseLatched;

    public LowBatteryPolicy(int budThreshold, int caseThreshold) {
        this.budThreshold = budThreshold;
        this.caseThreshold = caseThreshold;
    }

    /** Preserve edge latches when settings change; don't replay a warning for every preference edit. */
    public void updateThresholds(int buds, int caseValue) {
        if (!UserOptions.validThreshold(buds) || !UserOptions.validThreshold(caseValue))
            throw new IllegalArgumentException("Invalid battery warning threshold");
        budThreshold=buds; caseThreshold=caseValue;
    }

    public List<Part> evaluate(AirPodsSnapshot snapshot) {
        if (snapshot == null || !snapshot.connected) return Collections.emptyList();
        List<Part> warnings = new ArrayList<>();
        leftLatched = evaluateOne(snapshot.left, budThreshold, leftLatched, Part.LEFT, warnings);
        rightLatched = evaluateOne(snapshot.right, budThreshold, rightLatched, Part.RIGHT, warnings);
        caseLatched = evaluateOne(snapshot.caseBattery, caseThreshold, caseLatched, Part.CASE, warnings);
        return warnings;
    }

    public void resetForNewConnectionSession() {
        leftLatched = false;
        rightLatched = false;
        caseLatched = false;
    }

    private boolean evaluateOne(BatteryComponent component, int threshold, boolean latched,
                                Part part, List<Part> warnings) {
        if (component == null || component.percent == null) return latched;
        if (Boolean.TRUE.equals(component.charging) || component.percent >= threshold + 10) {
            return false;
        }
        if (component.percent <= threshold && !latched) {
            warnings.add(part);
            return true;
        }
        return latched;
    }
}
