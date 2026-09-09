package com.chocho.holidaysleep;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

final class ScheduleSettings {
    private ScheduleSettings() {}

    static ScheduleWindow read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("holiday_sleep", Context.MODE_PRIVATE);
        try {
            Map<String, ?> snapshot = prefs.getAll();
            return ScheduleWindow.restored(
                    snapshot.containsKey("start_second") ? (Integer) snapshot.get("start_second")
                            : ScheduleWindow.DEFAULT_START,
                    snapshot.containsKey("end_second") ? (Integer) snapshot.get("end_second")
                            : ScheduleWindow.DEFAULT_END);
        } catch (ClassCastException invalidStoredType) {
            return new ScheduleWindow(ScheduleWindow.DEFAULT_START, ScheduleWindow.DEFAULT_END);
        }
    }

    static boolean save(Context context, ScheduleWindow window) {
        ScheduleWindow before = read(context);
        SharedPreferences prefs = context.getSharedPreferences("holiday_sleep", Context.MODE_PRIVATE);
        boolean success = prefs.edit()
                .putInt("start_second", window.startSecond)
                .putInt("end_second", window.endSecond)
                .commit();
        if (!success) {
            // commit failure can still change the in-memory map. Restore it as well.
            prefs.edit().putInt("start_second", before.startSecond)
                    .putInt("end_second", before.endSecond).commit();
        }
        return success;
    }
}
