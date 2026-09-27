package de.yisrime.dimming.xposed;

import java.util.Arrays;

/* 自校准 */
public final class NitsCalibration {
    private static final int CAPACITY = 2048;
    private static final int SLOPE_SPAN = 16;
    private static final double MIN_EXPONENT = 1.0;
    private static final double MAX_EXPONENT = 3.0;
    private static final float LEVEL_EPSILON = 1.0e-4f;

    private final float[] levels = new float[CAPACITY];
    private final float[] nitValues = new float[CAPACITY];
    private int size = 0;

    void observe(float level, float nit) {
        if (!isPositiveFinite(level) || !isPositiveFinite(nit)) return;
        synchronized (this) {
            int found = Arrays.binarySearch(levels, 0, size, level);
            if (found >= 0) {
                nitValues[found] = nit;
                return;
            }
            int at = -found - 1;
            if (at > 0 && collides(levels[at - 1], level)) {
                nitValues[at - 1] = nit;
                return;
            }
            if (at < size && collides(levels[at], level)) {
                nitValues[at] = nit;
                return;
            }
            if (size == CAPACITY) {
                thin();
                at = insertionPoint(level);
            }
            System.arraycopy(levels, at, levels, at + 1, size - at);
            System.arraycopy(nitValues, at, nitValues, at + 1, size - at);
            levels[at] = level;
            nitValues[at] = nit;
            size++;
        }
    }

    float nitsAt(float level) {
        if (!isPositiveFinite(level)) return Float.NaN;
        synchronized (this) {
            if (size < 2) return Float.NaN;
            int found = Arrays.binarySearch(levels, 0, size, level);
            if (found >= 0) return nitValues[found];
            int hi = -found - 1;
            if (hi == 0) return nitValues[0];
            if (hi == size) return extrapolate(level);
            return segment(level, hi - 1, hi, false);
        }
    }

    int sampleCount() {
        synchronized (this) {
            return size;
        }
    }

    float topLevel() {
        synchronized (this) {
            return size == 0 ? Float.NaN : levels[size - 1];
        }
    }

    /* 对数线性，等价于过两端点的幂律 */
    private float segment(float level, int lo, int hi, boolean extrapolating) {
        var l0 = levels[lo];
        var l1 = levels[hi];
        var n0 = nitValues[lo];
        var n1 = nitValues[hi];
        if (!(l1 > l0) || !(n1 > n0)) return n1;
        var exponent = Math.log((double) n1 / n0) / Math.log((double) l1 / l0);
        if (extrapolating) {
            exponent = Math.min(MAX_EXPONENT, Math.max(MIN_EXPONENT, exponent));
        } else if (!(exponent > 0.0)) {
            return n1;
        }
        return (float) (n0 * Math.pow((double) level / l0, exponent));
    }

    private float extrapolate(float level) {
        var lo = Math.max(0, size - SLOPE_SPAN);
        if (lo == size - 1) lo = 0;
        return segment(level, lo, size - 1, true);
    }

    private void thin() {
        int kept = 0;
        for (int i = 0; i < size; i += 2) {
            levels[kept] = levels[i];
            nitValues[kept] = nitValues[i];
            kept++;
        }
        size = kept;
    }

    private int insertionPoint(float level) {
        var found = Arrays.binarySearch(levels, 0, size, level);
        return found >= 0 ? found : -found - 1;
    }

    private static boolean collides(float stored, float level) {
        return Math.abs(stored - level) <= stored * LEVEL_EPSILON;
    }

    private static boolean isPositiveFinite(float value) {
        return Float.isFinite(value) && value > 0.0f;
    }
}
