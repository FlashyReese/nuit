package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Shadow
    private SkyRenderer skyRenderer;

    @Unique
    private static float nuit$tickDelta;
    @Unique
    private static boolean nuit$skipNeoForgeVanillaSky;

    @Inject(method = "render", at = @At("HEAD"))
    private void nuit$captureTickDelta(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, CameraRenderState cameraRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, Vector4f shaderFogColor, boolean renderSky, CallbackInfo ci) {
        nuit$tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        return renderSky || skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes();
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowFabricSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return nuit$skyboxForPass(original);
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowNeoForgeSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return nuit$skyboxForPass(original);
    }

    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     */
    @Group(name = "nuit$renderCustomSkyboxes", min = 1, max = 1)
    @Inject(
            method = "lambda$addSkyPass$0(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private void nuit$renderCustomSkyboxesFabric(GpuBufferSlice fogParameters, SkyRenderState skyRenderState, CallbackInfo ci) {
        if (nuit$renderCustomSkyboxes(fogParameters, this.skyRenderer)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At("HEAD")
    )
    private void nuit$resetNeoForgeSkyReplacement(SkyRenderState skyRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, CallbackInfo ci) {
        nuit$skipNeoForgeVanillaSky = false;
    }

    @Group(name = "nuit$renderCustomSkyboxes", min = 1, max = 1)
    @Inject(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER)
    )
    private void nuit$renderCustomSkyboxesNeoForge(SkyRenderState skyRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, CallbackInfo ci) {
        nuit$skipNeoForgeVanillaSky = nuit$renderCustomSkyboxes(fogParameters, this.skyRenderer);
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndSky()V")
    )
    private boolean nuit$renderNeoForgeEndSky(SkyRenderer skyRenderer) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V")
    )
    private boolean nuit$renderNeoForgeEndFlash(SkyRenderer skyRenderer, PoseStack poseStack, float intensity, float xAngle, float yAngle) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(I)V")
    )
    private boolean nuit$renderNeoForgeSkyDisc(SkyRenderer skyRenderer, int color) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V")
    )
    private boolean nuit$renderNeoForgeSunriseAndSunset(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, int color) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V")
    )
    private boolean nuit$renderNeoForgeSunMoonAndStars(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderDarkDisc()V")
    )
    private boolean nuit$renderNeoForgeDarkDisc(SkyRenderer skyRenderer) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @Unique
    private static boolean nuit$renderCustomSkyboxes(GpuBufferSlice fogParameters, SkyRenderer skyRenderer) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes()) {
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
