package com.chocho.airpodsglance;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tap changes this widget's part only: no app launch, Bluetooth or lock/security setting changes. */
public abstract class AirPodsLockWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_CYCLE = "com.chocho.airpodsglance.action.LOCK_WIDGET_CYCLE";
    private static final String PREFS = "lock_widget_parts";
    public static final class Left extends AirPodsLockWidgetProvider { protected int part() { return 0; } }
    public static final class Right extends AirPodsLockWidgetProvider { protected int part() { return 1; } }
    public static final class Case extends AirPodsLockWidgetProvider { protected int part() { return 2; } }
    protected abstract int part();

    private static int selected(Context context, int id, int fallback) {
        return LockWidgetSelection.valid(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("widget_" + id, fallback), fallback);
    }

    private static Intent cycleIntent(Context context, Class<?> provider, int id) {
        return new Intent(context, provider).setAction(ACTION_CYCLE)
                .setData(Uri.parse("airpods-glance://lock-widget/" + provider.getSimpleName() + "/" + id))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
    }

    private static RemoteViews bind(Context context, RemoteViews views, Class<?> provider, int id) {
        PendingIntent cycle = PendingIntent.getBroadcast(context, id, cycleIntent(context, provider, id),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.lock_widget_image, cycle);
        return views;
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (ACTION_CYCLE.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            android.appwidget.AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            String expected = new ComponentName(context, getClass()).flattenToString();
            if (!LockWidgetSelection.accepts(id, expected, info == null ? null : info.provider.flattenToString())) return;
            int next = LockWidgetSelection.next(selected(context, id, part()));
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("widget_" + id, next).apply();
            manager.updateAppWidget(id, render(context, manager.getAppWidgetOptions(id), part(), getClass(), id));
            return;
        }
        super.onReceive(context, intent);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int id : ids) {
            edit.remove("widget_" + id);
            PendingIntent cycle = PendingIntent.getBroadcast(context, id, cycleIntent(context, getClass(), id),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (cycle != null) cycle.cancel();
        }
        edit.apply();
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) manager.updateAppWidget(id, render(context, manager.getAppWidgetOptions(id), part(), getClass(), id));
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        manager.updateAppWidget(id, render(context, options, part(), getClass(), id));
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Class<?>[] providers = {Left.class, Right.class, Case.class};
        for (int part = 0; part < providers.length; part++) {
            for (int id : manager.getAppWidgetIds(new ComponentName(context, providers[part]))) {
                manager.updateAppWidget(id, render(context, manager.getAppWidgetOptions(id), part, providers[part], id));
            }
        }
    }

    private static RemoteViews render(Context context, Bundle options, int fallback, Class<?> provider, int id) {
        int part = selected(context, id, fallback);
        LockWidgetState state = LockWidgetState.from(new BatteryStateStore(context).load(), System.currentTimeMillis(), part);
        ArrayList<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
        Map<SizeF, RemoteViews> mapping = new LinkedHashMap<>();
        if (sizes != null) for (SizeF size : sizes) {
            if (mapping.size() >= 8) break;
            if (size == null || !Float.isFinite(size.getWidth()) || !Float.isFinite(size.getHeight())
                    || size.getWidth() <= 0 || size.getHeight() <= 0) continue;
            mapping.put(size, bind(context, LockWidgetRenderer.render(context, state, size.getWidth(), size.getHeight()), provider, id));
        }
        if (mapping.isEmpty()) return bind(context, LockWidgetRenderer.render(context, state, 56, 56), provider, id);
        return mapping.size() == 1 ? mapping.values().iterator().next() : new RemoteViews(mapping);
    }
}
