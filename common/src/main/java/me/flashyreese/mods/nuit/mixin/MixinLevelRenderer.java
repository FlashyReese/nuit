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
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
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
    private void nuit$preAddSkyPass(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogParameters, CallbackInfo ci) {
        nuit$tickDelta = camera.getPartialTickTime();
        nuit$fogParameters = fogParameters;
    }

    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     */
    @Inject(
            method = {"method_62215", "lambda$addSkyPass$8"},
            require = 1,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private static void nuit$renderCustomSkyboxes(CallbackInfo ci, @Local(argsOnly = true) SkyRenderer skyRenderer) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes()) {
            Matrix4f skyModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
            skyModelViewMatrix.setTranslation(0.0F, 0.0F, 0.0F);
            Matrix4fStack skyModelViewStack = new Matrix4fStack(32);
            skyModelViewStack.set(skyModelViewMatrix);
            skyboxManager.renderSkyboxes(
                    skyRenderer,
                    skyModelViewStack,
                    nuit$tickDelta,
                    Minecraft.getInstance().gameRenderer.getMainCamera(),
                    nuit$fogParameters
            );
            ci.cancel();
        }
    }
}
