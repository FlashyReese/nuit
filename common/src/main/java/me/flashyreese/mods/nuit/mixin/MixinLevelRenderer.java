package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    /**
     * Replaces vanilla sky rendering with Nuit's frame pass when custom skyboxes are active.
     */
    @Inject(
            method = "addSkyPass",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void nuit$renderCustomSkyboxes(
            FrameGraphBuilder frameGraphBuilder,
            Camera camera,
            GpuBufferSlice fogParameters,
            CallbackInfo ci,
            FogType fogType,
            SkyRenderState skyRenderState,
            SkyRenderer skyRenderer,
            FramePass framePass
    ) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            Matrix4f skyModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
            skyModelViewMatrix.setTranslation(0.0F, 0.0F, 0.0F);
            framePass.executes(() -> {
                RenderSystem.setShaderFog(fogParameters);
                Matrix4fStack skyModelViewStack = new Matrix4fStack(32);
                skyModelViewStack.set(skyModelViewMatrix);
                skyboxManager.renderSkyboxes(
                        (SkyRendererAccessor) skyRenderer,
                        skyModelViewStack,
                        camera.getPartialTickTime(),
                        camera,
                        fogParameters,
                        Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource()
                );
            });
            ci.cancel();
        }
    }
}
