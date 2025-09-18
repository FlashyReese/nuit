package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.NuitSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.components.RGB;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {
    /**
     * Checks if we should change the fog color to whatever the skybox set it to, and sets it.
     */
    /*@Inject(method = "computeFogColor", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer;biomeChangedTime:J", ordinal = 6), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private static void nuit$modifyColors(Camera camera, float f, ClientLevel clientLevel, int i, float g, CallbackInfoReturnable<Vector4f> cir, FogType fogType, Entity entity, float u, float v, float w) {
        final RGB initialFogColor = new RGB(u, v, w);
        final RGB fogColor = Utils.alphaBlendFogColors(SkyboxManager.getInstance().getActiveSkyboxes(), initialFogColor);
        if (SkyboxManager.getInstance().isEnabled() && !fogColor.equals(initialFogColor)) {
            cir.setReturnValue(new Vector4f(fogColor.getRed(), fogColor.getGreen(), fogColor.getBlue(), 1.0f));
        }
    }

    @ModifyReturnValue(method = "setupFog", at = @At(value = "RETURN"))
    private static FogParameters nuit$redirectSetShaderFogColor(FogParameters original) {
        final float fogDensity = Utils.alphaBlendFogDensity(SkyboxManager.getInstance().getActiveSkyboxes(), original.alpha());
        return new FogParameters(
                original.start(),
                original.end(),
                original.shape(),
                original.red(),
                original.green(),
                original.blue(),
                SkyboxManager.getInstance().isEnabled() ? fogDensity : original.alpha());
    }

    // See AirBasedFogEnvironment
    @Redirect(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private static float nuit$redirectSkyAngle(ClientLevel instance, float v) {
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return Mth.positiveModulo(instance.getDayTime() / 24000F + 0.75F, 1);
        } else {
            return instance.getTimeOfDay(v);
        }
    }

    // See AirBasedFogEnvironment
    @Redirect(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSunAngle(F)F"))
    private static float nuit$redirectSkyAngleRadian(ClientLevel instance, float v) {
        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof DecorationBox decorBox && decorBox.getProperties().rotation().skyboxRotation())) {
            return (float) Math.toRadians(Mth.positiveModulo(instance.getDayTime() / 24000F + 0.75F, 1));
        } else {
            return instance.getSunAngle(v);
        }
    }

    // See AirBasedFogEnvironment
    @ModifyConstant(method = "computeFogColor", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;")), constant = @Constant(intValue = 4, ordinal = 0))
    private static int nuit$renderSkyColor(int original) {
        final SkyboxManager skyboxManager = SkyboxManager.getInstance();
        final Skybox skybox = skyboxManager.getCurrentSkybox();
        if (skyboxManager.isEnabled() && skybox instanceof NuitSkybox nuitSkybox && !nuitSkybox.getProperties().renderSunSkyTint()) {
            return Integer.MAX_VALUE;
        } else {
            return original;
        }
    }*/
}
