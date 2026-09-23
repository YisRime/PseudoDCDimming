package de.yisrime.dimming.xposed;

import java.lang.reflect.Method;

public class BacklightAdapterProxy {
    private final Object adapter;
    private volatile Method setBacklightMethod;
    private volatile Object[] lastArgs;

    public BacklightAdapterProxy(Object adapter) {
        this.adapter = adapter;
    }

    void record(Object sender, Method method, Object[] args) {
        if (sender != adapter) return;
        method.setAccessible(true);
        setBacklightMethod = method;
        lastArgs = args.clone();
    }

    public void setBacklight(float sdrBacklight, float sdrNits, float backlight, float nits) {
        final var method = setBacklightMethod;
        final var recorded = lastArgs;
        if (method == null || recorded == null) return;
        final var args = recorded.clone();
        args[0] = sdrBacklight;
        args[1] = sdrNits;
        args[2] = backlight;
        args[3] = nits;
        try {
            method.invoke(adapter, args);
        } catch (Exception e) {
            // ignore
        }
    }
}
