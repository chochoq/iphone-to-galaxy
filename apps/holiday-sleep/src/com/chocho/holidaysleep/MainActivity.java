package com.chocho.holidaysleep;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.chocho.ui.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Callable;

/** Grouped settings; the scheduler and its persisted second-precision interval remain authoritative. */
public final class MainActivity extends Activity implements EditorDialog.Host,DetailsScreen.Host {
    private static final int REQUEST_CALENDAR=41;
    private SettingsPage ui;
    private TextView timeValue,statusSummary,warningView;
    private DetailsContent.Summary statusView;
    private Button permissionButton;
    private Switch enabledSwitch;
    private boolean syncing;
    @Override protected void onCreate(Bundle state){super.onCreate(state);buildUi();}
    @Override protected void onResume(){super.onResume();refreshStatus();if(allPermissionsReady())refreshSchedule();}
    private void refreshSchedule(){
        Context app=getApplicationContext();
        new Thread(()->{
            String failure=null;
            try{Scheduler.configureAndReconcile(app);}catch(RuntimeException e){failure="실행 상태를 확인하지 못했어요. 권한과 설정을 확인해 주세요.";}
            final String message=failure;
            runOnUiThread(()->{if(isDestroyed())return;refreshStatus();if(message!=null)SettingsSheet.message(this,message);});
        },"HolidaySleepRefresh").start();
    }
    private void buildUi(){
        ui=new SettingsPage(this,"공휴일 수면 연장");
        LinearLayout schedule=ui.group(null);
        enabledSwitch=ui.toggle(schedule,"자동 연장",Scheduler.isEnabled(this),enabled->{
            if(syncing)return;
            try{Scheduler.setEnabled(getApplicationContext(),enabled);}
            catch(RuntimeException e){SettingsSheet.message(this,"실행 상태를 확인하지 못했어요. 권한을 확인해 주세요.");}
            refreshStatus();
        });
        enabledSwitch.setContentDescription("공휴일 자동 연장");
        ui.separator(schedule);
        timeValue=ui.setting(schedule,"연장 시간",()->EditorDialog.time(this));
        ui.footer("평일에 든 공휴일에만 방해 금지를 연장해요.");
        LinearLayout status=ui.group("상태");
        statusSummary=ui.note(status,"상태 확인 중");
        ui.separator(status);
        ui.row(status,"권한 및 상세 정보  ›",()->DetailsScreen.open(this));
        warningView=ui.footer("");warningView.setTextColor(Ui.RED);
        warningView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        permissionButton=ui.row(status,"필요한 권한 확인",this::handlePermissions);
        permissionButton.setTextColor(Ui.BLUE);
        ui.footer("알람 시간은 바꾸지 않아요.\n평소 수면 일정과 공휴일 알람은 삼성 모드·시계에서 설정해요.");
        ui.install();refreshStatus();
    }
    private void refreshStatus(){
        if(statusSummary==null||isDestroyed())return;
        ScheduleWindow saved=ScheduleSettings.read(this);
        timeValue.setText(saved.summary());
        syncing=true;enabledSwitch.setChecked(Scheduler.isEnabled(this));syncing=false;
        boolean calendar=HolidayCalendar.hasCalendarPermission(this),dnd=ZenController.hasPolicyAccess(this),exact=Scheduler.canScheduleExact(this);
        boolean sourceMissing=false,holidayToday=false,calendarFailed=false;
        StringBuilder detail=new StringBuilder();
        detail.append("자동 연장: ").append(Scheduler.isEnabled(this)?"켜짐":"꺼짐").append('\n');
        detail.append("저장된 시간: ").append(saved.summary()).append('\n');
        detail.append("캘린더 권한: ").append(calendar?"허용":"필요").append('\n');
        detail.append("방해 금지 권한: ").append(dnd?"허용":"필요").append('\n');
        detail.append("정확한 시간 실행: ").append(exact?"허용":"필요").append('\n');
        if(calendar){
            try{
                List<String> calendars=HolidayCalendar.detectedCalendarNames(this);sourceMissing=calendars.isEmpty();
                detail.append("사용 달력: ").append(sourceMissing?"공휴일 달력을 찾지 못함":String.join(", ",calendars)).append('\n');
                if(!sourceMissing){List<String> titles=HolidayCalendar.holidayTitlesForDate(this,LocalDate.now());holidayToday=!titles.isEmpty();detail.append("오늘: ").append(titles.isEmpty()?"법정공휴일 아님":String.join(", ",titles)).append('\n');}
                else detail.append("오늘: 공휴일 여부 확인 필요\n");
            }catch(RuntimeException e){calendarFailed=true;detail.append("오늘: 공휴일 정보를 읽지 못함\n");}
        }else detail.append("오늘: 캘린더 권한을 허용한 뒤 확인\n");
        detail.append("다음 시작 확인: ").append(Scheduler.nextScheduleSummary(this,Scheduler.ACTION_START)).append('\n');
        detail.append("다음 종료 확인: ").append(Scheduler.nextScheduleSummary(this,Scheduler.ACTION_STOP)).append('\n');
        detail.append("매일 예약 시각에 확인하고, 평일 공휴일에만 켭니다.\n");
        detail.append("현재 연장: ").append(ZenController.isActive(this)?"켜짐":"꺼짐");
        if(statusView!=null)statusView.setText(detail.toString());
        java.util.ArrayList<String> missing=new java.util.ArrayList<>();
        if(!calendar)missing.add("캘린더 읽기 권한");if(!dnd)missing.add("방해 금지 접근 권한");if(!exact)missing.add("정확한 알람 권한");
        if(sourceMissing)missing.add("폰에 동기화된 한국 공휴일 달력");if(calendarFailed)missing.add("공휴일 달력 읽기");
        boolean attention=!missing.isEmpty();
        warningView.setText(attention?"자동 연장을 위해 확인해 주세요.\n"+String.join(" · ",missing):"");
        warningView.setVisibility(attention?View.VISIBLE:View.GONE);
        permissionButton.setVisibility(!calendar||!dnd||!exact?View.VISIBLE:View.GONE);
        boolean weekend=LocalDate.now().getDayOfWeek().getValue()>=6;
        statusSummary.setText(!Scheduler.isEnabled(this)?"자동 연장 꺼짐"
                :attention?"자동 실행 준비 확인 필요"
                :ZenController.isActive(this)?"방해 금지 연장 중"
                :weekend?"주말 · 삼성 수면 일정 사용"
                :holidayToday?"오늘은 공휴일 · 설정한 시간에 연장":"대기 중 · 오늘은 공휴일이 아니에요.");
    }
    @Override public int[] editorValues(String key){
        if(!"schedule".equals(key))throw new IllegalArgumentException(key);
        ScheduleWindow saved=ScheduleSettings.read(this);return new int[]{saved.startSecond,saved.endSecond};
    }
    @Override public Callable<SaveJobs.Result> editorSave(String key,int[] before,int[] selected){
        if(!"schedule".equals(key))throw new IllegalArgumentException(key);
        Context app=getApplicationContext();
        return ()->{synchronized(Scheduler.class){
            ScheduleWindow current=ScheduleSettings.read(app);
            if(!Arrays.equals(before,new int[]{current.startSecond,current.endSecond}))return SaveJobs.Result.failed("다른 변경이 있어요. 취소한 뒤 다시 열어 주세요.");
            ScheduleWindow next=new ScheduleWindow(selected[0],selected[1]);
            boolean saved=false;
            try{saved=Scheduler.saveSchedule(app,next);}catch(RuntimeException ignored){/* Re-read below: persistence may have succeeded before scheduling failed. */}
            if(!saved){
                ScheduleWindow actual=ScheduleSettings.read(app);
                boolean valuesWritten=actual.startSecond==selected[0]&&actual.endSecond==selected[1];
                return SaveJobs.Result.failed(valuesWritten?"시간은 저장됐지만 자동 실행 상태를 확인해 주세요.":"저장하지 못했어요. 값을 확인하고 다시 시도해 주세요.");
            }
            if(!Scheduler.isEnabled(app))return SaveJobs.Result.saved("시간을 저장했어요. 자동 연장은 꺼진 상태예요.");
            boolean ready=HolidayCalendar.hasCalendarPermission(app)&&ZenController.hasPolicyAccess(app)&&Scheduler.canScheduleExact(app);
            return SaveJobs.Result.saved(ready?"시간을 저장하고 실행 일정을 갱신했어요.":"시간을 저장했어요. 자동 실행에 필요한 권한도 확인해 주세요.");
        }};
    }
    @Override public void editorUpdated(){refreshStatus();}
    @Override public View createDetails(){
        DetailsContent content=new DetailsContent(this);
        LinearLayout permissions=content.section("자동 실행에 필요한 권한");
        content.action(permissions,"캘린더 권한",()->requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},REQUEST_CALENDAR));
        content.action(permissions,"방해 금지 접근 설정",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        content.action(permissions,"정확한 알람 설정",this::openExactSettings);
        LinearLayout status=content.section("실행 상태");
        content.action(status,"상태 다시 확인",false,this::refreshSchedule);Ui.separator(status);statusView=content.summary(status,"");
        LinearLayout about=content.section("알아두기");
        content.paragraph(about,"삼성 수면 일정과 별개예요","주말과 평소 일정은 삼성 모드에서 설정해 주세요. 이 앱의 시작 시간과 자동으로 맞춰지지는 않아요.");
        content.paragraph(about,"방해 금지만 연장해요","수면 아이콘·잠금화면·기상 알람은 바꾸지 않아요. 통화 연결, 마이크와 Bluetooth도 건드리지 않아요.");
        content.paragraph(about,"폰에 있는 공휴일 달력을 읽어요","인터넷 접속 없이, 폰에 동기화된 한국 공휴일 달력을 사용해요.");
        content.paragraph(about,"초 단위도 유지해요","시·분을 바꿔도 기존 초는 남아요. ‘초까지 조정’이나 숫자 입력으로 직접 바꿀 수 있어요.");
        refreshStatus();return content;
    }
    private void openExactSettings(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}
    private void handlePermissions(){
        if(!HolidayCalendar.hasCalendarPermission(this)){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},REQUEST_CALENDAR);return;}
        if(!ZenController.hasPolicyAccess(this)){startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));return;}
        if(!Scheduler.canScheduleExact(this)){openExactSettings();return;}
        refreshSchedule();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);refreshStatus();}
    private boolean allPermissionsReady(){return HolidayCalendar.hasCalendarPermission(this)&&ZenController.hasPolicyAccess(this)&&Scheduler.canScheduleExact(this);}
}
