package me.flashyreese.mods.nuit.neoforge;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.minecraft.core.Registry;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NuitNeoForgePlatformHelper {
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry() {
        return NuitNeoForge.REGISTRY;
    }
}
