package com.chocho.airpodsglance;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import java.util.*;
public final class ListeningOrderDraftTests {
    private static int checks;
    private static void yes(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args){
        Mode[] all={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};
        for(Mode a:all)for(Mode b:all)if(a!=b){test(new ListeningWidgetOptions(a,b));for(Mode c:all)if(c!=a&&c!=b)test(new ListeningWidgetOptions(a,b,c));}
        System.out.println("PASS ListeningOrderDraftTests "+checks+" checks");
    }
    private static void test(ListeningWidgetOptions options){
        ListeningOrderDraft draft=new ListeningOrderDraft(options);yes(!draft.changed(),"clean initial");yes(draft.original().equals(options.encode()),"original retained");
        for(Mode mode:Mode.values())for(int target=-2;target<5;target++){
            ListeningOrderDraft copy=new ListeningOrderDraft(options);int before=copy.position(mode);
            boolean expected=before>=0&&target>=0&&target<copy.size()&&before!=target;
            yes(copy.move(mode,target)==expected,"move range");yes(copy.changed()==expected,"dirty matches move");
            if(expected){yes(copy.position(mode)==target,"target position");yes(copy.move(mode,before),"return move");yes(!copy.changed(),"return original clean");}
            valid(copy);
        }
        for(Mode mode:Mode.values()){
            ListeningOrderDraft copy=new ListeningOrderDraft(options);boolean selected=copy.contains(mode);
            boolean expected=ListeningProtocol.canWrite(mode)&&(!selected||copy.size()>2);
            yes(copy.toggle(mode)==expected,"toggle gate");if(expected){yes(copy.contains(mode)!=selected,"toggle state");yes(copy.changed(),"toggle dirty");}
            valid(copy);
            ListeningOrderDraft restored=new ListeningOrderDraft(ListeningWidgetOptions.decode(copy.original()),copy.options());
            yes(restored.options().encode().equals(copy.options().encode()),"draft restored");yes(restored.changed()==copy.changed(),"original restored independently");
        }
        Mode[] rows=draft.rows();rows[0]=Mode.UNKNOWN;yes(draft.rows()[0]!=Mode.UNKNOWN,"defensive rows");
    }
    private static void valid(ListeningOrderDraft draft){
        yes(draft.size()>=2&&draft.size()<=3,"two or three");Mode[] rows=draft.rows();yes(rows.length==3,"one list three rows");
        yes(new HashSet<>(Arrays.asList(rows)).size()==3,"no duplicates");
        for(int i=0;i<draft.size();i++)yes(rows[i]==draft.options().modes()[i],"selected prefix");
    }
}
