package com.chocho.holidaysleep;
import android.content.Context;
import java.time.LocalDate;
final class HolidayCalendar {
    static boolean permission=true, holiday=true;
    static boolean hasCalendarPermission(Context context) { return permission; }
    static boolean isHoliday(Context context,LocalDate date) { return holiday; }
}
