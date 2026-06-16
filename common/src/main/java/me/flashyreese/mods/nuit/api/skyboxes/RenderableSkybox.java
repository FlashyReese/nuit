package me.flashyreese.mods.nuit.api.skyboxes;

/**
 * Skybox contract for implementations that draw during Nuit's sky render pass.
 */
public interface RenderableSkybox extends Skybox {
    /**
     * Renders this skybox for the current frame.
     *
     * @param context stable access to sky render state and vanilla sky helpers.
     */
    void render(SkyboxRenderContext context);
}
