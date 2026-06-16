package me.flashyreese.mods.nuit.api;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        return 1;
    }

    /**
     * Registers a skybox type with Nuit's type registry.
     * <p>
     * Addons should call this during client/mod initialization, before resource
     * packs are loaded. Late registration can work for Nuit's parser, but the
     * platform registry may not mirror the type if its registry event already ran.
     *
     * @param name          Identifier for skybox type.
     * @param schemaVersion Schema version supported by codec.
     * @param codec         Skybox codec.
     * @return Registered skybox type.
     */
    static <T extends Skybox> SkyboxType<T> registerSkyboxType(Identifier name, int schemaVersion, Codec<T> codec) {
        return SkyboxType.register(name, schemaVersion, codec);
    }

    /**
     * Registers a skybox type with Nuit's type registry.
     *
     * @param type Skybox type.
     * @return Registered skybox type.
     */
    static <T extends Skybox> SkyboxType<T> registerSkyboxType(SkyboxType<T> type) {
        return SkyboxType.register(type);
    }

    /**
     * Gets all skybox types known to Nuit's parser.
     *
     * @return Read-only registered skybox types.
     */
    static Collection<SkyboxType<?>> getRegisteredSkyboxTypes() {
        return SkyboxType.values();
    }

    /**
     * Allows mods to add new skyboxes at runtime.
     *
     * @param resourceLocation Identifier for skybox.
     * @param skybox           Skybox implementation.
     */
    void addSkybox(Identifier resourceLocation, Skybox skybox);

    /**
     * Allows mods to add new skyboxes with a {@link JsonObject} at runtime.
     * This method applies {@link SkyboxManager#parseSkyboxJson(Identifier, JsonObject)}
     * serialization and adds the skybox with {@link #addSkybox(Identifier, Skybox)}
     *
     * @param resourceLocation Identifier for skybox.
     * @param jsonObject       Json Object.
     */
    void addSkybox(Identifier resourceLocation, JsonObject jsonObject);

    /**
     * Parses a skybox from its JSON representation without adding it to Nuit.
     *
     * @param resourceLocation Identifier for skybox.
     * @param jsonObject       Json Object.
     * @return Parsed skybox, or empty when the skybox is invalid.
     */
    Optional<Skybox> parseSkybox(Identifier resourceLocation, JsonObject jsonObject);

    /**
     * Allows mods to add new permanent skyboxes at runtime.
     *
     * @param resourceLocation Identifier for skybox.
     * @param skybox           Skybox implementation.
     */
    void addPermanentSkybox(Identifier resourceLocation, Skybox skybox);

    /**
     * Removes a non-permanent skybox.
     *
     * @param resourceLocation Identifier for skybox.
     * @return Whether a skybox was removed.
     */
    boolean removeSkybox(Identifier resourceLocation);

    /**
     * Removes a permanent skybox.
     *
     * @param resourceLocation Identifier for skybox.
     * @return Whether a skybox was removed.
     */
    boolean removePermanentSkybox(Identifier resourceLocation);

    /**
     * Clears all non-permanent skyboxes.
     */
    void clearSkyboxes();

    /**
     * Gets the current skybox that is being rendered.
     *
     * @return Current skybox being rendered, or empty when nothing is being rendered.
     */
    Optional<Skybox> getCurrentSkybox();

    /**
     * Gets a skybox by id.
     *
     * @param resourceLocation Identifier for skybox.
     * @return Skybox, or empty when no skybox is registered with that id.
     */
    Optional<Skybox> getSkybox(Identifier resourceLocation);

    /**
     * Gets all non-permanent skyboxes.
     *
     * @return Read-only map of skyboxes.
     */
    Map<Identifier, Skybox> getSkyboxes();

    /**
     * Gets a list of active skyboxes.
     *
     * @return Read-only current list of active skyboxes.
     */
    List<Skybox> getActiveSkyboxes();
}
