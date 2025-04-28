package me.flashyreese.mods.nuit.fabric;

import me.flashyreese.mods.nuit.api.NuitPlatformHelper;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.skybox.SkyboxType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;

import java.nio.file.Path;

public class NuitFabricPlatformHelper implements NuitPlatformHelper {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry() {
        return NuitClientFabric.REGISTRY;
    }
}
