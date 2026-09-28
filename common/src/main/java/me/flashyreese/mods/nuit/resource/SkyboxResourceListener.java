package me.flashyreese.mods.nuit.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.api.NuitApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class SkyboxResourceListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();

    @Override
    protected @NotNull Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> skyboxJson = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources("sky", resourceLocation -> resourceLocation.getNamespace().startsWith(NuitClient.MOD_ID) && resourceLocation.getPath().endsWith(".json"));
        resources.forEach((resourceLocation, resource) -> {
            try (InputStream inputStream = resource.open(); InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                skyboxJson.put(resourceLocation, json);
            } catch (Exception e) {
                NuitClient.getLogger().error("Error reading skybox {}", resourceLocation.toString(), e);
            }
        });
        return skyboxJson;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> skyboxJson, ResourceManager resourceManager, ProfilerFiller profiler) {
        NuitApi skyboxManager = NuitApi.getInstance();
        skyboxManager.clearSkyboxes();
        skyboxJson.forEach(skyboxManager::addSkybox);
    }
}
