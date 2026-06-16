package me.flashyreese.mods.nuit;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import me.flashyreese.mods.nuit.neoforge.NuitNeoForge;
import net.minecraft.core.Registry;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

final class NuitNeoForgePlatformHelper implements NuitPlatformHelper {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry() {
        return NuitNeoForge.REGISTRY;
    }
}
