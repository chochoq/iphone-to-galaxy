package com.chocho.taptotop;

public final class ScrollOptions {
    public final int taps, strength, distance;
    public ScrollOptions(int taps,int strength,int distance) {
        if((taps!=1 && taps!=2) || strength<60 || strength>160 || distance<40 || distance>75)
            throw new IllegalArgumentException("Scroll option outside safe range");
        this.taps=taps; this.strength=strength; this.distance=distance;
    }
    public static ScrollOptions defaults() { return new ScrollOptions(1,100,62); }
    public static ScrollOptions restore(Object taps,Object strength,Object distance) {
        int t=taps instanceof Integer?(Integer)taps:1;
        int s=strength instanceof Integer?(Integer)strength:100;
        int d=distance instanceof Integer?(Integer)distance:62;
        return new ScrollOptions(t==1||t==2?t:1,s>=60&&s<=160?s:100,d>=40&&d<=75?d:62);
    }
    public long durationMillis(boolean x) { return Math.round((x?45d:100d)*100d/strength); }
    public float startFraction() { return (90-distance)/100f; }
}
