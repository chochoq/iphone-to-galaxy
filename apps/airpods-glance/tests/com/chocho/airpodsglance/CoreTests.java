package com.chocho.airpodsglance;

import java.util.List;

public final class CoreTests {
    private static int assertions;

    public static void main(String[] args) {
        placementObservationsKeepUnknownAndSlotIdentity();
        rejectsGhostAndMalformedFrames();
        keepsUnknownSeparateFromZeroAndHundred();
        capodPodsifyLeftRightHypothesisIsExplicitlyUnconfirmed();
        chargingFlagsFollowTheSameUnconfirmedFlip();
        decodesAapExactBatteryFromRealPro3Capture();
        aapDisconnectedAndInvalidValuesNeverBecomeZero();
        rejectsMalformedAndNonBatteryAapPackets();
        freshnessHidesCachedCharging();
        lowBatteryAlertsOnlyOnEdges();
        userOptionsAndLiveThresholds();
        largeConnectionCardRespectsAttentionBoundaries();
        appleLikeSheetPreservesRealComponentTruth();
        productMotionRespectsSystemAnimationSetting();
        productCompositionExhaustivelyPreservesTruthAndAssetGates();
        System.out.println("CoreTests passed: " + assertions + " assertions");
    }

    private static void productCompositionExhaustivelyPreservesTruthAndAssetGates() {
        for (int bits = 0; bits < 64; bits++) {
            boolean left = (bits & 1) != 0;
            boolean right = (bits & 2) != 0;
            boolean caseKnown = (bits & 4) != 0;
            boolean motion = (bits & 8) != 0;
            boolean pairVerified = (bits & 16) != 0;
            boolean caseVerified = (bits & 32) != 0;
            ProductCompositionPolicy.Decision decision = ProductCompositionPolicy.decide(
                    left, right, caseKnown, motion, pairVerified, caseVerified);

            ProductCompositionPolicy.EarbudsArtwork expected = left && right
                    ? ProductCompositionPolicy.EarbudsArtwork.PAIR
                    : left ? ProductCompositionPolicy.EarbudsArtwork.LEFT_ONLY_CANDIDATE
                    : right ? ProductCompositionPolicy.EarbudsArtwork.RIGHT_ONLY_CANDIDATE
                    : ProductCompositionPolicy.EarbudsArtwork.NONE;
            check(decision.earbudsArtwork == expected, "composition preserves known sides " + bits);
            check(decision.showCase == caseKnown, "case presence is battery truth " + bits);
            check(decision.showAnyBatteryProduct() == (left || right || caseKnown),
                    "unknown never creates product " + bits);
            check(decision.animateEarbuds == (motion && left && right && pairVerified),
                    "pair motion requires all gates " + bits);
            check(decision.animateCase == (motion && caseKnown && caseVerified),
                    "case motion requires all gates " + bits);
            check(decision.needsSingleBudVisualValidation() == (left ^ right),
                    "single state remains candidate " + bits);
            if (!motion) {
                check(!decision.animateEarbuds && !decision.animateCase,
                        "reduced motion dominates asset state " + bits);
            }
        }
    }

    private static void placementObservationsKeepUnknownAndSlotIdentity() {
        check(AapPlacementDecoder.decode(null) == null, "null placement rejected");
        for (int length = 0; length <= 16; length++) {
            if (length == 8) continue;
            byte[] frame = new byte[length];
            if (length >= 6) { frame[0] = 4; frame[2] = 4; frame[4] = 6; }
            check(AapPlacementDecoder.decode(frame) == null, "unsupported placement length " + length);
        }
        for (int first = 0; first < 256; first++) {
            for (int second = 0; second < 256; second++) {
                byte[] frame = {4, 0, 4, 0, 6, 0, (byte) first, (byte) second};
                AapPlacementDecoder.Observation observation = AapPlacementDecoder.decode(frame);
                check(observation.primary == placementExpected(first), "primary slot is not a side");
                check(observation.secondary == placementExpected(second), "secondary slot preserved");
                check(observation.hasUnknown() == (first > 2 || second > 2), "unknown never means outside");
                check(observation.bucket().equals((first > 2 ? "unknown" : "" + first)
                        + "_" + (second > 2 ? "unknown" : "" + second)), "bounded diagnostic bucket");
            }
        }
        byte[] frame = {4, 0, 4, 0, 6, 0, 2, 2};
        for (int index = 0; index < 6; index++) {
            byte[] changed = frame.clone();
            changed[index] ^= 1;
            check(AapPlacementDecoder.decode(changed) == null, "wrong placement header rejected");
        }
        frame[4] = 4;
        check(AapPlacementDecoder.decode(frame) == null, "battery opcode cannot imply placement");
    }

