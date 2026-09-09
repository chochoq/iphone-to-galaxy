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
import android.widget.Toast;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public final class MainActivity extends Activity {
    private TextView statusView;
    private TextView tapsValue,strengthValue,distanceValue;
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
            if(!settings.setEnabled(value)) Toast.makeText(this,"저장하지 못했어요.",Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        statusView=ui.note(status,"");
        permissionButton=ui.row(status,"접근성 설정 열기  ›",this::openAccessibilitySettings);

        LinearLayout gesture=ui.group("실행 방법");
        tapsValue=ui.setting(gesture,"상단 탭 횟수",()->chooseTaps());
        ui.note(gesture,"두 번 탭은 같은 곳을 0.35초 안에 두 번 눌러요. 아래로 끌면 기존처럼 알림창이 열려요.");

        LinearLayout motion=ui.group("스크롤 조절");
        strengthValue=ui.setting(motion,"스크롤 세기",()->{
            ScrollOptions o=settings.options();
            ui.number("스크롤 세기","60–160%. 100%는 기존 세기예요. 높일수록 더 빠르게 쓸어내려요.",
                    o.strength,60,160,value->{
                        ScrollOptions current=settings.options();
                        return saveOptions(new ScrollOptions(current.taps,value,current.distance));
                    });
        });
        ui.separator(motion);
        distanceValue=ui.setting(motion,"쓸어내리는 길이",()->{
            ScrollOptions o=settings.options();
            ui.number("쓸어내리는 길이","대상 영역의 40–75%. 실제 목록의 이동 거리는 앱마다 달라요.",
                    o.distance,40,75,value->{
                        ScrollOptions current=settings.options();
                        return saveOptions(new ScrollOptions(current.taps,current.strength,value));
                    });
        });
        ui.note(motion,"변경은 다음 탭부터 적용돼요. 한 번만 움직이며 자동으로 계속 반복하지 않아요.");

        LinearLayout tools=ui.group(null);
        ui.row(tools,"조절값 기본값으로",()->ui.confirmReset(()->saveOptions(ScrollOptions.defaults())));
        ui.separator(tools);
        ui.row(tools,"앱 정보  ›",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:"+getPackageName()))));
        LinearLayout info=ui.group("알아두기");
        ui.note(info,"아주 긴 피드는 한 번에 맨 위까지 가지 않을 수 있어요. 앱마다 스크롤 방식이 달라 아이폰과 완전히 같지는 않아요.");
        ui.note(info,"화면 내용과 사용 기록을 저장하거나 전송하지 않아요. 접근성 권한은 스크롤할 영역을 찾고 제스처를 실행하는 데만 사용해요.");
        return ui.scroll;
    }

    private void chooseTaps() {
        final int[] selected={new AppSettings(this).options().taps};
        new android.app.AlertDialog.Builder(this).setTitle("상단 탭 횟수")
                .setSingleChoiceItems(new String[]{"한 번 탭","두 번 탭"},selected[0]-1,(d,w)->selected[0]=w+1)
                .setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{
                    ScrollOptions current=new AppSettings(this).options();
                    saveOptions(new ScrollOptions(selected[0],current.strength,current.distance));
                }).show();
    }

    private boolean saveOptions(ScrollOptions value) {
        boolean saved=new AppSettings(this).saveOptions(value);
        refreshStatus();
        Toast.makeText(this,saved?"설정을 저장했어요. 다음 탭부터 적용돼요.":"저장하지 못했어요.",Toast.LENGTH_SHORT).show();
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
        tapsValue.setText(o.taps==1?"한 번  ›":"두 번  ›");
        strengthValue.setText(o.strength+"%  ›");
        distanceValue.setText(o.distance+"%  ›");
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
