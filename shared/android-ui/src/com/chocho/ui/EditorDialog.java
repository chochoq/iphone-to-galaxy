package com.chocho.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

/** Restorable, single-commit editors. Hosts supply domain policy, never a captured Activity callback. */
public final class EditorDialog extends DialogFragment {
    public interface Host {
        int[] editorValues(String key);
        Callable<SaveJobs.Result> editorSave(String key,int[] before,int[] selected);
        void editorUpdated();
    }
    public static final String TAG="settings.editor.008",NUMBER="number",TIME="time",CARD="card",RESET="reset";
    private String key,type,title,help,unit,jobId,errorMessage="",raw="";
    private int min,max,active,lastAuto=10;
    private boolean restoreInputFocus,inputHadFocus;
    private int selectionStart,selectionEnd;
    private int[] original,draft;
    private boolean seconds,direct,inputValid=true,binding;
    private LinearLayout root,body;
    private Button save,cancel;
    private TextView error;
    private final TextView[] timeLabels=new TextView[2];
    private EditText input;
    private SeekBar seek;
    private final ArrayList<WheelColumn> wheels=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    public static void open(Activity a,String key,String title,String type,int min,int max,String unit,String help,int[] defaults){
        if(a.getFragmentManager().isStateSaved()||a.getFragmentManager().findFragmentByTag(TAG)!=null)return;
        Bundle b=new Bundle();b.putString("key",key);b.putString("title",title);b.putString("type",type);b.putInt("min",min);b.putInt("max",max);b.putString("unit",unit);b.putString("help",help);
        b.putIntArray("before",((Host)a).editorValues(key));b.putIntArray("defaults",defaults);
        EditorDialog d=new EditorDialog();d.setArguments(b);d.show(a.getFragmentManager(),TAG);a.getFragmentManager().executePendingTransactions();
    }
    public static void number(Activity a,String key,String title,int min,int max,String unit,String help){open(a,key,title,NUMBER,min,max,unit,help,null);}
    public static void time(Activity a){open(a,"schedule","연장 시간",TIME,0,86399,"","공휴일 연장 시간 안이라면 저장 후 바로 적용될 수 있어요.",null);}
    public static void card(Activity a){open(a,"card","카드 닫는 방식",CARD,3,60,"초","다음 카드에 적용돼요.",null);}
    public static void reset(Activity a,int[] defaults,String description){open(a,"reset","조절값을 되돌릴까요?",RESET,0,0,"",description,defaults);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);Bundle args=getArguments();key=args.getString("key");title=args.getString("title");type=args.getString("type");help=args.getString("help");unit=args.getString("unit");min=args.getInt("min");max=args.getInt("max");
        original=(state==null?args:state).getIntArray("before").clone();
        draft=state==null?(RESET.equals(type)?args.getIntArray("defaults"):original).clone():state.getIntArray("draft").clone();
        if(state!=null){active=state.getInt("active");seconds=state.getBoolean("seconds");direct=state.getBoolean("direct");raw=state.getString("raw","");inputValid=state.getBoolean("input_valid",true);lastAuto=state.getInt("auto",10);jobId=state.getString("job");errorMessage=state.getString("error","");restoreInputFocus=state.getBoolean("input_focus");selectionStart=state.getInt("selection_start");selectionEnd=state.getInt("selection_end");}
        else{lastAuto=draft[0]>=3?draft[0]:10;direct=TIME.equals(type)&&getResources().getConfiguration().fontScale>1.4f;raw=TIME.equals(type)?EditorModel.time(draft[0]):String.valueOf(draft[0]);}
    }
    @Override public Dialog onCreateDialog(Bundle state){Dialog d=new Dialog(getActivity());d.requestWindowFeature(Window.FEATURE_NO_TITLE);d.setCanceledOnTouchOutside(false);build(d);return d;}
    private Host host(){return (Host)getActivity();}
    private boolean busy(){return jobId!=null;}
    private boolean valid(){return inputValid&&(TIME.equals(type)?EditorModel.validInterval(draft[0],draft[1]):CARD.equals(type)?draft[0]==0||(draft[0]>=3&&draft[0]<=60):NUMBER.equals(type)?draft[0]>=min&&draft[0]<=max:true);}
    private boolean dirty(){return !Arrays.equals(original,draft);}
    private void build(Dialog dialog){
        Activity a=getActivity();binding=true;wheels.clear();input=null;seek=null;timeLabels[0]=timeLabels[1]=null;
        // A 12-hour wheel with seconds cannot keep enlarged labels readable on a narrow window.
        // Preserve the same draft, but use the normal-sized text entry at large font settings.
        if(TIME.equals(type)&&getResources().getConfiguration().fontScale>1.4f&&!direct){direct=true;raw=EditorModel.time(draft[active]);}
        root=new LinearLayout(a){@Override protected void onMeasure(int w,int h){int bound=(int)(a.getWindowManager().getCurrentWindowMetrics().getBounds().height()*.92f);super.onMeasure(w,MeasureSpec.makeMeasureSpec(Math.min(bound,MeasureSpec.getSize(h)),MeasureSpec.AT_MOST));}};
        root.setOrientation(LinearLayout.VERTICAL);root.setBackground(Ui.rounded(a,Ui.BG,20));root.setClipToOutline(true);
        root.setPadding(0,0,0,RESET.equals(type)?0:Ui.dp(a,12));
        root.setOnApplyWindowInsetsListener((v,insets)->{int bottom=insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom;v.setPadding(0,0,0,RESET.equals(type)?0:Math.max(Ui.dp(a,12),bottom));return insets;});
        error=Ui.text(a,"",14,Ui.RED,false);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);error.setPadding(Ui.dp(a,16),Ui.dp(a,12),Ui.dp(a,16),Ui.dp(a,12));
        if(RESET.equals(type)){buildReset(a);dialog.setContentView(root);binding=false;update();return;}
        LinearLayout header=Ui.column(a);LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);
        // 8dp outside + the button's 12dp inside = 20dp from the sheet edge to its text.
        // Keep the hit area intact; visual breathing room is separate from the 48dp minimum.
        bar.setPadding(Ui.dp(a,8),Ui.dp(a,8),Ui.dp(a,8),Ui.dp(a,8));
        cancel=Ui.button(a,"취소",Ui.BLUE);cancel.setBackground(Ui.pressed(a,Ui.BG,8));cancel.setOnClickListener(v->dismiss());
        save=Ui.button(a,"저장",Ui.BLUE);save.setBackground(Ui.pressed(a,Ui.BG,8));save.setOnClickListener(v->commit());
        TextView heading=Ui.text(a,title,17,Ui.INK,true);heading.setGravity(Gravity.CENTER);heading.setAccessibilityHeading(true);heading.setPadding(Ui.dp(a,6),Ui.dp(a,12),Ui.dp(a,6),Ui.dp(a,12));
        if(getResources().getConfiguration().fontScale>1.4f){bar.addView(cancel);bar.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));bar.addView(save);header.addView(bar);header.addView(heading,new LinearLayout.LayoutParams(-1,-2));}
        else{bar.addView(cancel);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));bar.addView(save);header.addView(bar);}
        root.addView(header);Ui.separator(root);
        ScrollView scroll=new ScrollView(a);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setVerticalScrollBarEnabled(false);
        body=Ui.column(a);body.setPadding(Ui.dp(a,16),Ui.dp(a,20),Ui.dp(a,16),Ui.dp(a,12));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        if(TIME.equals(type))buildTime(a);else if(CARD.equals(type))buildCard(a);else buildNumber(a,body);
        body.addView(error);body.addView(Ui.footnote(a,help));dialog.setContentView(root);binding=false;update();
    }
    private void buildReset(Activity a){
        TextView heading=Ui.text(a,title,18,Ui.INK,true);heading.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);heading.setAccessibilityHeading(true);heading.setGravity(Gravity.CENTER);heading.setPadding(Ui.dp(a,20),Ui.dp(a,24),Ui.dp(a,20),Ui.dp(a,12));root.addView(heading);
        TextView detail=Ui.text(a,help,14,Ui.INK,false);detail.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_BALANCED);detail.setGravity(Gravity.CENTER);detail.setPadding(Ui.dp(a,20),0,Ui.dp(a,20),Ui.dp(a,20));root.addView(detail);root.addView(error);
        int available=Math.min(a.getWindowManager().getCurrentWindowMetrics().getBounds().width()-Ui.dp(a,48),Ui.dp(a,340))-Ui.dp(a,40);
        Ui.keepAlertWords(heading,available);Ui.keepAlertWords(detail,available);
        LinearLayout buttons=new LinearLayout(a);if(getResources().getConfiguration().fontScale>1.5)buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),Ui.dp(a,16));
        cancel=Ui.button(a,"취소",Ui.BLUE);cancel.setOnClickListener(v->dismiss());save=Ui.button(a,"되돌리기",Ui.RED);save.setOnClickListener(v->commit());
        boolean vertical=buttons.getOrientation()==LinearLayout.VERTICAL;buttons.addView(cancel,new LinearLayout.LayoutParams(vertical?-1:0,-2,vertical?0:1));
        View gap=new View(a);gap.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        buttons.addView(gap,new LinearLayout.LayoutParams(vertical?1:Ui.dp(a,12),vertical?Ui.dp(a,12):1));
        buttons.addView(save,new LinearLayout.LayoutParams(vertical?-1:0,-2,vertical?0:1));root.addView(buttons);
    }
    private String timeLabel(int value){
        if(android.text.format.DateFormat.is24HourFormat(getActivity()))return EditorModel.time(value);
        int h=value/3600;return(h<12?"오전 ":"오후 ")+String.format(java.util.Locale.KOREAN,value%60==0?"%d:%02d":"%d:%02d:%02d",h%12==0?12:h%12,value%3600/60,value%60);
    }
    private void buildTime(Activity a){
        LinearLayout group=Ui.group(a);body.addView(group);
        for(int field=0;field<2;field++){
            final int f=field;if(field>0)Ui.separator(group);
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(a,16),Ui.dp(a,10),Ui.dp(a,16),Ui.dp(a,10));row.setMinimumHeight(Ui.dp(a,50));
            boolean stacked=getResources().getConfiguration().fontScale>1.25;row.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
            TextView label=Ui.text(a,field==0?"시작":"종료",17,Ui.INK,false);row.addView(label,stacked?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
            TextView value=Ui.text(a,timeLabel(draft[field]),18,active==field?Ui.BLUE:Ui.INK,false);value.setFontFeatureSettings("tnum");value.setPadding(Ui.dp(a,9),Ui.dp(a,5),Ui.dp(a,9),Ui.dp(a,5));value.setBackground(Ui.rounded(a,active==field?0xffeaf2fc:0xffeeeef0,7));row.addView(value);timeLabels[field]=value;
            row.setFocusable(true);row.setClickable(true);row.setBackground(SettingsSheet.pressed(a,Ui.PAPER,8));row.setContentDescription((field==0?"시작":"종료")+" 시간 "+timeLabel(draft[field])+", 변경");
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);value.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.setOnClickListener(v->{if(!inputValid){showError("입력한 시간을 먼저 확인해 주세요.");return;}settleWheels();active=f;raw=EditorModel.time(draft[f]);rebuild();});group.addView(row);
            if(active==field&&!direct)buildWheels(a,group);
        }
        if(direct){TextView label=Ui.footnote(a,(active==0?"시작":"종료")+" 시간 · 24시간제 HH:MM 또는 HH:MM:SS");body.addView(label);input=numberInput(a,raw);input.setInputType(android.text.InputType.TYPE_CLASS_DATETIME|android.text.InputType.TYPE_DATETIME_VARIATION_TIME);input.setContentDescription((active==0?"시작":"종료")+" 시간 직접 입력, 24시간제");body.addView(input);input.addTextChangedListener(watcher(()->{
            raw=input.getText().toString();int value=EditorModel.parseTime(raw,draft[active]);inputValid=value>=0;if(inputValid)draft[active]=value;errorMessage="";update();
        }));}
        if(draft[active]%60>0){TextView precision=Ui.footnote(a,"시·분을 바꿔도 "+draft[active]%60+"초는 유지돼요.");body.addView(precision);}
        LinearLayout actions=new LinearLayout(a);if(getResources().getConfiguration().fontScale>1.25)actions.setOrientation(LinearLayout.VERTICAL);
        Button precise=Ui.button(a,seconds?"초 조정 접기":"초까지 조정",Ui.MUTED);precise.setTextSize(14);precise.setBackground(Ui.pressed(a,Ui.BG,8));precise.setVisibility(direct?View.GONE:View.VISIBLE);precise.setOnClickListener(v->{settleWheels();seconds=!seconds;rebuild();});actions.addView(precise);
        Button method=Ui.button(a,direct?"휠로 고르기":"숫자로 입력",Ui.MUTED);method.setTextSize(14);method.setBackground(Ui.pressed(a,Ui.BG,8));method.setOnClickListener(v->{if(!inputValid){showError("입력한 시간을 먼저 확인해 주세요.");return;}settleWheels();direct=!direct;raw=EditorModel.time(draft[active]);rebuild();});
        if(getResources().getConfiguration().fontScale>1.4f)body.addView(Ui.footnote(a,"큰 글씨에서는 숫자로 시간을 입력해요."));else actions.addView(method);body.addView(actions);
    }
    private void buildWheels(Activity a,LinearLayout group){
        boolean twentyFour=android.text.format.DateFormat.is24HourFormat(a);int columns=(seconds?3:2)+(twentyFour?0:1);
        FrameLayout frame=new FrameLayout(a);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);ArrayList<String> names=new ArrayList<>();
        addWheel(a,row,active==0?"시작 시":"종료 시",twentyFour?24:12,twentyFour?draft[active]/3600:draft[active]/3600%12,twentyFour?null:new String[]{"12","01","02","03","04","05","06","07","08","09","10","11"},value->{int h=twentyFour?value:value+(draft[active]/3600>=12?12:0);draft[active]=EditorModel.withHourMinute(draft[active],h,draft[active]%3600/60);update();});names.add("시");
        addWheel(a,row,active==0?"시작 분":"종료 분",60,draft[active]%3600/60,null,value->{draft[active]=EditorModel.withHourMinute(draft[active],draft[active]/3600,value);update();});names.add("분");
        if(seconds){addWheel(a,row,active==0?"시작 초":"종료 초",60,draft[active]%60,null,value->{draft[active]=EditorModel.withSecond(draft[active],value);update();});names.add("초");}
        if(!twentyFour){addWheel(a,row,"오전 또는 오후",2,draft[active]/3600>=12?1:0,new String[]{"오전","오후"},value->{draft[active]=EditorModel.withHourMinute(draft[active],draft[active]/3600%12+value*12,draft[active]%3600/60);update();});names.add("오전/오후");}
        View selected=new View(a);selected.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);selected.setBackground(Ui.rounded(a,0xffeeeef0,8));FrameLayout.LayoutParams band=new FrameLayout.LayoutParams(-1,wheels.get(0).rowPixels(),Gravity.CENTER);band.leftMargin=band.rightMargin=Ui.dp(a,16);frame.addView(selected,band);
        float windowDp=a.getWindowManager().getCurrentWindowMetrics().getBounds().width()/getResources().getDisplayMetrics().density;
        int lane=Math.min(Ui.dp(a,72),Math.max(Ui.dp(a,48),Ui.dp(a,(windowDp>=600?440:windowDp)-32)/columns));
        FrameLayout.LayoutParams centered=new FrameLayout.LayoutParams(lane*columns,-2,Gravity.CENTER);frame.addView(row,centered);group.addView(frame,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout units=new LinearLayout(a);units.setGravity(Gravity.CENTER);for(String n:names){TextView t=Ui.text(a,n,11,Ui.MUTED,false);t.setGravity(Gravity.CENTER);t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);units.addView(t,new LinearLayout.LayoutParams(lane,-2));}units.setPadding(0,0,0,Ui.dp(a,10));group.addView(units);
    }
    private void addWheel(Activity a,LinearLayout row,String label,int count,int value,String[] labels,java.util.function.IntConsumer changed){
        WheelColumn column=new WheelColumn(a,label,count,value,labels,changed);wheels.add(column);row.addView(column,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void buildCard(Activity a){
        LinearLayout group=Ui.group(a);body.addView(group);for(int choice=0;choice<2;choice++){
            final boolean automatic=choice==1;if(choice>0)Ui.separator(group);
            Button b=Ui.navigation(a,automatic?"자동으로 닫기":"직접 닫기",false);boolean selected=(draft[0]>0)==automatic;
            b.setCompoundDrawablesRelative(null,null,selected?new TrailingMark(a,true):null,null);
            b.setContentDescription((automatic?"자동으로 닫기":"직접 닫기")+(selected?", 선택됨":""));
            b.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View v,android.view.accessibility.AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(v,n);n.setClassName("android.widget.RadioButton");n.setCheckable(true);n.setChecked(selected);}});
            b.setOnClickListener(v->{if(automatic){draft[0]=lastAuto;raw=String.valueOf(lastAuto);}else{if(draft[0]>=3)lastAuto=draft[0];draft[0]=0;raw="0";}inputValid=true;errorMessage="";rebuild();});group.addView(b,new LinearLayout.LayoutParams(-1,-2));
        }
        if(draft[0]>0)buildNumber(a,body);else body.addView(Ui.footnote(a,"화면을 누르거나 ×를 누르면 닫혀요."));
    }
    private EditText numberInput(Activity a,String value){
        EditText e=new EditText(a);e.setId(0x1008002);e.setText(value);e.setTextColor(Ui.INK);e.setTextSize(22);e.setSelectAllOnFocus(true);e.setSingleLine(true);e.setPadding(Ui.dp(a,12),Ui.dp(a,10),Ui.dp(a,12),Ui.dp(a,10));e.setMinimumHeight(Ui.dp(a,48));e.setBackground(Ui.rounded(a,Ui.PAPER,8));e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        inputHadFocus=restoreInputFocus;e.setOnFocusChangeListener((v,focused)->{if(focused)inputHadFocus=true;else if(v.hasWindowFocus()&&isResumed())inputHadFocus=false;});return e;
    }
    private void buildNumber(Activity a,LinearLayout parent){
        LinearLayout numeric=Ui.group(a);numeric.setPadding(Ui.dp(a,16),Ui.dp(a,12),Ui.dp(a,16),Ui.dp(a,16));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=CARD.equals(type)?Ui.dp(a,16):0;parent.addView(numeric,np);
        LinearLayout controls=new LinearLayout(a);controls.setGravity(Gravity.CENTER);
        Button less=Ui.button(a,"−",Ui.BLUE),more=Ui.button(a,"+",Ui.BLUE);less.setContentDescription("1 감소");more.setContentDescription("1 증가");
        input=numberInput(a,raw);input.setTextSize(28);input.setFontFeatureSettings("tnum");input.setGravity(Gravity.CENTER);input.setContentDescription(title+" 직접 입력, "+unit);input.setBackground(Ui.rounded(a,Ui.BG,8));
        controls.addView(less,new LinearLayout.LayoutParams(Ui.dp(a,48),-2));LinearLayout.LayoutParams field=new LinearLayout.LayoutParams(0,-2,1);field.leftMargin=Ui.dp(a,4);field.rightMargin=Ui.dp(a,6);controls.addView(input,field);
        TextView suffix=Ui.text(a,unit,17,Ui.MUTED,false);suffix.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);controls.addView(suffix);controls.addView(more,new LinearLayout.LayoutParams(Ui.dp(a,48),-2));numeric.addView(controls);
        seek=new SettingsSlider(a);seek.setMax(max-min);seek.setProgress(Math.max(0,draft[0]-min));seek.setContentDescription(title);numeric.addView(seek,new LinearLayout.LayoutParams(-1,Ui.dp(a,52)));
        LinearLayout limits=new LinearLayout(a);limits.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),0);TextView low=Ui.text(a,min+unit,12,Ui.MUTED,false),high=Ui.text(a,max+unit,12,Ui.MUTED,false);high.setGravity(Gravity.END);limits.addView(low,new LinearLayout.LayoutParams(0,-2,1));limits.addView(high,new LinearLayout.LayoutParams(0,-2,1));numeric.addView(limits);
        less.setOnClickListener(v->setNumber(Math.max(min,draft[0]-1)));more.setOnClickListener(v->setNumber(Math.min(max,draft[0]+1)));
        input.addTextChangedListener(watcher(()->{raw=input.getText().toString();int value=EditorModel.parseInteger(raw,min,max);inputValid=value>=0;if(inputValid){draft[0]=value;if(CARD.equals(type))lastAuto=value;}errorMessage="";update();}));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean fromUser){if(fromUser)setNumber(min+p);}});
    }
    private TextWatcher watcher(Runnable r){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){}public void afterTextChanged(Editable e){if(!binding)r.run();}};}
    private void setNumber(int value){draft[0]=value;raw=String.valueOf(value);inputValid=true;errorMessage="";if(CARD.equals(type))lastAuto=value;binding=true;if(input!=null)input.setText(raw);binding=false;update();}
    private void update(){
        if(save==null)return;save.setEnabled(!busy()&&valid()&&dirty());save.setTextColor(save.isEnabled()?(RESET.equals(type)?Ui.RED:Ui.BLUE):Ui.MUTED);cancel.setEnabled(!busy());setCancelable(!busy());
        if(body!=null)setInputsEnabled(body,!busy());
        String message=busy()?"저장하고 있어요.":!errorMessage.isEmpty()?errorMessage:!inputValid?(TIME.equals(type)?"00:00–23:59:59 범위로 입력해 주세요.":min+"–"+max+" 사이의 정수를 입력해 주세요."):!valid()?"종료는 같은 날의 시작보다 늦어야 해요.":"";
        error.setText(message);error.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);
        if(TIME.equals(type))for(int i=0;i<2;i++)if(timeLabels[i]!=null){timeLabels[i].setText(timeLabel(draft[i]));((View)timeLabels[i].getParent()).setContentDescription((i==0?"시작":"종료")+" 시간 "+timeLabel(draft[i])+", 변경");}
        if(inputValid&&seek!=null)seek.setProgress(draft[0]-min);
    }
    private void setInputsEnabled(View view,boolean enabled){if(view!=error)view.setEnabled(enabled);if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)setInputsEnabled(((android.view.ViewGroup)view).getChildAt(i),enabled);}
    private void showError(String message){errorMessage=message;update();}
    private void settleWheels(){for(WheelColumn w:wheels)w.settle();}
    private void rebuild(){if(getDialog()!=null){build(getDialog());sizeWindow();}}
    private void commit(){
        if(busy())return;settleWheels();if(!valid()||!dirty())return;
        if(!Arrays.equals(original,host().editorValues(key))){showError("다른 변경이 있어요. 취소한 뒤 다시 열어 주세요.");return;}
        root.clearFocus();android.view.inputmethod.InputMethodManager imm=getActivity().getSystemService(android.view.inputmethod.InputMethodManager.class);if(imm!=null)imm.hideSoftInputFromWindow(root.getWindowToken(),0);
        jobId=SaveJobs.start(host().editorSave(key,original.clone(),draft.clone()));update();handler.post(poll);
    }
    private final Runnable poll=new Runnable(){public void run(){
        if(jobId==null||!isAdded()||getDialog()==null)return;SaveJobs.Job job=SaveJobs.get(jobId);
        if(job==null){jobId=null;original=host().editorValues(key);showError("지난 저장 결과를 확인할 수 없어 저장된 값을 다시 읽었어요. 확인 후 다시 조절해 주세요.");host().editorUpdated();return;}
        if(job.result==null){handler.postDelayed(this,100);return;}
        SaveJobs.Result result=job.result;SaveJobs.consume(jobId);jobId=null;host().editorUpdated();
        if(result.close){Activity a=getActivity();dismiss();SettingsSheet.message(a,result.message);}else{original=host().editorValues(key);showError(result.message);}
    }};
    @Override public void onStart(){super.onStart();sizeWindow();handler.removeCallbacks(poll);if(jobId!=null)handler.post(poll);}
    @Override public void onResume(){super.onResume();
        if(restoreInputFocus&&input!=null){final EditText restored=input;restored.post(()->{
            if(restored!=input||!isResumed())return;restored.setSelectAllOnFocus(false);restored.requestFocus();int n=restored.length();restored.setSelection(Math.max(0,Math.min(selectionStart,n)),Math.max(0,Math.min(selectionEnd,n)));restoreInputFocus=false;
        });}
    }
    private void sizeWindow(){
        if(getDialog()==null||getDialog().getWindow()==null)return;Window window=getDialog().getWindow();Activity a=getActivity();
        int width=a.getWindowManager().getCurrentWindowMetrics().getBounds().width();boolean wide=width/a.getResources().getDisplayMetrics().density>=600,reset=RESET.equals(type);
        window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(reset?Math.min(width-Ui.dp(a,48),Ui.dp(a,340)):wide?Ui.dp(a,440):width,-2);
        window.setGravity(reset||wide?Gravity.CENTER:Gravity.BOTTOM);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=window.getAttributes();p.dimAmount=.22f;p.y=0;window.setAttributes(p);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    @Override public void onStop(){handler.removeCallbacks(poll);super.onStop();}
    @Override public void onSaveInstanceState(Bundle state){settleWheels();state.putIntArray("before",original);state.putIntArray("draft",draft);state.putInt("active",active);state.putBoolean("seconds",seconds);state.putBoolean("direct",direct);state.putString("raw",raw);state.putBoolean("input_valid",inputValid);state.putInt("auto",lastAuto);state.putString("job",jobId);state.putString("error",errorMessage);state.putBoolean("input_focus",input!=null&&(inputHadFocus||input.hasFocus()));if(input!=null){state.putInt("selection_start",input.getSelectionStart());state.putInt("selection_end",input.getSelectionEnd());}super.onSaveInstanceState(state);}
    @Override public void onDismiss(DialogInterface dialog){super.onDismiss(dialog);if(getActivity() instanceof Host)host().editorUpdated();}
    @Override public void onDestroyView(){handler.removeCallbacks(poll);wheels.clear();body=null;root=null;save=null;cancel=null;input=null;super.onDestroyView();}
}
