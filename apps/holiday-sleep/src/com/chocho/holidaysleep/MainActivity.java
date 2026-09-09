package com.chocho.holidaysleep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
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
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_CALENDAR = 41;

    private TextView statusView;
    private Switch enabledSwitch;
    private Button startTimeButton;
    private Button endTimeButton;
    private Button saveTimeButton;
    private Button resetTimeButton;
    private TextView timeHint;
    private TextView startPrecision;
    private TextView endPrecision;
    private TextView statusSummary;
    private TextView warningView;
    private Button permissionButton;
    private Button detailsButton;
    private LinearLayout detailContent;
    private LinearLayout editActions;
    private boolean detailsExpanded;
    private int draftStart;
    private int draftEnd;
    private boolean savingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScheduleWindow saved = ScheduleSettings.read(this);
        draftStart = savedInstanceState == null ? saved.startSecond
                : savedInstanceState.getInt("draft_start", saved.startSecond);
        draftEnd = savedInstanceState == null ? saved.endSecond
                : savedInstanceState.getInt("draft_end", saved.endSecond);
        detailsExpanded = savedInstanceState != null && savedInstanceState.getBoolean("details_expanded",false);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(buildUi());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("draft_start", draftStart);
        outState.putInt("draft_end", draftEnd);
        outState.putBoolean("details_expanded", detailsExpanded);
        super.onSaveInstanceState(outState);
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(UiStyle.BACKGROUND);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(20) + insets.getSystemWindowInsetLeft(),
                    dp(16) + insets.getSystemWindowInsetTop(),
                    dp(20) + insets.getSystemWindowInsetRight(),
                    dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        FrameLayout frame = new FrameLayout(this);
        LinearLayout content = new LinearLayout(this) {
            @Override protected void onMeasure(int width, int height) {
                int available = MeasureSpec.getSize(width);
                super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(available, dp(560)),
                        MeasureSpec.EXACTLY), height);
            }
        };
        content.setOrientation(LinearLayout.VERTICAL);
        frame.addView(content, new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);

        TextView title = UiStyle.text(this,"공휴일 수면 연장",30,UiStyle.INK,true);
        title.setPadding(0,dp(12),0,0);
        content.addView(title,matchWrap(dp(8)));
        content.addView(UiStyle.text(this,"평일 공휴일에도 방해받지 않도록",14,UiStyle.MUTED,false),matchWrap(dp(28)));

        TextView timeSection=UiStyle.text(this,"방해 금지 시간",13,UiStyle.MUTED,false);
        timeSection.setPadding(dp(16),0,0,0);
        content.addView(timeSection,matchWrap(dp(8)));

        LinearLayout times = UiStyle.panel(this);
        times.setPadding(dp(16),dp(18),dp(16),dp(16));
        LinearLayout timeRow = new LinearLayout(this);
        boolean narrow = getResources().getConfiguration().screenWidthDp < 340
                || getResources().getConfiguration().fontScale > 1.25f;
        timeRow.setOrientation(narrow ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout start = timeColumn(true);
        LinearLayout end = timeColumn(false);
        if (narrow) {
            timeRow.addView(start,matchWrap(dp(12)));
            timeRow.addView(end,matchWrap(0));
        } else {
            timeRow.addView(start,new LinearLayout.LayoutParams(0,-2,1f));
            View divider=new View(this);
            divider.setBackgroundColor(UiStyle.SEPARATOR);
            LinearLayout.LayoutParams dividerParams=new LinearLayout.LayoutParams(dp(1),dp(60));
            dividerParams.setMargins(dp(12),dp(6),dp(12),0);
            timeRow.addView(divider,dividerParams);
            timeRow.addView(end,new LinearLayout.LayoutParams(0,-2,1f));
        }
        times.addView(timeRow,matchWrap(dp(12)));
        View timeDivider=new View(this);
        timeDivider.setBackgroundColor(UiStyle.SEPARATOR);
        times.addView(timeDivider,new LinearLayout.LayoutParams(-1,dp(1)));
        TextView timeHelp=UiStyle.text(this,"시간을 누르면 수정할 수 있어요.",13,UiStyle.MUTED,false);
        timeHelp.setPadding(0,dp(12),0,0);
        times.addView(timeHelp,matchWrap(0));
        content.addView(times,matchWrap(dp(8)));

        timeHint = UiStyle.text(this,"",13,UiStyle.MUTED,false);
        timeHint.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(timeHint,matchWrap(dp(10)));
        editActions = new LinearLayout(this);
        editActions.setOrientation(LinearLayout.HORIZONTAL);
        resetTimeButton = UiStyle.button(this,"취소",false);
        resetTimeButton.setOnClickListener(v -> {
            ScheduleWindow saved = ScheduleSettings.read(this);
            draftStart=saved.startSecond; draftEnd=saved.endSecond;
            refreshTimeEditor();
        });
        editActions.addView(resetTimeButton,new LinearLayout.LayoutParams(0,-2,1f));
        View gap = new View(this);
        editActions.addView(gap,new LinearLayout.LayoutParams(dp(10),1));
        saveTimeButton = UiStyle.button(this,"변경사항 저장",true);
        saveTimeButton.setOnClickListener(v -> saveTimes());
        editActions.addView(saveTimeButton,new LinearLayout.LayoutParams(0,-2,2f));
        content.addView(editActions,matchWrap(dp(16)));

        LinearLayout automatic = UiStyle.panel(this);
        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.addView(UiStyle.text(this,"자동 연장",17,UiStyle.INK,false),
                new LinearLayout.LayoutParams(0,-2,1f));
        enabledSwitch = new UiStyle.SettingSwitch(this);
        enabledSwitch.setContentDescription("공휴일 자동 연장");
        enabledSwitch.setMinHeight(dp(48));
        enabledSwitch.setChecked(Scheduler.isEnabled(this));
        enabledSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            Scheduler.setEnabled(getApplicationContext(),checked);
            refreshStatus();
        });
        switchRow.addView(enabledSwitch,new LinearLayout.LayoutParams(-2,-2));
        automatic.addView(switchRow,matchWrap(dp(4)));
        View autoDivider=new View(this);
        autoDivider.setBackgroundColor(UiStyle.SEPARATOR);
        automatic.addView(autoDivider,new LinearLayout.LayoutParams(-1,dp(1)));
        TextView applies=UiStyle.text(this,"평일 공휴일에만 적용",14,UiStyle.INK,false);
        applies.setPadding(0,dp(12),0,0);
        automatic.addView(applies,matchWrap(dp(6)));
        statusSummary = UiStyle.text(this,"상태 확인 중",13,UiStyle.MUTED,false);
        automatic.addView(statusSummary,matchWrap(0));
        content.addView(automatic,matchWrap(dp(24)));

        warningView = UiStyle.text(this,"",14,UiStyle.WARNING,false);
        warningView.setBackground(UiStyle.rounded(this,UiStyle.WARNING_BG,18));
        warningView.setPadding(dp(16),dp(14),dp(16),dp(14));
        warningView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        warningView.setVisibility(View.GONE);
        content.addView(warningView,matchWrap(dp(8)));
        permissionButton = UiStyle.button(this,"필요한 권한 확인",true);
        permissionButton.setOnClickListener(v -> handlePermissions());
        permissionButton.setVisibility(View.GONE);
        content.addView(permissionButton,matchWrap(dp(14)));

        LinearLayout details = UiStyle.panel(this);
        detailsButton = UiStyle.button(this,"",false);
        detailsButton.setBackground(UiStyle.rounded(this,UiStyle.SURFACE,12));
        detailsButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        detailsButton.setTextSize(17);
        detailsButton.setTypeface(android.graphics.Typeface.create("sans-serif",0));
        detailsButton.setPadding(0,0,0,0);
        detailsButton.setOnClickListener(v -> {
            detailsExpanded=!detailsExpanded;
            refreshDetails();
        });
        details.addView(detailsButton,matchWrap(0));
        detailContent = new LinearLayout(this);
        detailContent.setOrientation(LinearLayout.VERTICAL);
        detailContent.setPadding(0,dp(12),0,0);
        statusView = UiStyle.text(this,"",14,UiStyle.MUTED,false);
        detailContent.addView(statusView,matchWrap(dp(16)));
        Button refresh = UiStyle.button(this,"상태 다시 확인",false);
        refresh.setOnClickListener(v -> new Thread(() -> {
            Scheduler.configureAndReconcile(getApplicationContext());
            runOnUiThread(this::refreshStatus);
        },"HolidaySleepManualRefresh").start());
        detailContent.addView(refresh,matchWrap(dp(10)));
        Button permissions = UiStyle.button(this,"권한 및 설정",false);
        permissions.setOnClickListener(v -> handlePermissions());
        detailContent.addView(permissions,matchWrap(dp(18)));
        TextView note = UiStyle.text(this,
                "주말과 평소 수면 일정은 삼성 모드에서 설정해 주세요. 이 앱의 시작 시간과 자동으로 맞춰지지는 않아요.\n\n"
                + "삼성 수면 아이콘·잠금화면·기상 알람은 바꾸지 않아요. 통화 연결, 마이크와 블루투스도 그대로예요.\n\n"
                + "인터넷 접속 없이 폰에 동기화된 한국 공휴일 달력을 사용해요.\n\n"
                + "기존 시작 시간의 15초는 직접 시작 시간을 다시 고르기 전까지 유지돼요. 다시 고르면 해당 분의 00초가 돼요.",
                13,UiStyle.MUTED,false);
        detailContent.addView(note,matchWrap(0));
        details.addView(detailContent);
        content.addView(details,matchWrap(dp(18)));
        content.addView(UiStyle.text(this,"알람과 반복 전화는 허용해요.\n평소 수면 일정과 공휴일 알람은 삼성 모드·시계에서 설정해요.",
                12,UiStyle.MUTED,false),matchWrap(0));
        refreshTimeEditor();
        refreshDetails();
        return scroll;
    }

    private LinearLayout timeColumn(boolean start) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView label = UiStyle.text(this,start?"시작":"종료",13,UiStyle.MUTED,false);
        label.setPadding(0,0,0,dp(4));
        column.addView(label);
        Button time = UiStyle.button(this,"",false);
        time.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x11007aff),
                UiStyle.rounded(this,UiStyle.SURFACE,8),null));
        time.setTextColor(UiStyle.INK);
        time.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        time.setPadding(0,0,0,0);
        time.setTextSize(34);
        time.setFontFeatureSettings("tnum");
        time.setSingleLine(true);
        time.setAutoSizeTextTypeUniformWithConfiguration(22,34,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        time.setOnClickListener(v -> chooseTime(start));
        int timeHeight = Math.max(dp(52), Math.round(44 * getResources().getDisplayMetrics().scaledDensity));
        column.addView(time,new LinearLayout.LayoutParams(-1,timeHeight));
        TextView precision = UiStyle.text(this,"",12,UiStyle.MUTED,false);
        precision.setPadding(0,dp(4),0,0);
        column.addView(precision);
        if(start) { startTimeButton=time; startPrecision=precision; }
        else { endTimeButton=time; endPrecision=precision; }
        return column;
    }

    private void refreshDetails() {
        detailContent.setVisibility(detailsExpanded?View.VISIBLE:View.GONE);
        detailsButton.setText(detailsExpanded?"상세 상태 접기  ∧":"상세 상태 보기  ∨");
        detailsButton.setContentDescription(detailsExpanded?"상세 상태, 펼쳐짐. 접기":"상세 상태, 접힘. 펼치기");
    }

    private void refreshStatus() {
        if (statusView == null || isDestroyed()) return;
        boolean calendar = HolidayCalendar.hasCalendarPermission(this);
        boolean dnd = ZenController.hasPolicyAccess(this);
        boolean exact = Scheduler.canScheduleExact(this);
        boolean missingCalendarSource = false;
        boolean holidayToday = false;

        StringBuilder text = new StringBuilder();
        text.append("자동 연장: ").append(Scheduler.isEnabled(this) ? "켜짐" : "꺼짐").append('\n');
        text.append("저장된 시간: ").append(ScheduleSettings.read(this).summary()).append('\n');
        text.append("캘린더 권한: ").append(calendar ? "허용" : "필요").append('\n');
        text.append("방해 금지 권한: ").append(dnd ? "허용" : "필요").append('\n');
        text.append("정확한 시간 실행: ").append(exact ? "허용" : "필요").append('\n');

        if (calendar) {
            List<String> calendars = HolidayCalendar.detectedCalendarNames(this);
            missingCalendarSource = calendars.isEmpty();
            text.append("사용 달력: ")
                    .append(calendars.isEmpty() ? "공휴일 달력을 찾지 못함" : String.join(", ", calendars))
                    .append('\n');
            List<String> today = HolidayCalendar.holidayTitlesForDate(this, LocalDate.now());
            holidayToday = !today.isEmpty();
            text.append("오늘: ")
                    .append(today.isEmpty() ? "법정공휴일 아님" : String.join(", ", today))
                    .append('\n');
        }

        text.append("다음 시작 확인: ").append(Scheduler.nextScheduleSummary(this, Scheduler.ACTION_START)).append('\n');
        text.append("다음 종료 확인: ").append(Scheduler.nextScheduleSummary(this, Scheduler.ACTION_STOP)).append('\n');
        text.append("매일 예약 시각에 확인하고, 평일 공휴일에만 켭니다.\n");
        text.append("현재 연장: ").append(ZenController.isActive(this) ? "켜짐" : "꺼짐");
        statusView.setText(text.toString());
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (!calendar) missing.add("캘린더 읽기 권한");
        if (!dnd) missing.add("방해 금지 접근 권한");
        if (!exact) missing.add("정확한 알람 권한");
        if (missingCalendarSource) missing.add("폰에 동기화된 한국 공휴일 달력");
        boolean attention = !missing.isEmpty();
        warningView.setText("자동 연장을 위해 확인해 주세요.\n" + String.join(" · ",missing));
        warningView.setVisibility(attention ? View.VISIBLE : View.GONE);
        permissionButton.setVisibility(!calendar || !dnd || !exact ? View.VISIBLE : View.GONE);
        boolean weekend = LocalDate.now().getDayOfWeek().getValue() >= 6;
        statusSummary.setText(!Scheduler.isEnabled(this) ? "자동 연장이 꺼져 있어요."
                : attention ? "자동 실행 준비를 확인해 주세요."
                : ZenController.isActive(this) ? "지금은 조용하게 쉬는 시간이에요."
                : weekend ? "주말은 삼성 수면 일정이 담당해요."
                : holidayToday ? "오늘은 공휴일이에요. 저장된 시간에만 적용해요."
                : "오늘은 공휴일이 아니에요.");
    }

    private void chooseTime(boolean start) {
        LocalTime initial = LocalTime.ofSecondOfDay(start ? draftStart : draftEnd);
        TimePickerDialog dialog = new TimePickerDialog(this, (picker, hour, minute) -> {
            int selected = hour * 3600 + minute * 60;
            if (start) draftStart = selected;
            else draftEnd = selected;
            refreshTimeEditor();
        }, initial.getHour(), initial.getMinute(), true);
        dialog.setTitle(start ? "방해 금지 시작 시간" : "방해 금지 종료 시간");
        dialog.show();
    }

    private void refreshTimeEditor() {
        startTimeButton.setText(ScheduleWindow.format(draftStart - draftStart % 60));
        endTimeButton.setText(ScheduleWindow.format(draftEnd - draftEnd % 60));
        startTimeButton.setContentDescription("시작 시간 " + ScheduleWindow.format(draftStart) + ", 변경");
        endTimeButton.setContentDescription("종료 시간 " + ScheduleWindow.format(draftEnd) + ", 변경");
        startPrecision.setText((draftStart % 60) + "초부터 시작");
        endPrecision.setText((draftEnd % 60) + "초에 종료");
        startPrecision.setVisibility(draftStart % 60 == 0 ? View.INVISIBLE : View.VISIBLE);
        endPrecision.setVisibility(draftEnd % 60 == 0 ? View.INVISIBLE : View.VISIBLE);
        ScheduleWindow saved = ScheduleSettings.read(this);
        boolean changed = draftStart != saved.startSecond || draftEnd != saved.endSecond;
        boolean valid = ScheduleWindow.valid(draftStart, draftEnd);
        String hint = savingTime ? "시간을 저장하고 있어요."
                : !valid ? "종료는 같은 날의 시작보다 늦어야 해요."
                : "저장하지 않은 변경사항이 있어요.";
        timeHint.setText(hint);
        timeHint.setTextColor(valid ? UiStyle.MUTED : UiStyle.WARNING);
        timeHint.setVisibility(changed || savingTime ? View.VISIBLE : View.GONE);
        editActions.setVisibility(changed || savingTime ? View.VISIBLE : View.GONE);
        saveTimeButton.setEnabled(changed && valid && !savingTime);
        resetTimeButton.setEnabled(changed && !savingTime);
        startTimeButton.setEnabled(!savingTime);
        endTimeButton.setEnabled(!savingTime);
    }

    private void saveTimes() {
        if (savingTime || !ScheduleWindow.valid(draftStart, draftEnd)) return;
        final ScheduleWindow selected = new ScheduleWindow(draftStart, draftEnd);
        savingTime = true;
        refreshTimeEditor();
        new Thread(() -> {
            boolean result;
            try {
                result = Scheduler.saveSchedule(getApplicationContext(), selected);
            } catch (RuntimeException executionFailed) {
                // Storage may have succeeded while a system service failed afterwards.
                // Do not leave the editor locked or claim that automation was applied.
                result = false;
            }
            final boolean saved = result;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                savingTime = false;
                refreshTimeEditor();
                refreshStatus();
                String message = !saved ? "저장을 확인하지 못했어요. 저장된 시간을 다시 확인해 주세요."
                        : !Scheduler.isEnabled(this) ? "시간을 저장했어요. 자동 연장은 꺼진 상태예요."
                        : !allPermissionsReady() ? "시간을 저장했어요. 자동 실행에 필요한 권한도 확인해 주세요."
                        : "시간을 저장하고 실행 일정을 갱신했어요.";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            });
        }, "HolidaySleepSaveTime").start();
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
