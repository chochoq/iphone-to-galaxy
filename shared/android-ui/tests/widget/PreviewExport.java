package com.chocho.widgettest;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import com.chocho.airpodsglance.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/** Rasterizes production RemoteViews, not a mockup or a screen crop. Isolated, no-permission test build. */
public final class PreviewExport extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        final StringBuilder result=new StringBuilder();final Throwable[] error={null};
        runOnMainSync(()->{try{
            for(boolean dark:new boolean[]{false,true})for(WidgetVariant variant:WidgetVariant.values()){
                Configuration config=new Configuration(getTargetContext().getResources().getConfiguration());
                config.fontScale=1;config.setLocale(Locale.KOREAN);
                config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
                Context c=getTargetContext().createConfigurationContext(config);long now=MainActivity.NOW;
                AirPodsSnapshot example=new AirPodsSnapshot("AirPods",true,AirPodsSnapshot.IdentityConfidence.EXACT,null,new BatteryComponent(80,false),new BatteryComponent(65,false),new BatteryComponent(50,false),now,AirPodsSnapshot.Source.AAP_EXACT_PERCENT);
                RemoteViews rv=WidgetRenderer.render(c,example,now,variant.previewWidth,variant.previewHeight,1,20,20,variant);
                rv.setTextViewText(R.id.widget_status,"미리보기");
                View view=rv.apply(c,new FrameLayout(c));float density=c.getResources().getDisplayMetrics().density;
                int w=Math.round(variant.previewWidth*density),h=Math.round(variant.previewHeight*density);
                view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
                Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));
                File file=new File(getTargetContext().getFilesDir(),"preview-"+variant.name().toLowerCase(Locale.ROOT)+(dark?"-dark":"")+".png");
                try(FileOutputStream stream=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IllegalStateException("PNG export failed");}
                result.append(file.getName()).append(' ').append(w).append('x').append(h).append('\n');bitmap.recycle();
            }
        }catch(Throwable t){error[0]=t;}});
        Bundle b=new Bundle();b.putString("stream",error[0]==null?result.toString():error[0].toString());finish(error[0]==null?0:1,b);
    }
}
