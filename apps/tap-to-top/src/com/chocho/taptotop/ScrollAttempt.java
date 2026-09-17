package com.chocho.taptotop;

/** One volatile, non-identifying result. No disk storage, timestamps or app history. */
final class ScrollAttempt {
    private static volatile String last = "아직 시도하지 않았어요.";
    static void record(String message) { last = message; }
    static String summary() { return last; }
    private ScrollAttempt() {}
}
