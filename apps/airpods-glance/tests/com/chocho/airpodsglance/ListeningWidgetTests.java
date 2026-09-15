package com.chocho.airpodsglance;

import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningWidgetRequest.Step;
public final class ListeningWidgetTests {
    static int checks;
    static void check(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
    static ListeningState state(Mode mode,long at){ListeningState s=new ListeningState();s.ready();s.observe(ListeningProtocol.decode(new byte[]{4,0,4,0,9,0,13,(byte)mode.wire,0,0,0}),at);return s;}
    static void invalid(Runnable r){boolean rejected=false;try{r.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid options/request rejected");}
    public static void main(String[] args){
        Mode[] supported={Mode.ANC,Mode.ADAPTIVE,Mode.TRANSPARENCY};int orders=0;
        for(Mode a:supported)for(Mode b:supported)if(a!=b){
            ListeningWidgetOptions two=new ListeningWidgetOptions(a,b);testOrder(two);orders++;
            for(Mode c:supported)if(c!=a&&c!=b){testOrder(new ListeningWidgetOptions(a,b,c));orders++;}
        }
        check(orders==12,"all two/three-mode permutations");
        for(String bad:new String[]{null,"","2","2,2","0,2","1,2","2,3,4,2","a,3","2,3,"})check(ListeningWidgetOptions.decode(bad).encode().equals("2,4,3"),"invalid saved options fallback");
        invalid(()->new ListeningWidgetOptions(Mode.ANC));invalid(()->new ListeningWidgetOptions(Mode.ANC,Mode.OFF));
        invalid(()->new ListeningWidgetOptions(Mode.ANC,Mode.ANC));invalid(()->new ListeningWidgetOptions(Mode.ANC,null));
        invalid(()->new ListeningWidgetRequest(0,Mode.OFF,ListeningWidgetOptions.defaults()));
        for(Mode current:Mode.values())for(Mode direct:Mode.values())if(direct==Mode.UNKNOWN||ListeningProtocol.canWrite(direct)){
            ListeningWidgetRequest request=new ListeningWidgetRequest(100,direct,ListeningWidgetOptions.defaults());
            ListeningState s=state(current,99);
            check(request.observe(s.snapshot(),100)==Step.WAIT,"pre-tap cached report never cycles");
            s=state(current,101);Step first=request.observe(s.snapshot(),101);
            if(current==Mode.UNKNOWN){check(first==Step.WAIT,"unknown no write");continue;}
            Mode expected=direct==Mode.UNKNOWN?ListeningWidgetOptions.defaults().next(current):direct;
            if(expected==current){check(first==Step.SUCCESS&&!request.hasSent(),"same mode no write");}
            else {
                check(first==Step.SEND&&request.target()==expected,"one requested target");
                for(int n=0;n<10;n++)check(request.observe(s.snapshot(),102+n)==Step.WAIT,"duplicate reports no writes");
                check(request.observe(state(expected,100).snapshot(),110)==Step.WAIT,"old target not success");
                check(request.observe(state(expected,111).snapshot(),111)==Step.SUCCESS,"new target success");
            }
            check(request.observe(state(expected,112).snapshot(),112)==Step.WAIT,"terminal no replay");
        }
        for(int offset=0;offset<=16000;offset+=50){ListeningWidgetRequest r=new ListeningWidgetRequest(100,Mode.UNKNOWN,ListeningWidgetOptions.defaults());
            Step step=r.observe(state(Mode.ANC,100+offset).snapshot(),100+offset);
            check(step==(offset<=10000?Step.SEND:Step.FAILURE),"remaining confirmation time");}
        ListeningWidgetRequest late=new ListeningWidgetRequest(100,Mode.UNKNOWN,ListeningWidgetOptions.defaults());
        check(late.observe(state(Mode.ANC,200).snapshot(),199)==Step.WAIT,"future report no write");
        check(late.observe(null,99)==Step.FAILURE,"clock rollback fails");
        for(ListeningState.End end:ListeningState.End.values())if(end!=ListeningState.End.NONE){ListeningWidgetRequest r=new ListeningWidgetRequest(100,Mode.UNKNOWN,ListeningWidgetOptions.defaults());ListeningState s=state(Mode.ANC,101);s.close(end);check(r.observe(s.snapshot(),102)==Step.FAILURE,"closed no replay");}
        ListeningWidgetRequest canceled=new ListeningWidgetRequest(100,Mode.UNKNOWN,ListeningWidgetOptions.defaults());canceled.cancel();check(canceled.observe(state(Mode.ANC,101).snapshot(),101)==Step.WAIT,"cancel no write");
        System.out.println("PASS ListeningWidgetTests "+checks+" checks");
    }
    static void testOrder(ListeningWidgetOptions option){Mode[] copy=option.modes();Mode first=copy[0];check(option.next(Mode.UNKNOWN)==Mode.UNKNOWN,"unknown never guesses");check(option.next(null)==Mode.UNKNOWN,"null never guesses");
        check(option.next(Mode.OFF)==first,"known OFF starts order");
        Mode current=first;for(int i=0;i<copy.length;i++){check(option.next(copy[i])==copy[(i+1)%copy.length],"order adjacency");current=option.next(current);}check(current==first,"cycle returns");
        check(ListeningWidgetOptions.decode(option.encode()).encode().equals(option.encode()),"save roundtrip");copy[0]=Mode.UNKNOWN;check(option.modes()[0]==first,"defensive copy");}
}
