package com.chocho.listeninguitest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import com.chocho.ui.Ui;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/** Renders the actual private production icon Views, without a controller or Bluetooth permission. */
final class GlyphChecks {
    interface Check {void test(boolean value,String message);}
    static void collect(View view,ArrayList<ViewGroup> tiles){
        String desc=String.valueOf(view.getContentDescription());
        if(view instanceof ViewGroup&&(desc.startsWith("노이즈 캔슬링")||desc.startsWith("적응형")||desc.startsWith("주변음 허용")))
            tiles.add((ViewGroup)view);
        else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),tiles);}
    }
    static int alpha(Bitmap b,int x,int y){return Color.alpha(b.getPixel(x,y));}
    static boolean bodySame(Bitmap a,Bitmap b){
        for(int y=(int)Math.ceil(17*a.getWidth()/26f);y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)
            if(alpha(a,x,y)!=alpha(b,x,y))return false;
        return true;
    }
    static int components(Bitmap b){
        int size=b.getWidth(),count=0;boolean[] seen=new boolean[size*size];int[] queue=new int[seen.length];
        for(int start=0;start<seen.length;start++){
            if(seen[start]||alpha(b,start%size,start/size)<100)continue;
            count++;int front=0,end=0;queue[end++]=start;seen[start]=true;
            while(front<end){int p=queue[front++],x=p%size,y=p/size;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int xx=x+dx,yy=y+dy;if(xx<0||xx>=size||yy<0||yy>=size)continue;
                    int n=yy*size+xx;if(!seen[n]&&alpha(b,xx,yy)>=100){seen[n]=true;queue[end++]=n;}
                }
            }
        }return count;
    }
    static void run(Context context,View root,Check check){
        ArrayList<ViewGroup> tiles=new ArrayList<>();collect(root,tiles);check.test(tiles.size()==3,"three actual glyph views");
        Bitmap sheet=Bitmap.createBitmap(640,450,Bitmap.Config.ARGB_8888);Canvas page=new Canvas(sheet);page.drawColor(Ui.BG);
        Paint caption=new Paint(3);caption.setColor(Ui.INK);caption.setTextSize(18);
        page.drawText("Android Canvas: 26px / 68px / 156px",24,28,caption);
        ArrayList<Runnable> assertions=new ArrayList<>();StringBuilder metrics=new StringBuilder();
        int[] sizes={26,68,156},tops={60,140,240};
        for(int row=0;row<sizes.length;row++){
            int size=sizes[row];Bitmap[] rendered=new Bitmap[3];
            for(int col=0;col<3;col++){
                ViewGroup tile=tiles.get(col);View icon=tile.getChildAt(0);
                Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);rendered[col]=b;Canvas canvas=new Canvas(b);
                canvas.scale(size/(float)icon.getWidth(),size/(float)icon.getHeight());icon.draw(canvas);
                page.drawBitmap(b,120+col*200-size/2f,tops[row],null);
                int ink=0;boolean borderClear=true,colorCorrect=true;int expected=tile.isSelected()?Ui.BLUE:Ui.INK;
                for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                    int pixel=b.getPixel(x,y),a=Color.alpha(pixel);if(a>100)ink++;
                    if((x==0||y==0||x==size-1||y==size-1)&&a>8)borderClear=false;
                    if(a==255&&(pixel&0xffffff)!=(expected&0xffffff))colorCorrect=false;
                }
                final boolean inside=borderClear,tint=colorCorrect,hasInk=ink>size*size*0.12&&ink<size*size*0.55;
                final int componentCount=components(b),expectedComponents=new int[]{3,4,13}[col];
                String name="glyph "+col+" at "+size;
                metrics.append(name).append(" components=").append(componentCount).append(" ink=").append(ink).append('\n');
                assertions.add(()->check.test(inside,name+" no clipping"));
                assertions.add(()->check.test(tint,name+" exact opaque tint"));
                assertions.add(()->check.test(hasInk,name+" visible silhouette without solid block"));
                assertions.add(()->check.test(alpha(b,size/2,(int)(10.2f*size/26))>240,name+" head present"));
                assertions.add(()->check.test(alpha(b,size/2,(int)(20f*size/26))>240,name+" shoulders present"));
                assertions.add(()->check.test(componentCount==expectedComponents,name+" separate head/body/decorations"));
            }
            final Bitmap first=rendered[0],second=rendered[1],third=rendered[2];
            assertions.add(()->check.test(bodySame(first,second)&&bodySame(first,third),"same body silhouette at "+size));
            int[] a=new int[size*size],b=new int[a.length],c=new int[a.length];
            for(int i=0;i<a.length;i++){a[i]=alpha(first,i%size,i/size);b[i]=alpha(second,i%size,i/size);c[i]=alpha(third,i%size,i/size);}
            assertions.add(()->check.test(!Arrays.equals(a,b)&&!Arrays.equals(a,c)&&!Arrays.equals(b,c),"three distinct silhouettes at "+size));
        }
        String[] names={"노이즈 캔슬링","적응형","주변음 허용"};caption.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<3;i++)page.drawText(names[i],120+i*200,430,caption);
        try{
            File dir=context.getExternalFilesDir(null);if(dir==null)throw new IllegalStateException("No test artifact directory");
            try(FileOutputStream out=new FileOutputStream(new File(dir,"glyph-review.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}
            try(FileOutputStream out=new FileOutputStream(new File(dir,"glyph-metrics.txt"))){out.write(metrics.toString().getBytes("UTF-8"));}
        }catch(Exception error){throw new AssertionError(error);}
        for(Runnable assertion:assertions)assertion.run();
    }
}
