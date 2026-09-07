package com.chocho.taptotop;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public final class MainActivity extends Activity {
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildUi() {
        boolean dark = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int background = dark ? Color.rgb(22, 22, 25) : Color.rgb(250, 248, 252);
        int primaryText = dark ? Color.rgb(242, 239, 246) : Color.rgb(30, 28, 32);
        int secondaryText = dark ? Color.rgb(205, 199, 210) : Color.rgb(76, 71, 78);
        int card = dark ? Color.rgb(42, 39, 46) : Color.rgb(238, 232, 242);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(52), dp(24), dp(44));
        content.setBackgroundColor(background);

        TextView title = new TextView(this);
        title.setText("맨 위로 톡");
        title.setTextSize(29);
        title.setTextColor(primaryText);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(title, matchWrap(dp(12)));

        TextView description = new TextView(this);
        description.setText(
                "아이폰처럼 화면 맨 위의 상태 표시줄을 한 번 탭하면 "
                        + "현재 보고 있는 목록이나 웹페이지를 한 번의 자연스러운 "
                        + "관성으로 위쪽으로 빠르게 이동합니다.\n\n"
                        + "상태 표시줄을 아래로 끌면 기존처럼 알림창이 열립니다.");
        description.setTextSize(17);
        description.setTextColor(secondaryText);
        description.setLineSpacing(0, 1.18f);
        content.addView(description, matchWrap(dp(24)));

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setTextColor(primaryText);
        statusView.setBackgroundColor(card);
        statusView.setPadding(dp(18), dp(18), dp(18), dp(18));
        content.addView(statusView, matchWrap(dp(18)));

        Button settingsButton = new Button(this);
        settingsButton.setText("접근성 설정 열기");
        settingsButton.setTextSize(16);
        settingsButton.setOnClickListener(v -> openAccessibilitySettings());
        content.addView(settingsButton, matchWrap(dp(10)));

        Button appInfoButton = new Button(this);
        appInfoButton.setText("앱 정보 열기");
        appInfoButton.setTextSize(16);
        appInfoButton.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        content.addView(appInfoButton, matchWrap(dp(20)));

        TextView privacy = new TextView(this);
        privacy.setText(
                "개인정보 보호\n"
                        + "• 인터넷, 사진, 마이크, 위치 권한을 사용하지 않습니다.\n"
                        + "• 접근성 권한은 스크롤 가능한 화면을 찾고 위로 이동하는 데만 사용합니다.\n"
                        + "• 화면 내용과 사용 기록을 저장하거나 전송하지 않습니다.\n\n"
                        + "아주 긴 X 피드처럼 앱이 맨 위 이동 기능을 외부에 제공하지 "
                        + "않는 화면은 한 번에 완전한 처음까지 가지 않을 수 있습니다.\n\n"
                        + "참고: 게임이나 특수한 화면처럼 일반 스크롤을 사용하지 않는 일부 앱에서는 "
                        + "동작하지 않을 수 있습니다.");
        privacy.setTextSize(14);
        privacy.setTextColor(secondaryText);
        privacy.setLineSpacing(0, 1.15f);
        content.addView(privacy, matchWrap(0));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(background);
        scrollView.addView(content);
        return scrollView;
    }

    private void refreshStatus() {
        boolean enabled = isServiceEnabled();
        statusView.setText(enabled
                ? "상태: 사용 중 ✓\n상단 상태 표시줄을 한 번 탭해 보세요."
                : "상태: 사용 준비 필요\n아래 버튼을 눌러 ‘맨 위로 톡’을 켜 주세요.");
    }

    private boolean isServiceEnabled() {
        AccessibilityManager manager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        ComponentName expected = new ComponentName(this, TapToTopService.class);
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            ComponentName actual = new ComponentName(
                    info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
            if (expected.equals(actual)) return true;
        }
        return false;
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomMargin;
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
