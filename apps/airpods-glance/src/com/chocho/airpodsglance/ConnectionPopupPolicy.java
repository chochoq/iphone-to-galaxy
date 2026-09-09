package com.chocho.airpodsglance;

/** Pure policy shared by the automatic connection card and its explicit preview. */
public final class ConnectionPopupPolicy {
    private ConnectionPopupPolicy() {}

    public static boolean shouldShow(boolean overlayPermission,
                                     boolean screenInteractive,
                                     boolean keyguardLocked,
                                     boolean dndActive,
                                     boolean hasReading,
                                     boolean explicitPreview) {
        // DND governs audible interruptions. This card is silent, never wakes the screen,
        // and is still gated by active use plus an unlocked keyguard. Suppressing it here
        // also suppresses the notification fallback on Samsung, leaving no visible result.
        return overlayPermission
                && screenInteractive
                && !keyguardLocked
                && hasReading;
    }

    /** Both automatic and preview cards stay until the user dismisses them. */
    public static boolean shouldAutoDismiss(boolean explicitPreview) {
        return false;
    }

    public static boolean shouldAutoDismiss(boolean explicitPreview, int seconds) {
        return UserOptions.validCardSeconds(seconds) && seconds>0;
    }
}
