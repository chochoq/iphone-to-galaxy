package com.chocho.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Compact grouped settings shared by all three apps. OS navigation and permissions remain native. */
public class SettingsPage {
    public static final int BG=Ui.BG,WHITE=Ui.PAPER,INK=Ui.INK,MUTED=Ui.MUTED,BLUE=Ui.BLUE,GREEN=Ui.GREEN,LINE=Ui.LINE;
    public final Activity activity;
    public final FrameLayout page;
    public final LinearLayout root;
    public final ScrollView scroll;
    public SettingsPage(Activity a,String title){
        activity=a;a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        page=new FrameLayout(a);page.setBackgroundColor(BG);page.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setVerticalScrollBarEnabled(false);scroll.setSaveEnabled(true);scroll.setId(0x1008001);
        FrameLayout frame=new FrameLayout(a);
        root=new LinearLayout(a){@Override protected void onMeasure(int w,int h){super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(w),dp(632)),MeasureSpec.EXACTLY),h);}};
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(22),dp(16),dp(28));
        frame.addView(root,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));scroll.addView(frame);page.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        TextView heading=text(title,32,INK,true);heading.setAccessibilityHeading(true);root.addView(heading,params(26));
        TextView compact=text(title,17,INK,true);compact.setGravity(Gravity.CENTER);compact.setPadding(dp(16),dp(12),dp(16),dp(12));compact.setBackgroundColor(BG);compact.setVisibility(View.INVISIBLE);
        page.addView(compact,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        scroll.setOnScrollChangeListener((View.OnScrollChangeListener)(v,x,y,oldX,oldY)->compact.setVisibility(y>heading.getBottom()?View.VISIBLE:View.INVISIBLE));
    }
    public void install(){activity.setContentView(page);}
    public int dp(int n){return Ui.dp(activity,n);}
    public LinearLayout.LayoutParams params(int bottom){return Ui.space(activity,bottom);}
    public TextView text(String value,int size,int color,boolean bold){return Ui.text(activity,value,size,color,bold);}
    public LinearLayout group(String title){
        if(root.getChildCount()>1){View gap=new View(activity);root.addView(gap,new LinearLayout.LayoutParams(1,dp(18)));}
        if(title!=null){TextView heading=text(title,13,MUTED,false);heading.setPadding(dp(16),0,dp(16),dp(6));heading.setAccessibilityHeading(true);root.addView(heading);}
        LinearLayout g=Ui.group(activity);root.addView(g,new LinearLayout.LayoutParams(-1,-2));return g;
    }
    public void separator(LinearLayout group){Ui.separator(group);}
    public TextView footer(String value){TextView t=Ui.footnote(activity,value);root.addView(t);return t;}
    public TextView note(LinearLayout group,String value){TextView t=text(value,13,MUTED,false);t.setPadding(dp(16),dp(8),dp(16),dp(8));group.addView(t);return t;}
    public Button row(LinearLayout group,String label,Runnable action){
        Button b=Ui.navigation(activity,label,label.contains("›"));b.setOnClickListener(v->action.run());group.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
    }
    public TextView setting(LinearLayout group,String label,Runnable action){
        LinearLayout r=new LinearLayout(activity);r.setGravity(Gravity.CENTER_VERTICAL);r.setMinimumHeight(dp(50));r.setPadding(dp(16),dp(12),dp(16),dp(12));
        boolean stacked=activity.getResources().getConfiguration().screenWidthDp<360||activity.getResources().getConfiguration().fontScale>1.25;
        r.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        TextView name=text(label,17,INK,false),value=text("",16,MUTED,false);value.setFontFeatureSettings("tnum");
        value.setCompoundDrawablesRelative(null,null,new TrailingMark(activity,false),null);value.setCompoundDrawablePadding(dp(10));
        r.addView(name,stacked?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
        value.setPadding(stacked?0:dp(12),stacked?dp(6):0,0,0);r.addView(value,new LinearLayout.LayoutParams(-2,-2));
        r.setFocusable(true);r.setClickable(true);r.setBackground(SettingsSheet.pressed(activity,WHITE,8));r.setOnClickListener(v->action.run());
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);value.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        value.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(android.text.Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){r.setContentDescription(label+", "+s.toString().replace("›","")+", 변경");}});
        group.addView(r,new LinearLayout.LayoutParams(-1,-2));return value;
    }
    public Switch toggle(LinearLayout group,String label,boolean checked,Consumer<Boolean> change){
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(2),dp(12),dp(2));
        TextView name=text(label,17,INK,false);name.setPadding(0,dp(10),dp(8),dp(10));name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(name,new LinearLayout.LayoutParams(0,-2,1));Switch s=new SettingSwitch(activity);s.setContentDescription(label);s.setChecked(checked);s.setOnCheckedChangeListener((v,on)->change.accept(on));
        row.addView(s,new LinearLayout.LayoutParams(dp(55),dp(48)));group.addView(row);return s;
    }
    public Segmented segmented(LinearLayout group,String label,int selected,IntPredicate save){
        TextView name=text(label,15,INK,false);name.setPadding(dp(16),dp(12),dp(16),dp(6));group.addView(name);
        Segmented control=new Segmented(activity,selected,save);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(16),0,dp(16),dp(12));group.addView(control,p);return control;
    }
    public static final class Segmented extends LinearLayout {
        private final Button[] buttons=new Button[2];private int selected;
        Segmented(Activity a,int current,IntPredicate save){super(a);setPadding(Ui.dp(a,3),Ui.dp(a,3),Ui.dp(a,3),Ui.dp(a,3));setBackground(Ui.rounded(a,Ui.LINE,9));
            for(int i=0;i<2;i++){final int value=i+1;Button b=Ui.button(a,i==0?"한 번":"두 번",Ui.INK);buttons[i]=b;
                b.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View h,AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(h,n);n.setClassName("android.widget.RadioButton");n.setCheckable(true);n.setChecked(selected==value);}});
                b.setOnClickListener(v->{if(selected==value)return;if(save.test(value))setSelectedValue(value);else SettingsSheet.message(a,"저장하지 못했어요. 기존 선택을 유지해요.");});addView(b,new LinearLayout.LayoutParams(0,-2,1));}
            setSelectedValue(current);
        }
        public void setSelectedValue(int value){selected=value;for(int i=0;i<2;i++){boolean on=value==i+1;buttons[i].setSelected(on);buttons[i].setBackground(Ui.rounded(getContext(),on?Ui.PAPER:Ui.LINE,7));}}
    }
    public static class SettingSwitch extends Switch {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);float position;android.animation.ValueAnimator animator;
        public SettingSwitch(Context c){super(c);setShowText(false);setBackground(null);setSwitchMinWidth(0);}
        @Override public void setChecked(boolean checked){super.setChecked(checked);if(animator!=null)animator.cancel();float end=checked?1:0;if(!isLaidOut()||!android.animation.ValueAnimator.areAnimatorsEnabled()){position=end;invalidate();return;}animator=android.animation.ValueAnimator.ofFloat(position,end);animator.setDuration(180);animator.addUpdateListener(a->{position=(float)a.getAnimatedValue();invalidate();});animator.start();}
        @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=51*d,h=31*d,l=(getWidth()-w)/2,t=(getHeight()-h)/2;paint.setColor(isChecked()?GREEN:LINE);c.drawRoundRect(l,t,l+w,t+h,h/2,h/2,paint);float fraction=getLayoutDirection()==LAYOUT_DIRECTION_RTL?1-position:position;float x=l+h/2+(w-h)*fraction,y=t+h/2;paint.setColor(0x16000000);c.drawCircle(x,y+d,14*d,paint);paint.setColor(WHITE);c.drawCircle(x,y,13.5f*d,paint);}
    }
}
