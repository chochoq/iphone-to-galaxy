package android.content;
public class Intent {
    public String action;
    public Intent(Context context, Class<?> receiver) {}
    public Intent setAction(String action) { this.action=action; return this; }
}
