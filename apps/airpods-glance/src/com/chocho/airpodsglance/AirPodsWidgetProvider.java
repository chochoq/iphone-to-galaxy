package com.chocho.airpodsglance;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class AirPodsWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH =
            "com.chocho.airpodsglance.action.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) manager.updateAppWidget(id, render(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppSettings settings = new AppSettings(context);
            if (settings.selectedAddress() != null && settings.monitorEnabled()) {
                new ConnectionSessionStore(context).beginIfNeeded();
                AirPodsMonitorService.start(context, false);
            } else {
                Intent open = new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(open);
            }
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, AirPodsWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) manager.updateAppWidget(id, render(context));
    }

    private static RemoteViews render(Context context) {
        AirPodsSnapshot snapshot = new BatteryStateStore(context).load();
        long now = System.currentTimeMillis();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.airpods_widget);
        views.setTextViewText(R.id.widget_title,
                snapshot.selectedDeviceName == null ? "에어팟 한눈에" : snapshot.selectedDeviceName);
        views.setTextViewText(R.id.widget_batteries,
                "왼쪽 " + compact(snapshot, snapshot.left, now)
                        + "  ·  오른쪽 " + compact(snapshot, snapshot.right, now)
                        + "  ·  케이스 " + compact(snapshot, snapshot.caseBattery, now));
        views.setTextViewText(R.id.widget_status, status(snapshot, now));

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 601, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, open);

        Intent refreshIntent = new Intent(context, AirPodsWidgetProvider.class)
                .setAction(ACTION_REFRESH);
        PendingIntent refresh = PendingIntent.getBroadcast(context, 602, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refresh);
        return views;
    }

    private static String compact(AirPodsSnapshot snapshot, BatteryComponent component, long now) {
        String percent = BatteryPresentation.percent(component);
        return BatteryPresentation.showCharging(snapshot, component, now) ? percent + "⚡" : percent;
    }

    private static String status(AirPodsSnapshot snapshot, long now) {
        if (snapshot.identityConfidence == AirPodsSnapshot.IdentityConfidence.NEEDS_CALIBRATION) {
            return "기기 확인 필요 · 다른 AirPods 값은 숨겼어요";
        }
        if (snapshot.connected && snapshot.observedAtEpochMillis == 0L) return "배터리 확인 중";
        String age = age(snapshot.observedAtEpochMillis, now);
        if (snapshot.connected && BatteryPresentation.isFresh(snapshot, now)) return "연결됨 · 방금 확인";
        if (snapshot.connected) return "연결됨 · 마지막 확인 " + age;
        if (snapshot.observedAtEpochMillis > 0L) return "연결 안 됨 · 마지막 확인 " + age;
        return snapshot.selectedDeviceName == null ? "앱에서 AirPods를 연결해 주세요" : "연결 안 됨";
    }

    private static String age(long observed, long now) {
        if (observed <= 0L || now < observed) return "알 수 없음";
        long minutes = (now - observed) / 60000L;
        if (minutes < 1) return "방금";
        if (minutes < 60) return minutes + "분 전";
        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";
        return (hours / 24) + "일 전";
    }
}
