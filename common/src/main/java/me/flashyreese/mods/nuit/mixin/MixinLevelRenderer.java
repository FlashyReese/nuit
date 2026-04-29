package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Unique
    private static float nuit$tickDelta;

    @Unique
    private static GpuBufferSlice nuit$fogParameters;

    @Inject(method = "addSkyPass", at = @At(value = "HEAD"))
    private void nuit$preAddSkyPass(final FrameGraphBuilder frameGraphBuilder, final Camera camera, final GpuBufferSlice fogParameters, final CallbackInfo ci) {
        nuit$tickDelta = camera.getPartialTickTime();
        nuit$fogParameters = fogParameters;
    }

    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = {"method_62215", "lambda$addSkyPass$13"}, require = 1, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER), cancellable = true)
    private static void nuit$renderCustomSkyboxes(CallbackInfo ci, @Local(argsOnly = true) SkyRenderer skyRenderer) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            skyboxManager.renderSkyboxes(
                    (SkyRendererAccessor) skyRenderer,
                    RenderSystem.getModelViewStack(),
                    nuit$tickDelta,
                    Minecraft.getInstance().gameRenderer.getMainCamera(),
                    nuit$fogParameters,
                    Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource()
            );
            ci.cancel();
        }
    }
}
