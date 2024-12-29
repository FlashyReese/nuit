package io.github.amerebagatelle.mods.nuit.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private SkyRenderer skyRenderer;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = "method_62215", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lnet/minecraft/client/renderer/FogParameters;)V", shift = At.Shift.AFTER), cancellable = true)
    private void renderCustomSkyboxes(FogParameters fogParameters, DimensionSpecialEffects.SkyType skyType, float tickDelta, DimensionSpecialEffects dimensionSpecialEffects, CallbackInfo ci) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            PoseStack poseStack = new PoseStack();
            MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
            skyboxManager.renderSkyboxes(
                    (SkyRendererAccessor) skyRenderer,
                    poseStack,
                    tickDelta,
                    Minecraft.getInstance().gameRenderer.getMainCamera(),
                    bufferSource,
                    fogParameters,
                    FogRenderer::toggleFog
            );
            ci.cancel();
        }
    }
}
