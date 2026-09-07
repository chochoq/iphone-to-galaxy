package com.chocho.taptotop;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

public final class TapToTopService extends AccessibilityService {
    private static final long GENERAL_FLING_DURATION_MS = 100;
    private static final long X_FLING_DURATION_MS = 45;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlay;
    private float downX;
    private float downY;
    private long downTime;
    private boolean dragHandled;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        installOverlay();
    }

    private void installOverlay() {
        removeOverlay();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        overlay = new View(this);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        overlay.setContentDescription("맨 위로 이동");
        overlay.setOnTouchListener(this::handleOverlayTouch);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setTitle("TapToTopStatusBarTarget");
        windowManager.addView(overlay, params);
    }

    private boolean handleOverlayTouch(View view, MotionEvent event) {
        float movementThreshold = dp(14);
        float notificationThreshold = dp(24);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                downTime = SystemClock.uptimeMillis();
                dragHandled = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                openNotificationsForDownwardDrag(event, notificationThreshold);
                return true;

            case MotionEvent.ACTION_UP:
                float totalX = Math.abs(event.getRawX() - downX);
                float totalY = Math.abs(event.getRawY() - downY);
                long elapsed = SystemClock.uptimeMillis() - downTime;
                if (!dragHandled && totalX < movementThreshold && totalY < movementThreshold
                        && elapsed < 500) {
                    scrollToTop();
                    view.performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                openNotificationsForDownwardDrag(event, notificationThreshold);
                handler.removeCallbacksAndMessages(null);
                return true;

            case MotionEvent.ACTION_OUTSIDE:
                handler.removeCallbacksAndMessages(null);
                return true;

            default:
                return true;
        }
    }

    private void openNotificationsForDownwardDrag(
            MotionEvent event, float notificationThreshold) {
        float dx = event.getRawX() - downX;
        float dy = event.getRawY() - downY;
        if (!dragHandled && dy > notificationThreshold && Math.abs(dy) > Math.abs(dx)) {
            dragHandled = true;
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        }
    }

    private void scrollToTop() {
        handler.removeCallbacksAndMessages(null);
        if (windowManager == null) return;
        AccessibilityNodeInfo target = findBestScrollableNode();

        Rect bounds = gestureBounds(target);
        float x = bounds.centerX();
        float startY = bounds.top + bounds.height() * 0.28f;
        float endY = bounds.top + bounds.height() * 0.90f;
        if (endY - startY < dp(160)) return;

        Path path = new Path();
        path.moveTo(x, startY);
        path.lineTo(x, endY);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(
                        path, 0, flingDuration(target)))
                .build();
        dispatchGesture(gesture, null, null);
    }

    private long flingDuration(AccessibilityNodeInfo target) {
        if (target != null && target.getPackageName() != null
                && "com.twitter.android".contentEquals(target.getPackageName())) {
            return X_FLING_DURATION_MS;
        }
        return GENERAL_FLING_DURATION_MS;
    }

    private int preferredUpAction(AccessibilityNodeInfo node) {
        int scrollUp = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId();
        if (supportsAction(node, scrollUp)) return scrollUp;
        int backward = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.getId();
        return supportsAction(node, backward) ? backward : 0;
    }

    private boolean supportsAction(AccessibilityNodeInfo node, int actionId) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getId() == actionId) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findBestScrollableNode() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        for (AccessibilityWindowInfo window : getWindows()) {
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && !isOwnOrSystemUi(root)) roots.add(root);
        }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && !isOwnOrSystemUi(root)) roots.add(root);
        }

        NodeCandidate best = new NodeCandidate();
        for (AccessibilityNodeInfo root : roots) {
            collectBestScrollable(root, 0, best);
        }
        return best.node;
    }

    private boolean isOwnOrSystemUi(AccessibilityNodeInfo node) {
        CharSequence packageName = node.getPackageName();
        if (packageName == null) return false;
        String value = packageName.toString();
        return getPackageName().equals(value) || "com.android.systemui".equals(value);
    }

    private void collectBestScrollable(
            AccessibilityNodeInfo node, int depth, NodeCandidate best) {
        if (node == null || depth > 45 || !node.isVisibleToUser()) return;

        boolean hasUpAction = preferredUpAction(node) != 0;
        boolean canJump = supportsAction(node,
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId());
        if (node.isScrollable() || hasUpAction || canJump) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            long area = Math.max(0, bounds.width()) * (long) Math.max(0, bounds.height());
            long score = area + (canJump ? 1_000_000_000L : 0L);
            if (score > best.score) {
                best.score = score;
                best.node = node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) collectBestScrollable(child, depth + 1, best);
        }
    }

    private Rect gestureBounds(AccessibilityNodeInfo target) {
        Rect display = windowManager.getCurrentWindowMetrics().getBounds();
        Rect bounds = new Rect(display);
        bounds.top = Math.max(bounds.top + statusBarHeight() + dp(12), bounds.top);
        bounds.bottom -= dp(28);

        if (target != null) {
            Rect targetBounds = new Rect();
            target.getBoundsInScreen(targetBounds);
            if (targetBounds.width() >= dp(160) && targetBounds.height() >= dp(240)) {
                if (targetBounds.intersect(bounds)) bounds = targetBounds;
            }
        }
        return bounds;
    }

    private int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int systemHeight = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
        return Math.max(systemHeight, dp(28));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void removeOverlay() {
        if (overlay == null || windowManager == null) return;
        try {
            windowManager.removeView(overlay);
        } catch (RuntimeException ignored) {
            // The system may already have detached the accessibility overlay.
        }
        overlay = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Events are not recorded or inspected. They only keep the service connected.
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeOverlay();
        super.onDestroy();
    }

    private static final class NodeCandidate {
        AccessibilityNodeInfo node;
        long score = Long.MIN_VALUE;
    }
}
