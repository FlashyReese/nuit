package me.flashyreese.mods.nuit.mixin;

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
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/dimension/DimensionType$Skybox;NONE:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowFabricSkyPassForNoneSkybox() {
        return nuit$skyboxNoneSentinel();
    }

    @Redirect(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V",
            require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/dimension/DimensionType$Skybox;NONE:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowNeoForgeSkyPassForNoneSkybox() {
        return nuit$skyboxNoneSentinel();
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

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndSky()V")
    )
    private void nuit$skipNeoForgeEndSky(SkyRenderer skyRenderer) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderEndSky();
        }
    }

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V")
    )
    private void nuit$skipNeoForgeEndFlash(SkyRenderer skyRenderer, PoseStack poseStack, float intensity, float xAngle, float yAngle) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderEndFlash(poseStack, intensity, xAngle, yAngle);
        }
    }

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(I)V")
    )
    private void nuit$skipNeoForgeSkyDisc(SkyRenderer skyRenderer, int color) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderSkyDisc(color);
        }
    }

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V")
    )
    private void nuit$skipNeoForgeSunriseAndSunset(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, int color) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderSunriseAndSunset(poseStack, sunAngle, color);
        }
    }

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V")
    )
    private void nuit$skipNeoForgeSunMoonAndStars(SkyRenderer skyRenderer, PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderSunMoonAndStars(poseStack, sunAngle, moonAngle, starAngle, moonPhase, rainBrightness, starBrightness);
        }
    }

    @Redirect(
            method = "lambda$addSkyPass$0(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderDarkDisc()V")
    )
    private void nuit$skipNeoForgeDarkDisc(SkyRenderer skyRenderer) {
        if (!nuit$skipNeoForgeVanillaSky) {
            skyRenderer.renderDarkDisc();
        }
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
    private static DimensionType.Skybox nuit$skyboxNoneSentinel() {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes()) {
            // This value is only used as the right side of skybox == NONE.
            return null;
        }
        return DimensionType.Skybox.NONE;
    }
}
