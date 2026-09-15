package com.chocho.airpodsglance;

import com.chocho.airpodsglance.ListeningProtocol.Mode;
import java.util.ArrayList;

/** Independent, user-chosen cycle order. Never uses a saved display value as a live observation. */
public final class ListeningWidgetOptions {
    private final Mode[] order;
    public ListeningWidgetOptions(Mode... values){
        if(values==null||values.length<2||values.length>3)throw new IllegalArgumentException("Choose two or three modes");
        for(int i=0;i<values.length;i++){
            if(!ListeningProtocol.canWrite(values[i]))throw new IllegalArgumentException("Unsupported mode");
            for(int j=0;j<i;j++)if(values[i]==values[j])throw new IllegalArgumentException("Duplicate mode");
        }
        order=values.clone();
    }
    public static ListeningWidgetOptions defaults(){return new ListeningWidgetOptions(Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY);}
    public Mode[] modes(){return order.clone();}
    public Mode next(Mode observed){
        if(observed==null||observed==Mode.UNKNOWN)return Mode.UNKNOWN;
        for(int i=0;i<order.length;i++)if(order[i]==observed)return order[(i+1)%order.length];
        return order[0];
    }
    public String encode(){StringBuilder s=new StringBuilder();for(Mode m:order){if(s.length()>0)s.append(',');s.append(m.wire);}return s.toString();}
    public static ListeningWidgetOptions decode(String value){
        try{String[] parts=value.split(",",-1);ArrayList<Mode> parsed=new ArrayList<>();for(String p:parts)parsed.add(Mode.fromWire(Integer.parseInt(p)));return new ListeningWidgetOptions(parsed.toArray(new Mode[0]));}
        catch(RuntimeException ignored){return defaults();}
    }
}
