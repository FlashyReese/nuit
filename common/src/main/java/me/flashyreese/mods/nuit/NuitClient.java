package me.flashyreese.mods.nuit;

import me.flashyreese.mods.nuit.config.NuitConfig;
import me.flashyreese.mods.nuit.resource.SkyboxResourceListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NuitClient {
    public static final String MOD_ID = "nuit";
    private static final SkyboxResourceListener skyboxResourceListener = new SkyboxResourceListener();
    private static Logger LOGGER;
    private static NuitConfig CONFIG;

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

    public static SkyboxResourceListener skyboxResourceListener() {
        return skyboxResourceListener;
    }

    private static NuitConfig loadConfig() {
        return NuitConfig.load(NuitPlatformHelper.INSTANCE.getConfigDir().resolve("nuit-config.json").toFile());
    }
}
