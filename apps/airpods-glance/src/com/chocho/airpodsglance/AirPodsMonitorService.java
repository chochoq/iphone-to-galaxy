package com.chocho.airpodsglance;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AirPodsMonitorService extends Service {
    private static final String ACTION_START = "com.chocho.airpodsglance.action.START";
    private static final String ACTION_STOP = "com.chocho.airpodsglance.action.STOP";
    private static final String EXTRA_VERIFIED = "verified_connection";
    private static final String EXTRA_MANUAL_AAP = "manual_aap_once";
    private static final long MANUAL_START_TIMEOUT = 30_000L;
    private static final long DIRECT_WINDOW_TIMEOUT = 30_000L;
    private static final long MIN_RENDER_INTERVAL = 60_000L;

    private BluetoothLeScanner scanner;
    private AapClient aapClient;
    private ConnectionPopupOverlay connectionPopupOverlay;
    private boolean scanning;
    private boolean exactFrameSeen;
    private boolean verifiedConnection;
    private long lastRenderAt;
    private final Map<String, Long> pendingDiagnostics = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LowBatteryPolicy lowBatteryPolicy = new LowBatteryPolicy(20, 15);

    private final Runnable manualTimeout = () -> {
        if (!verifiedConnection && !exactFrameSeen) stopMonitoring(true);
    };

    private final Runnable directWindowTimeout = () -> {
        if (aapClient == null) return;
        noteDiagnostic("aap_direct_window_closed");
        aapClient.stop();
        aapClient = null;
    };

    private final Runnable diagnosticsFlush = new Runnable() {
        @Override public void run() {
            flushDiagnostics();
            handler.postDelayed(this, 1000L);
        }
    };

    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) handleResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            DiagnosticsStore diagnostics = new DiagnosticsStore(AirPodsMonitorService.this);
            noteDiagnostic("scan_failed");
            flushDiagnostics();
            diagnostics.setLastScanFailure(errorCode);
            stopMonitoring(false);
        }
    };

    public static void start(Context context, boolean verifiedConnection) {
        Intent intent = new Intent(context, AirPodsMonitorService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_VERIFIED, verifiedConnection);
        context.startForegroundService(intent);
    }

    public static void startManualDirect(Context context) {
        Intent intent = new Intent(context, AirPodsMonitorService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_VERIFIED, false)
                .putExtra(EXTRA_MANUAL_AAP, true);
        context.startForegroundService(intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, AirPodsMonitorService.class));
        NotificationHelper.cancelMonitor(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannels(this);
        connectionPopupOverlay = new ConnectionPopupOverlay(this);
        lowBatteryPolicy.resetForNewConnectionSession();
        handler.postDelayed(diagnosticsFlush, 1000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMonitoring(true);
            return START_NOT_STICKY;
        }
        boolean verifiedStart = intent != null
                && intent.getBooleanExtra(EXTRA_VERIFIED, false);
        verifiedConnection = verifiedConnection || verifiedStart;
        boolean manualAap = intent != null
                && intent.getBooleanExtra(EXTRA_MANUAL_AAP, false);
        AppSettings appSettings = new AppSettings(this);
        AirPodsSnapshot initial = new BatteryStateStore(this)
                .markConnected(true, appSettings.selectedName());
        startForeground(NotificationHelper.ID_MONITOR,
                NotificationHelper.monitor(this, initial),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        AirPodsWidgetProvider.updateAll(this);

        if (!appSettings.monitorEnabled() || !hasBluetoothPermissions()) {
            stopMonitoring(false);
            return START_NOT_STICKY;
        }
        ConnectionSessionStore sessionStore = new ConnectionSessionStore(this);
        sessionStore.beginIfNeeded();
        startScan();
        boolean automaticAap = verifiedStart && sessionStore.claimAutomaticAapAttempt();
        if (automaticAap || manualAap) {
            noteDiagnostic(automaticAap ? "aap_automatic_started" : "aap_manual_started");
            startAap(appSettings);
            handler.removeCallbacks(directWindowTimeout);
            handler.postDelayed(directWindowTimeout, DIRECT_WINDOW_TIMEOUT);
        }
        handler.removeCallbacks(manualTimeout);
        if (!verifiedConnection) handler.postDelayed(manualTimeout, MANUAL_START_TIMEOUT);
        return START_NOT_STICKY;
    }

    private void startAap(final AppSettings settings) {
        if (aapClient != null) return;
        aapClient = new AapClient(this, settings.selectedAddress(), new AapClient.Listener() {
            @Override public void onStage(String stage) {
                noteDiagnostic("aap_" + stage);
            }

            @Override public void onBattery(final AapBatteryDecoder.Decoded battery) {
                handler.post(new Runnable() {
                    @Override public void run() { handleAapBattery(settings, battery); }
                });
            }
        });
        aapClient.start();
    }

    private void startScan() {
        if (scanning) return;
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        if (manager == null || manager.getAdapter() == null || !manager.getAdapter().isEnabled()) {
            stopMonitoring(false);
            return;
        }
        scanner = manager.getAdapter().getBluetoothLeScanner();
        if (scanner == null) {
            stopMonitoring(false);
            return;
        }
        // Filter only by Apple's company id here. Type/length/prefix belong to the decoder so
        // diagnostics can distinguish "no Apple frame" from "Apple frame rejected".
        ScanFilter filter = new ScanFilter.Builder()
                .setManufacturerData(AirPodsPacketDecoder.APPLE_COMPANY_ID, new byte[0])
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        try {
            scanner.startScan(Collections.singletonList(filter), settings, callback);
            scanning = true;
        } catch (SecurityException | IllegalStateException error) {
            noteDiagnostic("scan_failed");
            stopMonitoring(false);
        }
    }

    private void handleResult(ScanResult result) {
        if (!hasBluetoothPermissions()) {
            stopMonitoring(false);
            return;
        }
        ScanRecord record = result == null ? null : result.getScanRecord();
        byte[] bytes = record == null ? null
                : record.getManufacturerSpecificData(AirPodsPacketDecoder.APPLE_COMPANY_ID);
        AppSettings settings = new AppSettings(this);
        String selectedAddress = settings.selectedAddress();
        String resultAddress;
        try {
            resultAddress = result.getDevice().getAddress();
        } catch (SecurityException error) {
            stopMonitoring(false);
            return;
        }
        boolean exactAddress = selectedAddress != null && selectedAddress.equals(resultAddress);
        noteDiagnostic(exactAddress ? "apple_exact_address" : "apple_other_address");
        if (bytes != null && bytes.length > 0) {
            noteDiagnostic(String.format(Locale.ROOT, "type_%02x", bytes[0] & 0xff));
        }
        AirPodsPacketDecoder.Result decoded = AirPodsPacketDecoder.decode(bytes);
        noteDiagnostic("filtered");
        if (!decoded.accepted()) {
            noteDiagnostic("rejected");
            noteDiagnostic("rejected_" + decoded.rejection.name().toLowerCase(Locale.ROOT));
            return;
        }

        if (!exactAddress) {
            noteDiagnostic("address_mismatch");
            BatteryStateStore store = new BatteryStateStore(this);
            AirPodsSnapshot previous = store.load();
            if (previous.source == AirPodsSnapshot.Source.AAP_EXACT_PERCENT
                    && BatteryPresentation.isFresh(previous, System.currentTimeMillis())) {
                noteDiagnostic("ble_mismatch_ignored_after_aap");
                return;
            }
            AirPodsSnapshot calibration = store.markNeedsCalibration(settings.selectedName());
            if (previous.identityConfidence
                    != AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION) {
                updateSurfaces(calibration, true);
            }
            return;
        }

        exactFrameSeen = true;
        handler.removeCallbacks(manualTimeout);
        noteDiagnostic("valid");
        AirPodsPacketDecoder.Decoded value = decoded.decoded;
        AirPodsSnapshot snapshot = new AirPodsSnapshot(
                settings.selectedName(), true, AirPodsSnapshot.IdentityConfidence.EXACT,
                value.modelId, value.left, value.right, value.caseBattery,
                System.currentTimeMillis(), AirPodsSnapshot.Source.BLE_PUBLIC_DECILE);
        acceptSnapshot(settings, snapshot);
    }

    private void handleAapBattery(AppSettings settings, AapBatteryDecoder.Decoded value) {
        BatteryStateStore store = new BatteryStateStore(this);
        AirPodsSnapshot previous = store.load();
        BatteryComponent left = value.left != null ? value.left : value.single;
        AirPodsSnapshot snapshot = new AirPodsSnapshot(
                settings.selectedName(), true, AirPodsSnapshot.IdentityConfidence.EXACT,
                previous.modelId, left, value.right, value.caseBattery,
                System.currentTimeMillis(), AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
        exactFrameSeen = true;
        handler.removeCallbacks(manualTimeout);
        acceptSnapshot(settings, snapshot);
    }

    private void acceptSnapshot(AppSettings settings, AirPodsSnapshot snapshot) {
        BatteryStateStore store = new BatteryStateStore(this);
        AirPodsSnapshot previous = store.load();
        if (snapshot.source == AirPodsSnapshot.Source.BLE_PUBLIC_DECILE
                && previous.source == AirPodsSnapshot.Source.AAP_EXACT_PERCENT
                && BatteryPresentation.isFresh(previous, System.currentTimeMillis())) {
            noteDiagnostic("ble_ignored_after_aap");
            return;
        }
        store.save(snapshot);

        List<LowBatteryPolicy.Part> warnings = settings.lowBatteryEnabled()
                ? lowBatteryPolicy.evaluate(snapshot) : Collections.emptyList();
        if (!warnings.isEmpty()) {
            NotificationHelper.showLowBattery(this, snapshot, warnings);
            new ConnectionSessionStore(this).claimPopup();
        } else if (settings.popupEnabled() && new ConnectionSessionStore(this).claimPopup()) {
            boolean overlayShown = connectionPopupOverlay != null
                    && connectionPopupOverlay.show(snapshot, false);
            noteDiagnostic(overlayShown ? "popup_overlay_shown" : "popup_notification_fallback");
            if (!overlayShown) NotificationHelper.showConnection(this, snapshot);
        }
        boolean visibleChange = !snapshot.sameVisibleState(previous)
                || System.currentTimeMillis() - lastRenderAt >= MIN_RENDER_INTERVAL;
        updateSurfaces(snapshot, visibleChange);
    }

    private void updateSurfaces(AirPodsSnapshot snapshot, boolean updateWidget) {
        getSystemService(android.app.NotificationManager.class).notify(
                NotificationHelper.ID_MONITOR, NotificationHelper.monitor(this, snapshot));
        if (updateWidget) {
            lastRenderAt = System.currentTimeMillis();
            AirPodsWidgetProvider.updateAll(this);
        }
    }

    private boolean hasBluetoothPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void stopMonitoring(boolean explicitDisconnect) {
        if (connectionPopupOverlay != null) connectionPopupOverlay.dismissImmediately();
        if (aapClient != null) {
            aapClient.stop();
            aapClient = null;
        }
        handler.removeCallbacks(directWindowTimeout);
        handler.removeCallbacksAndMessages(null);
        flushDiagnostics();
        if (scanner != null && scanning && hasBluetoothPermissions()) {
            try { scanner.stopScan(callback); } catch (SecurityException ignored) { }
        }
        scanning = false;
        if (explicitDisconnect) {
            AppSettings settings = new AppSettings(this);
            new BatteryStateStore(this).markConnected(false, settings.selectedName());
            new ConnectionSessionStore(this).end();
            AirPodsWidgetProvider.updateAll(this);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        NotificationHelper.cancelMonitor(this);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (connectionPopupOverlay != null) connectionPopupOverlay.dismissImmediately();
        if (aapClient != null) {
            aapClient.stop();
            aapClient = null;
        }
        handler.removeCallbacks(directWindowTimeout);
        handler.removeCallbacksAndMessages(null);
        flushDiagnostics();
        if (scanner != null && scanning && hasBluetoothPermissions()) {
            try { scanner.stopScan(callback); } catch (SecurityException ignored) { }
        }
        scanning = false;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private synchronized void noteDiagnostic(String key) {
        Long current = pendingDiagnostics.get(key);
        pendingDiagnostics.put(key, current == null ? 1L : current + 1L);
    }

    private synchronized void flushDiagnostics() {
        if (pendingDiagnostics.isEmpty()) return;
        Map<String, Long> copy = new HashMap<>(pendingDiagnostics);
        pendingDiagnostics.clear();
        new DiagnosticsStore(this).addAll(copy);
    }
}
