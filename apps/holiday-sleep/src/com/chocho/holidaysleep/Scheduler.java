package com.chocho.holidaysleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private static final LocalTime START_TIME = LocalTime.of(7, 0, 15);
    private static final LocalTime STOP_TIME = LocalTime.NOON;

    private Scheduler() {}

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            configureAndReconcile(context);
        } else {
            cancel(context);
            ZenController.setActive(context, false);
        }
    }

    static void configureAndReconcile(Context context) {
        if (!isEnabled(context)) return;
        scheduleNext(context, ACTION_START);
        scheduleNext(context, ACTION_STOP);
        reconcileNow(context);
    }

    static void onAlarm(Context context, String action) {
        if (!isEnabled(context)) {
            ZenController.setActive(context, false);
            return;
        }
        scheduleNext(context, action);
        if (ACTION_STOP.equals(action)) {
            ZenController.setActive(context, false);
            return;
        }
        if (ACTION_START.equals(action)) {
            LocalDate today = LocalDate.now();
            boolean shouldStart = isWeekday(today)
                    && HolidayCalendar.isHoliday(context, today);
            ZenController.setActive(context, shouldStart);
        }
    }

    static void reconcileNow(Context context) {
        if (!isEnabled(context)) {
            ZenController.setActive(context, false);
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        boolean inExtensionWindow = !time.isBefore(START_TIME) && time.isBefore(STOP_TIME);
        boolean shouldBeActive = isWeekday(today)
                && inExtensionWindow
                && HolidayCalendar.isHoliday(context, today);
        ZenController.setActive(context, shouldBeActive);
    }

    static boolean canScheduleExact(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return false;
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || manager.canScheduleExactAlarms();
    }

    static String nextScheduleSummary(String action) {
        ZonedDateTime next = nextTime(action);
        return next.format(DateTimeFormatter.ofPattern("M월 d일(E) HH:mm:ss", Locale.KOREAN));
    }

    private static void scheduleNext(Context context, String action) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        int requestCode = ACTION_START.equals(action) ? REQUEST_START : REQUEST_STOP;
        PendingIntent operation = pendingIntent(context, action, requestCode);
        long triggerAt = nextTime(action).toInstant().toEpochMilli();

        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        }
    }

    private static ZonedDateTime nextTime(String action) {
        LocalTime target = ACTION_START.equals(action) ? START_TIME : STOP_TIME;
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.toLocalDate().atTime(target).atZone(now.getZone());
        if (!next.isAfter(now)) next = next.plusDays(1);
        return next;
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

    private static boolean isWeekday(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
