package me.flashyreese.mods.nuit.api;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.skybox.SkyboxType;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.core.Registry;

import java.nio.file.Path;

public interface NuitPlatformHelper {
    NuitPlatformHelper INSTANCE = Utils.loadService(NuitPlatformHelper.class);

    Path getConfigDir();

    Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry();
}
