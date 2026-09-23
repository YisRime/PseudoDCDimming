package de.yisrime.dimming.xposed;

import java.lang.reflect.Method;

public class DisplayDeviceConfigProxy {
    private final Object obj;
    private final Method getNitsFromBacklightMethod;

    public DisplayDeviceConfigProxy(Object obj) {
        this.obj = obj;
        getNitsFromBacklightMethod = Compat.findMethod(obj.getClass(), "getNitsFromBacklight", float.class);
    }

    float getNitsFromBacklight(float backlight) {
        try {
            var value = getNitsFromBacklightMethod.invoke(obj, backlight);
            if (value != null) {
                return (float)value;
            }
        } catch (Exception e) {
            // ignore
        }
        return -1;
    }
}
