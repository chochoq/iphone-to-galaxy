package com.chocho.listeningwidgetrender;
import android.app.*;import android.content.*;import android.content.res.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;
import com.chocho.airpodsglance.*;import com.chocho.airpodsglance.ListeningProtocol.Mode;import com.chocho.airpodsglance.ListeningWidgetProvider.Kind;
import java.io.*;

/** Local fixture package, no Bluetooth permissions. Parcel, native inflation, touch sizes, theme and configuration. */
public final class Checks extends Instrumentation {
    private int checks,frames;private Throwable failure;private Context app;private Canvas gallery;private Bitmap sheet;
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    private void yes(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
    @Override public void onStart(){app=getTargetContext();try{
        runOnMainSync(()->{try{render();options();}catch(Throwable e){failure=e;}});if(failure!=null)throw new RuntimeException(failure);
        Activity a=startActivitySync(new Intent(app,ListeningWidgetConfigActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runOnMainSync(()->{try{
            View decor=a.getWindow().getDecorView();
            yes(find(decor,"저장")!=null&&find(decor,"취소")!=null,"Save and Cancel available");
            View cancel=find(decor,"취소"),save=find(decor,"저장");
            yes(cancel.getWidth()>0&&save.getWidth()>0,"Actions have layout");
            int[] left=new int[2],right=new int[2];cancel.getLocationOnScreen(left);save.getLocationOnScreen(right);
            yes(right[0]-(left[0]+cancel.getWidth())>=dp(app,16),"Save/Cancel spacing >=16dp");
            ((ListeningOrderEditor)decor.findViewWithTag("listening-order-editor")).handle(Mode.ANC).performAccessibilityAction(ListeningOrderEditor.MOVE_AFTER,null);
            yes(new ListeningWidgetStore(app).options(0).encode().equals("2,4,3"),"Draft does not persist");
            find(a.getWindow().getDecorView(),"취소").performClick();
        }catch(Throwable e){failure=e;}});if(failure!=null)throw new RuntimeException(failure);
        Activity saved=startActivitySync(new Intent(app,ListeningWidgetConfigActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runOnMainSync(()->{try{
            ((ListeningOrderEditor)saved.getWindow().getDecorView().findViewWithTag("listening-order-editor")).handle(Mode.ANC).performAccessibilityAction(ListeningOrderEditor.MOVE_AFTER,null);
            find(saved.getWindow().getDecorView(),"저장").performClick();
            yes(new ListeningWidgetStore(app).options(0).encode().equals("4,2,3"),"Save persists chosen order");
            new ListeningWidgetStore(app).saveOptions(0,ListeningWidgetOptions.defaults());
        }catch(Throwable e){failure=e;}});if(failure!=null)throw new RuntimeException(failure);
        Bundle result=new Bundle();result.putString("stream","PASS ListeningWidgetNative "+checks+" checks; "+frames+" RemoteViews frames; no Bluetooth\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){Bundle r=new Bundle();r.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,r);}}
    private void options(){
        ListeningWidgetStore store=new ListeningWidgetStore(app);store.saveOptions(0,ListeningWidgetOptions.defaults());store.deleteOptions(25001);store.deleteOptions(25002);
        yes(store.options(25001).encode().equals("2,4,3"),"Inherit defaults");
        store.saveOptions(25001,new ListeningWidgetOptions(Mode.ANC,Mode.TRANSPARENCY));store.saveOptions(0,new ListeningWidgetOptions(Mode.ADAPTIVE,Mode.ANC));
        yes(store.options(25001).encode().equals("2,3"),"Per widget order isolated");yes(store.options(25002).encode().equals("4,2"),"Default affects inheritors");
        store.deleteOptions(25001);yes(store.options(25001).encode().equals("4,2"),"Delete only selected override");store.saveOptions(0,ListeningWidgetOptions.defaults());
        yes(!ListeningWidgetProvider.validRequest(app,new Intent()),"External/empty action invalid");
    }
    private void render()throws Exception{
        sheet=Bitmap.createBitmap(1440,1560,Bitmap.Config.ARGB_8888);gallery=new Canvas(sheet);gallery.drawColor(0xffe8e8ed);int slot=0;
        for(boolean dark:new boolean[]{false,true})for(float font:new float[]{.85f,1f,1.3f,2f})for(Mode mode:Mode.values())for(Kind kind:Kind.values()){
            Configuration cfg=new Configuration(app.getResources().getConfiguration());cfg.fontScale=font;cfg.uiMode=(cfg.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
            Context c=app.createConfigurationContext(cfg);
            for(int[] size:new int[][]{{kind.width,kind.height},{kind.cycle()?48:144,48}}){
                ListeningWidgetStore.Record record=new ListeningWidgetStore.Record(mode,mode==Mode.UNKNOWN?0:1700000000000L,"");
                RemoteViews original=ListeningWidgetRenderer.render(c,record,ListeningWidgetOptions.defaults(),kind,size[0],size[1]);
                Parcel parcel=Parcel.obtain();original.writeToParcel(parcel,0);yes(parcel.dataSize()<300000,"Bounded RemoteViews parcel");parcel.setDataPosition(0);RemoteViews remote=RemoteViews.CREATOR.createFromParcel(parcel);parcel.recycle();
                FrameLayout host=new FrameLayout(c);View v=remote.apply(c,host);host.addView(v);
                host.measure(View.MeasureSpec.makeMeasureSpec(dp(c,size[0]),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(c,size[1]),View.MeasureSpec.EXACTLY));host.layout(0,0,host.getMeasuredWidth(),host.getMeasuredHeight());
                int[] ids=kind.cycle()?new int[]{R.id.listening_single}:new int[]{R.id.listening_anc,R.id.listening_adaptive,R.id.listening_transparency};
                for(int id:ids){View action=host.findViewById(id);yes(action!=null,"Action exists");yes(action.getWidth()>=dp(c,48)-1&&action.getHeight()>=dp(c,48)-1,"48dp touch target "+kind+" "+size[0]+"x"+size[1]);yes(action.getContentDescription()!=null&&action.getContentDescription().toString().contains("확인"),"Honest accessible history");}
                int ink=dark?0xfff2f2f7:0xff1c1c1e,blue=dark?0xff64aaff:0xff007aff;
                if(kind.cycle())checkArtwork(host,R.id.listening_single,mode,kind==Kind.LOCK?Color.WHITE:ink,true,kind==Kind.LOCK,dark);
                else{
                    checkArtwork(host,R.id.listening_anc_icon,Mode.ANC,mode==Mode.ANC?blue:ink,false,false,dark);
                    checkArtwork(host,R.id.listening_adaptive_icon,Mode.ADAPTIVE,mode==Mode.ADAPTIVE?blue:ink,false,false,dark);
                    checkArtwork(host,R.id.listening_transparency_icon,Mode.TRANSPARENCY,mode==Mode.TRANSPARENCY?blue:ink,false,false,dark);
                }
                checkBounds(host);frames++;
                if(font==1f&&size[0]==kind.width&&(mode==Mode.UNKNOWN||mode==Mode.ANC||mode==Mode.ADAPTIVE)){
                    int col=slot%4,row=slot/4;float x=col*360+20,y=row*260+50;gallery.save();gallery.translate(x,y);Paint p=new Paint(3);p.setColor(dark?0xff242426:0xfff2f2f7);gallery.drawRoundRect(0,0,320,180,20,20,p);p.setColor(dark?Color.WHITE:0xff1c1c1e);p.setTextSize(18);gallery.drawText(kind.name()+" / "+mode.name(),12,24,p);
                    gallery.translate((320-size[0])/2f,50);gallery.scale(1/c.getResources().getDisplayMetrics().density,1/c.getResources().getDisplayMetrics().density);host.draw(gallery);gallery.restore();slot++;
                }
            }
        }
        try(FileOutputStream out=new FileOutputStream(new File(app.getExternalFilesDir(null),"widget-gallery.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();
    }
    private void checkArtwork(View root,int id,Mode mode,int color,boolean circle,boolean lock,boolean dark){
        Bitmap actual=((android.graphics.drawable.BitmapDrawable)((ImageView)root.findViewById(id)).getDrawable()).getBitmap();
        int size=circle?192:96;Bitmap expected=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(expected);c.scale(size/80f,size/80f);
        if(circle&&!lock){Paint p=new Paint(3);p.setColor(dark?0xff2c2c2e:0xfffafafa);c.drawCircle(40,40,39,p);}
        float side=circle?52:66;c.translate((80-side)/2,(80-side)/2);new ListeningArtwork().draw(c,mode,color,side,side);
        yes(actual.sameAs(expected),"Only centered mode artwork, no arrow/clock badge");expected.recycle();
    }
    private void checkBounds(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView){TextView t=(TextView)view;if(t.getLayout()!=null)for(int i=0;i<t.getLayout().getLineCount();i++)yes(t.getLayout().getLineWidth(i)<=t.getWidth()-t.getPaddingLeft()-t.getPaddingRight()+1,"Text not clipped");}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;yes(child.getLeft()>=-1&&child.getTop()>=-1&&child.getRight()<=group.getWidth()+1&&child.getBottom()<=group.getHeight()+1,"Child in bounds");checkBounds(child);}}
    }
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static View find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r;}return null;}
    private static View findDescription(View v,String text){if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=findDescription(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r;}return null;}
}