    private static AapPlacementDecoder.State placementExpected(int code) {
        if (code == 0) return AapPlacementDecoder.State.IN_EAR;
        if (code == 1) return AapPlacementDecoder.State.OUT_OF_EAR;
        if (code == 2) return AapPlacementDecoder.State.IN_CASE;
        return AapPlacementDecoder.State.UNKNOWN;
    }

    private static void rejectsGhostAndMalformedFrames() {
        byte[] valid = packet(0x20, 0x28, 0x47);
        check(AirPodsPacketDecoder.decode(valid).accepted(), "valid accepted");
        check(AirPodsPacketDecoder.decode(new byte[4]).rejection
                == AirPodsPacketDecoder.Rejection.TOO_SHORT, "short rejected");
        valid[0] = 0x06;
        check(AirPodsPacketDecoder.decode(valid).rejection
                == AirPodsPacketDecoder.Rejection.WRONG_TYPE, "type rejected");
        valid = packet(0x20, 0x28, 0x47);
        valid[1] = 0x18;
        check(AirPodsPacketDecoder.decode(valid).rejection
                == AirPodsPacketDecoder.Rejection.WRONG_DECLARED_LENGTH, "length rejected");
        valid = packet(0x20, 0x28, 0x47);
        valid[2] = 0x00;
        check(AirPodsPacketDecoder.decode(valid).rejection
                == AirPodsPacketDecoder.Rejection.WRONG_PREFIX, "ghost prefix rejected");
    }

    private static void keepsUnknownSeparateFromZeroAndHundred() {
        AirPodsPacketDecoder.Decoded decoded = AirPodsPacketDecoder.decode(
                packet(0x20, 0xfe, 0x0b)).decoded;
        check(decoded.left.percent == null, "15 is unknown");
        check(decoded.right.percent == null, "11 is unknown, not clamped");
        decoded = AirPodsPacketDecoder.decode(packet(0x20, 0x0a, 0x00)).decoded;
        check(decoded.left.percent == 100, "ten is 100 percent");
        check(decoded.right.percent == 0, "zero remains real zero");
    }

    private static void capodPodsifyLeftRightHypothesisIsExplicitlyUnconfirmed() {
        AirPodsPacketDecoder.Decoded bitSet = AirPodsPacketDecoder.decode(
                packet(0x20, 0x28, 0x04)).decoded;
        check(bitSet.left.percent == 80, "bit set: low nibble hypothesized left");
        check(bitSet.right.percent == 20, "bit set: high nibble hypothesized right");
        AirPodsPacketDecoder.Decoded bitClear = AirPodsPacketDecoder.decode(
                packet(0x00, 0x28, 0x04)).decoded;
        check(bitClear.left.percent == 20, "bit clear: high nibble hypothesized left");
        check(bitClear.right.percent == 80, "bit clear: low nibble hypothesized right");
    }

    private static void chargingFlagsFollowTheSameUnconfirmedFlip() {
        AirPodsPacketDecoder.Decoded bitSet = AirPodsPacketDecoder.decode(
                packet(0x20, 0x28, 0x54)).decoded;
        check(Boolean.TRUE.equals(bitSet.left.charging), "bit set: bit4 left charging");
        check(!bitSet.right.charging, "bit set: bit5 right charging off");
        check(Boolean.TRUE.equals(bitSet.caseBattery.charging), "bit6 case charging");
        AirPodsPacketDecoder.Decoded bitClear = AirPodsPacketDecoder.decode(
                packet(0x00, 0x28, 0x24)).decoded;
        check(Boolean.TRUE.equals(bitClear.left.charging), "bit clear: bit5 left charging");
        check(!bitClear.right.charging, "bit clear: bit4 right charging off");
    }

    private static void freshnessHidesCachedCharging() {
        long now = 1_000_000L;
        AirPodsSnapshot fresh = snapshot(true, 20, true, now);
        check(BatteryPresentation.isFresh(fresh, now), "connected exact reading fresh");
        check(BatteryPresentation.showCharging(fresh, fresh.left, now), "fresh charging shown");
        AirPodsSnapshot old = snapshot(true, 20, true,
                now - BatteryPresentation.FRESH_FOR_MILLIS - 1);
        check(!BatteryPresentation.showCharging(old, old.left, now), "stale charging hidden");
        check(!BatteryPresentation.showCharging(fresh.withConnected(false), fresh.left, now),
                "disconnected charging hidden");
    }

