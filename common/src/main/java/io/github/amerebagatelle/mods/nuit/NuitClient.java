package io.github.amerebagatelle.mods.nuit;

import io.github.amerebagatelle.mods.nuit.api.NuitPlatformHelper;
import io.github.amerebagatelle.mods.nuit.config.NuitConfig;
import io.github.amerebagatelle.mods.nuit.resource.SkyboxResourceListener;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NuitClient {
    public static final String MOD_ID = "nuit";

    private static Logger LOGGER;
    private static NuitConfig CONFIG;
    private static final SkyboxResourceListener skyboxResourceListener = new SkyboxResourceListener();

    public static void init() {
        SkyboxManager.getInstance().setEnabled(config().generalSettings.enable);
    }

    public static Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger("Nuit");
        }

        return LOGGER;
    }

    public static NuitConfig config() {
        if (CONFIG == null) {
            CONFIG = loadConfig();
        }

        return CONFIG;
    }

    public static ResourceLocation withId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static SkyboxResourceListener skyboxResourceListener() {
        return skyboxResourceListener;
    }

    private static NuitConfig loadConfig() {
        return NuitConfig.load(NuitPlatformHelper.INSTANCE.getConfigDir().resolve("nuit-config.json").toFile());
    }
}
