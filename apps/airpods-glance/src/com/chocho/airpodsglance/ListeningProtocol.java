package com.chocho.airpodsglance;

/** The fixed listening report verified in D-020; no arbitrary command or raw-frame storage. */
public final class ListeningProtocol {
    public enum Mode {
        UNKNOWN(0,"확인 못함"), OFF(1,"끔"), ANC(2,"노이즈 캔슬링"),
        TRANSPARENCY(3,"주변음 허용"), ADAPTIVE(4,"적응형");
        public final int wire; public final String label;
        Mode(int wire,String label){this.wire=wire;this.label=label;}
        public static Mode fromWire(int value){for(Mode m:values())if(m.wire==value)return m;return UNKNOWN;}
    }
    public enum Kind { OTHER, MALFORMED, MODE }
    public static final class Report {
        public final Kind kind;public final Mode mode;
        private Report(Kind kind,Mode mode){this.kind=kind;this.mode=mode;}
    }
    private ListeningProtocol(){}
    public static Report decode(byte[] p){
        if(p==null||p.length<7||p[0]!=4||p[1]!=0||p[2]!=4||p[3]!=0||p[4]!=9||p[5]!=0||p[6]!=13)
            return new Report(Kind.OTHER,Mode.UNKNOWN);
        if(p.length!=11||p[8]!=0||p[9]!=0||p[10]!=0)return new Report(Kind.MALFORMED,Mode.UNKNOWN);
        Mode m=Mode.fromWire(p[7]&255);
        return new Report(m==Mode.UNKNOWN?Kind.MALFORMED:Kind.MODE,m);
    }
    public static boolean canWrite(Mode mode){return mode==Mode.ANC||mode==Mode.TRANSPARENCY||mode==Mode.ADAPTIVE;}
    public static byte[] command(Mode mode){
        if(!canWrite(mode))throw new IllegalArgumentException("Unverified listening mode");
        return new byte[]{4,0,4,0,9,0,13,(byte)mode.wire,0,0,0};
    }
}
