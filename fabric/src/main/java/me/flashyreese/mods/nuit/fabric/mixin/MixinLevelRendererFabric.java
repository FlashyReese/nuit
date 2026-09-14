package me.flashyreese.mods.nuit.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import me.flashyreese.mods.nuit.render.NuitSkyboxRenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRendererFabric {
    @Unique
    private static float nuit$tickDelta;

    @Inject(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At("HEAD")
    )
    private void nuit$captureTickDelta(
            FrameGraphBuilder frameGraphBuilder,
            Camera camera,
            GpuBufferSlice fogParameters,
            CallbackInfo ci
    ) {
        nuit$tickDelta = camera.getPartialTickTime();
    }

    @ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        return NuitSkyboxRenderHooks.allowCustomSkyPass(renderSky);
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;"
            )
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return NuitSkyboxRenderHooks.skyboxForPass(original);
    }

    @Inject(
            method = "method_62215(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/SkyRenderState;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private static void nuit$renderCustomSkyboxes(
            GpuBufferSlice fogParameters,
            SkyRenderState skyRenderState,
            SkyRenderer skyRenderer,
            CallbackInfo ci
    ) {
        if (NuitSkyboxRenderHooks.renderCustomSkyboxes(fogParameters, skyRenderer, nuit$tickDelta)) {
            ci.cancel();
        }
    }
}
