package io.github.amerebagatelle.mods.nuit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

public class IrisCompat {
    private static boolean isIrisPresent;
    private static MethodHandle handle;
    private static Object apiInstance;

    static {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInstance = api.cast(api.getDeclaredMethod("getInstance").invoke(null));
            handle = MethodHandles.lookup().findVirtual(api, "getSunPathRotation", MethodType.methodType(float.class));
            isIrisPresent = true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            isIrisPresent = false;
        }
    }

    public static boolean isIrisPresent() {
        return isIrisPresent;
    }

    public static float getSunPathRotation() {
        if (isIrisPresent) {
            try {
                return (float) handle.invoke(apiInstance);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return 0.0F;
    }
}