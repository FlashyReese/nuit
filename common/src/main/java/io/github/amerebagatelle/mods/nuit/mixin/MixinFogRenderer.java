package io.github.amerebagatelle.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.NuitSkybox;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.components.RGB;
import io.github.amerebagatelle.mods.nuit.skybox.decorations.DecorationBox;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Unique
    @Nullable
    private static Float nuit$fogRed = null;

    @Unique
    @Nullable
    private static Float nuit$fogGreen = null;

    @Unique
    @Nullable
    private static Float nuit$fogBlue = null;

    /**
     * Checks if we should change the fog color to whatever the skybox set it to, and sets it.
     */
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private static void nuit$modifyColors(Camera camera, float tickDelta, ClientLevel clientLevel, int i, float g, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f fogColorVec = cir.getReturnValue();
        RGB initialFogColor = new RGB(fogColorVec.x, fogColorVec.y, fogColorVec.z);
        RGB fogColor = Utils.alphaBlendFogColors(SkyboxManager.getInstance().getActiveSkyboxes(), initialFogColor);
        if (SkyboxManager.getInstance().isEnabled()) {
            nuit$fogRed = fogColor.getRed();
            nuit$fogBlue = fogColor.getBlue();
            nuit$fogGreen = fogColor.getGreen();
        }
    }

    @ModifyReturnValue(method = "setupFog", at = @At(value = "RETURN"))
    private static FogParameters nuit$redirectSetShaderFogColor(FogParameters original) {
        float fogDensity = Utils.alphaBlendFogDensity(SkyboxManager.getInstance().getActiveSkyboxes(), original.alpha());
        boolean enabled = SkyboxManager.getInstance().isEnabled();
        return new FogParameters(
                original.start(),
                original.end(),
                original.shape(),
                (enabled && nuit$fogRed != null) ? nuit$fogRed : original.red(),
                (enabled && nuit$fogGreen != null) ? nuit$fogGreen : original.green(),
                (enabled && nuit$fogBlue != null) ? nuit$fogBlue : original.blue(),
                fogDensity);
    }

    @Redirect(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private static float nuit$redirectSkyAngle(ClientLevel instance, float v) {
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return Mth.positiveModulo(instance.getDayTime() / 24000F + 0.75F, 1);
        } else {
            return instance.getTimeOfDay(v);
        }
    }

    @Redirect(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSunAngle(F)F"))
    private static float nuit$redirectSkyAngleRadian(ClientLevel instance, float v) {
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return (float) Math.toRadians(Mth.positiveModulo(instance.getDayTime() / 24000F + 0.75F, 1));
        } else {
            return instance.getSunAngle(v);
        }
    }

    @ModifyConstant(method = "computeFogColor", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;")), constant = @Constant(intValue = 4, ordinal = 0))
    private static int nuit$renderSkyColor(int original) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        Skybox skybox = skyboxManager.getCurrentSkybox();
        if (skyboxManager.isEnabled() && skybox instanceof NuitSkybox nuitSkybox && !nuitSkybox.getProperties().renderSunSkyTint()) {
            return Integer.MAX_VALUE;
        } else {
            return original;
        }
    }
}
