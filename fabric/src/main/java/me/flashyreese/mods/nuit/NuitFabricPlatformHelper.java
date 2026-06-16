package me.flashyreese.mods.nuit;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import me.flashyreese.mods.nuit.fabric.NuitClientFabric;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;

import java.nio.file.Path;

final class NuitFabricPlatformHelper implements NuitPlatformHelper {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry() {
        return NuitClientFabric.REGISTRY;
    }
}
