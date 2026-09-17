package com.chocho.scroll006test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;

public final class MainActivity extends Activity {
    public CountingList list;
    public LinearLayoutManager manager;
    public CountingList distractor;
    public CountingWeb web;
    public volatile boolean webReady;
    public ViewGroup basic;
    public volatile int basicRequests,basicTouches;
    public CountingLegacy legacy;
    public boolean reject, horizontal, unsupported;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        reject=getIntent().getBooleanExtra("reject",false);
        horizontal=getIntent().getBooleanExtra("horizontal",false)||getIntent().getBooleanExtra("gridHorizontal",false);
        unsupported=getIntent().getBooleanExtra("unsupported",false);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(Color.WHITE);
        page.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(i.left,i.top,i.right,i.bottom);return insets;});
        if(getIntent().getBooleanExtra("legacy",false)){
            legacy=new CountingLegacy();legacy.setDividerHeight(0);legacy.setAdapter(new BaseAdapter(){
                public int getCount(){return 2000;}public Object getItem(int p){return p;}public long getItemId(int p){return p;}
                public View getView(int p,View old,ViewGroup parent){TextView v=old instanceof TextView?(TextView)old:new TextView(MainActivity.this);v.setText(p==0?"TOP":"Offline row "+p);v.setTextColor(Color.BLACK);v.setTextSize(22);v.setBackgroundColor(p%2==0?0xfff2f2f7:Color.WHITE);v.setLayoutParams(new AbsListView.LayoutParams(-1,210));return v;}
            });page.addView(legacy,new LinearLayout.LayoutParams(-1,-1));setContentView(page);
            legacy.post(()->legacy.setSelectionFromTop(getIntent().getIntExtra("at",100),-17));return;
        }
        if(getIntent().getBooleanExtra("platform",false)||getIntent().getBooleanExtra("nested",false)){
            basic=getIntent().getBooleanExtra("nested",false)?new CountingNested():new CountingScroll();
            LinearLayout content=new LinearLayout(this);content.setOrientation(1);
            for(int i=0;i<600;i++){TextView row=new TextView(this);row.setText("Offline row "+i);row.setTextColor(Color.BLACK);content.addView(row,new LinearLayout.LayoutParams(-1,210));}
            basic.addView(content);page.addView(basic,new LinearLayout.LayoutParams(-1,-1));setContentView(page);
            basic.post(()->basic.scrollTo(0,60000));return;
        }
        if(getIntent().getBooleanExtra("web",false)){
            web=new CountingWeb();web.getSettings().setJavaScriptEnabled(false);web.setInitialScale(100);
            web.setWebViewClient(new android.webkit.WebViewClient(){@Override public void onPageFinished(android.webkit.WebView v,String url){v.postDelayed(()->{v.scrollTo(0,60000);webReady=true;},400);}});
            page.addView(web,new LinearLayout.LayoutParams(-1,-1));setContentView(page);
            StringBuilder html=new StringBuilder("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'></head><body style='margin:0'>");
            for(int i=0;i<2000;i++)html.append("<div style='height:100px;background:").append(i%2==0?"#f2f2f7":"#ffffff").append("'>Offline test row ").append(i).append("</div>");
            web.loadDataWithBaseURL(null,html.append("</body></html>").toString(),"text/html","UTF-8",null);return;
        }
        TextView heading=new TextView(this);heading.setText("실제 RecyclerView 1.4.0 · 2,000개 목록");heading.setTextSize(18);page.addView(heading);
        list=new CountingList();
        boolean grid=getIntent().getBooleanExtra("grid",false)||getIntent().getBooleanExtra("gridHorizontal",false)||getIntent().getBooleanExtra("gridReverse",false);
        boolean reverse=getIntent().getBooleanExtra("reverse",false)||getIntent().getBooleanExtra("gridReverse",false);
        manager=grid?new GridLayoutManager(this,3,horizontal?RecyclerView.HORIZONTAL:RecyclerView.VERTICAL,reverse):
                new LinearLayoutManager(this,horizontal?RecyclerView.HORIZONTAL:RecyclerView.VERTICAL,reverse);
        list.setLayoutManager(manager);list.setAdapter(new Rows());
        if(getIntent().getBooleanExtra("missingRows",false))list.setAccessibilityDelegateCompat(new androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate(list){
            final androidx.core.view.AccessibilityDelegateCompat item=new ItemDelegate(this){
                @Override public void onInitializeAccessibilityNodeInfo(View v,androidx.core.view.accessibility.AccessibilityNodeInfoCompat info){
                    super.onInitializeAccessibilityNodeInfo(v,info);info.setCollectionItemInfo(null);
                }
            };
            @Override public androidx.core.view.AccessibilityDelegateCompat getItemDelegate(){return item==null?super.getItemDelegate():item;}
        });
        if(unsupported)androidx.core.view.ViewCompat.setAccessibilityDelegate(list,new androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate(list){
            @Override public void onInitializeAccessibilityNodeInfo(View v,androidx.core.view.accessibility.AccessibilityNodeInfoCompat info){
                super.onInitializeAccessibilityNodeInfo(v,info);info.setGranularScrollingSupported(false);
            }
        });
        if(getIntent().getBooleanExtra("mixed",false)){
            distractor=new CountingList();distractor.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));distractor.setAdapter(new Rows());
            page.addView(distractor,new LinearLayout.LayoutParams(-1,700));
            distractor.post(()->distractor.scrollToPosition(100));
        }
        if(getIntent().getBooleanExtra("wrapper",false)) {
            FrameLayout wrapper=new FrameLayout(this);wrapper.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            wrapper.setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){
                    super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.ScrollView");
                    info.setScrollable(true);info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            });
            FrameLayout.LayoutParams child=new FrameLayout.LayoutParams(-1,-1);child.topMargin=400;
            wrapper.addView(list,child);page.addView(wrapper,new LinearLayout.LayoutParams(-1,0,1));
        } else page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
        int at=getIntent().getIntExtra("at",100);
        list.post(()->manager.scrollToPositionWithOffset(at,-17));
    }
    public final class CountingLegacy extends ListView {
        public volatile int requests,touchDowns;
        CountingLegacy(){super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)touchDowns++;return super.dispatchTouchEvent(e);}
        @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId())requests++;return super.performAccessibilityAction(action,args);}
    }
    public final class CountingScroll extends ScrollView {
        CountingScroll(){super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)basicTouches++;return super.dispatchTouchEvent(e);}
        @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==8192||action==16908344)basicRequests++;return super.performAccessibilityAction(action,args);}
    }
    public final class CountingNested extends androidx.core.widget.NestedScrollView {
        CountingNested(){super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)basicTouches++;return super.dispatchTouchEvent(e);}
        @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==8192||action==16908344)basicRequests++;return super.performAccessibilityAction(action,args);}
    }
    public final class CountingWeb extends android.webkit.WebView {
        public volatile int touchDowns;
        CountingWeb(){super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)touchDowns++;return super.dispatchTouchEvent(e);}
    }
    public final class CountingList extends RecyclerView {
        public volatile int requests;
        public volatile int touchDowns;
        public volatile float lastAmount;
        CountingList(){super(MainActivity.this);}
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)touchDowns++;return super.dispatchTouchEvent(e);}
        @Override public boolean performAccessibilityAction(int action,Bundle args){
            if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){
                requests++;lastAmount=args==null?0:args.getFloat("androidx.core.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT",0);
                if(reject)return false;
            }
            return super.performAccessibilityAction(action,args);
        }
    }
    final class Holder extends RecyclerView.ViewHolder { Holder(TextView v){super(v);} }
    final class Rows extends RecyclerView.Adapter<Holder>{
        public int getItemCount(){return 2000;}
        public Holder onCreateViewHolder(ViewGroup parent,int type){TextView v=new TextView(MainActivity.this);v.setTextColor(Color.BLACK);v.setTextSize(22);v.setPadding(24,18,24,18);v.setLayoutParams(new RecyclerView.LayoutParams(horizontal?450:-1,getIntent().getBooleanExtra("tall",false)?5000:210));return new Holder(v);}
        public void onBindViewHolder(Holder h,int position){TextView v=(TextView)h.itemView;v.setText(position==0?"맨 처음 · TOP":"시험 항목 "+position);v.setBackgroundColor(position%2==0?0xfff2f2f7:Color.WHITE);}
    }
}
