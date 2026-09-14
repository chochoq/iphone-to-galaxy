package com.chocho.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** A fixed trailing slot; never align navigation/selection with spaces in a label. */
public final class TrailingMark extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean check;
    public TrailingMark(Context context,boolean check){
        this.check=check;setBounds(0,0,Ui.dp(context,20),Ui.dp(context,22));
        paint.setColor(check?Ui.BLUE:0xffaeaeb2);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(context,check?2:1.7f));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();float x=b.left,y=b.top,w=b.width(),h=b.height();Path p=new Path();
        if(check){p.moveTo(x+w*.18f,y+h*.52f);p.lineTo(x+w*.42f,y+h*.72f);p.lineTo(x+w*.84f,y+h*.28f);}
        else{p.moveTo(x+w*.40f,y+h*.28f);p.lineTo(x+w*.64f,y+h*.50f);p.lineTo(x+w*.40f,y+h*.72f);}
        canvas.drawPath(p,paint);
    }
    @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
