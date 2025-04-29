package io.github.amerebagatelle.mods.nuit.util;

import com.mojang.blaze3d.pipeline.BlendFunction;

public class OverrideUtils {
    private static boolean renderTypeBlending = false;
    private static BlendFunction renderTypeBlendFunction = null;

    public static void enableBlendingOverride(BlendFunction blendFunction) {
        renderTypeBlending = true;
        renderTypeBlendFunction = blendFunction;
    }

    public static boolean isOverridingBlending() {
        return renderTypeBlending;
    }

    public static BlendFunction getOverridenBlendFunction() {
        return renderTypeBlendFunction;
    }

    public static void disableBlendingOverride() {
        renderTypeBlending = false;
        renderTypeBlendFunction = null;
    }
}