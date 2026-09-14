package com.chocho.taptotop;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.chocho.ui.SettingsSheet;
import com.chocho.ui.EditorDialog;
import com.chocho.ui.DetailsScreen;
import com.chocho.ui.DetailsContent;
import com.chocho.ui.SettingsPage;
import com.chocho.ui.SaveJobs;
import com.chocho.ui.Ui;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public final class MainActivity extends Activity implements EditorDialog.Host,DetailsScreen.Host {
    private TextView statusView;
    private TextView tapsValue,strengthValue,distanceValue;
    private SettingsPage.Segmented tapsControl;
    private SettingsUi ui;
    private android.widget.Switch enabledToggle;
    private boolean syncing;
    private Button permissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildUi() {
        ui=new SettingsUi(this,"맨 위로 톡","상태 표시줄을 톡, 위쪽으로 한 번에");
        AppSettings settings=new AppSettings(this);
        LinearLayout status=ui.group(null);
        enabledToggle=ui.toggle(status,"스크롤 사용",settings.enabled(),value->{
            if(syncing) return;
            if(!settings.setEnabled(value)) SettingsSheet.message(this,"저장하지 못했어요.");
            refreshStatus();
        });
        statusView=ui.footer("");
        permissionButton=ui.row(status,"접근성 설정 열기  ›",this::openAccessibilitySettings);

        LinearLayout gesture=ui.group("실행 방법");
        tapsControl=ui.segmented(gesture,"상단 탭 횟수",settings.options().taps,value->{
            synchronized(AppSettings.class){ScrollOptions current=settings.options();return saveOptions(new ScrollOptions(value,current.strength,current.distance));}
        });
        ui.footer("선택하면 바로 적용돼요.\n두 번 탭은 같은 곳을 0.35초 안에 눌러요.");

        LinearLayout motion=ui.group("스크롤 조절");
        strengthValue=ui.setting(motion,"스크롤 세기",()->{
            EditorDialog.number(this,"strength","스크롤 세기",60,160,"%","100%는 기존 세기예요. 높일수록 빠르게 쓸어내려요.\n다음 탭부터 적용돼요.");
        });
        ui.separator(motion);
        distanceValue=ui.setting(motion,"쓸어내리는 길이",()->{
            EditorDialog.number(this,"distance","쓸어내리는 길이",40,75,"%","대상 영역의 비율이에요. 실제 이동 거리는 앱마다 달라요.\n다음 탭부터 적용돼요.");
        });
        ui.footer("다음 탭부터 적용돼요.\n한 번만 움직이며 자동으로 계속 반복하지 않아요.");

        LinearLayout tools=ui.group(null);
        ui.row(tools,"조절값 기본값으로",()->{ScrollOptions d=ScrollOptions.defaults();EditorDialog.reset(this,new int[]{d.taps,d.strength,d.distance},"한 번 탭 · 세기 100%\n길이 62%로 돌아가요.\n\n사용 여부와 권한은 유지해요.");}).setTextColor(Ui.RED);
        ui.separator(tools);
        ui.row(tools,"사용법 및 권한  ›",()->DetailsScreen.open(this));
        ui.footer("아주 긴 피드는 한 번에 맨 위까지 가지 않을 수 있어요.");
        return ui.page;
    }

    private void chooseTaps() {
        ScrollOptions options=new AppSettings(this).options();
        SettingsSheet.choice(this,"상단 탭 횟수","상태 표시줄을 몇 번 눌러 실행할지 고르세요.",
                new String[]{"한 번 탭","두 번 탭"},options.taps-1,selected->{
                    ScrollOptions current=new AppSettings(this).options();
                    return saveOptions(new ScrollOptions(selected+1,current.strength,current.distance));
                });
    }

    private boolean saveOptions(ScrollOptions value) {
        boolean saved=new AppSettings(this).saveOptions(value);
        refreshStatus();
        SettingsSheet.message(this,saved?"설정을 저장했어요. 다음 탭부터 적용돼요.":"저장하지 못했어요.");
        return saved;
    }

    private void refreshStatus() {
        AppSettings settings=new AppSettings(this);
        boolean permission=isServiceEnabled(),enabled=settings.enabled();
        syncing=true; enabledToggle.setChecked(enabled); syncing=false;
        statusView.setText(!permission?"접근성 설정에서 ‘맨 위로 톡’을 켜 주세요."
                : enabled?"사용 중 · 설정한 횟수로 상태 표시줄을 탭해 보세요."
                : "잠시 멈췄어요. 다시 켜면 바로 사용할 수 있어요.");
        permissionButton.setVisibility(permission?View.GONE:View.VISIBLE);
        ScrollOptions o=settings.options();
        tapsControl.setSelectedValue(o.taps);
        strengthValue.setText(o.strength+"%");
        distanceValue.setText(o.distance+"%");
    }

    private boolean isServiceEnabled() {
        AccessibilityManager manager =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        ComponentName expected = new ComponentName(this, TapToTopService.class);
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            ComponentName actual = new ComponentName(
                    info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
            if (expected.equals(actual)) return true;
        }
        return false;
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    @Override public int[] editorValues(String key){return values(new AppSettings(this).options(),key);}
    private static int[] values(ScrollOptions o,String key){
        if("strength".equals(key))return new int[]{o.strength};if("distance".equals(key))return new int[]{o.distance};
        if("reset".equals(key))return new int[]{o.taps,o.strength,o.distance};throw new IllegalArgumentException(key);
    }
    @Override public java.util.concurrent.Callable<SaveJobs.Result> editorSave(String key,int[] before,int[] selected){
        Context context=getApplicationContext();return ()->{synchronized(AppSettings.class){AppSettings settings=new AppSettings(context);ScrollOptions current=settings.options();
            if(!java.util.Arrays.equals(before,values(current,key)))return SaveJobs.Result.failed("다른 변경이 있어요. 취소한 뒤 다시 열어 주세요.");
            ScrollOptions next="reset".equals(key)?new ScrollOptions(selected[0],selected[1],selected[2]):new ScrollOptions(current.taps,"strength".equals(key)?selected[0]:current.strength,"distance".equals(key)?selected[0]:current.distance);
            return settings.saveOptions(next)?SaveJobs.Result.saved("설정을 저장했어요. 다음 탭부터 적용돼요."):SaveJobs.Result.failed("저장하지 못했어요. 값을 확인하고 다시 시도해 주세요.");
        }};
    }
    @Override public void editorUpdated(){if(statusView!=null&&!isDestroyed())refreshStatus();}
    @Override public View createDetails(){
        DetailsContent content=new DetailsContent(this);LinearLayout permissions=content.section("권한 및 설정");
        content.action(permissions,"접근성 설정 열기",this::openAccessibilitySettings);
        content.action(permissions,"앱 정보 열기",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        LinearLayout usage=content.section("사용법");
        content.paragraph(usage,"알림창은 그대로 열 수 있어요","상단에서 아래로 끌면 기존처럼 알림창이 열려요.");
        content.paragraph(usage,"앱마다 올라가는 방식이 달라요","아주 긴 피드는 한 번에 맨 위까지 가지 않을 수 있어요. 아이폰과 완전히 같지는 않아요.");
        LinearLayout privacy=content.section("개인정보");
        content.paragraph(privacy,"화면 내용은 저장하지 않아요","화면 내용과 사용 기록을 저장하거나 전송하지 않아요. 접근성 권한은 스크롤 영역을 찾고 제스처를 실행하는 데만 사용해요.");return content;
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomMargin;
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
