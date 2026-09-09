package com.chocho.taptotop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.function.IntPredicate;

/** Project-local native widgets. No Apple fonts/assets; standard Android accessibility semantics. */
final class SettingsUi {
    static final int BG=0xfff2f2f7, WHITE=0xffffffff, INK=0xff000000, MUTED=0xff636366;
    static final int BLUE=0xff007aff, GREEN=0xff34c759, LINE=0xffe5e5ea;
    final Activity activity;
    final LinearLayout root;
    final ScrollView scroll;
    SettingsUi(Activity a,String title,String subtitle) {
        activity=a;
        a.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        scroll=new ScrollView(a); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),dp(16)+insets.getSystemWindowInsetTop(),
                    dp(20)+insets.getSystemWindowInsetRight(),dp(28)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        FrameLayout frame=new FrameLayout(a);
        root=new LinearLayout(a) {
            @Override protected void onMeasure(int w,int h) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(w),dp(560)),
                        MeasureSpec.EXACTLY),h);
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        frame.addView(root,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame);
        TextView heading=text(title,30,INK,true); heading.setPadding(0,dp(12),0,0);
        root.addView(heading,params(8));
        root.addView(text(subtitle,14,MUTED,false),params(24));
    }
    void install() { activity.setContentView(scroll); }
    int dp(int n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }
    LinearLayout.LayoutParams params(int margin) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(margin); return p;
    }
    static GradientDrawable rounded(Context c,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color);
        d.setCornerRadius(radius*c.getResources().getDisplayMetrics().density); return d;
    }
    TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(activity); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",0));
        t.setIncludeFontPadding(false); t.setLineSpacing(dp(3),1); return t;
    }
    LinearLayout group(String title) {
        if(title!=null) {
            TextView label=text(title,13,MUTED,false); label.setPadding(dp(16),0,0,0);
            root.addView(label,params(8));
        }
        LinearLayout p=new LinearLayout(activity); p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16),dp(8),dp(16),dp(8)); p.setBackground(rounded(activity,WHITE,16));
        root.addView(p,params(20)); return p;
    }
    void separator(LinearLayout group) {
        View v=new View(activity); v.setBackgroundColor(LINE);
        group.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    TextView note(LinearLayout group,String text) {
        TextView t=text(text,13,MUTED,false); t.setPadding(0,dp(8),0,dp(8));
        group.addView(t); return t;
    }
    Button row(LinearLayout group,String label,Runnable action) {
        Button b=new Button(activity); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setMinHeight(dp(52));
        b.setPadding(0,dp(10),0,dp(10)); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setTypeface(Typeface.create("sans-serif",0)); b.setStateListAnimator(null);
        b.setTextColor(BLUE);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x11007aff),rounded(activity,WHITE,8),null));
        b.setOnClickListener(v->action.run()); group.addView(b,new LinearLayout.LayoutParams(-1,-2)); return b;
    }
    TextView setting(LinearLayout group,String label,Runnable action) {
        LinearLayout r=new LinearLayout(activity);
        boolean narrow=activity.getResources().getConfiguration().screenWidthDp<360
                || activity.getResources().getConfiguration().fontScale>1.25;
        r.setOrientation(narrow?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL); r.setMinimumHeight(dp(56));
        r.setPadding(0,dp(10),0,dp(10));
        TextView name=text(label,16,INK,false);
        r.addView(name,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
        TextView value=text("",15,BLUE,false);
        value.setPadding(narrow?0:dp(12),narrow?dp(6):0,0,0);
        r.addView(value,new LinearLayout.LayoutParams(-2,-2));
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x11007aff),rounded(activity,WHITE,8),null));
        r.setFocusable(true); r.setClickable(true);
        r.setOnClickListener(v->action.run());
        group.addView(r,new LinearLayout.LayoutParams(-1,-2));
        value.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                r.setContentDescription(label+", "+s+", 변경");
            }
            public void afterTextChanged(android.text.Editable e) {}
        });
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        value.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return value;
    }
    Switch toggle(LinearLayout group,String label,boolean checked,java.util.function.Consumer<Boolean> change) {
        LinearLayout r=new LinearLayout(activity); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView t=text(label,16,INK,false); t.setPadding(0,dp(8),dp(12),dp(8));
        r.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new SettingSwitch(activity); s.setContentDescription(label); s.setChecked(checked);
        s.setOnCheckedChangeListener((v,b)->change.accept(b));
        r.addView(s,new LinearLayout.LayoutParams(dp(55),dp(48)));
        group.addView(r); return s;
    }
    void number(String title,String explanation,int current,int min,int max,IntPredicate save) {
        LinearLayout box=new LinearLayout(activity); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24),dp(12),dp(24),dp(8));
        box.addView(text(explanation,14,MUTED,false));
        NumberPicker picker=new NumberPicker(activity); picker.setMinValue(min); picker.setMaxValue(max);
        picker.setValue(current); picker.setWrapSelectorWheel(false);
        box.addView(picker,new LinearLayout.LayoutParams(-1,dp(180)));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setView(box)
                .setNegativeButton("취소",null).setPositiveButton("저장",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            picker.clearFocus();
            if(save.test(picker.getValue())) dialog.dismiss();
            else Toast.makeText(activity,"저장하지 못했어요. 다시 시도해 주세요.",Toast.LENGTH_LONG).show();
        }));
        dialog.show();
    }
    void confirmReset(Runnable reset) {
        new AlertDialog.Builder(activity).setTitle("조절값을 기본값으로 되돌릴까요?")
                .setMessage("기기 선택과 권한, 켜짐·꺼짐 상태는 그대로 유지해요.")
                .setNegativeButton("취소",null).setPositiveButton("되돌리기",(d,w)->reset.run()).show();
    }
    // Native Switch semantics; own painting avoids OEM thumb/track clipping.
    static final class SettingSwitch extends Switch {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float position;
        android.animation.ValueAnimator animator;
        SettingSwitch(Context c) { super(c); setShowText(false); setBackground(null); setSwitchMinWidth(0); }
        @Override public void setChecked(boolean checked) {
            super.setChecked(checked);
            if(animator!=null) animator.cancel();
            float end=checked?1:0;
            if(!isLaidOut()) {position=end; invalidate(); return;}
            animator=android.animation.ValueAnimator.ofFloat(position,end); animator.setDuration(180);
            animator.addUpdateListener(a->{position=(float)a.getAnimatedValue(); invalidate();}); animator.start();
        }
        @Override protected void onDetachedFromWindow() {
            if(animator!=null) animator.cancel(); super.onDetachedFromWindow();
        }
        @Override protected void onDraw(Canvas c) {
            float d=getResources().getDisplayMetrics().density,w=51*d,h=31*d;
            float l=(getWidth()-w)/2,t=(getHeight()-h)/2;
            p.setColor(isChecked()?GREEN:LINE); c.drawRoundRect(l,t,l+w,t+h,h/2,h/2,p);
            float progress=getLayoutDirection()==LAYOUT_DIRECTION_RTL?1-position:position;
            float x=l+h/2+(w-h)*progress,y=t+h/2;
            p.setColor(0x16000000); c.drawCircle(x,y+d,14*d,p);
            p.setColor(WHITE); c.drawCircle(x,y,13.5f*d,p);
        }
    }
}
