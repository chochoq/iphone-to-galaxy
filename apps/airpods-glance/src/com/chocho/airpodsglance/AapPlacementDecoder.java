package com.chocho.airpodsglance;

/** Experimental protocol observation only: slots are NOT left/right; never drives artwork. */
public final class AapPlacementDecoder {
    public enum State { IN_EAR, OUT_OF_EAR, IN_CASE, UNKNOWN }

    public static final class Observation {
        public final State primary;
        public final State secondary;

        private Observation(State primary, State secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public boolean hasUnknown() {
            return primary == State.UNKNOWN || secondary == State.UNKNOWN;
        }

        /** Bounded, anonymous diagnostic bucket; not a current-state record. */
        public String bucket() { return code(primary) + "_" + code(secondary); }
    }

    private AapPlacementDecoder() { }

    public static boolean isPlacementReport(byte[] frame) {
        return frame != null && frame.length >= 6
                && frame[0] == 4 && frame[1] == 0 && frame[2] == 4 && frame[3] == 0
                && frame[4] == 6 && frame[5] == 0;
    }

    public static Observation decode(byte[] frame) {
        if (!isPlacementReport(frame) || frame.length != 8) return null;
        return new Observation(state(frame[6] & 0xff), state(frame[7] & 0xff));
    }

    private static State state(int code) {
        switch (code) {
            case 0: return State.IN_EAR;
            case 1: return State.OUT_OF_EAR;
            case 2: return State.IN_CASE;
            default: return State.UNKNOWN;
        }
    }

    private static String code(State state) {
        switch (state) {
            case IN_EAR: return "0";
            case OUT_OF_EAR: return "1";
            case IN_CASE: return "2";
            default: return "unknown";
        }
    }
}
