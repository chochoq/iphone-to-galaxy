package com.chocho.holidaysleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                Scheduler.configureAndReconcile(appContext);
            } finally {
                pendingResult.finish();
            }
        }, "HolidaySleepSystemEvent").start();
    }
}
