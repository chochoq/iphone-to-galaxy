package com.chocho.airpodsglance;

import java.util.ArrayList;
import java.util.Arrays;
import com.chocho.airpodsglance.ListeningProtocol.Mode;
import com.chocho.airpodsglance.ListeningState.Phase;
import com.chocho.airpodsglance.ListeningState.End;

public final class ListeningTests {
    private static int count;
    private static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    private static byte[] packet(int m){return new byte[]{4,0,4,0,9,0,13,(byte)m,0,0,0};}
    private static ListeningProtocol.Report report(int m){return ListeningProtocol.decode(packet(m));}
    static final class Clock implements AapConnectionOwner.Clock {long at=100;public long now(){return at;}}
    static final class Wire implements AapConnectionOwner.Transport {
        final AapConnectionOwner.Receiver receiver;int starts,stops,writes;boolean accept=true;
        Wire(AapConnectionOwner.Receiver r){receiver=r;}
        public void start(){starts++;}public void stop(){stops++;}
        public boolean write(Mode mode){writes++;check(ListeningProtocol.canWrite(mode),"wire allowlist");return accept;}
    }
    static final class Factory implements AapConnectionOwner.Factory {
        final ArrayList<Wire> wires=new ArrayList<>();
        public AapConnectionOwner.Transport create(String address,AapConnectionOwner.Receiver r){Wire w=new Wire(r);wires.add(w);return w;}
        Wire last(){return wires.get(wires.size()-1);}
    }
    static final class Seen implements AapConnectionOwner.Listener {
        ListeningState.Snapshot state;int states,batteries;AapConnectionOwner.BatteryReport lastBattery;
        public void state(ListeningState.Snapshot s){state=s;states++;}
        public void battery(AapConnectionOwner.BatteryReport b){batteries++;lastBattery=b;}
    }
    public static void main(String[] args){
        check(ListeningProtocol.decode(null).kind==ListeningProtocol.Kind.OTHER,"null");
        for(int m=0;m<256;m++){
            check((report(m).kind==ListeningProtocol.Kind.MODE)==(m>=1&&m<=4),"mode domain");
            check(report(m).mode.wire==(m>=1&&m<=4?m:0),"no clamp");
        }
        for(int n=0;n<30;n++)check((ListeningProtocol.decode(Arrays.copyOf(packet(2),n)).kind==ListeningProtocol.Kind.MODE)==(n==11),"exact length");
        for(int i=0;i<11;i++)if(i!=7){byte[] b=packet(2);b[i]^=1;check(ListeningProtocol.decode(b).kind!=ListeningProtocol.Kind.MODE,"framing mutation");}
        for(Mode m:Mode.values()){
            if(ListeningProtocol.canWrite(m))check(Arrays.equals(ListeningProtocol.command(m),packet(m.wire)),"wire vector");
            else{boolean rejected=false;try{ListeningProtocol.command(m);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"unverified writes disabled");}
        }
        ListeningState s=new ListeningState();check(!s.request(Mode.ANC,100,30100),"no state");s.ready();
        s.observe(report(2),110);check(!s.request(Mode.ANC,120,30100),"same");
        check(!s.request(Mode.OFF,120,30100),"OFF remains outside allowlist");
        check(s.request(Mode.TRANSPARENCY,120,30100),"first request");
        check(s.snapshot().observed==Mode.ANC&&s.snapshot().phase==Phase.APPLYING,"no optimistic state");
        check(!s.request(Mode.ANC,125,30100),"pending rejects");
        s.observe(report(3),115);check(s.snapshot().phase==Phase.APPLYING,"queued pre-request report not confirmation");
        s.observe(report(3),130);check(s.snapshot().phase==Phase.READY,"target report");
        check(s.request(Mode.ANC,140,30100),"next explicit request");
        s.observe(report(3),135);check(s.snapshot().phase==Phase.APPLYING,"other report");
        s.observe(report(2),150);check(s.snapshot().phase==Phase.READY,"reverse response");
        s.observe(report(3),149);check(s.snapshot().observed==Mode.ANC,"older observation ignored");
        s.observe(report(255),160);check(s.snapshot().observed==Mode.UNKNOWN&&!s.request(Mode.TRANSPARENCY,170,30100),"unknown not usable");
        s.close(End.DISCONNECTED);s.observe(report(3),200);check(s.snapshot().observed==Mode.UNKNOWN,"closed ignores report");

        Clock clock=new Clock();Factory f=new Factory();AapConnectionOwner owner=new AapConnectionOwner(clock,f);
        Seen battery=new Seen(),ui=new Seen();long b=owner.acquire("device-a",false,battery),u=owner.acquire("device-a",true,ui);
        check(f.wires.size()==1&&f.last().starts==1&&owner.clientCount()==2,"one shared connection");
        Wire first=f.last();first.receiver.ready();clock.at=110;first.receiver.listening(report(2),110);
        check(battery.state.observed==Mode.ANC&&ui.state.observed==Mode.ANC,"shared current");
        check(!owner.request(b,Mode.TRANSPARENCY),"battery lease read only");
        clock.at=120;check(owner.request(u,Mode.TRANSPARENCY)&&first.writes==1,"one write");
        check(!owner.request(u,Mode.TRANSPARENCY)&&first.writes==1,"no duplicate");
        clock.at=130;first.receiver.listening(report(3),130);check(ui.state.phase==Phase.READY,"real report");
        owner.release(b);check(first.stops==0&&owner.contains(u),"battery ending preserves UI");
        owner.release(u);check(first.stops==1&&!owner.active(),"last lease closes");
        first.receiver.listening(report(2),200);check(!owner.active(),"old callback ignored");

        clock.at=1000;b=owner.acquire("device-a",false,battery);u=owner.acquire("device-a",true,ui);
        Wire second=f.last();owner.release(u);check(second.stops==0&&owner.contains(b),"UI ending preserves battery");
        clock.at=30_999;owner.acquire("device-a",true,ui);check(f.wires.size()==2,"joining does not reopen");
        clock.at=31_000;owner.tick();check(second.stops==1&&ui.state.end==End.EXPIRED,"hard window not extended");

        clock.at=40_000;u=owner.acquire("device-a",true,ui);Wire old=f.last();old.receiver.ready();
        clock.at=40_010;old.receiver.listening(report(3),clock.at);
        Seen replacement=new Seen();long next=owner.acquire("device-b",true,replacement);Wire current=f.last();
        check(old.stops==1&&!owner.contains(u)&&owner.contains(next),"target changes generation");
        int states=replacement.states;old.receiver.ready();old.receiver.listening(report(2),40_020);old.receiver.failed();
        check(replacement.states==states&&current.stops==0,"old callbacks cannot affect new target");
        current.receiver.ready();current.receiver.listening(report(3),40_020);clock.at=40_030;
        check(owner.request(next,Mode.ANC),"reverse request accepted");
        clock.at=45_029;owner.tick();check(owner.active(),"before reply deadline");
        clock.at=45_030;owner.tick();check(!owner.active()&&replacement.state.end==End.TIMEOUT&&current.stops==1,"timeout closes");
        current.receiver.listening(report(2),45_040);check(replacement.state.observed==Mode.TRANSPARENCY,"late timeout report ignored");
        check(current.writes==1&&!owner.request(next,Mode.ANC),"no timeout retry");

        clock.at=50_000;u=owner.acquire("device-b",true,ui);current=f.last();current.receiver.ready();
        current.receiver.listening(report(3),50_010);clock.at=75_001;
        check(!owner.request(u,Mode.ANC)&&current.writes==0,"insufficient reply window");
        owner.close(End.DISCONNECTED);check(ui.state.end==End.DISCONNECTED,"disconnect");
        clock.at=80_000;u=owner.acquire("device-b",true,ui);current=f.last();
        check(current.writes==0&&ui.state.requested==Mode.UNKNOWN,"no reconnect replay");
        current.receiver.ready();current.receiver.listening(report(3),80_010);clock.at=80_020;current.accept=false;
        check(!owner.request(u,Mode.ANC)&&ui.state.end==End.FAILED,"write rejection closes");

        // Detached observers cannot receive another listener's reentrant close/reopen.
        final AapConnectionOwner reentrant=new AapConnectionOwner(clock,new Factory());
        final long[] id={0};final Seen survivor=new Seen();
        id[0]=reentrant.acquire("device-a",false,new Seen());
        long keep=reentrant.acquire("device-a",true,survivor);
        reentrant.release(id[0]);check(reentrant.contains(keep),"released lease does not close survivor");
        reentrant.close(End.RELEASED);
        Factory reFactory=new Factory();AapConnectionOwner r=new AapConnectionOwner(clock,reFactory);
        r.acquire("device-a",true,new AapConnectionOwner.Listener(){
            public void state(ListeningState.Snapshot value){if(value.live())r.close(End.RELEASED);}
        });
        check(reFactory.wires.isEmpty()&&!r.active(),"reentrant initial close never creates transport");
        final long[] victim={0};Seen stale=new Seen();
        r.acquire("device-a",false,new AapConnectionOwner.Listener(){
            public void battery(AapConnectionOwner.BatteryReport ignored){r.release(victim[0]);}
        });
        victim[0]=r.acquire("device-a",true,stale);
        reFactory.last().receiver.ready();
        AapBatteryDecoder.Decoded actualBattery=AapBatteryDecoder.decode(new byte[]{4,0,4,0,4,0,1,4,0,75,2,0}).decoded;
        reFactory.last().receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,clock.at,123_456));
        check(stale.batteries==0,"reentrant release skips stale battery recipient");
        r.close(End.RELEASED);
        r.acquire("device-a",false,new Seen());
        long control=r.acquire("device-a",true,new Seen());Wire pendingWire=reFactory.last();
        pendingWire.receiver.ready();pendingWire.receiver.listening(report(3),clock.at);
        check(r.request(control,Mode.ANC),"pending authorization");r.release(control);
        check(pendingWire.stops==1&&!r.active(),"foreground exit closes a pending-write window even with battery owner");
        check(pendingWire.writes==1,"foreground exit never replays or restores");
        r.acquire("device-a",false,new Seen());long cancel=r.acquire("device-a",true,new AapConnectionOwner.Listener(){
            public void state(ListeningState.Snapshot value){if(value.phase==Phase.APPLYING)r.close(End.RELEASED);}
        });
        Wire canceled=reFactory.last();canceled.receiver.ready();canceled.receiver.listening(report(3),clock.at);
        check(!r.request(cancel,Mode.ANC)&&canceled.writes==0,"reentrant pending close cancels before transport write");
        r.close(End.RELEASED);
        r.acquire("device-a",true,new Seen());reFactory.last().receiver.ready();
        long observedAt=clock.at;
        reFactory.last().receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,observedAt,123_456));
        clock.at+=4_000;
        Seen lateBattery=new Seen();r.acquire("device-a",false,lateBattery);
        check(lateBattery.batteries==1,"late battery owner receives this connection's already-observed battery");
        check(lateBattery.lastBattery.value==actualBattery&&lateBattery.lastBattery.receivedAt==observedAt
                &&lateBattery.lastBattery.observedAtEpochMillis==123_456,"cached report retains both original clocks and data");
        Wire batteryWire=reFactory.last();
        batteryWire.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,observedAt,123_999));
        batteryWire.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,clock.at+1,123_999));
        batteryWire.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,observedAt-1,123_999));
        check(lateBattery.batteries==1,"duplicate, future, and older battery reports rejected");
        r.close(End.DISCONNECTED);Seen freshBattery=new Seen();r.acquire("device-a",false,freshBattery);
        check(freshBattery.batteries==0,"new connection never replays previous battery");
        batteryWire.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,clock.at,124_000));
        check(freshBattery.batteries==0,"old transport cannot populate new battery cache");
        Wire newest=reFactory.last();newest.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,clock.at,124_000));
        check(freshBattery.batteries==0,"battery before handshake ready rejected");
        newest.receiver.ready();newest.receiver.battery(new AapConnectionOwner.BatteryReport(actualBattery,clock.at,124_000));
        check(freshBattery.batteries==1,"fresh battery delivered once");
        clock.at+=AapConnectionOwner.WINDOW_MS;
        Seen expired=new Seen();r.acquire("device-a",false,expired);
        check(expired.batteries==0,"expired connection cache cleared before new acquire");
        // D-022: every ordered pair now includes adaptive, without optimistic state or retries.
        for(Mode from:new Mode[]{Mode.ANC,Mode.TRANSPARENCY,Mode.ADAPTIVE})
            for(Mode to:new Mode[]{Mode.ANC,Mode.TRANSPARENCY,Mode.ADAPTIVE}){
                ListeningState change=new ListeningState();change.ready();change.observe(report(from.wire),100);
                if(from==to){check(!change.request(to,110,30100),"same mode never writes: "+from);continue;}
                check(change.request(to,110,30100),"mode pair accepted: "+from+" to "+to);
                check(change.snapshot().observed==from&&change.snapshot().requested==to,"pair not optimistic");
                check(!change.request(to,111,30100),"pair duplicate rejected");
                change.observe(report(to.wire),109);check(change.snapshot().phase==Phase.APPLYING,"older target not confirmation");
                change.observe(report(to.wire),120);check(change.snapshot().phase==Phase.READY&&change.snapshot().observed==to,"fresh pair target");
                change.close(End.DISCONNECTED);check(!change.request(from,130,30100),"pair no disconnected retry");
            }
        r.close(End.RELEASED);Seen adaptiveUi=new Seen();
        long adaptiveControl=r.acquire("device-a",true,adaptiveUi);Wire adaptiveWire=reFactory.last();adaptiveWire.receiver.ready();
        adaptiveWire.receiver.listening(report(3),clock.at);
        check(r.request(adaptiveControl,Mode.ADAPTIVE)&&adaptiveWire.writes==1,"shared owner writes adaptive once");
        clock.at+=ListeningState.REPLY_MS;r.tick();
        adaptiveWire.receiver.listening(report(4),clock.at);
        check(!r.active()&&adaptiveUi.state.end==End.TIMEOUT&&adaptiveUi.state.observed==Mode.TRANSPARENCY,"late adaptive RX cannot confirm timeout");
        check(adaptiveWire.writes==1&&!r.request(adaptiveControl,Mode.ADAPTIVE),"adaptive timeout no replay");
        System.out.println("Listening product contracts: "+count+" assertions passed");
    }
}
