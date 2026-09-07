package com.chocho.holidaysleep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_CALENDAR = 41;

    private TextView statusView;
    private Switch enabledSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsReady()) {
            new Thread(() -> {
                Scheduler.configureAndReconcile(getApplicationContext());
                runOnUiThread(this::refreshStatus);
            }, "HolidaySleepRefresh").start();
        } else {
            refreshStatus();
        }
    }

    private View buildUi() {
        int padding = dp(24);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, dp(42), padding, dp(42));

        TextView title = new TextView(this);
        title.setText("공휴일 수면 연장");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(25, 25, 25));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(title, matchWrap(dp(12)));

        TextView description = new TextView(this);
        description.setText(
                "평일에 한국 법정공휴일이 겹치면 오전 7시 00분 15초부터 정오까지 "
                        + "방해 금지를 이어갑니다. 알람과 15분 안에 두 번 걸려온 전화는 울립니다.\n\n"
                        + "인터넷 권한 없이 휴대폰에 동기화된 한국 공휴일 달력을 사용합니다.");
        description.setTextSize(17);
        description.setTextColor(Color.rgb(70, 70, 70));
        description.setLineSpacing(0, 1.18f);
        content.addView(description, matchWrap(dp(24)));

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("자동 연장 사용");
        enabledSwitch.setTextSize(18);
        enabledSwitch.setChecked(Scheduler.isEnabled(this));
        enabledSwitch.setPadding(0, dp(10), 0, dp(10));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Scheduler.setEnabled(getApplicationContext(), isChecked);
            refreshStatus();
        });
        content.addView(enabledSwitch, matchWrap(dp(22)));

        statusView = new TextView(this);
        statusView.setTextSize(16);
        statusView.setTextColor(Color.rgb(40, 40, 40));
        statusView.setBackgroundColor(Color.rgb(240, 244, 248));
        statusView.setPadding(dp(18), dp(18), dp(18), dp(18));
        content.addView(statusView, matchWrap(dp(18)));

        Button permissionButton = new Button(this);
        permissionButton.setText("권한 확인 및 설정");
        permissionButton.setTextSize(16);
        permissionButton.setOnClickListener(v -> handlePermissions());
        content.addView(permissionButton, matchWrap(dp(10)));

        Button refreshButton = new Button(this);
        refreshButton.setText("지금 상태 다시 확인");
        refreshButton.setTextSize(16);
        refreshButton.setOnClickListener(v -> new Thread(() -> {
            Scheduler.configureAndReconcile(getApplicationContext());
            runOnUiThread(() -> {
                refreshStatus();
                Toast.makeText(this, "확인했습니다", Toast.LENGTH_SHORT).show();
            });
        }, "HolidaySleepManualRefresh").start());
        content.addView(refreshButton, matchWrap(dp(8)));

        TextView note = new TextView(this);
        note.setText(
                "참고: 공휴일 오전 7시 이후에는 삼성 ‘수면’ 아이콘이 사라질 수 있지만, "
                        + "이 앱의 전용 방해 금지 규칙이 정오까지 같은 역할을 이어갑니다. "
                        + "통화 연결, 마이크, 블루투스는 변경하지 않습니다.");
        note.setTextSize(14);
        note.setTextColor(Color.rgb(90, 90, 90));
        note.setLineSpacing(0, 1.15f);
        content.addView(note, matchWrap(0));

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        return scrollView;
    }

    private void refreshStatus() {
        if (statusView == null) return;
        boolean calendar = HolidayCalendar.hasCalendarPermission(this);
        boolean dnd = ZenController.hasPolicyAccess(this);
        boolean exact = Scheduler.canScheduleExact(this);

        StringBuilder text = new StringBuilder();
        text.append("상태: ").append(Scheduler.isEnabled(this) ? "사용 중" : "꺼짐").append('\n');
        text.append("캘린더 권한: ").append(calendar ? "허용" : "필요").append('\n');
        text.append("방해 금지 권한: ").append(dnd ? "허용" : "필요").append('\n');
        text.append("정확한 시간 실행: ").append(exact ? "허용" : "필요").append('\n');

        if (calendar) {
            List<String> calendars = HolidayCalendar.detectedCalendarNames(this);
            text.append("사용 달력: ")
                    .append(calendars.isEmpty() ? "공휴일 달력을 찾지 못함" : String.join(", ", calendars))
                    .append('\n');
            List<String> today = HolidayCalendar.holidayTitlesForDate(this, LocalDate.now());
            text.append("오늘: ")
                    .append(today.isEmpty() ? "법정공휴일 아님" : String.join(", ", today))
                    .append('\n');
        }

        text.append("다음 오전 확인: ").append(Scheduler.nextScheduleSummary(Scheduler.ACTION_START)).append('\n');
        text.append("다음 정오 해제: ").append(Scheduler.nextScheduleSummary(Scheduler.ACTION_STOP)).append('\n');
        text.append("현재 연장: ").append(ZenController.isActive(this) ? "켜짐" : "꺼짐");
        statusView.setText(text.toString());
    }

    private void handlePermissions() {
        if (!HolidayCalendar.hasCalendarPermission(this)) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, REQUEST_CALENDAR);
            return;
        }
        if (!ZenController.hasPolicyAccess(this)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            return;
        }
        if (!Scheduler.canScheduleExact(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        Scheduler.configureAndReconcile(this);
        refreshStatus();
        Toast.makeText(this, "필요한 권한이 모두 허용되어 있습니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
        if (requestCode == REQUEST_CALENDAR
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handlePermissions();
        }
    }

    private boolean allPermissionsReady() {
        return HolidayCalendar.hasCalendarPermission(this)
                && ZenController.hasPolicyAccess(this)
                && Scheduler.canScheduleExact(this);
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
