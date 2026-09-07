package com.chocho.airpodsglance;

/** Pure product-art decision. Placement is deliberately not an input. */
public final class ProductCompositionPolicy {
    public enum EarbudsArtwork {
        NONE,
        PAIR,
        LEFT_ONLY_CANDIDATE,
        RIGHT_ONLY_CANDIDATE
    }

    public static final class Decision {
        public final EarbudsArtwork earbudsArtwork;
        public final boolean showCase;
        public final boolean animateEarbuds;
        public final boolean animateCase;

        private Decision(EarbudsArtwork earbudsArtwork, boolean showCase,
                         boolean animateEarbuds, boolean animateCase) {
            this.earbudsArtwork = earbudsArtwork;
            this.showCase = showCase;
            this.animateEarbuds = animateEarbuds;
            this.animateCase = animateCase;
        }

        public boolean showAnyBatteryProduct() {
            return earbudsArtwork != EarbudsArtwork.NONE || showCase;
        }

        /** A candidate means protocol truth is known but iOS artwork behavior is not. */
        public boolean needsSingleBudVisualValidation() {
            return earbudsArtwork == EarbudsArtwork.LEFT_ONLY_CANDIDATE
                    || earbudsArtwork == EarbudsArtwork.RIGHT_ONLY_CANDIDATE;
        }
    }

    private ProductCompositionPolicy() { }

    public static Decision decide(boolean leftKnown, boolean rightKnown, boolean caseKnown,
                                  boolean motionEnabled, boolean pairAssetVerified,
                                  boolean caseAssetVerified) {
        EarbudsArtwork earbuds;
        if (leftKnown && rightKnown) {
            earbuds = EarbudsArtwork.PAIR;
        } else if (leftKnown) {
            earbuds = EarbudsArtwork.LEFT_ONLY_CANDIDATE;
        } else if (rightKnown) {
            earbuds = EarbudsArtwork.RIGHT_ONLY_CANDIDATE;
        } else {
            earbuds = EarbudsArtwork.NONE;
        }

        // Candidate single-bud art is never animated before its visual behavior is validated.
        boolean animateEarbuds = motionEnabled
                && earbuds == EarbudsArtwork.PAIR
                && pairAssetVerified;
        boolean animateCase = motionEnabled && caseKnown && caseAssetVerified;
        return new Decision(earbuds, caseKnown, animateEarbuds, animateCase);
    }
}
