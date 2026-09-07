package com.chocho.airpodsglance;

/**
 * Decoder for the public Apple proximity battery frame carried in manufacturer data.
 * The input starts after Bluetooth's Apple company id (0x004c).
 */
public final class AirPodsPacketDecoder {
    public static final int APPLE_COMPANY_ID = 0x004c;

    public enum Rejection {
        NONE,
        TOO_SHORT,
        WRONG_TYPE,
        WRONG_DECLARED_LENGTH,
        WRONG_PREFIX
    }

    public static final class Result {
        public final Rejection rejection;
        public final Decoded decoded;

        private Result(Rejection rejection, Decoded decoded) {
            this.rejection = rejection;
            this.decoded = decoded;
        }

        public boolean accepted() {
            return rejection == Rejection.NONE && decoded != null;
        }
    }

    public static final class Decoded {
        public final int modelId;
        public final BatteryComponent left;
        public final BatteryComponent right;
        public final BatteryComponent caseBattery;

        private Decoded(int modelId, BatteryComponent left,
                        BatteryComponent right, BatteryComponent caseBattery) {
            this.modelId = modelId;
            this.left = left;
            this.right = right;
            this.caseBattery = caseBattery;
        }
    }

    private AirPodsPacketDecoder() {}

    public static Result decode(byte[] data) {
        if (data == null || data.length < 10) return rejected(Rejection.TOO_SHORT);
        if (u(data[0]) != 0x07) return rejected(Rejection.WRONG_TYPE);
        if (u(data[1]) != 0x19) return rejected(Rejection.WRONG_DECLARED_LENGTH);
        if (u(data[2]) != 0x01) return rejected(Rejection.WRONG_PREFIX);

        int modelId = (u(data[3]) << 8) | u(data[4]);
        int status = u(data[5]);
        int buds = u(data[6]);
        int caseAndCharging = u(data[7]);
        boolean primaryIsLeft = (status & 0x20) != 0;

        int high = (buds >>> 4) & 0x0f;
        int low = buds & 0x0f;

        // Initial, explicitly unconfirmed hypothesis: CAPod and Podsify agree on this flip.
        Integer leftPercent = percent(primaryIsLeft ? low : high);
        Integer rightPercent = percent(primaryIsLeft ? high : low);
        boolean leftCharging = (caseAndCharging & (primaryIsLeft ? 0x10 : 0x20)) != 0;
        boolean rightCharging = (caseAndCharging & (primaryIsLeft ? 0x20 : 0x10)) != 0;
        Integer casePercent = percent(caseAndCharging & 0x0f);
        boolean caseCharging = (caseAndCharging & 0x40) != 0;

        Decoded decoded = new Decoded(
                modelId,
                new BatteryComponent(leftPercent, leftCharging),
                new BatteryComponent(rightPercent, rightCharging),
                new BatteryComponent(casePercent, caseCharging));
        return new Result(Rejection.NONE, decoded);
    }

    private static Result rejected(Rejection rejection) {
        return new Result(rejection, null);
    }

    private static Integer percent(int nibble) {
        return nibble >= 0 && nibble <= 10 ? nibble * 10 : null;
    }

    private static int u(byte value) {
        return value & 0xff;
    }
}
