package com.chocho.holidaysleep;
import android.content.Context;
final class ZenController {
    static boolean active;
    static boolean setActive(Context context,boolean value) { active=value; return true; }
}
