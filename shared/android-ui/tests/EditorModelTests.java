package com.chocho.ui;
public final class EditorModelTests {
    static int checks;
    static void eq(int expected,int actual){checks++;if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
    static void ok(boolean actual){checks++;if(!actual)throw new AssertionError("expected true");}
    public static void main(String[] args){
        eq(30615,EditorModel.withHourMinute(25215,8,30));
        eq(30600,EditorModel.withSecond(30615,0));
        eq(25215,EditorModel.parseTime("07:00",25215));
        eq(25200,EditorModel.parseTime("07:00:00",25215));
        eq(-1,EditorModel.parseTime("25:00",25215));
        eq(-1,EditorModel.parseTime("07:60",25215));
        eq(-1,EditorModel.parseTime("",25215));
        ok(EditorModel.validInterval(25215,25230));
        ok(!EditorModel.validInterval(25215,25215));
        ok(!EditorModel.validInterval(25215,21600));
        ok(EditorModel.validInterval(0,86399));
        eq(-1,EditorModel.parseInteger("63.5",40,75));
        eq(-1,EditorModel.parseInteger("76",40,75));
        eq(-1,EditorModel.parseInteger("",40,75));
        eq(63,EditorModel.parseInteger("63",40,75));
        eq(60,EditorModel.parseInteger("60",3,60));
        for(int hour=0;hour<24;hour++)for(int minute=0;minute<60;minute++){
            int selected=EditorModel.withHourMinute(15,hour,minute);
            eq(15,selected%60);eq(hour,selected/3600);eq(minute,selected%3600/60);
        }
        for(int value=-240;value<240;value++)ok(EditorModel.wrap(value,60)>=0&&EditorModel.wrap(value,60)<60);
        System.out.println("PASS EditorModel "+checks+" checks");
    }
}
