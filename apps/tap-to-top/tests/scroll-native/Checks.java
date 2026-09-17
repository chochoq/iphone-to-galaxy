package com.chocho.scroll006test;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;

public final class Checks extends Instrumentation {
    MainActivity activity;UiAutomation automation;int assertions;String mode;
    final ArrayList<String> log=new ArrayList<>();
    @Override public void onCreate(Bundle args){super.onCreate(args);mode=args==null?"direct":args.getString("mode","direct");start();}
    void check(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;log.add(message);Bundle progress=new Bundle();progress.putString("stream","OK "+message+"\n");sendStatus(0,progress);}
    void pause(long ms){SystemClock.sleep(ms);}
    void main(Runnable r){runOnMainSync(r);}
    void launch(int at,boolean reject){
        launch(at,reject,"none");
    }
    void launch(int at,boolean reject,String variant){
        if(activity!=null){main(()->activity.finish());pause(200);}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("at",at).putExtra("reject",reject).putExtra(variant,true));pause(650);
    }
    int first(){int[] n={-1};main(()->n[0]=activity.manager.findFirstVisibleItemPosition());return n[0];}
    boolean top(){boolean[] b={false};main(()->b[0]=activity.manager.findFirstVisibleItemPosition()==0&&activity.manager.findViewByPosition(0).getTop()>=activity.list.getPaddingTop());return b[0];}
    AccessibilityNodeInfo listNode(AccessibilityNodeInfo n){if(n==null)return null;if(n.isScrollable()&&n.getCollectionInfo()!=null&&n.getCollectionInfo().getRowCount()==2000)return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=listNode(n.getChild(i));if(found!=null)return found;}return null;}
    void touch(int action,float x,float y,long down){long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(down,now,action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);automation.injectInputEvent(e,true);e.recycle();}
    void tapTop(){long t=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,350,24,t);pause(40);touch(MotionEvent.ACTION_UP,350,24,t);}
    void begin(){
        if(!mode.equals("direct")){tapTop();return;}
        AccessibilityNodeInfo node=listNode(automation.getRootInActiveWindow());check(node!=null,"real list node available");
        check((node.getExtras().getInt("androidx.view.accessibility.AccessibilityNodeInfoCompat.BOOLEAN_PROPERTY_KEY",0)&(1<<26))!=0,"released RecyclerView advertises AndroidX granular support");
        Bundle b=new Bundle();b.putFloat("androidx.core.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT",Float.POSITIVE_INFINITY);
        check(node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,b),"infinity request accepted");
    }
    void arrival(int position){
        launch(position,false);check(first()==position,"starts at "+position);long start=SystemClock.uptimeMillis();begin();boolean intermediate=false;
        while(SystemClock.uptimeMillis()-start<45000&&!top()){int p=first();if(p>0&&p<position)intermediate=true;pause(30);}
        check(top(),"actual first row and top edge reached from "+position);check(intermediate,"intermediate rows observed from "+position);
        check(activity.list.requests==1,"one native request from "+position);check(activity.list.lastAmount==Float.POSITIVE_INFINITY,"AndroidX infinity argument delivered");
        log.add("arrival_ms position="+position+" time="+(SystemClock.uptimeMillis()-start));
    }
    void cancel(){
        launch(1000,false);begin();pause(450);check(first()<1000&&first()>0,"long native motion in progress before grab");
        long t=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,540,1200,t);pause(120);int stopped=first();pause(900);
        check(first()==stopped,"touch-down holds current list position");touch(MotionEvent.ACTION_UP,540,1200,t);pause(800);
        check(first()==stopped,"release does not restart automatic scroll");check(activity.list.requests==1,"no request reissued after touch");
    }
    void regression(){
        launch(100,false,"wrapper");tapTop();pause(2400);
        check(top()&&activity.list.requests==1,"native main list inside larger non-native wrapper reaches top");
        check(activity.list.touchDowns==0,"wrapper delegation does not inject a fallback gesture");
        launch(1000,true);tapTop();pause(1800);
        check(activity.list.requests==1,"rejected request attempted once");
        check(first()==1000&&activity.list.touchDowns==0,"rejection does not trigger a fallback gesture");
        launch(1000,false,"horizontal");int before=first();tapTop();pause(800);
        check(first()==before&&activity.list.requests==0&&activity.list.touchDowns==0,"horizontal collection is not targeted");
        launch(1000,false,"unsupported");tapTop();pause(2200);
        check(activity.list.requests==0&&activity.list.touchDowns==1,"unsupported uses exactly one gesture and no native request");
        check(first()<1000&&first()>0,"unsupported fallback moves up but does not pretend to reach top");
        int downs=activity.list.touchDowns;pause(1200);check(activity.list.touchDowns==downs,"fallback does not repeat");
        launch(1000,false,"reverse");tapTop();pause(1000);
        check(activity.list.requests==0,"reverse order never receives adapter-zero request");
        launch(10,false,"mixed");tapTop();pause(1600);
        check(top()&&activity.list.requests==1,"main list reaches top with large horizontal carousel present");
        check(activity.distractor.requests==0&&activity.distractor.touchDowns==0,"carousel is untouched");
        tapTop();pause(450);
        check(top()&&activity.list.requests==1&&activity.list.touchDowns==0,"already at top causes no added action or gesture");
        launch(1000,false);begin();pause(350);
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);pause(1200);
        check(activity.list.requests==1,"home switch does not create a second request");
    }
    AccessibilityNodeInfo label(AccessibilityNodeInfo n,String text){
        if(n==null)return null;
        if(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=label(n.getChild(i),text);if(found!=null)return found;}
        return null;
    }
    void openSettings(){getTargetContext().startActivity(new Intent().setClassName("com.chocho.taptotop","com.chocho.taptotop.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));pause(650);}
    AccessibilityNodeInfo toggle(AccessibilityNodeInfo n,String name){
        if(n==null)return null;
        if(n.isCheckable()&&name.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=toggle(n.getChild(i),name);if(found!=null)return found;}return null;
    }
    void switchSetting(String name,boolean wanted){
        openSettings();AccessibilityNodeInfo n=toggle(automation.getRootInActiveWindow(),name);
        check(n!=null,"setting found: "+name);if(n.isChecked()!=wanted)check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"setting changed: "+name);
        pause(300);n=toggle(automation.getRootInActiveWindow(),name);check(n!=null&&n.isChecked()==wanted,"setting persisted: "+name);
    }
    void clickSetting(String name){openSettings();AccessibilityNodeInfo n=label(automation.getRootInActiveWindow(),name);check(n!=null&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"clicked "+name);pause(300);}
    void capture(String name)throws Exception{
        android.graphics.Bitmap bitmap=automation.takeScreenshot();check(bitmap!=null,"screenshot "+name);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    void settings()throws Exception{
        switchSetting("앱의 맨 위 이동 사용",false);launch(1000,false);tapTop();pause(2000);
        check(activity.list.requests==0&&activity.list.touchDowns==1,"native OFF preserves single-fling behavior (requests="+activity.list.requests+", touchDowns="+activity.list.touchDowns+", first="+first()+")");
        switchSetting("앱의 맨 위 이동 사용",true);launch(10,false);tapTop();pause(1200);check(top()&&activity.list.requests==1,"native ON restored through app UI");
        clickSetting("두 번");launch(100,false);tapTop();pause(500);check(activity.list.requests==0&&first()==100,"double-tap option ignores single tap");
        tapTop();pause(70);tapTop();pause(1800);check(top()&&activity.list.requests==1,"double tap makes one destination request");
        clickSetting("한 번");switchSetting("스크롤 사용",false);launch(100,false);tapTop();pause(800);check(activity.list.requests==0&&activity.list.touchDowns==0,"disabled service overlay does not scroll");
        switchSetting("스크롤 사용",true);launch(10,false);tapTop();pause(1200);check(top(),"re-enabled service works");
        openSettings();capture("settings-native-top");
    }
    void web(){
        launch(0,false,"web");long start=SystemClock.uptimeMillis();while(!activity.webReady&&SystemClock.uptimeMillis()-start<8000)pause(100);
        check(activity.webReady,"offline long web document loaded");final int[] y={0};main(()->y[0]=activity.web.getScrollY());check(y[0]==60000,"web starts deep in document");
        tapTop();
        if(mode.equals("webcancel")){
            long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,540,1200,down);pause(700);
            main(()->y[0]=activity.web.getScrollY());
            check(activity.web.touchDowns==1&&y[0]==60000,"touch during deferred tree lookup cancels the pending movement");
            touch(MotionEvent.ACTION_UP,540,1200,down);pause(500);check(activity.web.touchDowns==1,"release after cancelled lookup does not start a gesture");return;
        }
        pause(2000);main(()->y[0]=activity.web.getScrollY());
        if(activity.web.touchDowns!=1)describe(automation.getRootInActiveWindow(),0);
        check(activity.web.touchDowns==1,"unsupported WebView receives one gesture (touchDowns="+activity.web.touchDowns+", y="+y[0]+")");check(y[0]>0&&y[0]<60000,"WebView fallback moves but remains short of top");
        pause(1000);check(activity.web.touchDowns==1,"WebView fallback never loops");
    }
    void describe(AccessibilityNodeInfo n,int depth){
        if(n==null||depth>5)return;android.graphics.Rect b=new android.graphics.Rect();n.getBoundsInScreen(b);
        log.add("fixture node depth="+depth+" class="+n.getClassName()+" bounds="+b+" visible="+n.isVisibleToUser()+" scrollable="+n.isScrollable()+" actions="+n.getActions());
        for(int i=0;i<Math.min(n.getChildCount(),3);i++)describe(n.getChild(i),depth+1);
    }
    void notifications(){
        launch(100,false);long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,350,24,down);
        for(int i=1;i<=12;i++){pause(16);touch(MotionEvent.ACTION_MOVE,350,24+i*65,down);}touch(MotionEvent.ACTION_UP,350,804,down);pause(600);
        check(activity.list.requests==0,"status-bar downward drag never triggers scroll");
        AccessibilityNodeInfo root=automation.getRootInActiveWindow();check(root!=null&&"com.android.systemui".contentEquals(root.getPackageName()),"notification shade opens");
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
    }
    AccessibilityNodeInfo collectionNode(AccessibilityNodeInfo n){
        if(n==null)return null;if(n.getCollectionInfo()!=null&&n.isScrollable())return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=collectionNode(n.getChild(i));if(found!=null)return found;}return null;
    }
    boolean action(AccessibilityNodeInfo n,int id){for(AccessibilityNodeInfo.AccessibilityAction a:n.getActionList())if(a.getId()==id)return true;return false;}
    void structuralEvidence(String variant,AccessibilityNodeInfo n){
        android.graphics.Rect rect=new android.graphics.Rect();n.getBoundsInScreen(rect);
        AccessibilityNodeInfo.CollectionInfo c=n.getCollectionInfo();
        log.add("structure="+variant+" class="+n.getClassName()+" collection="+(c==null?"none":c.getRowCount()+"x"+c.getColumnCount())
                +" compat="+((n.getExtras().getInt("androidx.view.accessibility.AccessibilityNodeInfoCompat.BOOLEAN_PROPERTY_KEY",0)&(1<<26))!=0)
                +" framework="+n.isGranularScrollingSupported()+" up="+action(n,16908344)+" down="+action(n,16908346)
                +" backward="+action(n,8192)+" bounds="+rect);
    }
    void coverage(){
        for(String variant:new String[]{"grid","gridHorizontal","gridReverse","missingRows","tall"}){
            launch(999,false,variant);AccessibilityNodeInfo n=collectionNode(automation.getRootInActiveWindow());
            check(n!=null,variant+" exposes a real collection");structuralEvidence(variant,n);
            int before=first();tapTop();pause(2400);
            check(activity.list.requests==0,variant+" receives no unproven adapter-zero request");
            check(activity.list.touchDowns==1,variant+" uses one fallback gesture");
            check(!top(),variant+" is explicitly NOT full-top support");
            log.add("coverage="+variant+" first_before="+before+" first_after="+first());
            pause(650);check(activity.list.touchDowns==1,variant+" does not repeat fallback");
        }
        for(String variant:new String[]{"platform","nested"}){
            launch(0,false,variant);final int[] y={0};main(()->y[0]=activity.basic.getScrollY());
            check(y[0]==60000,variant+" starts at known deep offset");describe(automation.getRootInActiveWindow(),0);
            tapTop();pause(2400);main(()->y[0]=activity.basic.getScrollY());
            check(activity.basicRequests==0,variant+" receives no unsupported destination request");
            check(activity.basicTouches==1,variant+" receives a single fallback gesture");
            check(y[0]>0&&y[0]<60000,variant+" moves but remains short of top");
            pause(650);check(activity.basicTouches==1,variant+" does not loop");
            log.add("coverage="+variant+" y_after="+y[0]);
        }
    }
    boolean canScroll(boolean horizontal,int direction){boolean[] b={false};main(()->b[0]=horizontal?activity.list.canScrollHorizontally(direction):activity.list.canScrollVertically(direction));return b[0];}
    void gridContract(){
        for(String variant:new String[]{"grid","gridHorizontal","gridReverse"}){
            launch(99,false,variant);AccessibilityNodeInfo n=collectionNode(automation.getRootInActiveWindow());
            check(n!=null,variant+" direct collection exists");structuralEvidence(variant,n);
            Bundle b=new Bundle();b.putFloat("androidx.core.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT",Float.POSITIVE_INFINITY);
            check(n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,b),variant+" accepts a direct infinity request");
            long start=SystemClock.uptimeMillis();boolean horizontal=variant.equals("gridHorizontal"),reverse=variant.equals("gridReverse");
            while(SystemClock.uptimeMillis()-start<16000&&canScroll(horizontal,reverse?1:-1))pause(30);
            check(!canScroll(horizontal,reverse?1:-1),variant+" reaches its adapter-zero edge");
            check(activity.list.requests==1&&activity.list.touchDowns==0,variant+" used one native request without a gesture");
            if(reverse)check(canScroll(false,-1),"reverse grid adapter zero is BOTTOM, not visual top");
            else if(horizontal)check(!canScroll(false,-1)&&canScroll(true,1),"horizontal grid moved to LEFT edge, not upward");
            else check(top(),"vertical grid direct request reaches actual top");
        }
    }
    int legacyOffset(){int[] y={0};main(()->y[0]=activity.legacy.getFirstVisiblePosition()*210-(activity.legacy.getChildAt(0).getTop()-activity.legacy.getListPaddingTop()));return y[0];}
    void legacyRequest(){
        AccessibilityNodeInfo n=collectionNode(automation.getRootInActiveWindow());check(n!=null,"platform ListView node exists");structuralEvidence("legacy",n);
        Bundle b=new Bundle();b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT,0);
        check(n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId(),b),"ListView position-zero request accepted");
    }
    void legacy(){
        launch(100,false,"legacy");check(legacyOffset()>20000,"ListView starts deep");legacyRequest();
        long start=SystemClock.uptimeMillis();boolean intermediate=false;int last=legacyOffset();
        while(SystemClock.uptimeMillis()-start<30000&&legacyOffset()>0){pause(50);int y=legacyOffset();if(y>0&&y<20000)intermediate=true;log.add("legacy_sample_ms="+(SystemClock.uptimeMillis()-start)+" y="+y+" dy="+(y-last));last=y;}
        check(legacyOffset()==0,"ListView direct request reaches top edge");check(intermediate,"ListView has intermediate positions, not instant jump");
        check(activity.legacy.requests==1&&activity.legacy.touchDowns==0,"ListView receives one position request");
        launch(1000,false,"legacy");legacyRequest();pause(450);int moving=legacyOffset();check(moving>0&&moving<210000,"ListView motion active before touch");
        long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,540,1200,down);pause(120);int stopped=legacyOffset();pause(800);
        check(legacyOffset()==stopped,"ListView native position scroller stops on touch");touch(MotionEvent.ACTION_UP,540,1200,down);pause(900);
        check(legacyOffset()==stopped&&activity.legacy.requests==1,"ListView does not resume after release");
    }
    final class FrameSamples implements android.view.Choreographer.FrameCallback {
        final boolean oldList;boolean running=true;int frames,changes,wholeRows,partialRows,previous;
        final ArrayList<String> samples=new ArrayList<>();long start;
        FrameSamples(boolean oldList){this.oldList=oldList;previous=offset();start=SystemClock.uptimeMillis();}
        int offset(){
            if(oldList)return activity.legacy.getFirstVisiblePosition()*210-(activity.legacy.getChildAt(0).getTop()-activity.legacy.getListPaddingTop());
            int p=activity.manager.findFirstVisibleItemPosition();return p*210-(activity.manager.findViewByPosition(p).getTop()-activity.list.getPaddingTop());
        }
        public void doFrame(long frameTimeNanos){
            if(!running)return;frames++;int y=offset(),delta=y-previous;
            if(delta!=0){changes++;if(Math.abs(delta)%210==0)wholeRows++;else partialRows++;samples.add("frame_ms="+(SystemClock.uptimeMillis()-start)+" y="+y+" dy="+delta);}
            previous=y;android.view.Choreographer.getInstance().postFrameCallback(this);
        }
    }
    void motion(){
        for(boolean oldList:new boolean[]{false,true}){
            launch(100,false,oldList?"legacy":"none");FrameSamples[] holder={null};
            main(()->{holder[0]=new FrameSamples(oldList);android.view.Choreographer.getInstance().postFrameCallback(holder[0]);});
            if(oldList)legacyRequest();else tapTop();pause(6000);
            main(()->{holder[0].running=false;android.view.Choreographer.getInstance().removeFrameCallback(holder[0]);});
            FrameSamples s=holder[0];check(s.changes>10,"motion frames captured for "+(oldList?"ListView":"RecyclerView"));
            check(oldList?legacyOffset()==0:top(),"motion run reached actual top");
            log.add("motion="+(oldList?"ListView":"RecyclerView")+" frames="+s.frames+" changes="+s.changes+" whole_row_changes="+s.wholeRows+" partial_row_changes="+s.partialRows);
            log.addAll(s.samples);
        }
    }
    @Override public void onStart(){
        Bundle out=new Bundle();try{
            automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            if(mode.equals("motion"))motion();else if(mode.equals("legacy"))legacy();else if(mode.equals("gridcontract"))gridContract();else if(mode.equals("coverage"))coverage();else if(mode.equals("regression"))regression();else if(mode.equals("settings"))settings();else if(mode.equals("notifications"))notifications();else if(mode.startsWith("web"))web();else {arrival(10);arrival(100);arrival(1000);cancel();}
            out.putString("stream","PASS "+assertions+" assertions\n"+String.join("\n",log));finish(-1,out);
        }catch(Throwable e){out.putString("stream","FAIL "+e+"\n"+String.join("\n",log));finish(1,out);}
    }
}
