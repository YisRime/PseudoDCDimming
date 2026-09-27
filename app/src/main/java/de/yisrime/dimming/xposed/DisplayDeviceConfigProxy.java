package de.yisrime.dimming.xposed;

import android.util.Log;

import java.lang.reflect.Method;

public class DisplayDeviceConfigProxy {
    private static final String TAG = "PseudoDcBacklight.Config";

    private final Object obj;
    private final Method getNitsFromBacklightMethod;

    public DisplayDeviceConfigProxy(Object obj) {
        this.obj = obj;
        Method method;
        try {
            method = Compat.findMethod(obj.getClass(), "getNitsFromBacklight", float.class);
        } catch (Throwable t) {
            Log.e(TAG, "getNitsFromBacklight unavailable on " + obj.getClass().getName(), t);
            method = null;
        }
        getNitsFromBacklightMethod = method;
    }

    float getNitsFromBacklight(float backlight) {
        if (getNitsFromBacklightMethod == null) return -1;
        try {
            var value = Compat.invoke(getNitsFromBacklightMethod, obj, new Object[]{backlight});
            if (value != null) {
                return (float)value;
            }
        } catch (Throwable e) {
            Log.e(TAG, "getNitsFromBacklight failed", e);
        }
        return -1;
    }
}
