package com.chocho.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.SeekBar;

/** Custom pixels, platform touch/keyboard/range semantics. No hidden resources or APIs. */
public final class SettingsSlider extends SeekBar {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public SettingsSlider(Context context){
        super(context);setThumb(null);setProgressDrawable(null);setBackground(null);
        setPadding(Ui.dp(context,16),0,Ui.dp(context,16),0);setMinimumHeight(Ui.dp(context,48));
    }
    @Override protected synchronized void onDraw(Canvas canvas){
        float d=getResources().getDisplayMetrics().density,left=getPaddingLeft(),right=getWidth()-getPaddingRight(),y=getHeight()/2f;
        float progress=getMax()==0?0:getProgress()/(float)getMax();
        boolean rtl=getLayoutDirection()==LAYOUT_DIRECTION_RTL;
        float x=left+(right-left)*(rtl?1-progress:progress);
        paint.setColor(Ui.LINE);canvas.drawRoundRect(left,y-2*d,right,y+2*d,2*d,2*d,paint);
        paint.setColor(isEnabled()?Ui.BLUE:0xffaeaeb2);canvas.drawRoundRect(rtl?x:left,y-2*d,rtl?right:x,y+2*d,2*d,2*d,paint);
        if(isFocused()||isPressed()){paint.setColor(0x180066cc);canvas.drawCircle(x,y,20*d,paint);}
        paint.setColor(0x0a000000);canvas.drawCircle(x,y+2*d,15*d,paint);
        paint.setColor(0x12000000);canvas.drawCircle(x,y+d,14*d,paint);
        paint.setColor(isEnabled()?Ui.PAPER:0xfff2f2f7);canvas.drawCircle(x,y,13*d,paint);
    }
}
