package android.app;
import android.content.Context;
import android.content.Intent;
public class PendingIntent {
    public static final int FLAG_UPDATE_CURRENT=1, FLAG_IMMUTABLE=2;
    public int requestCode;
    public String action;
    public static PendingIntent getBroadcast(Context context,int code,Intent intent,int flags) {
        PendingIntent result=new PendingIntent(); result.requestCode=code; result.action=intent.action;
        return result;
    }
}
