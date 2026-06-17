package me.flashyreese.mods.nuit.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.api.NuitApi;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SkyboxResourceListener implements PreparableReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setStrictness(Strictness.LENIENT).create();

    private Map<Identifier, JsonObject> readFiles(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> skyboxJson = new LinkedHashMap<>();
        Map<Identifier, Resource> resources = resourceManager.listResources("sky", resourceLocation -> resourceLocation.getNamespace().startsWith(NuitClient.MOD_ID) && resourceLocation.getPath().endsWith(".json"));
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

    private void applySkyboxes(Map<Identifier, JsonObject> skyboxJson) {
        NuitApi skyboxManager = NuitApi.getInstance();
        skyboxManager.clearSkyboxes();
        skyboxJson.forEach(skyboxManager::addSkybox);
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(SharedState sharedState, Executor preparationExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        return CompletableFuture.supplyAsync(() -> this.readFiles(sharedState.resourceManager()), preparationExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(this::applySkyboxes, reloadExecutor);
    }
}
