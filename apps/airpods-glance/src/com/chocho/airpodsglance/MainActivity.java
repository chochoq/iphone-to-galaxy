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
import com.chocho.ui.SettingsSheet;
import com.chocho.ui.EditorDialog;
import com.chocho.ui.DetailsScreen;
import com.chocho.ui.DetailsContent;
import com.chocho.ui.SaveJobs;
import com.chocho.ui.Ui;

public final class MainActivity extends Activity implements EditorDialog.Host,DetailsScreen.Host {
    private static final int REQUEST_BLUETOOTH = 101;
    private static final int REQUEST_NOTIFICATIONS = 102;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView deviceStatus;
    private TextView leftValue;
    private TextView rightValue;
    private TextView caseValue;
    private TextView precisionInfo;
    private TextView freshness;
    private DetailsContent.Summary permissionStatus;
    private DetailsContent.Summary diagnostics;
    private ConnectionPopupOverlay previewOverlay;
    private SettingsUi ui;
    private TextView cardTimeValue, budThresholdValue, caseThresholdValue;
    private TextView permissionSummary;
    private LinearLayout extraDetails;
    private boolean detailsExpanded;
    private Button detailsButton;
    private Switch monitorToggle;
    private ListeningController listening;

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
        listening.start();
        handler.removeCallbacks(refreshTask);
        handler.post(refreshTask);
    }

    @Override
    protected void onPause() {
        listening.stop();
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        // Dialogs don't pause their activity. Don't keep changing accessibility text behind a picker.
        handler.removeCallbacks(refreshTask);
        if(focused && !isFinishing()) handler.post(refreshTask);
    }

    @Override
    protected void onDestroy() {
        if(listening!=null)listening.stop();
        if (previewOverlay != null) previewOverlay.dismissImmediately();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH) { resolveDeviceIfPossible(); listening.permissionsChanged(); }
        refreshValues();
    }

    private void buildScreen() {
        ui=new SettingsUi(this,"에어팟 한눈에","");
        AppSettings settings=new AppSettings(this);
        LinearLayout battery=ui.group(null);
        deviceStatus=ui.text("AirPods",20,SettingsUi.INK,true);
        deviceStatus.setPadding(dp(16),dp(16),dp(16),0);battery.addView(deviceStatus);
        freshness=ui.note(battery,"아직 확인한 배터리가 없어요");
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(16),dp(16),dp(16),dp(12));
        leftValue=batteryColumn(row,"왼쪽");rightValue=batteryColumn(row,"오른쪽");caseValue=batteryColumn(row,"케이스");battery.addView(row);
        precisionInfo=ui.note(battery,"");ui.separator(battery);
        ui.row(battery,"연결 카드 미리보기  ›",this::previewLargeCard);
        ui.footer("미리보기는 마지막으로 확인한 배터리를 보여줘요.");

        ListeningControlsView listeningView=new ListeningControlsView(this,ui);
        listening=new ListeningController(this,listeningView);
        listeningView.bind(listening);

        LinearLayout connection=ui.group("연결과 카드");
        monitorToggle=ui.toggle(connection,"연결 중 배터리 확인",settings.monitorEnabled(),value->{
            settings.setMonitorEnabled(value);if(!value)stopMonitoringFromUi();
        });
        ui.separator(connection);ui.toggle(connection,"연결될 때 알려주기",settings.popupEnabled(),settings::setPopupEnabled);
        ui.separator(connection);cardTimeValue=ui.setting(connection,"카드 닫는 방식",()->EditorDialog.card(this));
        ui.footer("다음에 연결 카드가 표시될 때 적용돼요.");

        LinearLayout low=ui.group("배터리 부족 알림");
        ui.toggle(low,"배터리 부족 알림",settings.lowBatteryEnabled(),settings::setLowBatteryEnabled);
        ui.separator(low);budThresholdValue=ui.setting(low,"이어버드 기준",()->chooseThreshold(true));
        ui.separator(low);caseThresholdValue=ui.setting(low,"케이스 기준",()->chooseThreshold(false));
        ui.footer("다음 배터리 수신부터 적용해요.\n같은 낮은 잔량으로 알림을 반복하지 않아요.");

        LinearLayout tools=ui.group("도구 및 정보");
        ui.row(tools,"배터리 지금 확인  ›",this::startMonitoringFromUi);ui.separator(tools);
        ui.row(tools,"홈 화면에 위젯 추가  ›",this::requestWidgetPin);ui.separator(tools);
        ui.row(tools,"조절값 기본값으로",()->{
            UserOptions d=UserOptions.defaults();
            EditorDialog.reset(this,new int[]{d.cardSeconds,d.budThreshold,d.caseThreshold},
                    "직접 닫기 · 이어버드 20%\n케이스 15%로 돌아가요.\n\n사용 여부·권한은 유지해요.\n기기와 배터리 기록도 남아요.");
        }).setTextColor(Ui.RED);
        ui.separator(tools);ui.row(tools,"권한 및 진단  ›",()->DetailsScreen.open(this));
        permissionSummary=ui.footer("");
        ui.install();refreshValues();
    }
    private void chooseThreshold(boolean buds) {
        EditorDialog.number(this,buds?"buds":"case",buds?"이어버드 알림 기준":"케이스 알림 기준",
                5,50,"%","이 값 이하일 때 알려줘요.\n다음 유효 배터리 수신부터 적용돼요.");
    }
    private void refreshOptionLabels() {
        UserOptions options=new AppSettings(this).options();
        cardTimeValue.setText(options.cardSeconds==0?"직접 닫기":options.cardSeconds+"초 후");
        budThresholdValue.setText(options.budThreshold+"% 이하");
        caseThresholdValue.setText(options.caseThreshold+"% 이하");
    }
    @Override public int[] editorValues(String key){return optionValues(new AppSettings(this).options(),key);}
    private static int[] optionValues(UserOptions o,String key){
        if("card".equals(key))return new int[]{o.cardSeconds};
        if("buds".equals(key))return new int[]{o.budThreshold};
        if("case".equals(key))return new int[]{o.caseThreshold};
        if("reset".equals(key))return new int[]{o.cardSeconds,o.budThreshold,o.caseThreshold};
        throw new IllegalArgumentException(key);
    }
    @Override public java.util.concurrent.Callable<SaveJobs.Result> editorSave(String key,int[] before,int[] selected){
        android.content.Context context=getApplicationContext();
        return ()->{synchronized(AppSettings.class){
            AppSettings settings=new AppSettings(context);UserOptions current=settings.options();
            if(!java.util.Arrays.equals(before,optionValues(current,key)))return SaveJobs.Result.failed("다른 변경이 있어요. 취소한 뒤 다시 열어 주세요.");
            UserOptions next="reset".equals(key)?new UserOptions(selected[0],selected[1],selected[2]):
                    new UserOptions("card".equals(key)?selected[0]:current.cardSeconds,
                            "buds".equals(key)?selected[0]:current.budThreshold,
                            "case".equals(key)?selected[0]:current.caseThreshold);
            return settings.saveOptions(next)?SaveJobs.Result.saved("설정을 저장했어요."):SaveJobs.Result.failed("저장하지 못했어요. 값을 확인하고 다시 시도해 주세요.");
        }};
    }
    @Override public void editorUpdated(){if(deviceStatus!=null&&!isDestroyed())refreshValues();}
    @Override public View createDetails(){
        DetailsContent content=new DetailsContent(this);LinearLayout permissions=content.section("권한 설정");
        content.action(permissions,"Bluetooth 권한",this::requestBluetoothPermissions);
        content.action(permissions,"알림 권한",this::requestNotificationPermission);
        content.action(permissions,"다른 앱 위에 표시",this::requestOverlayPermission);
        content.action(permissions,"Bluetooth 설정 열기",()->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        permissionStatus=content.summary(content.section("허용 상태"),permissionText());
        LinearLayout privacy=content.section("개인정보");
        content.paragraph(privacy,"필요한 권한만 사용해요","Bluetooth는 내 AirPods 선택·배터리 수신·소음 제어에, 다른 앱 위에 표시 권한은 연결 카드에 사용해요. 인터넷·위치·마이크·접근성 권한은 사용하지 않아요.");
        diagnostics=content.summary(content.section("진단 정보"),new DiagnosticsStore(this).summary());
        return content;
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
        if(permissionStatus!=null)permissionStatus.setText(permissionText());
        if(diagnostics!=null)diagnostics.setText(new DiagnosticsStore(this).summary());
        permissionSummary.setText(hasBluetoothPermissions() && Settings.canDrawOverlays(this)
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
                ? "필요한 권한이 허용되어 있어요." : "일부 권한이 필요해요. ‘권한 및 진단’에서 확인해 주세요.");
        refreshOptionLabels();
        boolean monitor=new AppSettings(this).monitorEnabled();
        if(monitorToggle.isChecked()!=monitor) monitorToggle.setChecked(monitor);
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
        String bluetooth = hasBluetoothPermissions() ? "Bluetooth: 허용" : "Bluetooth: 필요";
        String notification = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED ? "알림: 허용" : "알림: 필요";
        String overlay = Settings.canDrawOverlays(this) ? "연결 카드: 허용" : "연결 카드: 권한 필요";
        return bluetooth + "\n" + notification + "\n" + overlay;
    }

    private String precisionText(AirPodsSnapshot.Source source) {
        if (source == AirPodsSnapshot.Source.AAP_EXACT_PERCENT) {
            return "에어팟이 보낸 1% 단위 배터리 값이에요.";
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
            SettingsSheet.message(this,"큰 연결 카드 권한이 이미 허용되어 있어요.");
            return;
        }
        Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(settings);
    }

    private void previewLargeCard() {
        if (!Settings.canDrawOverlays(this)) {
            SettingsSheet.message(this,"먼저 큰 연결 카드 권한을 허용해 주세요.");
            requestOverlayPermission();
            return;
        }
        AirPodsSnapshot snapshot = new BatteryStateStore(this).load();
        if (snapshot.left == null && snapshot.right == null && snapshot.caseBattery == null) {
            SettingsSheet.message(this,"미리 볼 배터리 값이 아직 없어요.");
            return;
        }
        if (previewOverlay == null) previewOverlay = new ConnectionPopupOverlay(this);
        if (!previewOverlay.show(snapshot, true)) {
            SettingsSheet.message(this,"큰 카드를 표시하지 못했어요.");
        }
    }

    private void startMonitoringFromUi() {
        if (!hasBluetoothPermissions()) {
            SettingsSheet.message(this,"먼저 Bluetooth 권한을 허용해 주세요.");
            return;
        }
        PairedAirPodsResolver.Resolution result = new PairedAirPodsResolver(this).resolve();
        if (result.status != PairedAirPodsResolver.Status.SELECTED) {
            SettingsSheet.message(this,"페어링된 AirPods를 찾지 못했어요.");
            return;
        }
        new AppSettings(this).setMonitorEnabled(true);
        new ConnectionSessionStore(this).beginIfNeeded();
        try {
            AirPodsMonitorService.startManualDirect(this);
            SettingsSheet.message(this,"현재 배터리를 확인해요. 직접 연결은 30초 뒤 자동 종료돼요.");
        } catch (RuntimeException error) {
            new DiagnosticsStore(this).increment("service_start_failed");
            SettingsSheet.message(this,"배터리 확인을 시작하지 못했어요.");
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
        startActivity(new Intent(this,WidgetPickerActivity.class));
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
        labelView.setGravity(android.view.Gravity.CENTER);
        TextView value = text("—", 25, SettingsUi.INK, true);
        value.setGravity(android.view.Gravity.CENTER);
        value.setPadding(0, dp(4), 0, 0);
        column.addView(labelView);
        column.addView(value);
        row.addView(column, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return value;
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
