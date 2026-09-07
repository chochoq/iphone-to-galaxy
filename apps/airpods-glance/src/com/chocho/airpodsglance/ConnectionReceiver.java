package com.chocho.airpodsglance;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public final class ConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) return;
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                disconnect(context);
            }
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        PairedAirPodsResolver resolver = new PairedAirPodsResolver(context);
        resolver.resolve();
        if (!resolver.isSelected(device)) return;

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            AppSettings settings = new AppSettings(context);
            if (!settings.monitorEnabled()) return;
            new ConnectionSessionStore(context).beginIfNeeded();
            try {
                AirPodsMonitorService.start(context, true);
            } catch (RuntimeException error) {
                new DiagnosticsStore(context).increment("service_start_failed");
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            disconnect(context);
        }
    }

    private static void disconnect(Context context) {
        AppSettings settings = new AppSettings(context);
        new BatteryStateStore(context).markConnected(false, settings.selectedName());
        new ConnectionSessionStore(context).end();
        AirPodsMonitorService.stop(context);
        AirPodsWidgetProvider.updateAll(context);
    }
}
