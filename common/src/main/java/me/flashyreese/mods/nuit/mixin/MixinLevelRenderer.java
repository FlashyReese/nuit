package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private SkyRenderer skyRenderer;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Unique
    private float nuit$tickDelta;

    @Unique
    private GpuBufferSlice nuit$fogParameters;

    @Inject(method = "addSkyPass", at = @At(value = "HEAD"))
    private void nuit$preAddSkyPass(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, GpuBufferSlice fogParameters, CallbackInfo ci) {
        this.nuit$tickDelta = tickDelta;
        this.nuit$fogParameters = fogParameters;
    }

    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = {"method_62215", "lambda$addSkyPass$13"}, require = 1, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER), cancellable = true)
    private void nuit$renderCustomSkyboxes(CallbackInfo ci) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            skyboxManager.renderSkyboxes(
                    (SkyRendererAccessor) skyRenderer,
                    RenderSystem.getModelViewStack(),
                    this.nuit$tickDelta,
                    Minecraft.getInstance().gameRenderer.getMainCamera(),
                    this.nuit$fogParameters,
                    this.renderBuffers.bufferSource()
            );
            ci.cancel();
        }
    }
}
