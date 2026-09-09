package com.chocho.holidaysleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Scheduler {
    static final String ACTION_START = "com.chocho.holidaysleep.START";
    static final String ACTION_STOP = "com.chocho.holidaysleep.STOP";

    private static final String PREFS = "holiday_sleep";
    private static final String KEY_ENABLED = "enabled";
    private static final int REQUEST_START = 70015;
    private static final int REQUEST_STOP = 120000;

    private Scheduler() {}

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    static synchronized void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            configureAndReconcile(context);
        } else {
            cancel(context);
            ZenController.setActive(context, false);
        }
    }

    static synchronized boolean saveSchedule(Context context, ScheduleWindow window) {
        if (!ScheduleSettings.save(context, window)) return false;
        configureAndReconcile(context);
        return true;
    }

    static synchronized void configureAndReconcile(Context context) {
        if (!isEnabled(context)) {
            cancel(context);
            ZenController.setActive(context, false);
            return;
        }
        scheduleNext(context, ACTION_START);
        scheduleNext(context, ACTION_STOP);
        reconcileNow(context);
    }

    static synchronized void onAlarm(Context context, String action) {
        if (!ACTION_START.equals(action) && !ACTION_STOP.equals(action)) return;
        // An old or delayed STOP must not override a newly saved interval.
        // Both events use the same current-state decision, including end exclusion.
        configureAndReconcile(context);
    }

    static synchronized void reconcileNow(Context context) {
        if (!isEnabled(context)) {
            ZenController.setActive(context, false);
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = now.toLocalDate();
        ScheduleWindow window = ScheduleSettings.read(context);
        boolean candidate = window.shouldActivate(now, true, true);
        boolean shouldBeActive = candidate && HolidayCalendar.hasCalendarPermission(context)
                && HolidayCalendar.isHoliday(context, today);
        ZenController.setActive(context, shouldBeActive);
    }

    static boolean canScheduleExact(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return false;
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms();
    }

    static String nextScheduleSummary(Context context, String action) {
        if (!isEnabled(context)) return "자동 연장이 꺼져 있어 예약하지 않음";
        ZonedDateTime next = nextTime(context, action);
        return next.format(DateTimeFormatter.ofPattern("M월 d일(E) HH:mm:ss", Locale.KOREAN));
    }

    private static void scheduleNext(Context context, String action) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        int requestCode = ACTION_START.equals(action) ? REQUEST_START : REQUEST_STOP;
        PendingIntent operation = pendingIntent(context, action, requestCode);
        long triggerAt = nextTime(context, action).toInstant().toEpochMilli();

        if (canScheduleExact(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
            } catch (SecurityException permissionChanged) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
            }
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        }
    }

    private static ZonedDateTime nextTime(Context context, String action) {
        return ScheduleSettings.read(context).nextBoundary(
                ZonedDateTime.now(), ACTION_START.equals(action));
    }

    private static void cancel(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        manager.cancel(pendingIntent(context, ACTION_START, REQUEST_START));
        manager.cancel(pendingIntent(context, ACTION_STOP, REQUEST_STOP));
    }

    private static PendingIntent pendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, HolidayAlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
