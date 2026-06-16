package me.flashyreese.mods.nuit.api.skyboxes;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Base skybox lifecycle contract.
 */
public interface Skybox {
    /**
     * Render order for active skyboxes. Lower layers render first.
     */
    default int getLayer() {
        return 0;
    }

    /**
     * Updates this skybox once per client world tick.
     */
    void tick(ClientLevel clientLevel);

    /**
     * @return whether this skybox should currently participate in rendering or skybox-dependent effects.
     */
    boolean isActive();
}
