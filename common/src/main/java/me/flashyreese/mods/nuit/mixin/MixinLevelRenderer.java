package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer {
    /**
     * Replaces vanilla sky rendering with Nuit's skyboxes when custom skyboxes are active.
     * Runs after fog setup so NeoForge's dimension renderer can handle the sky first.
     */
    @Inject(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", ordinal = 0, shift = At.Shift.AFTER),
            cancellable = true
    )
    private void nuit$renderCustomSkyboxes(
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float tickDelta,
            Camera camera,
            boolean thickFog,
            Runnable fogCallback,
            CallbackInfo ci
    ) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (!skyboxManager.isEnabled() || skyboxManager.getActiveSkyboxes().isEmpty()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelViewMatrix);
        skyboxManager.renderSkyboxes(
                (SkyRendererAccessor) this,
                poseStack,
                projectionMatrix,
                tickDelta,
                camera,
                thickFog,
                fogCallback
        );
        ci.cancel();
    }
}
