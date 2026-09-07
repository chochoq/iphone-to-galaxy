package com.chocho.airpodsglance;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** User-authorized connection card. It remains until tapped, closed, or its session ends. */
public final class ConnectionPopupOverlay {
    private static final long VISIBLE_MILLIS = 6_000L;

    private final Context context;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View currentView;

    private final Runnable autoDismiss = new Runnable() {
        @Override public void run() { dismissAnimated(); }
    };

    public ConnectionPopupOverlay(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = this.context.getSystemService(WindowManager.class);
    }

    public boolean show(AirPodsSnapshot snapshot, boolean explicitPreview) {
        if (snapshot == null || windowManager == null) return false;
        PowerManager power = context.getSystemService(PowerManager.class);
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        boolean interactive = power != null && power.isInteractive();
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        boolean dndActive = isDndActive();
        boolean hasReading = snapshot.left != null || snapshot.right != null
                || snapshot.caseBattery != null;
        if (!ConnectionPopupPolicy.shouldShow(
                Settings.canDrawOverlays(context), interactive, locked, dndActive,
                hasReading, explicitPreview)) {
            return false;
        }

        dismissImmediately();
        View card = buildCard(snapshot, explicitPreview);
        int availableWidth = context.getResources().getDisplayMetrics().widthPixels;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                Math.max(dp(304), availableWidth - dp(24)),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = dp(10);
        params.dimAmount = 0.30f;

        try {
            windowManager.addView(card, params);
        } catch (RuntimeException error) {
            return false;
        }
        currentView = card;
        card.setAlpha(0f);
        card.setTranslationY(dp(48));
        card.animate().alpha(1f).translationY(0f).setDuration(280L)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        if (ConnectionPopupPolicy.shouldAutoDismiss(explicitPreview)) {
            handler.postDelayed(autoDismiss, VISIBLE_MILLIS);
        }
        return true;
    }

    public void dismissImmediately() {
        handler.removeCallbacks(autoDismiss);
        View view = currentView;
        currentView = null;
        if (view == null || windowManager == null) return;
        try { windowManager.removeViewImmediate(view); } catch (RuntimeException ignored) { }
    }

