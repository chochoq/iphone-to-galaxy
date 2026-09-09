package com.chocho.holidaysleep;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Switch;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Original Android components; no Apple fonts, symbols or artwork bundled. */
final class UiStyle {
    static final int BACKGROUND=0xfff2f2f7, SURFACE=0xffffffff, INK=0xff000000;
    static final int MUTED=0xff636366, BLUE=0xff007aff, TINT=0xffefeff0;
    static final int GREEN=0xff34c759, SEPARATOR=0xffe5e5ea;
    static final int WARNING=0xff875119, WARNING_BG=0xfffff3e3;

    static int dp(Context c,int value) { return Math.round(value*c.getResources().getDisplayMetrics().density); }

    static GradientDrawable rounded(Context c,int color,int radius) {
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(c,radius)); return shape;
    }

    static TextView text(Context c,String value,int size,int color,boolean medium) {
        TextView t=new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("tnum");
        t.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setIncludeFontPadding(false); t.setLineSpacing(dp(c,3),1f); return t;
    }

    static LinearLayout panel(Context c) {
        LinearLayout p=new LinearLayout(c); p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
        p.setBackground(rounded(c,SURFACE,16)); return p;
    }

    static Button button(Context c,String label,boolean primary) {
        Button b=new Button(c); b.setText(label); b.setTextSize(16); b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(dp(c,48));
        b.setStateListAnimator(null); b.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22007aff),
                rounded(c,primary?BLUE:TINT,12),null));
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},
                new int[]{0xff8e8e93,primary?SURFACE:BLUE}));
        return b;
    }

    /** Keep native Switch touch/keyboard/TalkBack semantics; own rendering avoids OEM thumb clipping. */
    static final class SettingSwitch extends Switch {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float position;
        private android.animation.ValueAnimator animation;
        SettingSwitch(Context c) {
            super(c);
            setMinHeight(dp(c,48));
            setMinimumWidth(dp(c,55));
            setSwitchMinWidth(dp(c,51));
            setShowText(false);
            setBackground(null);
        }
        @Override public void setChecked(boolean checked) {
            super.setChecked(checked);
            float destination=checked?1f:0f;
            if(animation!=null) animation.cancel();
            if(!isLaidOut()) { position=destination; invalidate(); return; }
            animation=android.animation.ValueAnimator.ofFloat(position,destination);
            animation.setDuration(180);
            animation.addUpdateListener(a -> { position=(float)a.getAnimatedValue(); invalidate(); });
            animation.start();
        }
        @Override protected void onDetachedFromWindow() {
            if(animation!=null) animation.cancel();
            super.onDetachedFromWindow();
        }
        @Override protected void onDraw(Canvas canvas) {
            float density=getResources().getDisplayMetrics().density;
            float w=51*density,h=31*density;
            float left=(getWidth()-w)/2,top=(getHeight()-h)/2;
            paint.setColor(isChecked()?GREEN:SEPARATOR);
            canvas.drawRoundRect(left,top,left+w,top+h,h/2,h/2,paint);
            float progress=getLayoutDirection()==LAYOUT_DIRECTION_RTL?1-position:position;
            float cx=left+h/2+(w-h)*progress,cy=top+h/2;
            paint.setColor(0x16000000);
            canvas.drawCircle(cx,cy+density,14*density,paint);
            paint.setColor(SURFACE);
            canvas.drawCircle(cx,cy,13.5f*density,paint);
        }
    }
}
