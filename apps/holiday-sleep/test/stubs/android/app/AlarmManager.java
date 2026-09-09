package android.app;
import java.util.HashMap;
import java.util.Map;
public class AlarmManager {
    public static final int RTC_WAKEUP=0;
    public final Map<Integer,Long> scheduled=new HashMap<>();
    public boolean exactAllowed=true, revokeDuringSet=false;
    public int fallbackCalls;
    public boolean canScheduleExactAlarms() { return exactAllowed; }
    public void setExactAndAllowWhileIdle(int type,long at,PendingIntent intent) {
        if(revokeDuringSet) throw new SecurityException("test permission race");
        scheduled.put(intent.requestCode,at);
    }
    public void setAndAllowWhileIdle(int type,long at,PendingIntent intent) {
        fallbackCalls++; scheduled.put(intent.requestCode,at);
    }
    public void cancel(PendingIntent intent) { scheduled.remove(intent.requestCode); }
}
