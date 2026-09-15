package com.chocho.airpodsglance;

import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** One tap, one fresh observation, at most one write. This object is deliberately never persisted. */
public final class ListeningWidgetRequest {
    public static final long LIMIT_MS=15_000;
    public enum Step { WAIT, SEND, SUCCESS, FAILURE }
    private final long started;
    private final Mode explicit;
    private final ListeningWidgetOptions options;
    private Mode target=Mode.UNKNOWN;private long sentAt=-1;private boolean ended;
    public ListeningWidgetRequest(long now,Mode direct,ListeningWidgetOptions options){
        if(now<0||options==null||direct==null||(direct!=Mode.UNKNOWN&&!ListeningProtocol.canWrite(direct)))throw new IllegalArgumentException("Invalid request");
        started=now;explicit=direct;this.options=options;
    }
    public Mode target(){return target;}
    public boolean hasSent(){return sentAt>=0;}
    public boolean ended(){return ended;}
    public boolean expired(long now){return now<started||now-started>=LIMIT_MS;}
    public Step observe(ListeningState.Snapshot state,long now){
        if(ended)return Step.WAIT;
        if(expired(now)||state!=null&&!state.live()){ended=true;return Step.FAILURE;}
        if(state==null||state.phase!=ListeningState.Phase.READY||state.observed==Mode.UNKNOWN
                ||state.receivedAt<started||state.receivedAt>now)return Step.WAIT;
        if(sentAt>=0){
            if(state.observed==target&&state.receivedAt>=sentAt){ended=true;return Step.SUCCESS;}
            return Step.WAIT;
        }
        target=explicit==Mode.UNKNOWN?options.next(state.observed):explicit;
        if(!ListeningProtocol.canWrite(target)){ended=true;return Step.FAILURE;}
        if(target==state.observed){ended=true;return Step.SUCCESS;}
        if(LIMIT_MS-(now-started)<ListeningState.REPLY_MS){ended=true;return Step.FAILURE;}
        sentAt=now;return Step.SEND;
    }
    public void cancel(){ended=true;}
}
