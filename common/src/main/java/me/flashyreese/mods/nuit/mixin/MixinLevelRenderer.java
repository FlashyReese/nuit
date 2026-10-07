package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.render.NuitSkyRenderState;
import me.flashyreese.mods.nuit.render.NuitSkyRenderStateHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    /**
     * Keeps the sky visible under boss fog while custom skyboxes are active. Lava, powder snow and sky-blocking mob
     * effects still hide the sky.
     */
    @ModifyExpressionValue(
            method = "shouldRenderSky",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;shouldCreateBossFog:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean nuit$allowCustomSkyUnderBossFog(boolean shouldCreateBossFog) {
        return shouldCreateBossFog && !this.nuit$hasCustomSkyboxes();
    }

    @ModifyExpressionValue(
            method = "shouldRenderSky",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;", opcode = Opcodes.GETFIELD)
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        if (original == DimensionType.Skybox.NONE && this.nuit$hasCustomSkyboxes()) {
            return DimensionType.Skybox.OVERWORLD;
        }
        return original;
    }

    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     * <p>
     * The sky pass draws into the sky target without a depth attachment (improved transparency); the main pass draws
     * into the main target with one (classic transparency). In both cases the pass is already open and the fog uniforms
     * are bound, so Nuit draws straight into it.
     */
    @WrapOperation(
            method = {"lambda$addSkyPass$0", "lambda$addMainPass$0"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SkyRenderer;render(Lnet/minecraft/client/renderer/state/level/SkyRenderState;Lcom/mojang/renderpearl/api/commands/RenderPass;Lorg/joml/Vector4f;Z)V"
            ),
            require = 2
    )
    private void nuit$replaceVanillaSky(
            SkyRenderer skyRenderer,
            SkyRenderState state,
            RenderPass renderPass,
            Vector4f fogColor,
            boolean withDepthAttachment,
            Operation<Void> original
    ) {
        NuitSkyRenderState renderState = ((NuitSkyRenderStateHolder) state).nuit$getRenderState();
        if (renderState == null) {
            original.call(skyRenderer, state, renderPass, fogColor, withDepthAttachment);
            return;
        }

        Matrix4fStack skyModelViewStack = new Matrix4fStack(16);
        skyModelViewStack.set(RenderSystem.getModelViewStack());
        skyModelViewStack.setTranslation(0.0F, 0.0F, 0.0F);
        SkyboxManager.getInstance().renderSkyboxes(
                skyRenderer,
                state,
                renderState,
                renderPass,
                fogColor,
                withDepthAttachment,
                skyModelViewStack
        );
    }

    @Unique
    private boolean nuit$hasCustomSkyboxes() {
        @Nullable NuitSkyRenderState renderState = ((NuitSkyRenderStateHolder) this.levelRenderState.skyRenderState).nuit$getRenderState();
        return renderState != null;
    }
}
