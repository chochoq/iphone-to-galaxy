package com.chocho.holidaysleep;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Locale;

/** One same-day interval; no Android dependency so boundary behavior can be tested. */
final class ScheduleWindow {
    static final int DEFAULT_START = 7 * 3600 + 15;
    static final int DEFAULT_END = 12 * 3600;
    final int startSecond;
    final int endSecond;

    ScheduleWindow(int startSecond, int endSecond) {
        if (!valid(startSecond, endSecond)) {
            throw new IllegalArgumentException("종료 시간은 같은 날의 시작 시간보다 늦어야 합니다.");
        }
        this.startSecond = startSecond;
        this.endSecond = endSecond;
    }

    static boolean valid(int start, int end) {
        return start >= 0 && end < 86400 && start < end;
    }

    static ScheduleWindow restored(int start, int end) {
        return valid(start, end) ? new ScheduleWindow(start, end)
                : new ScheduleWindow(DEFAULT_START, DEFAULT_END);
    }

    boolean contains(LocalTime time) {
        int second = time.toSecondOfDay();
        return second >= startSecond && second < endSecond;
    }

    boolean shouldActivate(ZonedDateTime now, boolean enabled, boolean holiday) {
        DayOfWeek day = now.getDayOfWeek();
        return enabled && holiday && day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY && contains(now.toLocalTime());
    }

    ZonedDateTime nextBoundary(ZonedDateTime now, boolean start) {
        LocalTime time = LocalTime.ofSecondOfDay(start ? startSecond : endSecond);
        ZonedDateTime next = now.toLocalDate().atTime(time).atZone(now.getZone());
        if (!next.isAfter(now)) {
            next = now.toLocalDate().plusDays(1).atTime(time).atZone(now.getZone());
        }
        return next;
    }

    static String format(int seconds) {
        LocalTime time = LocalTime.ofSecondOfDay(seconds);
        String result = String.format(Locale.KOREAN, "%02d:%02d", time.getHour(), time.getMinute());
        return time.getSecond() == 0 ? result
                : result + String.format(Locale.KOREAN, ":%02d", time.getSecond());
    }

    String summary() {
        return format(startSecond) + " ~ " + format(endSecond);
    }
}
