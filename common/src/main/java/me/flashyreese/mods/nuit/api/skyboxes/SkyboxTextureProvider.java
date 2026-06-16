package me.flashyreese.mods.nuit.api.skyboxes;

import net.minecraft.resources.Identifier;

import java.util.Collection;

/**
 * Optional contract for skyboxes that want Nuit to preload and release their textures.
 */
public interface SkyboxTextureProvider {
    /**
     * @return texture identifiers required by this skybox.
     */
    Collection<Identifier> getTexturesToRegister();
}
