package com.chocho.holidaysleep;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ScheduleWindowTest {
    private static int checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        ScheduleWindow defaults = ScheduleWindow.restored(25215, 43200);
        check(defaults.summary().equals("07:00:15 ~ 12:00"), "legacy seconds preserved");
        check(ScheduleWindow.format(0).equals("00:00"), "midnight label");
        check(ScheduleWindow.format(86399).equals("23:59:59"), "seconds label");
        int[][] invalid = {{0,0},{43200,43200},{50000,10000},{-1,43200},{0,86400}};
        for (int[] pair : invalid) {
            check(!ScheduleWindow.valid(pair[0],pair[1]), "invalid interval");
            check(ScheduleWindow.restored(pair[0],pair[1]).summary().equals(defaults.summary()), "restore defaults");
            try {
                new ScheduleWindow(pair[0],pair[1]);
                throw new AssertionError("constructor accepted invalid interval");
            } catch (IllegalArgumentException expected) { checks++; }
        }

        ScheduleWindow custom = new ScheduleWindow(6*3600+30*60, 10*3600+45*60);
        check(custom.summary().equals("06:30 ~ 10:45"), "custom summary");
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        for (int day = 0; day < 7; day++) {
            LocalDate date = LocalDate.of(2026,9,7).plusDays(day);
            for (int second = 0; second < 86400; second++) {
                ZonedDateTime now = date.atTime(LocalTime.ofSecondOfDay(second)).atZone(seoul);
                boolean inside = second >= custom.startSecond && second < custom.endSecond;
                check(custom.contains(now.toLocalTime()) == inside, "half-open interval");
                check(custom.shouldActivate(now,true,true) == (day < 5 && inside), "weekday holiday");
                check(!custom.shouldActivate(now,false,true), "disabled always false");
                check(!custom.shouldActivate(now,true,false), "ordinary weekday false");
            }
        }

        ZonedDateTime holiday = LocalDate.of(2026,10,9).atTime(10,0).atZone(seoul);
        check(custom.shouldActivate(holiday,true,true), "old STOP at 10 after new end 10:45 stays active");
        check(!new ScheduleWindow(25200,32400).shouldActivate(holiday,true,true), "save earlier end deactivates");
        check(!custom.shouldActivate(holiday.withHour(11),true,true), "late START after end does not activate");
        check(!defaults.contains(LocalTime.of(7,0,14)), "legacy gap before start");
        check(defaults.contains(LocalTime.of(7,0,15)), "legacy inclusive start");
        check(!defaults.contains(LocalTime.NOON), "exclusive end");

        for (String zone : new String[]{"Asia/Seoul","America/New_York","Europe/Berlin"}) {
            for (LocalDate date : new LocalDate[]{LocalDate.of(2026,3,8),LocalDate.of(2026,11,1),LocalDate.of(2026,12,31)}) {
                for (int hour : new int[]{0,6,7,10,12,23}) {
                    ZonedDateTime now = date.atTime(hour,0).atZone(ZoneId.of(zone));
                    for (boolean start : new boolean[]{true,false}) {
                        ZonedDateTime next = custom.nextBoundary(now,start);
                        check(next.isAfter(now), "next reservation strictly future");
                        check(next.getZone().equals(now.getZone()), "current zone retained");
                        check(next.toLocalTime().toSecondOfDay() == (start ? custom.startSecond : custom.endSecond), "wall clock time retained");
                    }
                }
            }
        }
        ZonedDateTime boundary = holiday.withHour(6).withMinute(30);
        check(custom.nextBoundary(boundary,true).toLocalDate().equals(boundary.toLocalDate().plusDays(1)), "at boundary reserve tomorrow");
        System.out.println("ScheduleWindow: " + checks + " assertions passed");
    }
}
