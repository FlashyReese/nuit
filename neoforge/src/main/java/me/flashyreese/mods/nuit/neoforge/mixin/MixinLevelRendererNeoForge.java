package me.flashyreese.mods.nuit.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.render.NuitSkyboxRenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRendererNeoForge {
    @Unique
    private static float nuit$tickDelta;

    @Unique
    private boolean nuit$skipVanillaSky;

    @Dynamic("NeoForge calls this sky pass overload directly")
    @Inject(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V",
            remap = false,
            at = @At("HEAD")
    )
    private void nuit$captureTickDelta(
            FrameGraphBuilder frameGraphBuilder,
            Camera camera,
            GpuBufferSlice fogParameters,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        nuit$tickDelta = camera.getPartialTickTime();
    }

    @ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        return NuitSkyboxRenderHooks.allowCustomSkyPass(renderSky);
    }

    @Dynamic("NeoForge delegates the vanilla sky pass to this overload")
    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V",
            remap = false,
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;",
                    remap = true
            )
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return NuitSkyboxRenderHooks.skyboxForPass(original);
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @Inject(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At("HEAD")
    )
    private void nuit$resetSkyReplacement(
            SkyRenderState skyRenderState,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogParameters,
            SkyRenderer skyRenderer,
            CallbackInfo ci
    ) {
        this.nuit$skipVanillaSky = false;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @Inject(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER,
                    remap = true
            )
    )
    private void nuit$renderCustomSkyboxes(
            SkyRenderState skyRenderState,
            Matrix4f projectionMatrix,
            GpuBufferSlice fogParameters,
            SkyRenderer skyRenderer,
            CallbackInfo ci
    ) {
        this.nuit$skipVanillaSky = NuitSkyboxRenderHooks.renderCustomSkyboxes(
                fogParameters,
                skyRenderer,
                nuit$tickDelta
        );
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndSky()V",
                    remap = true
            )
    )
    private boolean nuit$renderEndSky(SkyRenderer skyRenderer) {
        return !this.nuit$skipVanillaSky;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderEndFlash(Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
                    remap = true
            )
    )
    private boolean nuit$renderEndFlash(
            SkyRenderer skyRenderer,
            PoseStack poseStack,
            float intensity,
            float xAngle,
            float yAngle
    ) {
        return !this.nuit$skipVanillaSky;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(I)V",
                    remap = true
            )
    )
    private boolean nuit$renderSkyDisc(SkyRenderer skyRenderer, int color) {
        return !this.nuit$skipVanillaSky;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V",
                    remap = true
            )
    )
    private boolean nuit$renderSunriseAndSunset(
            SkyRenderer skyRenderer,
            PoseStack poseStack,
            float sunAngle,
            int color
    ) {
        return !this.nuit$skipVanillaSky;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V",
                    remap = true
            )
    )
    private boolean nuit$renderSunMoonAndStars(
            SkyRenderer skyRenderer,
            PoseStack poseStack,
            float sunAngle,
            float moonAngle,
            float starAngle,
            MoonPhase moonPhase,
            float rainBrightness,
            float starBrightness
    ) {
        return !this.nuit$skipVanillaSky;
    }

    @Dynamic("NeoForge replaces the vanilla sky pass body with this synthetic lambda")
    @WrapWithCondition(
            method = "lambda$addSkyPass$8(Lnet/minecraft/client/renderer/state/SkyRenderState;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/SkyRenderer;)V",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;renderDarkDisc()V",
                    remap = true
            )
    )
    private boolean nuit$renderDarkDisc(SkyRenderer skyRenderer) {
        return !this.nuit$skipVanillaSky;
    }
}
