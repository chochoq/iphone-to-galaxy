package com.chocho.airpodsglance;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQUEST_BLUETOOTH = 101;
    private static final int REQUEST_NOTIFICATIONS = 102;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView deviceStatus;
    private TextView leftValue;
    private TextView rightValue;
    private TextView caseValue;
    private TextView precisionInfo;
    private TextView freshness;
    private TextView permissionStatus;
    private TextView diagnostics;
    private ConnectionPopupOverlay previewOverlay;

    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            refreshValues();
            handler.postDelayed(this, 1500L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.ensureChannels(this);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveDeviceIfPossible();
        handler.removeCallbacks(refreshTask);
        handler.post(refreshTask);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (previewOverlay != null) previewOverlay.dismissImmediately();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH) resolveDeviceIfPossible();
        refreshValues();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.cream));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(40));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(dp(22) + bars.left, dp(22) + bars.top,
                    dp(22) + bars.right, dp(40) + bars.bottom);
            return insets;
        });
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("에어팟 한눈에", 30, Color.rgb(23, 32, 54), true);
        root.addView(title);
        TextView subtitle = text("광고 없이, 연결된 동안만 배터리를 확인해요.",
                15, Color.rgb(74, 82, 104), false);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        LinearLayout deviceCard = card();
        deviceStatus = text("AirPods를 찾는 중", 16, Color.WHITE, true);
        deviceCard.addView(deviceStatus);
        freshness = text("아직 확인한 배터리가 없어요", 13, getColor(R.color.text_muted), false);
        freshness.setPadding(0, dp(5), 0, 0);
        deviceCard.addView(freshness);
        root.addView(deviceCard, cardParams());

        LinearLayout batteryCard = lightCard();
        TextView batteryTitle = text("배터리", 18, getColor(R.color.navy), true);
        batteryCard.addView(batteryTitle);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(14), 0, dp(8));
        leftValue = batteryColumn(row, "왼쪽");
        rightValue = batteryColumn(row, "오른쪽");
        caseValue = batteryColumn(row, "케이스");
        batteryCard.addView(row);
        precisionInfo = text("연결 후 실제 값을 확인해요.",
                12, Color.rgb(92, 99, 117), false);
        batteryCard.addView(precisionInfo);
        root.addView(batteryCard, cardParams());

        AppSettings appSettings = new AppSettings(this);
        LinearLayout settingsCard = lightCard();
        settingsCard.addView(text("자동화", 18, getColor(R.color.navy), true));
        settingsCard.addView(switchRow("연결 중 자동으로 확인", appSettings.monitorEnabled(),
                value -> {
                    appSettings.setMonitorEnabled(value);
                    if (!value) stopMonitoringFromUi();
                }));
        settingsCard.addView(switchRow("연결될 때 한 번 알려주기", appSettings.popupEnabled(),
                appSettings::setPopupEnabled));
        settingsCard.addView(switchRow("배터리 부족 알림 (이어버드 20% · 케이스 15%)",
                appSettings.lowBatteryEnabled(), appSettings::setLowBatteryEnabled));
        root.addView(settingsCard, cardParams());

        LinearLayout permissionCard = lightCard();
        permissionCard.addView(text("권한과 시작", 18, getColor(R.color.navy), true));
        permissionStatus = text("", 13, Color.rgb(74, 82, 104), false);
        permissionStatus.setPadding(0, dp(8), 0, dp(10));
        permissionCard.addView(permissionStatus);
        Button bluetoothPermission = button("1. Bluetooth 권한 허용");
        bluetoothPermission.setOnClickListener(v -> requestBluetoothPermissions());
        permissionCard.addView(bluetoothPermission);
        Button notificationPermission = button("2. 알림 권한 허용");
        notificationPermission.setOnClickListener(v -> requestNotificationPermission());
        permissionCard.addView(notificationPermission);
        Button overlayPermission = button("3. 큰 연결 카드 권한 허용");
        overlayPermission.setOnClickListener(v -> requestOverlayPermission());
        permissionCard.addView(overlayPermission);
        Button preview = button("큰 연결 카드 미리보기");
        preview.setOnClickListener(v -> previewLargeCard());
        permissionCard.addView(preview);
        Button start = button("배터리 지금 확인");
        start.setOnClickListener(v -> startMonitoringFromUi());
        permissionCard.addView(start);
        Button widget = button("홈 화면에 위젯 추가");
        widget.setOnClickListener(v -> requestWidgetPin());
        permissionCard.addView(widget);
        Button bluetoothSettings = button("Bluetooth 설정 열기");
        bluetoothSettings.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        permissionCard.addView(bluetoothSettings);
        root.addView(permissionCard, cardParams());

        LinearLayout privacyCard = lightCard();
        privacyCard.addView(text("개인정보와 진단", 18, getColor(R.color.navy), true));
        TextView privacy = text("인터넷·위치·마이크·접근성 권한을 사용하지 않아요. "
                        + "선택 권한인 ‘다른 앱 위에 표시’는 직접 닫는 연결 카드에만 사용해요. "
                        + "Bluetooth 주소와 원시 패킷도 화면이나 로그에 표시하지 않아요.",
                13, Color.rgb(74, 82, 104), false);
        privacy.setPadding(0, dp(8), 0, dp(10));
        privacyCard.addView(privacy);
        diagnostics = text("", 13, Color.rgb(74, 82, 104), false);
        privacyCard.addView(diagnostics);
        root.addView(privacyCard, cardParams());

        setContentView(scroll);
        refreshValues();
    }

    private void resolveDeviceIfPossible() {
        if (!hasBluetoothPermissions()) return;
        PairedAirPodsResolver.Resolution result = new PairedAirPodsResolver(this).resolve();
        if (result.status == PairedAirPodsResolver.Status.SELECTED) {
            AirPodsSnapshot old = new BatteryStateStore(this).load();
            if (old.selectedDeviceName == null) {
                new BatteryStateStore(this).save(new AirPodsSnapshot(
                        result.name, old.connected, old.identityConfidence, old.modelId,
                        old.left, old.right, old.caseBattery, old.observedAtEpochMillis,
                        old.source));
                AirPodsWidgetProvider.updateAll(this);
            }
        }
    }

    private void refreshValues() {
        AirPodsSnapshot snapshot = new BatteryStateStore(this).load();
        long now = System.currentTimeMillis();
        String name = snapshot.selectedDeviceName;
        if (name == null) name = new AppSettings(this).selectedName();
        deviceStatus.setText((name == null ? "AirPods 미선택" : name)
                + " · " + connectionText(snapshot));
        leftValue.setText(componentText(snapshot, snapshot.left, now));
        rightValue.setText(componentText(snapshot, snapshot.right, now));
        caseValue.setText(componentText(snapshot, snapshot.caseBattery, now));
        precisionInfo.setText(precisionText(snapshot.source));
        freshness.setText(freshnessText(snapshot, now));
        permissionStatus.setText(permissionText());
        diagnostics.setText(new DiagnosticsStore(this).summary());
    }

    private String connectionText(AirPodsSnapshot snapshot) {
        if (snapshot.identityConfidence == AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION) {
            return "기기 확인 필요";
        }
        return snapshot.connected ? "연결 중" : "연결 안 됨";
    }

    private String componentText(AirPodsSnapshot snapshot, BatteryComponent component, long now) {
        String value = BatteryPresentation.percent(component);
        return BatteryPresentation.showCharging(snapshot, component, now) ? value + "\n충전 중" : value;
    }

    private String freshnessText(AirPodsSnapshot snapshot, long now) {
        if (snapshot.identityConfidence == AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION) {
            return "주변 기기의 값은 쓰지 않았어요. 실기기 교정이 필요해요.";
        }
        if (snapshot.observedAtEpochMillis <= 0L) return snapshot.connected
                ? "배터리 신호를 기다리는 중" : "아직 확인한 배터리가 없어요";
        long minutes = Math.max(0, (now - snapshot.observedAtEpochMillis) / 60000L);
        if (snapshot.connected && minutes < 1) return "방금 확인 · 현재 값";
        if (minutes < 60) return "마지막 확인 " + minutes + "분 전"
                + (snapshot.connected ? "" : " · 저장된 값");
        long hours = minutes / 60;
        return "마지막 확인 " + hours + "시간 전" + (snapshot.connected ? "" : " · 저장된 값");
    }

    private String permissionText() {
        String bluetooth = hasBluetoothPermissions() ? "Bluetooth ✓" : "Bluetooth 필요";
        String notification = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED ? "알림 ✓" : "알림 필요";
        String overlay = Settings.canDrawOverlays(this) ? "큰 카드 ✓" : "큰 카드 권한 필요";
        return bluetooth + " · " + notification + " · " + overlay
                + "\nBluetooth는 내 AirPods 선택과 배터리 수신에만 사용해요.";
    }

    private String precisionText(AirPodsSnapshot.Source source) {
        if (source == AirPodsSnapshot.Source.AAP_EXACT_PERCENT) {
            return "연결된 AirPods에서 직접 확인한 1% 단위 값이에요.";
        }
        if (source == AirPodsSnapshot.Source.BLE_PUBLIC_DECILE) {
            return "공개 Bluetooth 정보라 10% 단위로 표시돼요.";
        }
        return "연결 후 실제 값을 확인해요.";
    }

    private void requestBluetoothPermissions() {
        requestPermissions(new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        }, REQUEST_BLUETOOTH);
    }

    private void requestNotificationPermission() {
        requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS },
                REQUEST_NOTIFICATIONS);
    }

    private void requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "큰 연결 카드 권한이 이미 허용되어 있어요.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(settings);
    }

    private void previewLargeCard() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "먼저 큰 연결 카드 권한을 허용해 주세요.",
                    Toast.LENGTH_LONG).show();
            requestOverlayPermission();
            return;
        }
        AirPodsSnapshot snapshot = new BatteryStateStore(this).load();
        if (snapshot.left == null && snapshot.right == null && snapshot.caseBattery == null) {
            Toast.makeText(this, "미리 볼 배터리 값이 아직 없어요.", Toast.LENGTH_LONG).show();
            return;
        }
        if (previewOverlay == null) previewOverlay = new ConnectionPopupOverlay(this);
        if (!previewOverlay.show(snapshot, true)) {
            Toast.makeText(this, "큰 카드를 표시하지 못했어요.", Toast.LENGTH_LONG).show();
        }
    }

    private void startMonitoringFromUi() {
        if (!hasBluetoothPermissions()) {
            Toast.makeText(this, "먼저 Bluetooth 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        PairedAirPodsResolver.Resolution result = new PairedAirPodsResolver(this).resolve();
        if (result.status != PairedAirPodsResolver.Status.SELECTED) {
            Toast.makeText(this, "페어링된 AirPods를 찾지 못했어요.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AppSettings(this).setMonitorEnabled(true);
        new ConnectionSessionStore(this).beginIfNeeded();
        try {
            AirPodsMonitorService.startManualDirect(this);
            Toast.makeText(this, "현재 배터리를 확인해요. 직접 연결은 30초 뒤 자동 종료돼요.",
                    Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            new DiagnosticsStore(this).increment("service_start_failed");
            Toast.makeText(this, "배터리 확인을 시작하지 못했어요.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopMonitoringFromUi() {
        AppSettings settings = new AppSettings(this);
        new BatteryStateStore(this).markConnected(false, settings.selectedName());
        new ConnectionSessionStore(this).end();
        AirPodsMonitorService.stop(this);
        AirPodsWidgetProvider.updateAll(this);
    }

    private void requestWidgetPin() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "홈 화면을 길게 눌러 위젯에서 추가해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        ComponentName provider = new ComponentName(this, AirPodsWidgetProvider.class);
        Intent successIntent = new Intent(this, MainActivity.class);
        PendingIntent success = PendingIntent.getActivity(this, 701, successIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.requestPinAppWidget(provider, null, success);
    }

    private boolean hasBluetoothPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.navy));
        background.setCornerRadius(dp(20));
        layout.setBackground(background);
        return layout;
    }

    private LinearLayout lightCard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(20));
        layout.setBackground(background);
        layout.setElevation(dp(2));
        return layout;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        return params;
    }

    private TextView batteryColumn(LinearLayout row, String label) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        TextView labelView = text(label, 13, Color.rgb(92, 99, 117), false);
        TextView value = text("—", 25, getColor(R.color.navy), true);
        value.setGravity(android.view.Gravity.CENTER);
        value.setPadding(0, dp(4), 0, 0);
        column.addView(labelView);
        column.addView(value);
        row.addView(column, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    private View switchRow(String label, boolean checked, OnToggle listener) {
        Switch toggle = new Switch(this);
        toggle.setText(label);
        toggle.setTextSize(14);
        toggle.setTextColor(getColor(R.color.navy));
        toggle.setPadding(0, dp(10), 0, dp(4));
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, value) -> listener.changed(value));
        return toggle;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        params.setMargins(0, dp(5), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface OnToggle { void changed(boolean value); }
}
