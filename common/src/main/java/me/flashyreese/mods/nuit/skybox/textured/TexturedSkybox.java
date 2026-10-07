package me.flashyreese.mods.nuit.skybox.textured;

import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import org.joml.Matrix4f;
import org.joml.Vector4f;


public abstract class TexturedSkybox extends AbstractSkybox implements SkyboxTextureProvider {
    private final Rotation rotation;

    protected TexturedSkybox(Properties properties, Conditions conditions) {
        super(properties, conditions);
        this.rotation = properties.rotation();
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param context The current skybox render context.
     */
    @Override
    public final void render(SkyboxRenderContext context) {
        float alpha = context.alpha();
        if (alpha <= 0.0F) {
            return;
        }

        Vector4f colorModifier = this.properties.blend().getColorModifier(alpha);
        Matrix4f modelViewMatrix = context.rotatedModelViewMatrix();
        this.renderSkybox(context, modelViewMatrix, colorModifier);
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     * The matrix and color are local to this render call and may be modified before drawing.
     * Create dynamic transform uniforms after applying changes, and recreate them if the matrix
     * or color changes between draws. Draws with unchanged values may share the same uniforms.
     *
     * @param context the current skybox render context
     * @param modelViewMatrix the model-view matrix after this skybox's rotation has been applied
     * @param colorModifier the color modifier with this skybox's blend mode and alpha applied
     */
    public abstract void renderSkybox(SkyboxRenderContext context, Matrix4f modelViewMatrix, Vector4f colorModifier);
}