    private void dismissAnimated() {
        handler.removeCallbacks(autoDismiss);
        final View view = currentView;
        currentView = null;
        if (view == null || windowManager == null) return;
        view.animate().alpha(0f).translationY(dp(28)).setDuration(200L)
                .setInterpolator(new AccelerateInterpolator(1.2f))
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        try { windowManager.removeViewImmediate(view); }
                        catch (RuntimeException ignored) { }
                    }
                }).start();
    }

    private View buildCard(AirPodsSnapshot snapshot, boolean preview) {
        boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int surface = dark ? Color.rgb(44, 44, 46) : Color.rgb(242, 242, 247);
        int primary = dark ? Color.WHITE : Color.rgb(17, 17, 20);
        int secondary = dark ? Color.rgb(174, 174, 178) : Color.rgb(99, 99, 102);
        int track = dark ? Color.rgb(142, 142, 147) : Color.rgb(142, 142, 147);
        ProductCompositionPolicy.Decision composition = ProductCompositionPolicy.decide(
                known(snapshot.left),
                known(snapshot.right),
                known(snapshot.caseBattery),
                true,
                true,
                true);
        boolean showCase = composition.showCase;

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(16), dp(7), dp(16), dp(20));
        card.setElevation(dp(18));
        card.setBackground(rounded(surface, dp(30)));
        card.setContentDescription((preview ? "큰 연결 카드 미리보기, " : "")
                + (snapshot.selectedDeviceName == null
                ? "AirPods" : snapshot.selectedDeviceName) + ", "
                + NotificationHelper.batteryLine(snapshot, System.currentTimeMillis()));

        FrameLayout header = new FrameLayout(context);
        header.setMinimumHeight(dp(54));
        TextView name = label(snapshot.selectedDeviceName == null
                ? "AirPods" : snapshot.selectedDeviceName, 18, primary, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        name.setPadding(dp(48), 0, dp(48), 0);
        header.addView(name, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(54), Gravity.CENTER));

        FrameLayout closeTarget = new FrameLayout(context);
        FrameLayout.LayoutParams closeTargetParams = new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.END | Gravity.CENTER_VERTICAL);
        TextView close = label("×", 21, primary, false);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("닫기");
        close.setOnClickListener(v -> dismissAnimated());
        close.setBackground(oval(dark ? Color.rgb(72, 72, 74) : Color.rgb(216, 216, 221)));
        FrameLayout.LayoutParams closeVisualParams = new FrameLayout.LayoutParams(
                dp(31), dp(31), Gravity.CENTER);
        closeTarget.addView(close, closeVisualParams);
        closeTarget.setContentDescription("닫기");
        closeTarget.setOnClickListener(v -> dismissAnimated());
        header.addView(closeTarget, closeTargetParams);
        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout productRow = new LinearLayout(context);
        productRow.setOrientation(LinearLayout.HORIZONTAL);
        productRow.setGravity(Gravity.CENTER);
        productRow.setPadding(0, dp(3), 0, dp(3));
        if (composition.earbudsArtwork != ProductCompositionPolicy.EarbudsArtwork.NONE) {
            // A one-sided battery report keeps the verified pair fallback until the iOS
            // single-bud visual is observed; the policy deliberately marks it as a candidate.
            addEarbudsGroup(productRow, snapshot, dark, primary, secondary, track, showCase);
        }
        if (showCase) addCaseGroup(productRow, snapshot, dark, primary, secondary, track);
        card.addView(productRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        card.setOnClickListener(v -> {
            dismissAnimated();
        });
        return card;
    }

    private void addEarbudsGroup(LinearLayout row, AirPodsSnapshot snapshot,
                                  boolean dark, int primary, int secondary, int track,
                                  boolean shareRow) {
        LinearLayout group = productGroup();
        ProductTurntableView art = new ProductTurntableView(
                context,
                R.drawable.airpods_pair_turntable_3d,
                R.drawable.airpods_pair_render,
                "AirPods 이어버드");
        group.addView(art, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(142)));

        LinearLayout values = new LinearLayout(context);
        values.setOrientation(LinearLayout.HORIZONTAL);
        values.setGravity(Gravity.CENTER);
        addBatteryValue(values, snapshot, "왼쪽", snapshot.left, primary, secondary, track);
        addBatteryValue(values, snapshot, "오른쪽", snapshot.right, primary, secondary, track);
        group.addView(values, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = shareRow
                ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f)
                : new LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT);
        row.addView(group, params);
    }

    private void addCaseGroup(LinearLayout row, AirPodsSnapshot snapshot,
                              boolean dark, int primary, int secondary, int track) {
        LinearLayout group = productGroup();
        ProductTurntableView art = new ProductTurntableView(
                context,
                R.drawable.airpods_case_closed_turntable_3d_v3,
                R.drawable.airpods_case_closed_render_v3,
                "AirPods 충전 케이스");
        group.addView(art, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(142)));
        LinearLayout values = new LinearLayout(context);
        values.setGravity(Gravity.CENTER);
        addBatteryValue(values, snapshot, "케이스", snapshot.caseBattery,
                primary, secondary, track);
        group.addView(values, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(group, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.85f));
    }

    private LinearLayout productGroup() {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setGravity(Gravity.CENTER_HORIZONTAL);
        group.setPadding(dp(3), 0, dp(3), dp(8));
        return group;
    }

    private static boolean known(BatteryComponent component) {
        return component != null && component.percent != null;
    }

    private void addBatteryValue(LinearLayout row, AirPodsSnapshot snapshot,
                                 String componentName, BatteryComponent component,
                                 int primary, int secondary, int track) {
        boolean charging = BatteryPresentation.showCharging(
                snapshot, component, System.currentTimeMillis());
        LinearLayout value = new LinearLayout(context);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setGravity(Gravity.CENTER_HORIZONTAL);
        value.setPadding(dp(5), 0, dp(5), 0);
        value.setContentDescription(ConnectionSheetPresentation.sideLabel(
                componentName, component) + (charging ? ", 충전 중" : ""));
        BatteryGlyphView glyph = new BatteryGlyphView(context, component, charging, track);
        value.addView(glyph, new LinearLayout.LayoutParams(dp(29), dp(14)));
        TextView percent = label(BatteryPresentation.percent(component),
                16, component == null || component.percent == null ? secondary : primary, true);
        percent.setSingleLine(true);
        percent.setGravity(Gravity.CENTER);
        percent.setPadding(0, dp(3), 0, 0);
        value.addView(percent, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView part = label(componentName, 11, secondary, false);
        part.setSingleLine(true);
        part.setGravity(Gravity.CENTER);
        part.setPadding(0, dp(1), 0, 0);
        value.addView(part, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(value, new LinearLayout.LayoutParams(
                dp(78), LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private boolean isDndActive() {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;
        try {
            int filter = manager.getCurrentInterruptionFilter();
            return filter != NotificationManager.INTERRUPTION_FILTER_ALL
                    && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private TextView label(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radius);
        return background;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
