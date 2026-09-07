package com.chocho.airpodsglance;

/** Pure decoder for one AAP L2CAP SEQPACKET battery message. */
public final class AapBatteryDecoder {
    public enum Rejection {
        NONE, TOO_SHORT, NOT_MESSAGE_PACKET, WRONG_SERVICE, NOT_BATTERY, MALFORMED_ENTRIES
    }

    public static final class Decoded {
        public final BatteryComponent single;
        public final BatteryComponent left;
        public final BatteryComponent right;
        public final BatteryComponent caseBattery;

        private Decoded(BatteryComponent single, BatteryComponent left,
                        BatteryComponent right, BatteryComponent caseBattery) {
            this.single = single;
            this.left = left;
            this.right = right;
            this.caseBattery = caseBattery;
        }
    }

    public static final class Result {
        public final Rejection rejection;
        public final Decoded decoded;

        private Result(Rejection rejection, Decoded decoded) {
            this.rejection = rejection;
            this.decoded = decoded;
        }

        public boolean accepted() { return rejection == Rejection.NONE && decoded != null; }
    }

    private AapBatteryDecoder() {}

    public static Result decode(byte[] packet) {
        if (packet == null || packet.length < 7) return reject(Rejection.TOO_SHORT);
        if (le16(packet, 0) != 0x0004) return reject(Rejection.NOT_MESSAGE_PACKET);
        if (le16(packet, 2) != 0x0004) return reject(Rejection.WRONG_SERVICE);
        if (le16(packet, 4) != 0x0004) return reject(Rejection.NOT_BATTERY);

        int count = u(packet[6]);
        int required = 7 + count * 5;
        if (packet.length < required) return reject(Rejection.MALFORMED_ENTRIES);

        BatteryComponent single = null;
        BatteryComponent left = null;
        BatteryComponent right = null;
        BatteryComponent caseBattery = null;
        int offset = 7;
        for (int i = 0; i < count; i++, offset += 5) {
            int type = u(packet[offset]);
            int rawPercent = u(packet[offset + 2]);
            int chargingState = u(packet[offset + 3]);
            Integer percent = rawPercent <= 100 && chargingState != 0x04
                    ? rawPercent : null;
            Boolean charging = charging(chargingState);
            BatteryComponent component = new BatteryComponent(percent, charging);
            switch (type) {
                case 0x01: single = component; break;
                case 0x02: right = component; break;
                case 0x04: left = component; break;
                case 0x08: caseBattery = component; break;
                default: break;
            }
        }
        return new Result(Rejection.NONE, new Decoded(single, left, right, caseBattery));
    }

    private static Boolean charging(int state) {
        if (state == 0x01 || state == 0x05) return true;
        if (state == 0x02) return false;
        return null;
    }

    private static Result reject(Rejection rejection) {
        return new Result(rejection, null);
    }

    private static int le16(byte[] data, int offset) {
        return u(data[offset]) | (u(data[offset + 1]) << 8);
    }

    private static int u(byte value) { return value & 0xff; }
}
