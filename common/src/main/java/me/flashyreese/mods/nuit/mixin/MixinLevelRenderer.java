package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Unique
    private static float nuit$tickDelta;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void nuit$captureTickDelta(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, CameraRenderState cameraRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, Vector4f shaderFogColor, boolean renderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        nuit$tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
    }

    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     */
    @Group(name = "nuit$renderCustomSkyboxes", min = 1, max = 1)
    @Inject(
            method = "lambda$addSkyPass$0(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private static void nuit$renderCustomSkyboxesFabric(GpuBufferSlice fogParameters, SkyRenderState skyRenderState, SkyRenderer skyRenderer, CallbackInfo ci) {
        nuit$renderCustomSkyboxes(fogParameters, skyRenderer, ci);
    }

    @Group(name = "nuit$renderCustomSkyboxes", min = 1, max = 1)
    @Inject(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private static void nuit$renderCustomSkyboxesNeoForge(SkyRenderState skyRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, SkyRenderer skyRenderer, CallbackInfo ci) {
        nuit$renderCustomSkyboxes(fogParameters, skyRenderer, ci);
    }

    @Unique
    private static void nuit$renderCustomSkyboxes(GpuBufferSlice fogParameters, SkyRenderer skyRenderer, CallbackInfo ci) {
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
                    fogParameters
            );
            ci.cancel();
        }
    }
}
