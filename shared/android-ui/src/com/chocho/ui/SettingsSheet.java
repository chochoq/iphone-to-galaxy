package com.chocho.ui;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;

/** Original Android UI inspired by contextual editing, not a UIKit implementation. */
public final class SettingsSheet {
    public static final int BG=0xfff2f2f7, PAPER=Color.WHITE, INK=0xff1c1c1e;
    public static final int MUTED=0xff636366, BLUE=0xff007aff, LINE=0xffe5e5ea;
    private static final WeakHashMap<Activity, Dialog> OPEN=new WeakHashMap<>();
    private static final WeakHashMap<Activity, View> MESSAGES=new WeakHashMap<>();
    private final Activity activity;
    private final Dialog dialog;
    private final LinearLayout root;
    private final TextView error;
    private final Button done;
    private final Application.ActivityLifecycleCallbacks lifecycle;
    private boolean saving;
    private BooleanSupplier commit=()->true;

    private SettingsSheet(Activity a,String title,String description,String action,boolean destructive) {
        activity=a;
        Dialog previous=OPEN.remove(a);
        if(previous!=null)previous.dismiss();
        dialog=new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a,20),dp(a,10),dp(a,20),dp(a,22));
        root.setBackground(rounded(a,BG,26));
        LinearLayout toolbar=new LinearLayout(a);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        Button cancel=button(a,"취소",BLUE);cancel.setOnClickListener(v->dialog.cancel());
        toolbar.addView(cancel,new LinearLayout.LayoutParams(-2,-2));
        toolbar.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
        done=button(a,action,destructive?0xffc9342c:BLUE);
        toolbar.addView(done,new LinearLayout.LayoutParams(-2,-2));root.addView(toolbar);
        TextView heading=text(a,title,22,INK,true);heading.setAccessibilityHeading(true);
        heading.setPadding(0,dp(a,10),0,dp(a,12));root.addView(heading);
        if(description!=null&&!description.isEmpty()){
            TextView detail=text(a,description,15,MUTED,false);
            detail.setPadding(0,0,0,dp(a,20));root.addView(detail);
        }
        error=text(a,"",14,0xffb42318,false);error.setVisibility(View.GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        done.setOnClickListener(v->{
            if(saving)return;saving=true;done.setEnabled(false);
            try {
                root.clearFocus();
                if(commit.getAsBoolean())dialog.dismiss();
                else showError();
            } catch(RuntimeException failure){showError();}
            finally{saving=false;done.setEnabled(true);}
        });
        ScrollView scroll=new ScrollView(a){
            @Override protected void onMeasure(int w,int h){
                int max=(int)(a.getWindowManager().getCurrentWindowMetrics().getBounds().height()*.82f);
                super.onMeasure(w,MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(h),max),MeasureSpec.AT_MOST));
            }
        };
        scroll.setFillViewport(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);scroll.addView(root);
        dialog.setContentView(scroll);
        lifecycle=new Application.ActivityLifecycleCallbacks(){
            public void onActivityDestroyed(Activity host){if(host==activity)dialog.dismiss();}
            public void onActivityCreated(Activity host,Bundle b){}
            public void onActivityStarted(Activity host){}
            public void onActivityResumed(Activity host){}
            public void onActivityPaused(Activity host){}
            public void onActivityStopped(Activity host){}
            public void onActivitySaveInstanceState(Activity host,Bundle b){}
        };
        dialog.setOnDismissListener(d->{
            if(OPEN.get(a)==dialog)OPEN.remove(a);
            a.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);
        });
    }
    private void showError(){error.setText("저장을 확인하지 못했어요. 값을 확인하고 다시 시도해 주세요.");error.setVisibility(View.VISIBLE);}
    private void show(){
        if(activity.isFinishing()||activity.isDestroyed())return;
        activity.getApplication().registerActivityLifecycleCallbacks(lifecycle);
        root.addView(error);OPEN.put(activity,dialog);dialog.show();
        Window window=dialog.getWindow();if(window==null)return;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        int width=activity.getWindowManager().getCurrentWindowMetrics().getBounds().width();
        window.setLayout(Math.min(width-dp(activity,24),dp(activity,560)),ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(activity.getResources().getConfiguration().screenWidthDp>=600?Gravity.CENTER:Gravity.BOTTOM);
        WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.28f;lp.y=dp(activity,12);window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    public static void choice(Activity a,String title,String description,String[] labels,int initial,IntPredicate save){
        if(labels.length==0)throw new IllegalArgumentException("No choices");
        SettingsSheet s=new SettingsSheet(a,title,description,"저장",false);
        final int[] selected={Math.max(0,Math.min(initial,labels.length-1))};
        LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);list.setBackground(rounded(a,PAPER,16));
        TextView[] ticks=new TextView[labels.length];View[] rows=new View[labels.length];
        for(int i=0;i<labels.length;i++){
            final int index=i;
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(a,56));
            row.setPadding(dp(a,16),dp(a,10),dp(a,16),dp(a,10));row.setBackground(pressed(a,PAPER,12));
            TextView label=text(a,labels[i],17,INK,false);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            ticks[i]=text(a,"✓",20,BLUE,true);ticks[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ticks[i].setVisibility(i==selected[0]?View.VISIBLE:View.INVISIBLE);
            row.addView(ticks[i],new LinearLayout.LayoutParams(dp(a,28),-2));
            row.setFocusable(true);row.setClickable(true);row.setContentDescription(labels[i]);
            row.setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
                    super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.RadioButton");
                    info.setCheckable(true);info.setChecked(selected[0]==index);
                }
            });
            row.setOnClickListener(v->{selected[0]=index;for(int j=0;j<ticks.length;j++){
                ticks[j].setVisibility(j==index?View.VISIBLE:View.INVISIBLE);rows[j].setSelected(j==index);
            }});rows[i]=row;row.setSelected(i==selected[0]);list.addView(row);
        }
        s.root.addView(list);s.commit=()->save.test(selected[0]);s.show();
    }
    public static void number(Activity a,String title,String description,int current,int min,int max,String unit,IntPredicate save){
        SettingsSheet s=new SettingsSheet(a,title,description,"저장",false);
        NumberPicker picker=s.wheel(title,current,min,max);
        LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER);
        line.addView(s.wheelFrame(picker),new LinearLayout.LayoutParams(0,dp(a,180),1));
        TextView suffix=text(a,unit,20,INK,true);suffix.setPadding(dp(a,12),0,dp(a,20),0);line.addView(suffix);
        s.root.addView(line);s.commit=()->{picker.clearFocus();return save.test(picker.getValue());};s.show();
    }
    public static void time(Activity a,String title,int hour,int minute,BiConsumer<Integer,Integer> apply){
        SettingsSheet s=new SettingsSheet(a,title,"시간을 골라 주세요.\n일정은 이전 화면에서 ‘변경사항 저장’을 눌러 바꿔요.","적용",false);
        NumberPicker hours=s.wheel("시",hour,0,23),minutes=s.wheel("분",minute,0,59);
        hours.setFormatter(value->String.format(java.util.Locale.getDefault(),"%02d",value));
        minutes.setFormatter(value->String.format(java.util.Locale.getDefault(),"%02d",value));
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(s.wheelFrame(hours),new LinearLayout.LayoutParams(0,dp(a,180),1));
        row.addView(text(a,"시",18,MUTED,false));
        row.addView(s.wheelFrame(minutes),new LinearLayout.LayoutParams(0,dp(a,180),1));
        row.addView(text(a,"분",18,MUTED,false));s.root.addView(row);
        s.commit=()->{hours.clearFocus();minutes.clearFocus();apply.accept(hours.getValue(),minutes.getValue());return true;};s.show();
    }
    public static void confirmReset(Activity a,BooleanSupplier reset){
        SettingsSheet s=new SettingsSheet(a,"조절값을 되돌릴까요?","조절한 값만 기본값으로 바꿔요. 연결 기기, 권한, 켜짐·꺼짐 상태는 그대로 유지해요.","되돌리기",true);
        s.commit=reset;s.show();
    }
    private NumberPicker wheel(String label,int value,int min,int max){
        if(min>max)throw new IllegalArgumentException("Invalid range");
        NumberPicker p=new NumberPicker(activity);p.setMinValue(min);p.setMaxValue(max);p.setValue(Math.max(min,Math.min(max,value)));
        p.setWrapSelectorWheel(false);p.setSelectionDividerHeight(0);p.setTextColor(INK);
        p.setTextSize(23*activity.getResources().getDisplayMetrics().scaledDensity);
        p.setContentDescription(label);p.setBackgroundColor(Color.TRANSPARENT);
        // OEM accessibility trees may expose only the editable child, not its wheel parent.
        for(int i=0;i<p.getChildCount();i++)if(p.getChildAt(i) instanceof android.widget.EditText)
            p.getChildAt(i).setContentDescription(label);
        return p;
    }
    private FrameLayout wheelFrame(NumberPicker picker){
        FrameLayout frame=new FrameLayout(activity);
        View selected=new View(activity);selected.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        selected.setBackground(rounded(activity,0xffe9e9ee,10));
        frame.addView(selected,new FrameLayout.LayoutParams(-1,dp(activity,46),Gravity.CENTER));
        frame.addView(picker,new FrameLayout.LayoutParams(-1,-1));return frame;
    }
    public static void message(Activity a,String text){
        if(Looper.myLooper()!=Looper.getMainLooper()){a.runOnUiThread(()->message(a,text));return;}
        if(a.isFinishing()||a.isDestroyed())return;
        ViewGroup decor=(ViewGroup)a.getWindow().getDecorView();
        View old=MESSAGES.remove(a);if(old!=null&&old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old);
        LinearLayout banner=new LinearLayout(a);banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(a,16),dp(a,8),dp(a,8),dp(a,8));banner.setElevation(dp(a,6));banner.setBackground(rounded(a,PAPER,16));
        TextView label=text(a,text,15,INK,false);label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        banner.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button close=button(a,"닫기",BLUE);banner.addView(close);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
        lp.setMargins(dp(a,16),dp(a,16),dp(a,16),dp(a,48));decor.addView(banner,lp);MESSAGES.put(a,banner);
        Runnable remove=()->{if(banner.getParent() instanceof ViewGroup)((ViewGroup)banner.getParent()).removeView(banner);if(MESSAGES.get(a)==banner)MESSAGES.remove(a);};
        close.setOnClickListener(v->remove.run());
        AccessibilityManager am=a.getSystemService(AccessibilityManager.class);
        int duration=am==null?8000:am.getRecommendedTimeoutMillis(8000,AccessibilityManager.FLAG_CONTENT_TEXT|AccessibilityManager.FLAG_CONTENT_CONTROLS);
        new Handler(Looper.getMainLooper()).postDelayed(remove,duration);
    }
    public static StateListDrawable pressed(Context c,int normal,int radius){
        StateListDrawable state=new StateListDrawable();
        // Filled buttons have light text; keep a dark background while pressed.
        int active=Color.luminance(normal)<.4f
                ?Color.rgb(Math.round(Color.red(normal)*.8f),Math.round(Color.green(normal)*.8f),Math.round(Color.blue(normal)*.8f))
                :0xffdfe8f5;
        state.addState(new int[]{-android.R.attr.state_enabled},rounded(c,LINE,radius));
        state.addState(new int[]{android.R.attr.state_pressed},rounded(c,active,radius));
        state.addState(new int[]{android.R.attr.state_focused},rounded(c,active,radius));
        state.addState(new int[]{},rounded(c,normal,radius));return state;
    }
    public static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    private static Button button(Context c,String label,int color){
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(color);
        b.setTypeface(Typeface.create("sans-serif-medium",0));b.setMinWidth(dp(c,64));b.setMinimumHeight(dp(c,48));
        b.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));b.setStateListAnimator(null);b.setBackground(pressed(c,BG,10));return b;
    }
    private static TextView text(Context c,String value,int size,int color,boolean bold){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",0));t.setIncludeFontPadding(false);t.setLineSpacing(dp(c,3),1);
        t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        if(android.os.Build.VERSION.SDK_INT>=33)t.setLineBreakWordStyle(android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE);
        return t;
    }
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
