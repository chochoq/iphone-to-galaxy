package com.chocho.holidaysleep;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HolidayCalendar {
    private HolidayCalendar() {}

    static boolean hasCalendarPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    static List<String> detectedCalendarNames(Context context) {
        List<HolidaySource> sources = findHolidaySources(context);
        Set<String> names = new LinkedHashSet<>();
        for (HolidaySource source : sources) {
            names.add(source.displayName);
        }
        return new ArrayList<>(names);
    }

    static List<String> holidayTitlesForDate(Context context, LocalDate date) {
        List<HolidaySource> sources = findHolidaySources(context);
        Set<String> titles = new LinkedHashSet<>();
        if (sources.isEmpty()) {
            return new ArrayList<>();
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime localStart = date.atStartOfDay(zone);
        long queryStart = localStart.minusDays(1).toInstant().toEpochMilli();
        long queryEnd = localStart.plusDays(2).toInstant().toEpochMilli();
        long localStartMillis = localStart.toInstant().toEpochMilli();
        long localEndMillis = localStart.plusDays(1).toInstant().toEpochMilli();

        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, queryStart);
        ContentUris.appendId(builder, queryEnd);

        StringBuilder selection = new StringBuilder(CalendarContract.Instances.CALENDAR_ID)
                .append(" IN (");
        String[] args = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) selection.append(',');
            selection.append('?');
            args[i] = Long.toString(sources.get(i).id);
        }
        selection.append(')');

        String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID
        };

        Map<Long, HolidaySource> sourcesById = new HashMap<>();
        for (HolidaySource source : sources) {
            sourcesById.put(source.id, source);
        }

        try (Cursor cursor = context.getContentResolver().query(
                builder.build(), projection, selection.toString(), args,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor == null) return new ArrayList<>();
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                long begin = cursor.getLong(1);
                long end = cursor.getLong(2);
                boolean allDay = cursor.getInt(3) != 0;
                HolidaySource source = sourcesById.get(cursor.getLong(4));
                if (source == null) continue;

                boolean matches;
                LocalDate eventStart = null;
                if (allDay) {
                    eventStart = Instant.ofEpochMilli(begin)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate eventEnd = Instant.ofEpochMilli(end)
                            .atZone(ZoneOffset.UTC).toLocalDate();
                    matches = !date.isBefore(eventStart) && date.isBefore(eventEnd);
                } else {
                    matches = begin < localEndMillis && end > localStartMillis;
                }

                String cleanTitle = title == null || title.trim().isEmpty()
                        ? "공휴일" : title.trim();
                boolean adjacentLunarHoliday = source.authoritativeSamsung
                        && allDay
                        && isLunarHolidayCenter(cleanTitle)
                        && Math.abs(ChronoUnit.DAYS.between(eventStart, date)) <= 1;
                boolean accepted = source.authoritativeSamsung
                        ? matches || adjacentLunarHoliday
                        : matches && isRecognizedPublicHolidayTitle(cleanTitle);

                if (accepted) {
                    titles.add(cleanTitle);
                }
            }
        } catch (SecurityException ignored) {
            return new ArrayList<>();
        }
        return new ArrayList<>(titles);
    }

    static boolean isHoliday(Context context, LocalDate date) {
        return !holidayTitlesForDate(context, date).isEmpty();
    }

    private static List<HolidaySource> findHolidaySources(Context context) {
        List<HolidaySource> samsung = new ArrayList<>();
        List<HolidaySource> fallback = new ArrayList<>();
        if (!hasCalendarPermission(context)) return samsung;

        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };
        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)) {
            if (cursor == null) return samsung;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String account = safe(cursor.getString(1));
                String name = safe(cursor.getString(2));
                String display = safe(cursor.getString(3));
                String accountLower = account.toLowerCase(Locale.ROOT);
                String label = (display + " " + name).trim();

                boolean samsungOfficial = accountLower.contains("samsungholiday")
                        && ("공휴일".equals(display) || "공휴일".equals(name));
                boolean koreaHolidayFallback = label.contains("대한민국의 휴일")
                        || label.contains("대한민국 공휴일")
                        || label.toLowerCase(Locale.ROOT).contains("holidays in south korea");

                if (samsungOfficial) {
                    samsung.add(new HolidaySource(id, "삼성 캘린더 공휴일", true));
                } else if (koreaHolidayFallback) {
                    fallback.add(new HolidaySource(id, "대한민국의 휴일", false));
                }
            }
        } catch (SecurityException ignored) {
            return samsung;
        }
        samsung.addAll(fallback);
        return samsung;
    }

    private static boolean isLunarHolidayCenter(String title) {
        String normalized = normalizeTitle(title);
        return "설날".equals(normalized) || "추석".equals(normalized);
    }

    private static boolean isRecognizedPublicHolidayTitle(String title) {
        String normalized = normalizeTitle(title);
        String[] recognized = {
                "새해첫날", "신정", "설날", "삼일절", "31절",
                "어린이날", "부처님오신날", "석가탄신일", "현충일",
                "광복절", "추석", "개천절", "한글날", "성탄절",
                "크리스마스", "대체공휴일", "대체휴일", "임시공휴일", "선거"
        };
        for (String keyword : recognized) {
            if (normalized.contains(keyword)) return true;
        }
        return false;
    }

    private static String normalizeTitle(String title) {
        return safe(title).toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("·", "")
                .replace(".", "")
                .replace("-", "")
                .replace("_", "")
                .replace("/", "")
                .replace("(", "")
                .replace(")", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class HolidaySource {
        final long id;
        final String displayName;
        final boolean authoritativeSamsung;

        HolidaySource(long id, String displayName, boolean authoritativeSamsung) {
            this.id = id;
            this.displayName = displayName;
            this.authoritativeSamsung = authoritativeSamsung;
        }
    }
}
