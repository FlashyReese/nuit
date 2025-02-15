package io.github.amerebagatelle.mods.nuit.api.skyboxes;

import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import net.minecraft.client.multiplayer.ClientLevel;

public interface NuitSkybox extends Skybox {
    float getAlpha();

    void updateAlpha(ClientLevel level);

    Properties getProperties();

    Conditions getConditions();
}

