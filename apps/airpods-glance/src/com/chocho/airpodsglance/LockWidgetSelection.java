package com.chocho.airpodsglance;

/** Pure selection contract shared by receiver tests; never reads a battery or starts a service. */
public final class LockWidgetSelection {
    private LockWidgetSelection() {}
    public static int valid(int stored, int fallback) {
        if (fallback < 0 || fallback > 2) throw new IllegalArgumentException("Unknown default part");
        return stored >= 0 && stored < 3 ? stored : fallback;
    }
    public static int next(int current) { return (valid(current, 0) + 1) % 3; }
    public static boolean accepts(int id, String expected, String actual) {
        return id > 0 && expected != null && expected.equals(actual);
    }
}
