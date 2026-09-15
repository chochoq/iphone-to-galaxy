package com.chocho.airpodsglance;
import android.graphics.Canvas;import android.graphics.Paint;import android.graphics.Path;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Shared original geometry from D-023. View and widget exports use the same painter. */
public final class ListeningArtwork {
    private final Paint paint=new Paint(3);
    private final Path shoulders=new Path(),largeSparkle=new Path(),smallSparkle=new Path();
    public ListeningArtwork(){
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        shoulders.moveTo(4.2f,22.2f);shoulders.cubicTo(4.2f,18.4f,8.3f,16.4f,13,16.4f);
        shoulders.cubicTo(17.7f,16.4f,21.8f,18.4f,21.8f,22.2f);
        shoulders.quadTo(21.8f,23.1f,20.9f,23.1f);shoulders.lineTo(5.1f,23.1f);
        shoulders.quadTo(4.2f,23.1f,4.2f,22.2f);shoulders.close();
        sparkle(largeSparkle,21,5.7f,3.3f);sparkle(smallSparkle,16.3f,2.7f,1.25f);
    }
    private static void sparkle(Path path,float x,float y,float radius){
        float waist=radius*.14f;path.moveTo(x,y-radius);path.quadTo(x+waist,y-waist,x+radius,y);
        path.quadTo(x+waist,y+waist,x,y+radius);path.quadTo(x-waist,y+waist,x-radius,y);
        path.quadTo(x-waist,y-waist,x,y-radius);path.close();
    }
    public void draw(Canvas canvas,Mode mode,int color,float width,float height){
        canvas.save();canvas.scale(width/26f,height/26f);paint.setColor(color);paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(13,10.2f,3.85f,paint);canvas.drawPath(shoulders,paint);
        if(mode==Mode.ANC){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.75f);canvas.drawArc(4.8f,2.1f,21.2f,18.5f,150,240,false,paint);}
        else if(mode==Mode.TRANSPARENCY){for(int i=0;i<11;i++){double angle=Math.toRadians(150+i*24);canvas.drawCircle(13+8.2f*(float)Math.cos(angle),10.3f+8.2f*(float)Math.sin(angle),.95f,paint);}}
        else if(mode==Mode.ADAPTIVE){canvas.drawPath(largeSparkle,paint);canvas.drawPath(smallSparkle,paint);}
        canvas.restore();
    }
}
