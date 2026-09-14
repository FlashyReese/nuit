package me.flashyreese.mods.nuit.skybox.textured;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix4f;
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
        if (this.alpha <= 0.0F) {
            return;
        }

        Camera camera = context.camera();
        ClientLevel level = Objects.requireNonNull((ClientLevel) camera.entity().level());
        double celestialAngle = camera.attributeProbe().getValue(
                EnvironmentAttributes.SUN_ANGLE,
                context.tickDelta()
        );
        Vector4f colorModifier = this.blend.getColorModifier(this.alpha);
        Matrix4f modelViewMatrix = this.rotation.apply(
                new Matrix4f(context.skyModelViewStack()),
                level,
                this.properties.clock(),
                context.tickDelta(),
                celestialAngle
        );
        GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(modelViewMatrix, colorModifier);
        this.renderSkybox(context, modelViewMatrix, dynamicTransforms);
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     *
     * @param context the current skybox render context
     * @param modelViewMatrix the model-view matrix after this skybox's rotation has been applied
     * @param dynamicTransforms dynamic transform uniforms created from {@code modelViewMatrix}
     */
    public abstract void renderSkybox(SkyboxRenderContext context, Matrix4f modelViewMatrix, GpuBufferSlice dynamicTransforms);
}
