package me.flashyreese.mods.nuit.fabric;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;

import java.nio.file.Path;

public final class NuitFabricPlatformHelper {
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry() {
        return NuitClientFabric.REGISTRY;
    }
}
