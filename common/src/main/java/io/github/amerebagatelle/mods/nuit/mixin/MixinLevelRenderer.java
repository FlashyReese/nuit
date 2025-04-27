package io.github.amerebagatelle.mods.nuit.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private float nuit$tickDelta;

    @Unique
    private FogParameters nuit$fogParameters;

    @Inject(method = "addSkyPass", at = @At(value = "HEAD"))
    private void nuit$preAddSkyPass(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, FogParameters fogParameters, CallbackInfo ci) {
        this.nuit$tickDelta = tickDelta;
        this.nuit$fogParameters = fogParameters;
    }

    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = {"method_62215", "lambda$addSkyPass$12"}, require = 1, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFog(Lnet/minecraft/client/renderer/FogParameters;)V", shift = At.Shift.AFTER), cancellable = true)
    private void nuit$renderCustomSkyboxes(CallbackInfo ci) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            PoseStack poseStack = new PoseStack();
            skyboxManager.renderSkyboxes(
                    (SkyRendererAccessor) skyRenderer,
                    poseStack,
                    this.nuit$tickDelta,
                    Minecraft.getInstance().gameRenderer.getMainCamera(),
                    this.renderBuffers.bufferSource(),
                    this.nuit$fogParameters
            );
            ci.cancel();
        }
    }
}
