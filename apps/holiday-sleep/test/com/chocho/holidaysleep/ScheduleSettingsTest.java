package com.chocho.holidaysleep;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public final class ScheduleSettingsTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError(checks); }
    public static void main(String[] args) {
        FakeContext context = new FakeContext();
        context.values.put("enabled",false);
        context.values.put("zen_rule_id","existing-rule");
        check(ScheduleSettings.read(context).summary().equals("07:00:15 ~ 12:00"));
        check(ScheduleSettings.save(context,new ScheduleWindow(21600,36000)));
        check(ScheduleSettings.read(context).summary().equals("06:00 ~ 10:00"));
        check(context.values.get("enabled").equals(false));
        check(context.values.get("zen_rule_id").equals("existing-rule"));
        check(context.commits == 1);
        context.fail = true;
        check(!ScheduleSettings.save(context,new ScheduleWindow(25200,40000)));
        check(ScheduleSettings.read(context).summary().equals("06:00 ~ 10:00"));
        context.values.put("start_second","bad type");
        check(ScheduleSettings.read(context).summary().equals("07:00:15 ~ 12:00"));
        context.values.put("start_second",60000);
        check(ScheduleSettings.read(context).summary().equals("07:00:15 ~ 12:00"));
        System.out.println("ScheduleSettings: " + checks + " assertions passed (fake preferences, not device storage)");
    }

    static class FakeContext extends Context implements SharedPreferences {
        final Map<String,Object> values = new HashMap<>();
        boolean fail;
        int commits;
        public Map<String, ?> getAll() { return new HashMap<>(values); }
        public SharedPreferences getSharedPreferences(String name,int mode) {
            if (!name.equals("holiday_sleep")) throw new AssertionError(name);
            return this;
        }
        public int getInt(String key,int fallback) {
            return values.containsKey(key) ? (Integer) values.get(key) : fallback;
        }
        public boolean getBoolean(String key,boolean fallback) {
            return values.containsKey(key) ? (Boolean) values.get(key) : fallback;
        }
        public Editor edit() {
            return new Editor() {
                final Map<String,Object> pending = new HashMap<>();
                public Editor putInt(String key,int value) { pending.put(key,value); return this; }
                public Editor putBoolean(String key,boolean value) { pending.put(key,value); return this; }
                public void apply() { commit(); }
                public boolean commit() {
                    commits++;
                    values.putAll(pending);
                    return !fail;
                }
            };
        }
    }
}
