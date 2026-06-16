package me.flashyreese.mods.nuit;

import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.minecraft.core.Registry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;

interface NuitPlatformHelper {
    NuitPlatformHelper INSTANCE = load();

    Path getConfigDir();

    Registry<SkyboxType<? extends Skybox>> getSkyboxTypeRegistry();

    private static NuitPlatformHelper load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = NuitPlatformHelper.class.getClassLoader();
        }

        String servicePath = "META-INF/services/" + NuitPlatformHelper.class.getName();
        try {
            Enumeration<URL> serviceFiles = classLoader.getResources(servicePath);
            while (serviceFiles.hasMoreElements()) {
                NuitPlatformHelper service = loadFrom(serviceFiles.nextElement(), classLoader);
                if (service != null) {
                    NuitClient.getLogger().debug("Loaded {} for service {}", service, NuitPlatformHelper.class);
                    return service;
                }
            }
        } catch (IOException | ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load service for " + NuitPlatformHelper.class.getName(), exception);
        }

        throw new IllegalStateException("Failed to load service for " + NuitPlatformHelper.class.getName());
    }

    private static NuitPlatformHelper loadFrom(URL serviceFile, ClassLoader classLoader) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(serviceFile.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String providerName = line.split("#", 2)[0].trim();
                if (providerName.isEmpty()) {
                    continue;
                }

                Class<?> providerClass = Class.forName(providerName, true, classLoader);
                if (!NuitPlatformHelper.class.isAssignableFrom(providerClass)) {
                    throw new IllegalStateException(providerName + " is not a " + NuitPlatformHelper.class.getName());
                }

                Constructor<? extends NuitPlatformHelper> constructor = providerClass.asSubclass(NuitPlatformHelper.class).getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
        }

        return null;
    }
}
