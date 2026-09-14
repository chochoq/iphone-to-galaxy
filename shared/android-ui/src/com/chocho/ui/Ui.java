package com.chocho.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Project-defined tokens; not imported Apple fonts, artwork, or measured UIKit dimensions. */
public final class Ui {
    public static final int BG=0xfff2f2f7,PAPER=0xffffffff,INK=0xff1c1c1e,MUTED=0xff636366;
    public static final int BLUE=0xff0066cc,GREEN=0xff34c759,RED=0xffc9342d,LINE=0xffe5e5ea;
    private Ui(){}
    public static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static float sp(Context c,float n){return n*c.getResources().getDisplayMetrics().scaledDensity;}
    public static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    public static android.graphics.drawable.StateListDrawable pressed(Context c,int color,int radius){
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},rounded(c,color,radius));
        states.addState(new int[]{android.R.attr.state_pressed},rounded(c,0xffe6edf7,radius));
        states.addState(new int[]{android.R.attr.state_focused},rounded(c,0xffe6edf7,radius));
        states.addState(new int[]{},rounded(c,color,radius));return states;
    }
    public static TextView text(Context c,String text,int size,int color,boolean bold){
        TextView t=new TextView(c);t.setText(text);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",0));t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(c,3),1);t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        if(android.os.Build.VERSION.SDK_INT>=33)t.setLineBreakWordStyle(android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE);
        return t;
    }
    public static Button button(Context c,String text,int color){
        Button b=new Button(c);b.setText(text);b.setTextColor(color);b.setTextSize(17);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif",0));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));
        b.setStateListAnimator(null);b.setBackground(pressed(c,PAPER,8));return b;
    }
    public static Button navigation(Context c,String label,boolean chevron){
        Button b=button(c,label.replace("›","").trim(),INK);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        b.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));b.setCompoundDrawablePadding(dp(c,12));
        if(chevron)b.setCompoundDrawablesRelative(null,null,new TrailingMark(c,false),null);return b;
    }
    /** Keep short Korean words whole in read-only alerts without inserting invisible characters. */
    public static void keepAlertWords(TextView view,int availablePixels){
        android.text.SpannableString text=new android.text.SpannableString(view.getText());
        java.util.regex.Matcher words=java.util.regex.Pattern.compile("\\S+").matcher(text);
        while(words.find()){
            String word=words.group();if(!word.matches(".*[가-힣].*")||view.getPaint().measureText(word)>availablePixels)continue;
            text.setSpan(new android.text.style.ReplacementSpan(){
                @Override public int getSize(android.graphics.Paint paint,CharSequence value,int start,int end,android.graphics.Paint.FontMetricsInt metrics){
                    if(metrics!=null)paint.getFontMetricsInt(metrics);return (int)Math.ceil(paint.measureText(value,start,end));
                }
                @Override public void draw(android.graphics.Canvas canvas,CharSequence value,int start,int end,float x,int top,int y,int bottom,android.graphics.Paint paint){canvas.drawText(value,start,end,x,y,paint);}
            },words.start(),words.end(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        view.setText(text);
    }
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout.LayoutParams space(Context c,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(c,bottom);return p;}
    public static void separator(LinearLayout group){
        View v=new View(group.getContext());v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,Math.max(1,dp(group.getContext(),.5f)));p.leftMargin=dp(group.getContext(),16);group.addView(v,p);
    }
    public static LinearLayout group(Context c){LinearLayout l=column(c);l.setBackground(rounded(c,PAPER,12));l.setClipToOutline(true);return l;}
    public static TextView footnote(Context c,String s){TextView t=text(c,s,13,MUTED,false);t.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,4));return t;}
}
