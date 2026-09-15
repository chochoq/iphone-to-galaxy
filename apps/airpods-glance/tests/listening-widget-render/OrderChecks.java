package com.chocho.listeningwidgetrender;

import android.app.*;import android.content.*;import android.content.res.*;import android.graphics.*;import android.os.*;import android.view.*;import android.view.accessibility.*;import android.widget.*;
import com.chocho.airpodsglance.*;import com.chocho.airpodsglance.ListeningProtocol.Mode;import java.io.*;

/** Native layout, actual touch dispatch, accessibility, persistence and Activity recreation. No Bluetooth. */
public final class OrderChecks extends Instrumentation {
    private Context app;private Throwable failure;private int checks,frames;private volatile Activity resumed;
    private interface Work{void run()throws Exception;}
    private void main(Work work)throws Exception{runOnMainSync(()->{try{work.run();}catch(Throwable e){failure=e;}});if(failure!=null)throw new RuntimeException(failure);}
    private void yes(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void callActivityOnResume(Activity a){super.callActivityOnResume(a);resumed=a;}
    @Override public void onStart(){app=getTargetContext();try{
        main(this::layouts);activityTests();
        Bundle b=new Bundle();b.putString("stream","PASS OrderChecks "+checks+" checks; "+frames+" native layouts; real touch and recreation; no Bluetooth\n");finish(Activity.RESULT_OK,b);
    }catch(Throwable e){Bundle b=new Bundle();b.putString("stream","FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,b);}}
    private void layouts()throws Exception{
        int[] confirmations={0};ListeningOrderDraft fresh=new ListeningOrderDraft(ListeningWidgetOptions.defaults());
        ListeningOrderEditor setup=new ListeningOrderEditor(app,fresh,true,true,()->{},()->confirmations[0]++);layout(setup,390,700);
        yes(find(setup,"저장").isEnabled(),"launcher configuration can confirm unchanged defaults");find(setup,"저장").performClick();
        yes(confirmations[0]==1&&!fresh.changed(),"default confirmation does not mutate draft");
        for(int width:new int[]{320,390,600})for(float font:new float[]{1f,1.3f,2f})for(int height:new int[]{280,700})for(boolean two:new boolean[]{false,true}){
            Configuration cfg=new Configuration(app.getResources().getConfiguration());cfg.fontScale=font;cfg.screenWidthDp=width;cfg.screenHeightDp=height;
            Context c=app.createConfigurationContext(cfg);ListeningWidgetOptions initial=two?new ListeningWidgetOptions(Mode.ANC,Mode.TRANSPARENCY):ListeningWidgetOptions.defaults();
            ListeningOrderDraft d=new ListeningOrderDraft(initial);ListeningOrderEditor v=new ListeningOrderEditor(c,d,false,()->{},()->{});layout(v,width,height);
            yes(count(v,"전환 순서")==1,"one compact title");yes(count(v,"노이즈 캔슬링")==1&&count(v,"적응형")==1&&count(v,"주변음 허용")==1,"each mode once");
            yes(countType(v,Switch.class)==0,"no duplicate switch list");yes(countType(v,Button.class)==2,"only cancel and save buttons");
            yes(!find(v,"저장").isEnabled(),"clean save disabled");
            for(Mode m:new Mode[]{Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY}){
                View selection=v.selection(m);yes(selection.getHeight()>=dp(c,48),"selection touch height");
                AccessibilityNodeInfo n=selection.createAccessibilityNodeInfo();yes(n.isCheckable()&&n.isChecked()==d.contains(m),"accessible check state");n.recycle();
                View handle=v.handle(m);if(d.contains(m)){yes(handle.getWidth()>=dp(c,48)&&handle.getHeight()>=dp(c,48),"handle touch area");}
                else yes(handle.getVisibility()!=View.VISIBLE,"excluded handle hidden");
            }
            bounds(v);frames++;
            if(height==700){
                Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));write("order-"+width+"-"+font+"-"+(two?"two":"three")+".png",b);
                localGesture(v,Mode.ANC,Mode.TRANSPARENCY,MotionEvent.ACTION_CANCEL);yes(d.options().encode().equals(initial.encode()),"layout drag cancel");
                localGesture(v,Mode.ANC,Mode.TRANSPARENCY,MotionEvent.ACTION_POINTER_DOWN);yes(d.options().encode().equals(initial.encode()),"multi-pointer cancels drag");
                localGesture(v,Mode.ANC,Mode.TRANSPARENCY,MotionEvent.ACTION_UP);yes(d.position(Mode.ANC)==d.size()-1,"variable-height drag to end");layout(v,width,height);
                localGesture(v,Mode.ANC,d.options().modes()[0],MotionEvent.ACTION_UP);yes(d.options().encode().equals(initial.encode()),"variable-height drag to start");layout(v,width,height);
            }
            if(two){v.selection(Mode.ANC).performClick();yes(d.options().encode().equals(initial.encode()),"minimum two no change");yes(!v.errorText().isEmpty(),"inline minimum message");}
            else {v.selection(Mode.ADAPTIVE).performClick();yes(d.options().encode().equals("2,3"),"single-list exclude");yes(find(v,"저장").isEnabled(),"changed save enabled");}
            v.handle(Mode.ANC).performAccessibilityAction(ListeningOrderEditor.MOVE_AFTER,null);yes(d.position(Mode.ANC)==1,"accessible move");
            v.handle(Mode.ANC).dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_UP));yes(d.position(Mode.ANC)==0,"keyboard move");
            layout(v,width,height);bounds(v);
        }
    }
    private void activityTests()throws Exception{
        main(()->new ListeningWidgetStore(app).saveOptions(0,ListeningWidgetOptions.defaults()));
        Activity a=open();ListeningOrderEditor v=editor(a);
        gesture(v,Mode.ANC,Mode.TRANSPARENCY,false);
        final ListeningOrderEditor first=v;
        main(()->{yes(first.draft().options().encode().equals("4,3,2"),"actual touch drop reorders: "+first.draft().options().encode());yes(new ListeningWidgetStore(app).options(0).encode().equals("2,4,3"),"drop does not persist");find(first,"취소").performClick();});
        a=open();v=editor(a);final ListeningOrderEditor canceled=v;
        gesture(v,Mode.ANC,Mode.TRANSPARENCY,true);
        main(()->yes(canceled.draft().options().encode().equals("2,4,3"),"touch cancel retains draft"));
        final Activity original=a;final ListeningOrderEditor before=v;
        main(()->{before.selection(Mode.ADAPTIVE).performClick();original.recreate();});
        for(int i=0;i<100&&resumed==original;i++){Thread.sleep(20);waitForIdleSync();}
        final Activity recreated=resumed;yes(recreated!=original,"actual Activity recreated");waitForIdleSync();
        main(()->{ListeningOrderEditor restored=editor(recreated);yes(restored.draft().options().encode().equals("2,3"),"recreation restores draft");yes(restored.draft().original().equals("2,4,3"),"recreation restores original");find(restored,"저장").performClick();yes(new ListeningWidgetStore(app).options(0).encode().equals("2,3"),"save persists");});
        Activity saved=open();main(()->{yes(editor(saved).draft().options().encode().equals("2,3"),"reopen saved order");saved.finish();new ListeningWidgetStore(app).saveOptions(0,ListeningWidgetOptions.defaults());});
        Activity conflict=open();main(()->{ListeningOrderEditor e=editor(conflict);e.selection(Mode.ADAPTIVE).performClick();new ListeningWidgetStore(app).saveOptions(0,new ListeningWidgetOptions(Mode.TRANSPARENCY,Mode.ADAPTIVE));find(e,"저장").performClick();yes(!conflict.isFinishing()&&!e.errorText().isEmpty(),"conflict not silently closed");yes(new ListeningWidgetStore(app).options(0).encode().equals("3,4"),"external setting preserved");conflict.finish();new ListeningWidgetStore(app).saveOptions(0,ListeningWidgetOptions.defaults());});
    }
    private void gesture(ListeningOrderEditor v,Mode from,Mode to,boolean cancel)throws Exception{
        float[] coords=new float[3];String initial=v.draft().options().encode();
        main(()->{View handle=v.handle(from),target=v.handle(to);int[] p=new int[2],q=new int[2];handle.getLocationOnScreen(p);target.getLocationOnScreen(q);coords[0]=p[0]+handle.getWidth()/2f;coords[1]=p[1]+handle.getHeight()/2f;coords[2]=q[1]+target.getHeight()/2f+dp(app,8);});
        long down=SystemClock.uptimeMillis();send(down,MotionEvent.ACTION_DOWN,coords[0],coords[1]);
        for(int i=1;i<=12;i++){send(down,MotionEvent.ACTION_MOVE,coords[0],coords[1]+(coords[2]-coords[1])*i/12f);}
        main(()->yes(v.draft().options().encode().equals(initial),"drag intermediate not committed"));
        send(down,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,coords[0],coords[2]);waitForIdleSync();
    }
    private void send(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);sendPointerSync(e);e.recycle();}
    private void localGesture(ListeningOrderEditor v,Mode from,Mode to,int end){
        Rect start=new Rect(),finish=new Rect();View source=v.handle(from),target=v.handle(to);source.getDrawingRect(start);target.getDrawingRect(finish);v.offsetDescendantRectToMyCoords(source,start);v.offsetDescendantRectToMyCoords(target,finish);
        float x=start.exactCenterX(),y=start.exactCenterY(),targetY=finish.exactCenterY()+(finish.centerY()>start.centerY()?dp(v.getContext(),8):-dp(v.getContext(),8));
        long down=SystemClock.uptimeMillis();String before=v.draft().options().encode();
        MotionEvent e=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,y,0);v.dispatchTouchEvent(e);e.recycle();
        for(int i=1;i<=12;i++){e=MotionEvent.obtain(down,down+i*16,MotionEvent.ACTION_MOVE,x,y+(targetY-y)*i/12f,0);v.dispatchTouchEvent(e);e.recycle();}
        yes(v.draft().options().encode().equals(before),"local drag is draft-preview only until drop");
        if(end==MotionEvent.ACTION_POINTER_DOWN){
            MotionEvent.PointerProperties[] props={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};
            for(int i=0;i<2;i++){props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].x=x+i*8;coords[i].y=targetY;coords[i].pressure=1;coords[i].size=1;}
            e=MotionEvent.obtain(down,down+250,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        }else e=MotionEvent.obtain(down,down+250,end,x,targetY,0);
        v.dispatchTouchEvent(e);e.recycle();
        if(end==MotionEvent.ACTION_POINTER_DOWN){e=MotionEvent.obtain(down,down+260,MotionEvent.ACTION_CANCEL,x,targetY,0);v.dispatchTouchEvent(e);e.recycle();}
    }
    private Activity open(){Activity a=startActivitySync(new Intent(app,ListeningWidgetConfigActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();return a;}
    private ListeningOrderEditor editor(Activity a){return (ListeningOrderEditor)a.getWindow().getDecorView().findViewWithTag("listening-order-editor");}
    private void layout(View v,int width,int height){v.measure(View.MeasureSpec.makeMeasureSpec(dp(v.getContext(),width),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dp(v.getContext(),height),View.MeasureSpec.EXACTLY));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());}
    private void bounds(View v){if(v.getVisibility()!=View.VISIBLE)return;if(v instanceof TextView){TextView t=(TextView)v;if(t.getLayout()!=null)for(int i=0;i<t.getLayout().getLineCount();i++)yes(t.getLayout().getLineWidth(i)<=t.getWidth()-t.getPaddingLeft()-t.getPaddingRight()+1,"text width: "+t.getText());}
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;yes(child.getLeft()>=-1&&child.getRight()<=group.getWidth()+1,"horizontal bounds");bounds(child);}}}
    private static View find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r;}return null;}
    private int count(View v,String text){int n=v instanceof TextView&&text.contentEquals(((TextView)v).getText())?1:0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=count(((ViewGroup)v).getChildAt(i),text);return n;}
    private int countType(View v,Class<?> type){int n=type.isInstance(v)?1:0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=countType(((ViewGroup)v).getChildAt(i),type);return n;}
    private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private void write(String name,Bitmap b)throws Exception{File dir=new File(app.getExternalFilesDir(null),"order-checks");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
