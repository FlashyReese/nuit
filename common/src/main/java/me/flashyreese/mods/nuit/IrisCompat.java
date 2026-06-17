package me.flashyreese.mods.nuit;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class IrisCompat {
    private static final int PIPELINE_API_REVISION = 3;
    private static final Set<RenderPipeline> REGISTERED_PIPELINES = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean irisPresent;
    private static Object apiInstance;
    private static Method sunPathRotationMethod;
    private static Method assignPipelineMethod;
    private static Object skyBasicProgram;
    private static Object skyTexturedProgram;

    static {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInstance = api.cast(api.getDeclaredMethod("getInstance").invoke(null));
            sunPathRotationMethod = findMethod(api, "getSunPathRotation");
            irisPresent = true;

            int minorApiRevision = getMinorApiRevision(api);
            if (minorApiRevision >= PIPELINE_API_REVISION) {
                Class<?> irisProgram = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
                assignPipelineMethod = api.getMethod("assignPipeline", RenderPipeline.class, irisProgram);
                skyBasicProgram = enumConstant(irisProgram, "SKY_BASIC");
                skyTexturedProgram = enumConstant(irisProgram, "SKY_TEXTURED");
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            irisPresent = false;
        }
    }

    public static boolean isIrisPresent() {
        return irisPresent;
    }

    public static boolean canAssignPipelines() {
        return assignPipelineMethod != null;
    }

    public static float getSunPathRotation() {
        if (irisPresent && sunPathRotationMethod != null) {
            try {
                return (float) sunPathRotationMethod.invoke(apiInstance);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                NuitClient.getLogger().debug("Failed to query Iris sun path rotation", exception);
            }
        }

        return 0.0F;
    }

    public static void assignSkyBasicPipeline(RenderPipeline pipeline) {
        assignPipeline(pipeline, skyBasicProgram);
    }

    public static void assignSkyTexturedPipeline(RenderPipeline pipeline) {
        assignPipeline(pipeline, skyTexturedProgram);
    }

    private static void assignPipeline(RenderPipeline pipeline, Object program) {
        if (!canAssignPipelines() || program == null || !REGISTERED_PIPELINES.add(pipeline)) {
            return;
        }

        try {
            assignPipelineMethod.invoke(apiInstance, pipeline, program);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            NuitClient.getLogger().debug("Failed to register Nuit render pipeline {} with Iris", pipeline.getLocation(), exception);
        }
    }

    private static Method findMethod(Class<?> type, String methodName) {
        try {
            return type.getMethod(methodName);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static int getMinorApiRevision(Class<?> api) throws InvocationTargetException, IllegalAccessException {
        Method method = findMethod(api, "getMinorApiRevision");
        if (method == null) {
            return 0;
        }

        return (int) method.invoke(apiInstance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }
}
