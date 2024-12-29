package io.github.amerebagatelle.mods.nuit.api;

import com.google.gson.JsonObject;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface NuitApi {

    /**
     * @since API v0.0
     */
    static NuitApi getInstance() {
        return SkyboxManager.getInstance();
    }

    /**
     * Gets the version of this API, This is incremented when changes are implemented
     * without breaking API. Mods can use this to check if given API functionality
     * is available on the current version of installed Nuit.
     *
     * @return The current version of the API
     */
    static int getApiVersion() {
        return 0;
    }

    /**
     * Allows mods to add new skyboxes at runtime.
     *
     * @param resourceLocation Identifier for skybox.
     * @param skybox     Skybox implementation.
     */
    void addSkybox(ResourceLocation resourceLocation, Skybox skybox);

    /**
     * Allows mods to add new skyboxes with a {@link JsonObject} at runtime.
     * This method applies {@link SkyboxManager#parseSkyboxJson(ResourceLocation, JsonObject)}
     * serialization and adds the skybox with {@link #addSkybox(ResourceLocation, Skybox)}
     *
     * @param resourceLocation Identifier for skybox.
     * @param jsonObject Json Object.
     */
    void addSkybox(ResourceLocation resourceLocation, JsonObject jsonObject);

    /**
     * Allows mods to add new permanent skyboxes at runtime.
     *
     * @param resourceLocation Identifier for skybox.
     * @param skybox     Skybox implementation.
     */
    void addPermanentSkybox(ResourceLocation resourceLocation, Skybox skybox);

    /**
     * Clears all non-permanent skyboxes.
     */
    void clearSkyboxes();

    /**
     * Gets the current skybox that is being rendered.
     *
     * @return Current skybox being render, returns null of nothing is being rendered.
     */
    Skybox getCurrentSkybox();

    /**
     * Gets a list of active skyboxes.
     *
     * @return Current list of active skyboxes.
     */
    List<Skybox> getActiveSkyboxes();
}
