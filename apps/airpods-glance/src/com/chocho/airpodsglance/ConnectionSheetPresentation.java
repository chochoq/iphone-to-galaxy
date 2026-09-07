package com.chocho.airpodsglance;

/** Truth-preserving labels for the Apple-like connection sheet. */
public final class ConnectionSheetPresentation {
    private ConnectionSheetPresentation() {}

    public static boolean showCase(BatteryComponent component) {
        return component != null && component.percent != null;
    }

    public static boolean showEarbuds(BatteryComponent left, BatteryComponent right) {
        return known(left) || known(right);
    }

    public static String sideLabel(String name, BatteryComponent component) {
        return name + " " + BatteryPresentation.percent(component);
    }

    private static boolean known(BatteryComponent component) {
        return component != null && component.percent != null;
    }
}
