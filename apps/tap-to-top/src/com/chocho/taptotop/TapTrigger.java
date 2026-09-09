package com.chocho.taptotop;

/** Only counts completed taps. Never schedules a delayed gesture. */
public final class TapTrigger {
    private long first=-1;
    private float x,y;
    public boolean accept(long now,float nextX,float nextY,int count,float slop) {
        if(count==1) { reset(); return true; }
        if(first>=0 && now>=first && now-first<=350
                && Math.abs(nextX-x)<=slop && Math.abs(nextY-y)<=slop) {
            reset(); return true;
        }
        first=now; x=nextX; y=nextY; return false;
    }
    public void reset() { first=-1; }
}
