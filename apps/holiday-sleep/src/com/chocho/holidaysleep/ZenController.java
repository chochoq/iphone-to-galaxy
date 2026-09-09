package com.chocho.holidaysleep;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenPolicy;

final class ZenController {
    static final Uri CONDITION_URI = Uri.parse(
            "condition://com.chocho.holidaysleep/public_holiday_sleep_extension");

    private static final String PREFS = "holiday_sleep";
    private static final String KEY_RULE_ID = "zen_rule_id";
    private static final String KEY_ACTIVE = "zen_active";
    private static final String RULE_NAME = "공휴일 수면 연장";

    private ZenController() {}

    static boolean hasPolicyAccess(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.isNotificationPolicyAccessGranted();
    }

    static boolean isActive(Context context) {
        return prefs(context).getBoolean(KEY_ACTIVE, false);
    }

    static boolean setActive(Context context, boolean active) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || !manager.isNotificationPolicyAccessGranted()) {
            if (!active) prefs(context).edit().putBoolean(KEY_ACTIVE, false).apply();
            return false;
        }
        try {
            String ruleId = ensureRule(context, manager);
            int state = active ? Condition.STATE_TRUE : Condition.STATE_FALSE;
            String summary = active ? activeSummary(context) : "대기 중";
            manager.setAutomaticZenRuleState(
                    ruleId, new Condition(CONDITION_URI, summary, state));
            prefs(context).edit().putBoolean(KEY_ACTIVE, active).apply();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static Condition currentCondition(Context context) {
        boolean active = isActive(context);
        return new Condition(
                CONDITION_URI,
                active ? activeSummary(context) : "대기 중",
                active ? Condition.STATE_TRUE : Condition.STATE_FALSE);
    }

    private static String activeSummary(Context context) {
        return "평일 공휴일 " + ScheduleSettings.read(context).summary();
    }

    private static String ensureRule(Context context, NotificationManager manager) {
        SharedPreferences preferences = prefs(context);
        String existingId = preferences.getString(KEY_RULE_ID, null);
        if (existingId != null && manager.getAutomaticZenRule(existingId) != null) {
            return existingId;
        }

        ZenPolicy policy = new ZenPolicy.Builder()
                .allowAlarms(true)
                .allowMedia(true)
                .allowSystem(false)
                .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
                .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
                .allowRepeatCallers(true)
                .allowEvents(false)
                .allowReminders(false)
                .allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
                .build();

        AutomaticZenRule rule = new AutomaticZenRule.Builder(RULE_NAME, CONDITION_URI)
                .setOwner(new ComponentName(context, HolidayConditionProvider.class))
                .setConfigurationActivity(new ComponentName(context, MainActivity.class))
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(policy)
                .setEnabled(true)
                .build();

        String ruleId = manager.addAutomaticZenRule(rule);
        preferences.edit().putString(KEY_RULE_ID, ruleId).apply();
        manager.setAutomaticZenRuleState(
                ruleId, new Condition(CONDITION_URI, "대기 중", Condition.STATE_FALSE));
        return ruleId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
