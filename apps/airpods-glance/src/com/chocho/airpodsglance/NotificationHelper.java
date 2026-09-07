package com.chocho.airpodsglance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public final class NotificationHelper {
    public static final String CHANNEL_MONITOR = "airpods_monitor";
    public static final String CHANNEL_CONNECTION = "airpods_connection";
    public static final String CHANNEL_LOW = "airpods_low_battery";
    public static final int ID_MONITOR = 2101;
    public static final int ID_CONNECTION = 2102;
    public static final int ID_LOW = 2103;

    private NotificationHelper() {}

    public static void ensureChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel monitor = new NotificationChannel(
                CHANNEL_MONITOR, "AirPods 연결 중", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("연결 중 배터리를 확인할 때만 표시");
        monitor.setSound(null, null);
        monitor.enableVibration(false);

        NotificationChannel connection = new NotificationChannel(
                CHANNEL_CONNECTION, "AirPods 연결 알림", NotificationManager.IMPORTANCE_HIGH);
        connection.setDescription("연결 세션마다 배터리를 한 번 표시");
        connection.setSound(null, null);
        connection.enableVibration(false);

        NotificationChannel low = new NotificationChannel(
                CHANNEL_LOW, "AirPods 배터리 부족", NotificationManager.IMPORTANCE_DEFAULT);
        low.setDescription("배터리가 기준 아래로 내려갈 때 한 번 표시");

        manager.createNotificationChannel(monitor);
        manager.createNotificationChannel(connection);
        manager.createNotificationChannel(low);
    }

    public static Notification monitor(Context context, AirPodsSnapshot snapshot) {
        String title = snapshot.selectedDeviceName == null
                ? "AirPods 배터리 확인 중" : snapshot.selectedDeviceName;
        String text = snapshot.identityConfidence == AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION
                ? "기기 확인 필요 · 주변 AirPods 값은 표시하지 않음"
                : hasReading(snapshot) ? batteryLine(snapshot, System.currentTimeMillis())
                : "배터리 확인 중";
        return base(context, CHANNEL_MONITOR, title, text)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    public static void showConnection(Context context, AirPodsSnapshot snapshot) {
        Notification notification = base(context, CHANNEL_CONNECTION,
                snapshot.selectedDeviceName == null ? "AirPods 연결됨" : snapshot.selectedDeviceName,
                batteryLine(snapshot, System.currentTimeMillis()))
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setTimeoutAfter(8000L)
                .build();
        context.getSystemService(NotificationManager.class).notify(ID_CONNECTION, notification);
    }

    public static void showLowBattery(Context context, AirPodsSnapshot snapshot,
                                      List<LowBatteryPolicy.Part> parts) {
        StringBuilder names = new StringBuilder();
        for (LowBatteryPolicy.Part part : parts) {
            if (names.length() > 0) names.append(" · ");
            switch (part) {
                case LEFT: names.append("왼쪽 ").append(BatteryPresentation.percent(snapshot.left)); break;
                case RIGHT: names.append("오른쪽 ").append(BatteryPresentation.percent(snapshot.right)); break;
                case CASE: names.append("케이스 ").append(BatteryPresentation.percent(snapshot.caseBattery)); break;
            }
        }
        Notification notification = base(context, CHANNEL_LOW,
                "AirPods 배터리가 부족해요", names.toString())
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .build();
        context.getSystemService(NotificationManager.class).notify(ID_LOW, notification);
    }

    public static void cancelMonitor(Context context) {
        context.getSystemService(NotificationManager.class).cancel(ID_MONITOR);
    }

    public static String batteryLine(AirPodsSnapshot snapshot, long now) {
        return "왼쪽 " + component(snapshot, snapshot.left, now)
                + " · 오른쪽 " + component(snapshot, snapshot.right, now)
                + " · 케이스 " + component(snapshot, snapshot.caseBattery, now);
    }

    private static String component(AirPodsSnapshot snapshot, BatteryComponent component, long now) {
        String value = BatteryPresentation.percent(component);
        return BatteryPresentation.showCharging(snapshot, component, now) ? value + " 충전 중" : value;
    }

    private static boolean hasReading(AirPodsSnapshot snapshot) {
        return snapshot.left != null || snapshot.right != null || snapshot.caseBattery != null;
    }

    private static Notification.Builder base(Context context, String channel,
                                             String title, String text) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 501, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(context, channel)
                .setSmallIcon(com.chocho.airpodsglance.R.drawable.ic_airpods)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setVisibility(Notification.VISIBILITY_PRIVATE);
    }
}
