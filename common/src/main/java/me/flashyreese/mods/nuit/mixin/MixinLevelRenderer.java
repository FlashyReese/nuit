package me.flashyreese.mods.nuit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {

    /**
     * Keeps the sky visible under boss fog while custom skyboxes are active. Lava, powder snow and sky-blocking mob
     * effects still hide the sky.
     */
    @ModifyExpressionValue(
            method = "shouldRenderSky",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;shouldCreateBossFog:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean nuit$allowCustomSkyUnderBossFog(boolean shouldCreateBossFog) {
        return shouldCreateBossFog && !nuit$hasCustomSkyboxes();
    }

    @ModifyExpressionValue(
            method = "shouldRenderSky",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;", opcode = Opcodes.GETFIELD)
    )
    private DimensionType.Skybox nuit$allowSkyPassForNoneSkybox(DimensionType.Skybox original) {
        if (original == DimensionType.Skybox.NONE && nuit$hasCustomSkyboxes()) {
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
        if (!nuit$hasCustomSkyboxes()) {
            original.call(skyRenderer, state, renderPass, fogColor, withDepthAttachment);
            return;
        }

        Matrix4f skyModelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrixCopy());
        skyModelViewMatrix.setTranslation(0.0F, 0.0F, 0.0F);
        Matrix4fStack skyModelViewStack = new Matrix4fStack(32);
        skyModelViewStack.set(skyModelViewMatrix);
        Minecraft minecraft = Minecraft.getInstance();
        SkyboxManager.getInstance().renderSkyboxes(
                skyRenderer,
                state,
                renderPass,
                fogColor,
                withDepthAttachment,
                skyModelViewStack,
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false),
                minecraft.gameRenderer.mainCamera()
        );
    }

    @Unique
    private static boolean nuit$hasCustomSkyboxes() {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        return skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes();
    }
}
