package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.render.NuitSkyRenderState;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean nuit$allowCustomSkyPass(boolean renderSky) {
        return renderSky || this.nuit$hasCustomSkyboxes();
    }

    @ModifyExpressionValue(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        return this.nuit$skyboxForPass(original);
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
        if (!nuit$renderCustomSkyboxes(fogParameters, skyRenderer, skyRenderState)) {
            original.call(skyRenderer, fogParameters, skyRenderState);
        }
    }

    @Unique
    private static boolean nuit$renderCustomSkyboxes(GpuBufferSlice fogParameters, SkyRenderer skyRenderer, SkyRenderState skyRenderState) {
        NuitSkyRenderState renderState = ((NuitSkyRenderStateHolder) skyRenderState).nuit$getRenderState();
        if (renderState == null) {
            return false;
        }

        RenderSystem.setShaderFog(fogParameters);
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
