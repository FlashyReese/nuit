package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.skybox.TextureRegistrar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements TextureRegistrar {
    public Rotation rotation;

    protected TexturedSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
        this.rotation = properties.rotation();
    }

    protected TexturedSkybox(Properties properties, Conditions conditions, Blend blend) {
        this(properties.withBlend(blend), conditions);
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param skyRendererAccess stable access to vanilla sky buffers
     * @param poseStack sky model-view stack
     * @param projectionMatrix frame projection matrix
     * @param tickDelta partial tick
     * @param camera active camera
     * @param thickFog whether thick fog is active
     * @param fogCallback restores vanilla fog state
     */
    @Override
    public final void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, Matrix4f projectionMatrix,
                             float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        if (this.alpha <= 0.0F) {
            return;
        }

        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        try {
            NuitRenderBackend.beginSkybox(this.properties.blend(), this.alpha, GameRenderer::getPositionTexShader);
            try (NuitRenderBackend.TransformScope transform = NuitRenderBackend.pushTransform(poseStack)) {
                this.rotation.apply(transform.poseStack(), world, this.properties.clock(), tickDelta);
                this.renderSkybox(
                        skyRendererAccess,
                        transform.poseStack(),
                        projectionMatrix,
                        tickDelta,
                        camera,
                        thickFog,
                        fogCallback
                );
            }
        } finally {
            NuitRenderBackend.endSkybox();
        }
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     *
     * @param skyRendererAccess stable access to vanilla sky buffers
     * @param poseStack sky model-view stack after this skybox's rotation has been applied
     * @param projectionMatrix frame projection matrix
     * @param tickDelta partial tick
     * @param camera active camera
     * @param thickFog whether thick fog is active
     * @param fogCallback restores vanilla fog state
     */
    public abstract void renderSkybox(SkyRendererAccessor skyRendererAccess, PoseStack poseStack,
                                      Matrix4f projectionMatrix, float tickDelta, Camera camera,
                                      boolean thickFog, Runnable fogCallback);

    public Blend getBlend() {
        return this.properties.blend();
    }

    public Rotation getRotation() {
        return this.rotation;
    }
}