    private static void decodesAapExactBatteryFromRealPro3Capture() {
        AapBatteryDecoder.Result result = AapBatteryDecoder.decode(hex(
                "04 00 04 00 04 00 03 04 01 50 05 01 02 01 4F 05 01 08 01 30 01 01"));
        check(result.accepted(), "real Pro 3 AAP battery accepted");
        check(result.decoded.left.percent == 80, "AAP left exact percent");
        check(result.decoded.right.percent == 79, "AAP right exact percent");
        check(result.decoded.caseBattery.percent == 48, "AAP case exact percent");
        check(Boolean.TRUE.equals(result.decoded.left.charging), "optimized left is charging");
        check(Boolean.TRUE.equals(result.decoded.right.charging), "optimized right is charging");
        check(Boolean.TRUE.equals(result.decoded.caseBattery.charging), "case charging decoded");
        check(new BatteryComponent(79, false).percent == 79,
                "common model retains AAP one-percent precision");
    }

    private static void aapDisconnectedAndInvalidValuesNeverBecomeZero() {
        AapBatteryDecoder.Result result = AapBatteryDecoder.decode(hex(
                "04 00 04 00 04 00 03 02 01 4D 02 01 04 01 50 02 01 08 01 00 04 01"));
        check(result.accepted(), "out-of-case AAP battery accepted");
        check(result.decoded.right.percent == 77, "right wire type exact");
        check(result.decoded.left.percent == 80, "left wire type exact");
        check(Boolean.FALSE.equals(result.decoded.left.charging), "not-charging decoded");
        check(result.decoded.caseBattery.percent == null,
                "disconnected case zero is unknown, not empty battery");
        check(result.decoded.caseBattery.charging == null, "disconnected charging is unknown");

        result = AapBatteryDecoder.decode(hex("04 00 04 00 04 00 01 04 01 7F 02 01"));
        check(result.decoded.left.percent == null, "AAP 127 is unknown");
    }

    private static void rejectsMalformedAndNonBatteryAapPackets() {
        check(AapBatteryDecoder.decode(new byte[] { 0x04 }).rejection
                == AapBatteryDecoder.Rejection.TOO_SHORT, "short AAP rejected");
        check(AapBatteryDecoder.decode(hex("01 00 04 00 00 00 01 00")).rejection
                == AapBatteryDecoder.Rejection.NOT_MESSAGE_PACKET, "handshake is not battery");
        check(AapBatteryDecoder.decode(hex("04 00 05 00 04 00 00")).rejection
                == AapBatteryDecoder.Rejection.WRONG_SERVICE, "wrong AAP service rejected");
        check(AapBatteryDecoder.decode(hex("04 00 04 00 09 00 00")).rejection
                == AapBatteryDecoder.Rejection.NOT_BATTERY, "control packet is not battery");
        check(AapBatteryDecoder.decode(hex("04 00 04 00 04 00 02 04 01 50 01 01")).rejection
                == AapBatteryDecoder.Rejection.MALFORMED_ENTRIES,
                "truncated AAP entries rejected");
    }

    private static void userOptionsAndLiveThresholds() {
        UserOptions defaults=UserOptions.restore(null,null,null);
        check(defaults.cardSeconds==0 && defaults.budThreshold==20 && defaults.caseThreshold==15,"migration preserves defaults");
        UserOptions invalid=UserOptions.restore(2,"wrong",100);
        check(invalid.cardSeconds==0 && invalid.budThreshold==20 && invalid.caseThreshold==15,"invalid storage repaired on read");
        for(int s=-5;s<=65;s++) {
            boolean valid=s==0 || (s>=3 && s<=60);
            check(UserOptions.validCardSeconds(s)==valid,"card range "+s);
            check(ConnectionPopupPolicy.shouldAutoDismiss(false,s)==(s>=3&&s<=60),"automatic timeout "+s);
            check(ConnectionPopupPolicy.shouldAutoDismiss(true,s)==(s>=3&&s<=60),"preview timeout "+s);
            if(valid) check(new UserOptions(s,20,15).dismissMillis()==s*1000L,"seconds conversion "+s);
        }
        for(int p=-5;p<=105;p++) check(UserOptions.validThreshold(p)==(p>=5&&p<=50),"battery range "+p);
        LowBatteryPolicy policy=new LowBatteryPolicy(20,15);
        check(policy.evaluate(snapshot(true,25,false,1)).isEmpty(),"initial value above old threshold");
        policy.updateThresholds(30,15);
        check(policy.evaluate(snapshot(true,25,false,2)).contains(LowBatteryPolicy.Part.LEFT),"updated threshold consumed");
        policy.updateThresholds(35,15);
        check(policy.evaluate(snapshot(true,25,false,3)).isEmpty(),"settings change doesn't reset edge latch");
        check(policy.evaluate(snapshot(false,5,false,4)).isEmpty(),"cached disconnected data doesn't warn");
        check(policy.evaluate(snapshot(true,5,true,5)).isEmpty(),"charging suppressed after option change");
    }

