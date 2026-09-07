package com.chocho.airpodsglance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * Minimal, read-only Apple Accessory Protocol client for exact AirPods battery state.
 *
 * This research adapter first checks Android 17's public BluetoothSocketSettings API and uses a
 * narrowly-scoped hidden L2CAP fallback only during an explicit foreground experiment. Raw AAP
 * frames live only for the duration of one decoder call and are never logged or stored.
 */
public final class AapClient {
    public interface Listener {
        void onStage(String stage);
        void onBattery(AapBatteryDecoder.Decoded battery);
    }

    private static final int MIN_PUBLIC_API = 37;
    private static final int TYPE_L2CAP = 3;
    private static final int AAP_PSM = 0x1001;
    private static final long CONNECT_TIMEOUT_MILLIS = 8_000L;
    private static final long HANDSHAKE_TIMEOUT_MILLIS = 10_000L;

    private static final byte[] HANDSHAKE = {
            0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private static final byte[] ENABLE_NOTIFICATIONS_PRIMARY = {
            0x04, 0x00, 0x04, 0x00, 0x0f, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xef, (byte) 0xff
    };
    private static final byte[] ENABLE_NOTIFICATIONS_ALL = {
            0x04, 0x00, 0x04, 0x00, 0x0f, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
    };
    private static final byte[] INIT_EXT = {
            0x04, 0x00, 0x04, 0x00, 0x4d, 0x00, (byte) 0xd7, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    private final Context context;
    private final String selectedAddress;
    private final Listener listener;
    private final Handler watchdog = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean handshakeReady = new AtomicBoolean(false);
    private final Object socketLock = new Object();
    private BluetoothSocket socket;

    private final Runnable connectTimeout = new Runnable() {
        @Override public void run() {
            if (!stopped.get() && !connected.get()) {
                report("connect_timeout");
                closeSocket();
            }
        }
    };

    private final Runnable handshakeTimeout = new Runnable() {
        @Override public void run() {
            if (!stopped.get() && connected.get() && !handshakeReady.get()) {
                report("handshake_timeout");
                closeSocket();
            }
        }
    };

    public AapClient(Context context, String selectedAddress, Listener listener) {
        this.context = context.getApplicationContext();
        this.selectedAddress = selectedAddress;
        this.listener = listener;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        if (selectedAddress == null || selectedAddress.length() == 0) {
            report("no_selected_device");
            return;
        }
        executor.execute(new Runnable() {
            @Override public void run() { runClient(); }
        });
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        watchdog.removeCallbacks(connectTimeout);
        watchdog.removeCallbacks(handshakeTimeout);
        closeSocket();
        executor.shutdownNow();
    }

    private void runClient() {
        if (Build.VERSION.SDK_INT < MIN_PUBLIC_API) {
            report("public_api_unavailable");
            return;
        }
        report("public_api_ready");
        report("protocol_v2_started");
        try {
            BluetoothManager manager = context.getSystemService(BluetoothManager.class);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                report("bluetooth_unavailable");
                return;
            }
            BluetoothDevice device = adapter.getRemoteDevice(selectedAddress);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                report("selected_device_not_bonded");
                return;
            }

            BluetoothSocket created;
            try {
                created = createPublicL2capSocket(device);
            } catch (Exception publicError) {
                report("public_socket_rejected");
                created = createHiddenL2capSocket(device);
            }
            synchronized (socketLock) {
                if (stopped.get()) {
                    try { created.close(); } catch (Exception ignored) { }
                    return;
                }
                socket = created;
            }
            report("socket_created");
            watchdog.postDelayed(connectTimeout, CONNECT_TIMEOUT_MILLIS);
            created.connect();
            connected.set(true);
            watchdog.removeCallbacks(connectTimeout);
            report("socket_connected");

            OutputStream output = created.getOutputStream();
            output.write(HANDSHAKE);
            output.flush();
            report("handshake_sent");
            watchdog.postDelayed(handshakeTimeout, HANDSHAKE_TIMEOUT_MILLIS);

            readFrames(created.getInputStream(), output);
        } catch (Exception error) {
            if (!stopped.get()) report("failed");
        } finally {
            watchdog.removeCallbacks(connectTimeout);
            watchdog.removeCallbacks(handshakeTimeout);
            closeSocket();
        }
    }

