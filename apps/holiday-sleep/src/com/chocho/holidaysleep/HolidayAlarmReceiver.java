package com.chocho.holidaysleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class HolidayAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        String action = intent == null ? null : intent.getAction();
        new Thread(() -> {
            try {
                Scheduler.onAlarm(appContext, action);
            } finally {
                pendingResult.finish();
            }
        }, "HolidaySleepAlarm").start();
    }
}
