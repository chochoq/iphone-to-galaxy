package com.chocho.listeningwidgetrender;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.chocho.airpodsglance.*;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningWidgetProvider.Kind;
import com.chocho.ui.Ui;
import java.io.*;

/** Documentation examples from product Views, not a live mode-switch demonstration.
 * Runs in the permissionless fixture package; never reads personal app data. */
public final class DocGallery extends Instrumentation {
    private Context app;
    private Throwable failure;
    private int files;
    @Override public void onCreate(Bundle b) { super.onCreate(b); start(); }
    @Override public void onStart() {
        app=getTargetContext();
        try {
            if (!app.getPackageName().equals("com.chocho.listeningwidgetrender"))
                throw new AssertionError("Fixture package required");
            main(()->{ overview(false); overview(true); stacked(false); stacked(true); });
            settings("settings-three-modes.png",ListeningWidgetOptions.defaults());
            settings("settings-two-modes.png",new ListeningWidgetOptions(Mode.ANC,Mode.TRANSPARENCY));
            main(()->new ListeningWidgetStore(app).saveOptions(0,ListeningWidgetOptions.defaults()));
            Bundle out=new Bundle();
            out.putString("stream","PASS DocGallery: "+files+" native images; fixture states only; no Bluetooth\n");
            finish(Activity.RESULT_OK,out);
        } catch(Throwable e) {
            Bundle out=new Bundle();out.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,out);
        }
    }
    private interface Work { void run() throws Exception; }
    private void main(Work work) throws Exception {
        runOnMainSync(()->{try{work.run();}catch(Throwable e){failure=e;}});
        if(failure!=null)throw new RuntimeException(failure);
    }
    private Context theme(boolean dark) {
        Configuration cfg=new Configuration(app.getResources().getConfiguration());
        cfg.fontScale=1f;
        cfg.uiMode=(cfg.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
        return app.createConfigurationContext(cfg);
    }
    private FrameLayout widget(Context c,Kind kind) {
        RemoteViews remote=ListeningWidgetRenderer.render(c,
            new ListeningWidgetStore.Record(Mode.ANC,1700000000000L,""),
            ListeningWidgetOptions.defaults(),kind,kind.width,kind.height);
        FrameLayout host=new FrameLayout(c);host.addView(remote.apply(c,host));
        host.measure(exact(dp(c,kind.width)),exact(dp(c,kind.height)));
        host.layout(0,0,host.getMeasuredWidth(),host.getMeasuredHeight());
        if(kind==Kind.WIDE&&!"마지막 · 노캔".contentEquals(((TextView)host.findViewById(R.id.listening_history)).getText()))
            throw new AssertionError("Expected fixture state");
        return host;
    }
    private void overview(boolean dark) throws Exception {
        Context c=theme(dark);float density=c.getResources().getDisplayMetrics().density;
        Bitmap b=Bitmap.createBitmap(dp(c,724),dp(c,202),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);canvas.drawColor(dark?0xff18181a:0xfff2f2f7);
        canvas.scale(density,density);
        Kind[] kinds={Kind.WIDE,Kind.SMALL,Kind.SINGLE};
        int[] starts={20,300,540},widths={264,224,164};
        String[] titles={"가로 · 3×1","미니 · 2×1","한 칸 · 1×1"};
        String[] subtitles={"이름과 아이콘으로 선택","아이콘으로 선택","누를 때마다 다음 모드"};
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<kinds.length;i++) {
            float center=starts[i]+widths[i]/2f;
            p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(17);p.setColor(dark?Color.WHITE:0xff1c1c1e);
            canvas.drawText(titles[i],center,35,p);
            p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(12);p.setColor(dark?0xffaeaeb2:0xff636366);
            canvas.drawText(subtitles[i],center,57,p);
            FrameLayout view=widget(c,kinds[i]);
            canvas.save();canvas.translate(center-kinds[i].width/2f,87+(80-kinds[i].height)/2f);
            canvas.scale(1/density,1/density);view.draw(canvas);canvas.restore();
        }
        write(dark?"widgets-dark.png":"widgets-light.png",b);
    }
    private void settings(String name,ListeningWidgetOptions options) throws Exception {
        main(()->new ListeningWidgetStore(app).saveOptions(0,options));
        Activity a=startActivitySync(new Intent(app,ListeningWidgetConfigActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            waitForIdleSync();
            main(()->{
                ListeningOrderEditor root=a.getWindow().getDecorView().findViewWithTag("listening-order-editor");
                if(root==null)throw new AssertionError("Actual product editor required");
                if(!root.draft().options().encode().equals(options.encode()))throw new AssertionError("Wrong example order");
                assertChoice(root,Mode.ANC,true);
                assertChoice(root,Mode.ADAPTIVE,options.encode().contains("4"));
                assertChoice(root,Mode.TRANSPARENCY,true);
                int width=dp(a,390);
                root.setPadding(0,0,0,0);
                root.measure(exact(width),exact(dp(a,780)));
                root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
                ScrollView scroll=root.findViewById(0x1008001);
                int height=root.getChildAt(0).getHeight()+scroll.getChildAt(0).getHeight();
                root.measure(exact(width),exact(height));root.layout(0,0,width,height);
                Bitmap b=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(b);canvas.drawColor(Ui.BG);root.draw(canvas);
                write(name,b);
                // Save is not clicked: fixture preferences only, no product broadcast or command.
                a.finish();
            });
        } finally { if(!a.isFinishing())main(a::finish); }
    }
    private void stacked(boolean dark) throws Exception {
        Context c=theme(dark);float density=c.getResources().getDisplayMetrics().density;
        Bitmap b=Bitmap.createBitmap(dp(c,342),dp(c,486),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);canvas.drawColor(dark?0xff18181a:0xfff2f2f7);canvas.scale(density,density);
        Kind[] kinds={Kind.WIDE,Kind.SMALL,Kind.SINGLE};
        String[] titles={"가로 · 3×1","미니 · 2×1","한 칸 · 1×1"};
        String[] subtitles={"이름과 아이콘으로 선택","아이콘으로 선택","누를 때마다 다음 모드"};
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<kinds.length;i++) {
            float top=i*160;
            p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(17);p.setColor(dark?Color.WHITE:0xff1c1c1e);
            canvas.drawText(titles[i],171,top+27,p);
            p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(12);p.setColor(dark?0xffaeaeb2:0xff636366);
            canvas.drawText(subtitles[i],171,top+46,p);
            FrameLayout view=widget(c,kinds[i]);canvas.save();canvas.translate((342-kinds[i].width)/2f,top+62+(80-kinds[i].height)/2f);
            canvas.scale(1/density,1/density);view.draw(canvas);canvas.restore();
        }
        write(dark?"widgets-dark-stacked.png":"widgets-light-stacked.png",b);
    }
    private void assertChoice(ListeningOrderEditor editor,Mode mode,boolean checked) {
        android.view.accessibility.AccessibilityNodeInfo node=editor.selection(mode).createAccessibilityNodeInfo();
        boolean valid=node.isCheckable()&&node.isChecked()==checked;node.recycle();
        if(!valid)throw new AssertionError("Wrong selection: "+mode);
    }
    private static View findDescription(View v,String text) {
        if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findDescription(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    private static View findText(View v,String text) {
        if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=findText(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    private void write(String name,Bitmap b) throws Exception {
        File dir=new File(app.getExternalFilesDir(null),"readme-images");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Image directory");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){if(!b.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG encode");}
        b.recycle();files++;
    }
    private static int exact(int n){return View.MeasureSpec.makeMeasureSpec(n,View.MeasureSpec.EXACTLY);}
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
