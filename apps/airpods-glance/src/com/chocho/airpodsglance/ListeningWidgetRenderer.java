package com.chocho.airpodsglance;

import android.content.Context;import android.content.res.Configuration;
import android.graphics.*;import android.text.format.DateFormat;import android.util.TypedValue;import android.view.View;import android.widget.RemoteViews;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningWidgetProvider.Kind;

/** Read-only presentation. Artwork is shared with the app, never a Bluetooth polling surface. */
public final class ListeningWidgetRenderer {
    public static String history(Context c,ListeningWidgetStore.Record record){
        return record.mode==Mode.UNKNOWN||record.at<=0?"상태 확인 전":record.mode.label+", "+DateFormat.getDateFormat(c).format(new java.util.Date(record.at))+" "+DateFormat.getTimeFormat(c).format(new java.util.Date(record.at))+" 마지막 확인";
    }
    public static RemoteViews render(Context c,ListeningWidgetStore.Record record,ListeningWidgetOptions options,Kind kind,int width,int height){
        boolean dark=(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        int ink=dark?0xfff2f2f7:0xff1c1c1e,muted=dark?0xffaeaeb2:0xff636366,blue=dark?0xff64aaff:0xff007aff;
        String history=history(c,record);
        if(kind.cycle()){
            RemoteViews r=new RemoteViews(c.getPackageName(),R.layout.listening_widget_single);
            int size=Math.max(48,Math.min(80,Math.min(width,height)));
            r.setViewLayoutWidth(R.id.listening_single,size,TypedValue.COMPLEX_UNIT_DIP);r.setViewLayoutHeight(R.id.listening_single,size,TypedValue.COMPLEX_UNIT_DIP);
            r.setImageViewBitmap(R.id.listening_single,art(record.mode,kind==Kind.LOCK?0xffffffff:ink,true,kind==Kind.LOCK,dark));
            r.setContentDescription(R.id.listening_single,"소음 제어, "+history+". 누르면 현재 모드를 확인한 뒤 "+order(options)+" 순서로 전환. "+record.note);
            return r;
        }
        RemoteViews r=new RemoteViews(c.getPackageName(),R.layout.listening_widget_wide);
        boolean labels=kind==Kind.WIDE&&height>=72&&width>=210&&c.getResources().getConfiguration().fontScale<=1.35f;
        Mode[] modes={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};int[] rows={R.id.listening_anc,R.id.listening_adaptive,R.id.listening_transparency};
        int[] icons={R.id.listening_anc_icon,R.id.listening_adaptive_icon,R.id.listening_transparency_icon};int[] texts={R.id.listening_anc_text,R.id.listening_adaptive_text,R.id.listening_transparency_text};
        String[] shortNames={"노캔","적응형","주변음"};
        for(int i=0;i<3;i++){
            r.setImageViewBitmap(icons[i],art(modes[i],record.mode==modes[i]?blue:ink,false,false,dark));
            r.setTextViewText(texts[i],shortNames[i]);r.setTextColor(texts[i],record.mode==modes[i]?blue:ink);r.setViewVisibility(texts[i],labels?View.VISIBLE:View.GONE);
            r.setContentDescription(rows[i],modes[i].label+"으로 전환. "+history+". "+record.note);
            r.setBoolean(rows[i],"setEnabled",width>=144);
        }
        r.setViewVisibility(R.id.listening_history,labels?View.VISIBLE:View.GONE);r.setTextColor(R.id.listening_history,muted);
        r.setTextViewText(R.id.listening_history,record.mode==Mode.UNKNOWN?"눌러서 상태 확인": "마지막 · "+shortName(record.mode));
        return r;
    }
    private static String shortName(Mode mode){return mode==Mode.ANC?"노캔":mode==Mode.TRANSPARENCY?"주변음":mode.label;}
    public static String order(ListeningWidgetOptions options){StringBuilder s=new StringBuilder();for(Mode m:options.modes()){if(s.length()>0)s.append(" → ");s.append(shortName(m));}return s.toString();}
    public static Bitmap art(Mode mode,int color,boolean circle,boolean lock,boolean dark){
        int size=circle?192:96;Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(size/80f,size/80f);
        Paint p=new Paint(3);if(circle&&!lock){p.setColor(dark?0xff2c2c2e:0xfffafafa);c.drawCircle(40,40,39,p);}
        // Mode artwork only. The old cycle arrow and history badge looked like refresh controls.
        float side=circle?52:66,left=(80-side)/2,top=(80-side)/2;
        c.save();c.translate(left,top);new ListeningArtwork().draw(c,mode,color,side,side);c.restore();
        return b;
    }
}
