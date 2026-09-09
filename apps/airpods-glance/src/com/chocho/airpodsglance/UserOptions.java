package com.chocho.airpodsglance;

/** User-facing values only. Transport deadlines deliberately remain outside this model. */
public final class UserOptions {
    public final int cardSeconds, budThreshold, caseThreshold;
    public UserOptions(int cardSeconds, int budThreshold, int caseThreshold) {
        if (!validCardSeconds(cardSeconds) || !validThreshold(budThreshold) || !validThreshold(caseThreshold))
            throw new IllegalArgumentException("User option outside safe range");
        this.cardSeconds=cardSeconds; this.budThreshold=budThreshold; this.caseThreshold=caseThreshold;
    }
    public static UserOptions defaults() { return new UserOptions(0,20,15); }
    public static boolean validCardSeconds(int v) { return v==0 || (v>=3 && v<=60); }
    public static boolean validThreshold(int v) { return v>=5 && v<=50; }
    public static UserOptions restore(Object seconds,Object buds,Object caseValue) {
        int s=seconds instanceof Integer?(Integer)seconds:0;
        int b=buds instanceof Integer?(Integer)buds:20;
        int c=caseValue instanceof Integer?(Integer)caseValue:15;
        return new UserOptions(validCardSeconds(s)?s:0,validThreshold(b)?b:20,validThreshold(c)?c:15);
    }
    public long dismissMillis() { return cardSeconds*1000L; }
}
