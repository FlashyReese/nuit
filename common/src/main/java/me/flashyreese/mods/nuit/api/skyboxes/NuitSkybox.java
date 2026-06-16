package me.flashyreese.mods.nuit.api.skyboxes;

import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Standard Nuit skybox contract for schema-backed skyboxes with alpha, properties, and conditions.
 */
public interface NuitSkybox extends RenderableSkybox {
    float getAlpha();

    void updateAlpha(ClientLevel level);

    Properties getProperties();

    Conditions getConditions();
}
