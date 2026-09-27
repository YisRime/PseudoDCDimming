package de.yisrime.dimming.xposed;

import android.util.Log;

import java.lang.reflect.Method;

public class DisplayTransformManagerProxy {
    private static final String TAG = "PseudoDcBacklight.DTM";

    private final Object obj;
    private final Method setColorMatrixMethod;
    private final Method needsLinearColorMatrixMethod;

    public DisplayTransformManagerProxy(Object obj) {
        this.obj = obj;
        setColorMatrixMethod = lookup(obj.getClass(), "setColorMatrix", int.class, float[].class);
        needsLinearColorMatrixMethod = lookup(obj.getClass(), "needsLinearColorMatrix");
    }

    private static Method lookup(Class<?> clazz, String name, Class<?>... types) {
        try {
            return Compat.findMethod(clazz, name, types);
        } catch (Throwable t) {
            Log.e(TAG, "member " + name + " unavailable on " + clazz.getName(), t);
            return null;
        }
    }

    public void setColorMatrix(int level, float[] value) {
        if (setColorMatrixMethod == null) return;
        try {
            Compat.invoke(setColorMatrixMethod, obj, new Object[]{level, value});
        } catch (Throwable t) {
            Log.e(TAG, "setColorMatrix failed", t);
        }
    }

    public boolean needsLinearColorMatrix() {
        if (needsLinearColorMatrixMethod == null) return true;
        try {
            var value = Compat.invoke(needsLinearColorMatrixMethod, obj, new Object[0]);
            return value instanceof Boolean b ? b : true;
        } catch (Throwable t) {
            Log.e(TAG, "needsLinearColorMatrix failed", t);
            return true;
        }
    }
}
