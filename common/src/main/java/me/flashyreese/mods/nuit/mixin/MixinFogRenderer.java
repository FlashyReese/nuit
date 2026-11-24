package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.NuitSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.components.RGB;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;

    /**
     * Checks if we should change the fog color to whatever the skybox set it to, and sets it.
     */
    @Inject(
            method = "setupColor",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/FogRenderer;biomeChangedTime:J",
                    ordinal = 6
            )
    )
    private static void nuit$modifyColors(
            Camera camera,
            float tickDelta,
            ClientLevel level,
            int renderDistance,
            float skyDarken,
            CallbackInfo ci
    ) {
        RGB initialFogColor = new RGB(fogRed, fogGreen, fogBlue);
        RGB fogColor = Utils.alphaBlendFogColors(SkyboxManager.getInstance().getActiveSkyboxes(), initialFogColor);
        if (SkyboxManager.getInstance().isEnabled() && !fogColor.equals(initialFogColor)) {
            fogRed = fogColor.getRed();
            fogGreen = fogColor.getGreen();
            fogBlue = fogColor.getBlue();
        }
    }

    /** Applies Nuit's fog alpha without redirecting RenderSystem's unmapped helper. */
    @Inject(method = "levelFogColor", at = @At("HEAD"), cancellable = true)
    private static void nuit$setFogDensity(CallbackInfo ci) {
        float initialFogDensity = 1.0F;
        float fogDensity = Utils.alphaBlendFogDensity(
                SkyboxManager.getInstance().getActiveSkyboxes(),
                initialFogDensity
        );
        if (SkyboxManager.getInstance().isEnabled() && fogDensity != initialFogDensity) {
            RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue, fogDensity);
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "setupColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"
            )
    )
    private static float nuit$modifySkyAngle(ClientLevel level, float tickDelta, Operation<Float> original) {
        SkyboxManager.CelestialController controller = nuit$getCelestialController();
        if (controller != null) {
            return (float) (controller.getSkyAngleDegrees(level, tickDelta) / 360.0D);
        }
        return original.call(level, tickDelta);
    }

    @WrapOperation(
            method = "setupColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSunAngle(F)F"
            )
    )
    private static float nuit$modifySkyAngleRadians(ClientLevel level, float tickDelta, Operation<Float> original) {
        SkyboxManager.CelestialController controller = nuit$getCelestialController();
        if (controller != null) {
            return (float) Math.toRadians(controller.getSkyAngleDegrees(level, tickDelta));
        }
        return original.call(level, tickDelta);
    }

    @ModifyConstant(
            method = "setupColor",
            slice = @Slice(from = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"
            )),
            constant = @Constant(intValue = 4, ordinal = 0)
    )
    private static int nuit$renderSkyColor(int original) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        Skybox skybox = skyboxManager.getCurrentSkybox();
        if (skyboxManager.isEnabled()
                && skybox instanceof NuitSkybox nuitSkybox
                && !nuitSkybox.getProperties().renderSunSkyTint()) {
            return Integer.MAX_VALUE;
        }
        return original;
    }

    private static SkyboxManager.CelestialController nuit$getCelestialController() {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        return skyboxManager.isEnabled() ? skyboxManager.getCelestialController().orElse(null) : null;
    }
}
