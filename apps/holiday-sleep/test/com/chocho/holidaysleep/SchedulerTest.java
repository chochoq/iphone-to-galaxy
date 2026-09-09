package com.chocho.holidaysleep;
import android.app.AlarmManager;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneId;

public final class SchedulerTest {
    private static int checks;
    private static void check(boolean ok,String label) { checks++; if(!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        TestContext context=new TestContext();
        Scheduler.setEnabled(context,false);
        check(Scheduler.saveSchedule(context,new ScheduleWindow(21600,36000)),"save while off");
        check(context.alarm.scheduled.isEmpty(),"no scheduled alarms while off");
        check(!ZenController.active && !Scheduler.isEnabled(context),"save does not enable");
        check(Scheduler.nextScheduleSummary(context,Scheduler.ACTION_START).contains("예약하지 않음"),"off status truthful");
        Scheduler.setEnabled(context,true);
        check(context.alarm.scheduled.size()==2,"both boundaries scheduled");
        check(seconds(context.alarm.scheduled.get(70015))==21600,"custom start");
        check(seconds(context.alarm.scheduled.get(120000))==36000,"custom end");
        Scheduler.saveSchedule(context,new ScheduleWindow(25200,50000));
        check(context.alarm.scheduled.size()==2,"replaces rather than duplicates");
        check(seconds(context.alarm.scheduled.get(120000))==50000,"end replaced");
        // Real Scheduler, fake Android services: no device dates or alarms are changed.
        ScheduleWindow allDay=new ScheduleWindow(0,86399);
        Scheduler.saveSchedule(context,allDay);
        boolean expected=allDay.shouldActivate(ZonedDateTime.now(),true,true);
        Scheduler.onAlarm(context,Scheduler.ACTION_STOP);
        check(ZenController.active==expected,"stale STOP reconciles current interval");
        Scheduler.saveSchedule(context,new ScheduleWindow(0,1));
        expected=new ScheduleWindow(0,1).shouldActivate(ZonedDateTime.now(),true,true);
        Scheduler.onAlarm(context,Scheduler.ACTION_START);
        check(ZenController.active==expected,"stale START does not blindly enable");
        HolidayCalendar.permission=false;
        Scheduler.configureAndReconcile(context);
        check(!ZenController.active,"revoked calendar permission clears own rule");
        HolidayCalendar.permission=true;
        HolidayCalendar.holiday=false;
        Scheduler.configureAndReconcile(context);
        check(!ZenController.active,"nonholiday inactive");
        context.alarm.exactAllowed=false;
        Scheduler.configureAndReconcile(context);
        check(context.alarm.fallbackCalls==2,"inexact fallback for both events");
        context.alarm.exactAllowed=true;
        context.alarm.revokeDuringSet=true;
        Scheduler.configureAndReconcile(context);
        check(context.alarm.fallbackCalls==4,"permission race fallback");
        Scheduler.setEnabled(context,false);
        check(context.alarm.scheduled.isEmpty(),"off cancels both events");
        Scheduler.onAlarm(context,Scheduler.ACTION_STOP);
        check(context.alarm.scheduled.isEmpty() && !ZenController.active,"old event after off stays off");
        Scheduler.onAlarm(context,"unknown");
        check(context.alarm.scheduled.isEmpty(),"unknown event ignored");
        System.out.println("Scheduler: "+checks+" assertions passed (real adapter, fake Android services)");
    }
    static int seconds(long millis) { return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay(); }
    static final class TestContext extends ScheduleSettingsTest.FakeContext {
        final AlarmManager alarm=new AlarmManager();
        public <T> T getSystemService(Class<T> type) { return type.cast(alarm); }
    }
}