    private static void lowBatteryAlertsOnlyOnEdges() {
        LowBatteryPolicy policy = new LowBatteryPolicy(20, 15);
        List<LowBatteryPolicy.Part> first = policy.evaluate(snapshot(true, 20, false, 1));
        check(first.contains(LowBatteryPolicy.Part.LEFT), "threshold entry warns");
        check(policy.evaluate(snapshot(true, 20, false, 2)).isEmpty(), "repeat suppressed");
        check(policy.evaluate(snapshot(true, 30, false, 3)).isEmpty(), "recovery only rearms");
        check(policy.evaluate(snapshot(true, 20, false, 4)).contains(LowBatteryPolicy.Part.LEFT),
                "second edge warns");
        policy.resetForNewConnectionSession();
        check(policy.evaluate(snapshot(true, 20, true, 5)).isEmpty(), "charging suppresses warning");
    }

    private static void largeConnectionCardRespectsAttentionBoundaries() {
        check(ConnectionPopupPolicy.shouldShow(true, true, false, false, true, false),
                "automatic card shown in active unlocked use");
        check(!ConnectionPopupPolicy.shouldShow(false, true, false, false, true, false),
                "overlay permission required");
        check(!ConnectionPopupPolicy.shouldShow(true, false, false, false, true, false),
                "screen-off card suppressed");
        check(!ConnectionPopupPolicy.shouldShow(true, true, true, false, true, false),
                "locked card suppressed");
        check(ConnectionPopupPolicy.shouldShow(true, true, false, true, true, false),
                "silent automatic card remains visible during DND when actively used");
        check(ConnectionPopupPolicy.shouldShow(true, true, false, true, true, true),
                "explicit preview remains visible during DND while unlocked");
        check(!ConnectionPopupPolicy.shouldShow(true, true, true, true, true, false),
                "DND never bypasses the locked-screen boundary");
        check(!ConnectionPopupPolicy.shouldShow(true, true, false, false, false, true),
                "preview never invents missing battery");
        check(!ConnectionPopupPolicy.shouldAutoDismiss(false),
                "automatic connection card waits for user dismissal");
        check(!ConnectionPopupPolicy.shouldAutoDismiss(true),
                "explicit preview remains visible for inspection");
    }

    private static void appleLikeSheetPreservesRealComponentTruth() {
        BatteryComponent left = new BatteryComponent(86, false);
        BatteryComponent right = new BatteryComponent(94, false);
        check(ConnectionSheetPresentation.showEarbuds(left, right),
                "sheet shows a known earbud group");
        check("왼쪽 86%".equals(ConnectionSheetPresentation.sideLabel("왼쪽", left)),
                "left exact percent remains separate");
        check("오른쪽 94%".equals(ConnectionSheetPresentation.sideLabel("오른쪽", right)),
                "right exact percent remains separate");
        check(!ConnectionSheetPresentation.showCase(new BatteryComponent(null, null)),
                "unknown AAP case removes the product group");
        check(ConnectionSheetPresentation.showCase(new BatteryComponent(84, true)),
                "known case restores the product group");
    }

    private static void productMotionRespectsSystemAnimationSetting() {
        check(ProductMotionPolicy.shouldAnimate(1f),
                "normal system animation scale enables product turntable");
        check(ProductMotionPolicy.shouldAnimate(0.5f),
                "reduced nonzero scale still permits product motion");
        check(!ProductMotionPolicy.shouldAnimate(0f),
                "disabled system animations keep the accessible static frame");
    }

    private static AirPodsSnapshot snapshot(boolean connected, int left, boolean charging, long at) {
        return new AirPodsSnapshot("AirPods", connected,
                AirPodsSnapshot.IdentityConfidence.EXACT, 0x2720,
                new BatteryComponent(left, charging), new BatteryComponent(70, false),
                new BatteryComponent(50, false), at,
                AirPodsSnapshot.Source.BLE_PUBLIC_DECILE);
    }

    private static byte[] packet(int status, int buds, int caseAndCharging) {
        return new byte[] { 0x07, 0x19, 0x01, 0x27, 0x20, (byte) status,
                (byte) buds, (byte) caseAndCharging, 0x00, 0x00 };
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static byte[] hex(String value) {
        String[] parts = value.trim().split("\\s+");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return bytes;
    }
}