    private BluetoothSocket createPublicL2capSocket(BluetoothDevice device) throws Exception {
        Class<?> settingsClass = Class.forName("android.bluetooth.BluetoothSocketSettings");
        Class<?> builderClass = Class.forName("android.bluetooth.BluetoothSocketSettings$Builder");
        report("socket_classes_loaded");
        Object builder = builderClass.getDeclaredConstructor().newInstance();
        report("socket_builder_created");
        builderClass.getMethod("setSocketType", int.class).invoke(builder, TYPE_L2CAP);
        report("socket_type_set");
        builderClass.getMethod("setL2capPsm", int.class).invoke(builder, AAP_PSM);
        report("socket_psm_set");
        builderClass.getMethod("setAuthenticationRequired", boolean.class).invoke(builder, false);
        builderClass.getMethod("setEncryptionRequired", boolean.class).invoke(builder, false);
        report("socket_security_set");
        Object settings = builderClass.getMethod("build").invoke(builder);
        report("socket_settings_built");
        Method create = BluetoothDevice.class.getMethod(
                "createUsingSocketSettings", settingsClass);
        report("socket_method_found");
        BluetoothSocket result = (BluetoothSocket) create.invoke(device, settings);
        report("socket_invoked");
        return result;
    }

    private BluetoothSocket createHiddenL2capSocket(BluetoothDevice device) throws Exception {
        report("hidden_fallback_started");
        if (!HiddenApiBypass.addHiddenApiExemptions("Landroid/bluetooth/BluetoothDevice;")) {
            throw new IllegalStateException("Hidden API exemption rejected");
        }
        report("hidden_exemption_ready");
        Method create = BluetoothDevice.class.getDeclaredMethod(
                "createInsecureL2capSocket", int.class);
        create.setAccessible(true);
        report("hidden_method_found");
        BluetoothSocket result = (BluetoothSocket) create.invoke(device, AAP_PSM);
        report("hidden_socket_invoked");
        return result;
    }

    private void readFrames(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[2048];
        while (!stopped.get()) {
            int count = input.read(buffer);
            if (count < 0) {
                report("remote_closed");
                return;
            }
            if (count == 0) continue;
            report("packet_received");
            byte[] frame = Arrays.copyOf(buffer, count);
            if (isConnectResponse(frame)) {
                if (littleEndian16(frame, 2) == 0x0004
                        && littleEndian16(frame, 4) == 0x0000) {
                    handshakeReady.set(true);
                    watchdog.removeCallbacks(handshakeTimeout);
                    report("handshake_ok");
                    sendPostHandshake(output);
                } else {
                    report("handshake_rejected");
                    closeSocket();
                    return;
                }
                continue;
            }

            if (handshakeReady.get() && !stopped.get()
                    && AapPlacementDecoder.isPlacementReport(frame)) {
                AapPlacementDecoder.Observation placement = AapPlacementDecoder.decode(frame);
                if (placement == null) {
                    report("placement_unsupported_length");
                } else {
                    report("placement_received");
                    report("placement_slots_" + placement.bucket());
                    if (placement.hasUnknown()) report("placement_unknown");
                }
            }

            AapBatteryDecoder.Result result = AapBatteryDecoder.decode(frame);
            if (result.accepted() && handshakeReady.get() && hasBattery(result.decoded)) {
                report("battery_received");
                if (!stopped.get()) listener.onBattery(result.decoded);
            }
        }
    }

    private void sendPostHandshake(OutputStream output) throws Exception {
        output.write(ENABLE_NOTIFICATIONS_PRIMARY);
        output.flush();
        output.write(ENABLE_NOTIFICATIONS_ALL);
        output.flush();
        output.write(INIT_EXT);
        output.flush();
        report("post_handshake_v2_sent");
    }

    private static boolean isConnectResponse(byte[] frame) {
        return frame != null && frame.length >= 6 && littleEndian16(frame, 0) == 0x0001;
    }

    private static boolean hasBattery(AapBatteryDecoder.Decoded decoded) {
        return decoded != null && (decoded.single != null || decoded.left != null
                || decoded.right != null || decoded.caseBattery != null);
    }

    private void report(String stage) {
        if (!stopped.get() && listener != null) listener.onStage(stage);
    }

    private void closeSocket() {
        BluetoothSocket current;
        synchronized (socketLock) {
            current = socket;
            socket = null;
        }
        if (current != null) {
            try { current.close(); } catch (Exception ignored) { }
        }
    }

    private static int littleEndian16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }
}
