package com.chocho.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.*;

/** Read-only information and navigation use the same grouped rhythm as the home. */
public final class DetailsContent extends LinearLayout {
    public DetailsContent(Context context){super(context);setOrientation(VERTICAL);}
    public LinearLayout section(String title){
        TextView heading=Ui.text(getContext(),title,13,Ui.MUTED,false);heading.setAccessibilityHeading(true);
        heading.setPadding(Ui.dp(getContext(),16),getChildCount()==0?0:Ui.dp(getContext(),24),Ui.dp(getContext(),16),Ui.dp(getContext(),7));addView(heading);
        LinearLayout group=Ui.group(getContext());addView(group,new LayoutParams(-1,-2));return group;
    }
    public Button action(LinearLayout group,String title,Runnable action){
        return action(group,title,true,action);
    }
    public Button action(LinearLayout group,String title,boolean navigates,Runnable action){
        if(group.getChildCount()>0)Ui.separator(group);Button b=Ui.navigation(getContext(),title,navigates);if(!navigates)b.setTextColor(Ui.BLUE);
        b.setOnClickListener(v->action.run());group.addView(b,new LayoutParams(-1,-2));return b;
    }
    public void paragraph(LinearLayout group,String title,String description){
        if(group.getChildCount()>0)Ui.separator(group);LinearLayout block=Ui.column(getContext());block.setPadding(Ui.dp(getContext(),16),Ui.dp(getContext(),13),Ui.dp(getContext(),16),Ui.dp(getContext(),13));
        TextView name=Ui.text(getContext(),title,16,Ui.INK,false);name.setAccessibilityHeading(true);block.addView(name);
        TextView detail=Ui.text(getContext(),description,14,Ui.MUTED,false);detail.setPadding(0,Ui.dp(getContext(),6),0,0);block.addView(detail);group.addView(block);
    }
    public Summary summary(LinearLayout group,String text){Summary s=new Summary(getContext());group.addView(s,new LayoutParams(-1,-2));s.setText(text);return s;}
    public static final class Summary extends LinearLayout {
        private String previous;
        public Summary(Context context){super(context);setOrientation(VERTICAL);}
        public void setText(String text){
            if(text.equals(previous))return;previous=text;removeAllViews();
            for(String line:text.split("\n")){
                if(line.trim().isEmpty())continue;if(getChildCount()>0)Ui.separator(this);
                int colon=line.indexOf(':');String label=colon>0?line.substring(0,colon):line,value=colon>0?line.substring(colon+1).trim():null;
                LinearLayout row=Ui.column(getContext());row.setPadding(Ui.dp(getContext(),16),Ui.dp(getContext(),12),Ui.dp(getContext(),16),Ui.dp(getContext(),12));
                boolean inline=value!=null&&value.length()<12&&getResources().getConfiguration().fontScale<=1.25f&&getResources().getConfiguration().screenWidthDp>=360;
                if(inline){row.setOrientation(HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);}
                TextView name=Ui.text(getContext(),label,15,Ui.INK,false);row.addView(name,inline?new LayoutParams(0,-2,1):new LayoutParams(-1,-2));
                if(value!=null){TextView detail=Ui.text(getContext(),value,15,Ui.MUTED,false);detail.setFontFeatureSettings("tnum");detail.setPadding(inline?Ui.dp(getContext(),12):0,inline?0:Ui.dp(getContext(),4),0,0);row.addView(detail);}
                addView(row,new LayoutParams(-1,-2));
            }
        }
    }
}
