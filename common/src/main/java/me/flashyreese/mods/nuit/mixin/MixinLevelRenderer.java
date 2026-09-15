package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {

    @Unique
    private static float nuit$tickDelta;

    @Inject(method = "render", at = @At("HEAD"))
    private void nuit$captureTickDelta(GraphicsResourceAllocator graphicsResourceAllocator, boolean renderBlockOutline, CameraRenderState cameraRenderState, GpuBufferSlice fogParameters, Vector4f shaderFogColor, boolean renderSky, boolean consistentDepthRequired, CallbackInfo ci) {
        nuit$tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        return renderSky || skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes();
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return nuit$skyboxForPass(original);
    }

    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     */
    @WrapOperation(
            method = "lambda$addSkyPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;render(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V"
            )
    )
    private void nuit$replaceVanillaSky(
            SkyRenderer skyRenderer,
            GpuBufferSlice fogParameters,
            SkyRenderState skyRenderState,
            Operation<Void> original
    ) {
        if (!nuit$renderCustomSkyboxes(fogParameters, skyRenderer)) {
            original.call(skyRenderer, fogParameters, skyRenderState);
        }
    }

    @Unique
    private static boolean nuit$renderCustomSkyboxes(GpuBufferSlice fogParameters, SkyRenderer skyRenderer) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes()) {
            RenderSystem.setShaderFog(fogParameters);
            Matrix4f skyModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrixCopy());
            skyModelViewMatrix.setTranslation(0.0F, 0.0F, 0.0F);
            Matrix4fStack skyModelViewStack = new Matrix4fStack(32);
            skyModelViewStack.set(skyModelViewMatrix);
            skyboxManager.renderSkyboxes(
                    skyRenderer,
                    skyModelViewStack,
                    nuit$tickDelta,
                    Minecraft.getInstance().gameRenderer.mainCamera(),
                    fogParameters
            );
            return true;
        }
        return false;
    }

    @Unique
    private static DimensionType.Skybox nuit$skyboxForPass(DimensionType.Skybox original) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (original == DimensionType.Skybox.NONE && skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes()) {
            return DimensionType.Skybox.OVERWORLD;
        }
        return original;
    }
}
