package de.yisrime.dimming.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * 以纯反射与 libxposed 接口替换原 XposedHelpers / XposedBridge 的能力，保持原有的异常类型语义：
 * 找不到成员时抛 NoSuchMethodError / NoClassDefFoundError，被调用方抛出的异常原样上抛。
 */
final class Compat {
    private static final WeakHashMap<Object, Boolean> internalDisplayFlags = new WeakHashMap<>();

    private Compat() {
    }

    static Class<?> findClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(name);
        }
    }

    static Method findMethod(Class<?> clazz, String name, Class<?>... types) {
        for (var c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name, types);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "." + name);
    }

    static Constructor<?> findConstructor(Class<?> clazz, Class<?>... types) {
        try {
            return clazz.getDeclaredConstructor(types);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(clazz.getName() + "<init>");
        }
    }

    static Object callStatic(Class<?> clazz, String name, Object... args) {
        return invoke(findBestMatch(clazz, name, args), null, args);
    }

    static Object call(Object receiver, String name, Object... args) {
        return invoke(findBestMatch(receiver.getClass(), name, args), receiver, args);
    }

    static boolean getBooleanField(Object receiver, String name) {
        for (var c = receiver.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.getBoolean(receiver);
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new NoSuchFieldError(name);
    }

    static void putInternalDisplay(Object receiver, boolean isInternal) {
        synchronized (internalDisplayFlags) {
            internalDisplayFlags.put(receiver, isInternal);
        }
    }

    static boolean isInternalDisplay(Object receiver) {
        synchronized (internalDisplayFlags) {
            return Boolean.TRUE.equals(internalDisplayFlags.get(receiver));
        }
    }

    private static Method findBestMatch(Class<?> clazz, String name, Object[] args) {
        for (var c = clazz; c != null; c = c.getSuperclass()) {
            for (var method : c.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (!parameterCountMatches(method, args)) continue;
                return method;
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "." + name);
    }

    private static boolean parameterCountMatches(Method method, Object[] args) {
        var types = method.getParameterTypes();
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            if (!boxed(types[i]).isInstance(args[i])) return false;
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        return Double.class;
    }

    private static Object invoke(Method method, Object receiver, Object[] args) {
        method.setAccessible(true);
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            var cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
