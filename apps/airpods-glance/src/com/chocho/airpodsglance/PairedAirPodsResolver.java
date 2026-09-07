package com.chocho.airpodsglance;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PairedAirPodsResolver {
    public enum Status { SELECTED, PERMISSION_REQUIRED, BLUETOOTH_OFF, NONE, MULTIPLE }

    public static final class Resolution {
        public final Status status;
        public final String name;
        public final int candidateCount;

        Resolution(Status status, String name, int candidateCount) {
            this.status = status;
            this.name = name;
            this.candidateCount = candidateCount;
        }
    }

    private final Context context;
    private final AppSettings settings;

    public PairedAirPodsResolver(Context context) {
        this.context = context;
        this.settings = new AppSettings(context);
    }

    public Resolution resolve() {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return new Resolution(Status.PERMISSION_REQUIRED, null, 0);
        }
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return new Resolution(Status.BLUETOOTH_OFF, null, 0);
        }
        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        List<BluetoothDevice> airPods = new ArrayList<>();
        for (BluetoothDevice device : bonded) {
            String name = device.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains("airpods")) {
                airPods.add(device);
            }
        }
        if (airPods.isEmpty()) return new Resolution(Status.NONE, null, 0);

        String savedAddress = settings.selectedAddress();
        if (savedAddress != null) {
            for (BluetoothDevice device : airPods) {
                if (savedAddress.equals(device.getAddress())) {
                    String name = safeName(device.getName());
                    settings.selectDevice(device.getAddress(), name);
                    return new Resolution(Status.SELECTED, name, airPods.size());
                }
            }
        }
        if (airPods.size() == 1) {
            BluetoothDevice device = airPods.get(0);
            String name = safeName(device.getName());
            settings.selectDevice(device.getAddress(), name);
            return new Resolution(Status.SELECTED, name, 1);
        }
        return new Resolution(Status.MULTIPLE, null, airPods.size());
    }

    public boolean isSelected(BluetoothDevice device) {
        if (device == null || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) return false;
        String selected = settings.selectedAddress();
        return selected != null && selected.equals(device.getAddress());
    }

    private static String safeName(String name) {
        return name == null || name.trim().isEmpty() ? "AirPods" : name;
    }
}
