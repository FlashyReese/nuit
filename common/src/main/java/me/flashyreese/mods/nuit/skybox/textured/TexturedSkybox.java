package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements SkyboxTextureProvider {
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
     * @param context The current skybox render context.
     */
    @Override
    public final void render(SkyboxRenderContext context) {
        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        try {
            Vector4f colorModifier = this.blend.getColorModifier(this.alpha);
            modelViewStack.set(context.skyModelViewStack());
            this.rotation.apply(modelViewStack, level);
            GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(modelViewStack), colorModifier);
            this.renderSkybox(context, modelViewStack, dynamicTransforms);
        } finally {
            modelViewStack.popMatrix();
        }
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     *
     * @param context the current skybox render context
     * @param modelViewStack the mutable model-view stack after this skybox's rotation has been applied
     * @param dynamicTransforms dynamic transform uniforms created from {@code modelViewStack}
     */
    public abstract void renderSkybox(SkyboxRenderContext context, Matrix4fStack modelViewStack, GpuBufferSlice dynamicTransforms);
}
