package com.chocho.airpodsglance;

import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Observations, explicit requests and connection validity are different facts. */
public final class ListeningState {
    public static final long REPLY_MS=5_000;
    public enum Phase { CHECKING, READY, APPLYING, CLOSED }
    public enum End { NONE, EXPIRED, DISCONNECTED, FAILED, TIMEOUT, RELEASED, TARGET_CHANGED }
    public static final class Snapshot {
        public final Phase phase;public final End end;
        public final Mode observed,requested;
        public final long receivedAt;public final int requests,reports;
        Snapshot(Phase p,End e,Mode o,Mode r,long at,int requests,int reports){
            phase=p;end=e;observed=o;requested=r;receivedAt=at;this.requests=requests;this.reports=reports;
        }
        public boolean live(){return phase!=Phase.CLOSED;}
    }
    private Phase phase=Phase.CHECKING;private End end=End.NONE;
    private Mode observed=Mode.UNKNOWN,requested=Mode.UNKNOWN;
    private boolean transportReady;private long receivedAt=-1,requestedAt=-1;
    private int requests,reports;
    public void ready(){if(phase!=Phase.CLOSED)transportReady=true;}
    public boolean observe(ListeningProtocol.Report r,long at){
        if(phase==Phase.CLOSED||!transportReady||r==null||r.kind==ListeningProtocol.Kind.OTHER||at<receivedAt)return false;
        receivedAt=at;
        if(r.kind==ListeningProtocol.Kind.MALFORMED){observed=Mode.UNKNOWN;if(phase!=Phase.APPLYING)phase=Phase.CHECKING;return true;}
        observed=r.mode;reports++;
        if(phase==Phase.APPLYING){
            if(observed==requested&&at>=requestedAt){requested=Mode.UNKNOWN;phase=Phase.READY;}
        }else phase=Phase.READY;
        return true;
    }
    public boolean request(Mode target,long now,long connectionDeadline){
        if(phase!=Phase.READY||!transportReady||!ListeningProtocol.canWrite(target)||target==observed
                ||observed==Mode.UNKNOWN||now<receivedAt||connectionDeadline-now<REPLY_MS)return false;
        requested=target;requestedAt=now;requests++;phase=Phase.APPLYING;return true;
    }
    public boolean timedOut(long now){return phase==Phase.APPLYING&&now-requestedAt>=REPLY_MS;}
    public void close(End reason){if(phase==Phase.CLOSED)return;phase=Phase.CLOSED;end=reason;transportReady=false;}
    public Snapshot snapshot(){return new Snapshot(phase,end,observed,requested,receivedAt,requests,reports);}
}
