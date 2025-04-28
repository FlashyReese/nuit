package me.flashyreese.mods.nuit.api.skyboxes;

import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import net.minecraft.client.multiplayer.ClientLevel;

public interface NuitSkybox extends Skybox {
    float getAlpha();

    void updateAlpha(ClientLevel level);

    Properties getProperties();

    Conditions getConditions();
}

