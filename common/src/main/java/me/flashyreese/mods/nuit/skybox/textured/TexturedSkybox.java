package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements TextureRegistrar {
    private final Rotation rotation;
    private final Blend blend;

    protected TexturedSkybox(Properties properties, Conditions conditions, Blend blend) {
        super(properties, conditions);
        this.blend = blend;
        this.rotation = properties.rotation();
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public Blend getBlend() {
        return this.blend;
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param poseStack         The current PoseStack.
     * @param skyRendererAccess Access to the skyRenderer as skyboxes often require it.
     * @param tickDelta         The current tick delta.
     * @param bufferSource
     */
    @Override
    public final void render(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            Vector4f colorModifier = this.blend.applyEquationAndGetColor(this.alpha);
            modelViewStack.set(matrix4fStack);
            this.rotation.apply(modelViewStack, level);
            GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(modelViewStack), colorModifier);
            this.renderSkybox(skyRendererAccess, modelViewStack, tickDelta, camera, dynamicTransforms, fogParameters, bufferSource);
        } finally {
            modelViewStack.popMatrix();
            GL46C.glBlendEquation(GL46C.GL_FUNC_ADD); // Fixme: avoid direct gl calls
        }
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(SkyRendererAccessor skyRendererAccess, Matrix4fStack matrix4f, float tickDelta, Camera camera, GpuBufferSlice dynamicTransforms, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource);
}
