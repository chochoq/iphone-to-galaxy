package com.chocho.holidaysleep;

import android.net.Uri;
import android.service.notification.ConditionProviderService;

public final class HolidayConditionProvider extends ConditionProviderService {
    @Override
    public void onConnected() {
        notifyCondition(ZenController.currentCondition(this));
    }

    @Override
    public void onSubscribe(Uri conditionId) {
        if (ZenController.CONDITION_URI.equals(conditionId)) {
            notifyCondition(ZenController.currentCondition(this));
        }
    }

    @Override
    public void onUnsubscribe(Uri conditionId) {
        // Nothing to do. Daily alarms keep the rule state current.
    }
}
