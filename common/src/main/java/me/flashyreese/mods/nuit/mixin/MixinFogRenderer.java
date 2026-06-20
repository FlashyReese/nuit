package me.flashyreese.mods.nuit.mixin;

import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @Inject(method = "computeFogColor", at = @At("TAIL"))
    private void nuit$redirectSetShaderFogColor(Camera camera, float tickDelta, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fogColor, CallbackInfo ci) {
        if (SkyboxManager.getInstance().isEnabled()) {
            final float fogDensity = Utils.alphaBlendFogDensity(SkyboxManager.getInstance().getActiveSkyboxes(), fogColor.w());
            fogColor.set(fogColor.x(), fogColor.y(), fogColor.z(), fogDensity);
        }
    }
}
