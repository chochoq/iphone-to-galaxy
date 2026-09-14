package com.chocho.airpodsglance;

import android.graphics.*;

/** Original code-drawn glyphs, not Apple symbols or copied product assets. */
public final class WidgetArtwork {
    private WidgetArtwork(){}
    public static Bitmap ring(int pixels,int part,Integer percent,boolean charging,boolean dark,boolean fresh,int threshold){
        Bitmap bitmap=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bitmap);c.scale(pixels/64f,pixels/64f);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStrokeCap(Paint.Cap.ROUND);
        int ink=dark?0xfff2f2f7:0xff1c1c1e,track=dark?0xff48484a:0xffe5e5ea;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4.8f);p.setColor(track);c.drawCircle(32,32,27,p);
        if(percent!=null&&percent>0){p.setColor(!fresh?(dark?0xff98989d:0xff8e8e93):percent<=threshold?0xffff453a:0xff34c759);c.drawArc(new RectF(5,5,59,59),-90,percent*3.6f,false,p);}
        p.setStyle(Paint.Style.FILL);p.setColor(percent==null?0xff8e8e93:ink);
        c.save();c.scale(1.15f,1.15f,32,32);
        if(part==2){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);c.drawRoundRect(new RectF(17,22,47,43),6,6,p);
            c.drawLine(18,29,46,29,p);p.setStyle(Paint.Style.FILL);c.drawCircle(32,36,1.2f,p);
        }else{
            c.save();if(part==1)c.scale(-1,1,32,32);
            c.drawRoundRect(new RectF(31,28,37,47),3,3,p);c.drawOval(new RectF(21,18,39,34),p);
            p.setColor(dark?0xff1c1c1e:0xfff2f2f7);c.drawOval(new RectF(23,22,27,29),p);c.restore();
        }
        c.restore();
        if(charging){
            p.setColor(dark?0xff1c1c1e:0xfff2f2f7);c.drawCircle(51,51,9,p);p.setColor(0xff34c759);
            Path bolt=new Path();bolt.moveTo(53,43);bolt.lineTo(46,52);bolt.lineTo(51,52);bolt.lineTo(49,59);bolt.lineTo(57,49);bolt.lineTo(52,49);bolt.close();c.drawPath(bolt,p);
        }
        return bitmap;
    }
    public static Bitmap refresh(int pixels,boolean dark){
        Bitmap b=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(pixels/24f,pixels/24f);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(dark?0xff0a84ff:0xff0066cc);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(new RectF(5,5,19,19),-70,295,false,p);Path arrow=new Path();arrow.moveTo(14,3);arrow.lineTo(15,7);arrow.lineTo(11,7);c.drawPath(arrow,p);return b;
    }
}
