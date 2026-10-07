package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.render.NuitSkyRenderState;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
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
    private ClientLevel level;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Unique
    private static boolean nuit$skipNeoForgeVanillaSky;

    @Inject(method = "extractLevel", at = @At("RETURN"))
    private void nuit$extractSkyboxes(DeltaTracker deltaTracker, Camera camera, float worldPartialTicks, CallbackInfo ci) {
        ((NuitSkyRenderStateHolder) this.levelRenderState.skyRenderState)
                .nuit$setRenderState(SkyboxManager.getInstance().extract(this.level, worldPartialTicks, camera));
    }

    @ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        return renderSky || this.nuit$hasCustomSkyboxes();
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowFabricSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return this.nuit$skyboxForPass(original);
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowNeoForgeSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return this.nuit$skyboxForPass(original);
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
        if (nuit$renderCustomSkyboxes(fogParameters, skyRenderer, skyRenderState)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At("HEAD")
    )
    private void nuit$resetNeoForgeSkyReplacement(SkyRenderState skyRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, SkyRenderer skyRenderer, CallbackInfo ci) {
        nuit$skipNeoForgeVanillaSky = false;
    }

    @Group(name = "nuit$renderCustomSkyboxes", min = 1, max = 1)
    @Inject(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER)
    )
    private static void nuit$renderCustomSkyboxesNeoForge(SkyRenderState skyRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, SkyRenderer skyRenderer, CallbackInfo ci) {
        nuit$skipNeoForgeVanillaSky = nuit$renderCustomSkyboxes(fogParameters, skyRenderer, skyRenderState);
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndSky()V")
    )
    private boolean nuit$renderNeoForgeEndSky(SkyRenderer skyRenderer) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V")
    )
    private boolean nuit$renderNeoForgeEndFlash(SkyRenderer skyRenderer, PoseStack poseStack, float intensity, float xAngle, float yAngle) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(I)V")
    )
    private boolean nuit$renderNeoForgeSkyDisc(SkyRenderer skyRenderer, int color) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V")
    )
    private boolean nuit$renderNeoForgeSunriseAndSunset(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, int color) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V")
    )
    private boolean nuit$renderNeoForgeSunMoonAndStars(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @WrapWithCondition(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderDarkDisc()V")
    )
    private boolean nuit$renderNeoForgeDarkDisc(SkyRenderer skyRenderer) {
        return !nuit$skipNeoForgeVanillaSky;
    }

    @Unique
    private static boolean nuit$renderCustomSkyboxes(GpuBufferSlice fogParameters, SkyRenderer skyRenderer, SkyRenderState skyRenderState) {
        NuitSkyRenderState renderState = ((NuitSkyRenderStateHolder) skyRenderState).nuit$getRenderState();
        if (renderState == null) {
            return false;
        }

        Matrix4fStack skyModelViewStack = new Matrix4fStack(16);
        skyModelViewStack.set(RenderSystem.getModelViewStack());
        skyModelViewStack.setTranslation(0.0F, 0.0F, 0.0F);
        SkyboxManager.getInstance().renderSkyboxes(skyRenderer, renderState, skyModelViewStack, fogParameters);
        return true;
    }

    @Unique
    private DimensionType.Skybox nuit$skyboxForPass(DimensionType.Skybox original) {
        if (original == DimensionType.Skybox.NONE && this.nuit$hasCustomSkyboxes()) {
            return DimensionType.Skybox.OVERWORLD;
        }
        return original;
    }

    @Unique
    private boolean nuit$hasCustomSkyboxes() {
        return ((NuitSkyRenderStateHolder) this.levelRenderState.skyRenderState).nuit$getRenderState() != null;
    }
}
