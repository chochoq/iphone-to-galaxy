package com.chocho.airpodsglance;

import com.chocho.airpodsglance.ListeningProtocol.Mode;
import java.util.ArrayList;
import java.util.Arrays;

/** Pure editor state. No preferences, Bluetooth, or widget updates. */
public final class ListeningOrderDraft {
    private static final Mode[] ALL={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};
    private final String original;
    private final ArrayList<Mode> selected=new ArrayList<>();
    public ListeningOrderDraft(ListeningWidgetOptions initial){this(initial,initial);}
    public ListeningOrderDraft(ListeningWidgetOptions initial,ListeningWidgetOptions draft){
        original=initial.encode();selected.addAll(Arrays.asList(draft.modes()));
    }
    public String original(){return original;}
    public ListeningWidgetOptions options(){return new ListeningWidgetOptions(selected.toArray(new Mode[0]));}
    public boolean changed(){return !original.equals(options().encode());}
    public boolean contains(Mode mode){return selected.contains(mode);}
    public int position(Mode mode){return selected.indexOf(mode);}
    public int size(){return selected.size();}
    public Mode[] rows(){ArrayList<Mode> rows=new ArrayList<>(selected);for(Mode m:ALL)if(!rows.contains(m))rows.add(m);return rows.toArray(new Mode[0]);}
    public boolean toggle(Mode mode){
        if(!ListeningProtocol.canWrite(mode))return false;
        if(contains(mode)){if(selected.size()==2)return false;selected.remove(mode);}else selected.add(mode);
        return true;
    }
    public boolean move(Mode mode,int target){
        int from=position(mode);if(from<0||target<0||target>=size()||from==target)return false;
        selected.remove(from);selected.add(target,mode);return true;
    }
}
