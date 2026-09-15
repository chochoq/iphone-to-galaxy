package com.chocho.airpodsglance;
import android.content.Context;import android.content.SharedPreferences;import android.os.SystemClock;
import com.chocho.airpodsglance.ListeningProtocol.Mode;

/** Only display observations and user options persist. No request, retry or busy queue is stored. */
public final class ListeningWidgetStore {
    public static final class Record {
        public final Mode mode;public final long at;public final String note;
        public Record(Mode mode,long at,String note){this.mode=mode;this.at=at;this.note=note;}
    }
    private final Context context;private final SharedPreferences prefs;
    public ListeningWidgetStore(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("listening_widgets",Context.MODE_PRIVATE);}
    public ListeningWidgetOptions options(int id){return ListeningWidgetOptions.decode(prefs.getString("order_"+id,prefs.getString("order_0","2,4,3")));}
    public void saveOptions(int id,ListeningWidgetOptions value){prefs.edit().putString("order_"+Math.max(0,id),value.encode()).apply();}
    public void deleteOptions(int id){if(id>0)prefs.edit().remove("order_"+id).apply();}
    public Record read(){String selected=new AppSettings(context).selectedAddress();
        if(selected==null||!selected.equals(prefs.getString("target",null)))return new Record(Mode.UNKNOWN,0,prefs.getString("note",""));
        long at=prefs.getLong("at",0);Mode mode=Mode.fromWire(prefs.getInt("mode",0));
        if(at<=0||at>System.currentTimeMillis())return new Record(Mode.UNKNOWN,0,"");
        return new Record(mode,at,prefs.getString("note",""));
    }
    public boolean record(String address,Mode mode,long receivedAt){
        long now=SystemClock.elapsedRealtime();if(address==null||!address.equals(new AppSettings(context).selectedAddress())||mode==Mode.UNKNOWN||receivedAt<0||receivedAt>now)return false;
        long at=System.currentTimeMillis()-(now-receivedAt);
        if(at<=0)return false;
        prefs.edit().putString("target",address).putInt("mode",mode.wire).putLong("at",at).putString("note","").apply();return true;
    }
    public void note(String text){prefs.edit().putString("note",text==null?"":text).apply();}
}
