package com.chocho.ui;

/** Pure value rules shared by the editor and its tests. No persistence or Android dependency. */
public final class EditorModel {
    private EditorModel() {}
    public static int wrap(int value,int count){return Math.floorMod(value,count);}
    public static boolean validInterval(int start,int end){return start>=0&&start<end&&end<86400;}
    public static int withHourMinute(int seconds,int hour,int minute){
        if(seconds<0||seconds>=86400||hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("Invalid time");
        return hour*3600+minute*60+seconds%60;
    }
    public static int withSecond(int seconds,int second){
        if(seconds<0||seconds>=86400||second<0||second>59)throw new IllegalArgumentException("Invalid time");
        return seconds-seconds%60+second;
    }
    public static int parseInteger(String raw,int min,int max){
        if(raw==null||!raw.matches("[0-9]+"))return -1;
        try{int v=Integer.parseInt(raw);return v>=min&&v<=max?v:-1;}catch(NumberFormatException e){return -1;}
    }
    public static int parseTime(String raw,int previous){
        if(raw==null||!raw.matches("[0-9]{1,2}:[0-9]{2}(:[0-9]{2})?"))return -1;
        String[] parts=raw.split(":");
        int h=parseInteger(parts[0],0,23),m=parseInteger(parts[1],0,59);
        int s=parts.length==3?parseInteger(parts[2],0,59):previous%60;
        return h<0||m<0||s<0?-1:h*3600+m*60+s;
    }
    public static String time(int seconds){
        return String.format(java.util.Locale.KOREAN,seconds%60==0?"%02d:%02d":"%02d:%02d:%02d",seconds/3600,seconds%3600/60,seconds%60);
    }
}
