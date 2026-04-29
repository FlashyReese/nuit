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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SkyboxResourceListener implements PreparableReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setStrictness(Strictness.LENIENT).create();

    public void readFiles(ResourceManager resourceManager) {
        NuitApi skyboxManager = NuitApi.getInstance();
        skyboxManager.clearSkyboxes();
        Map<Identifier, Resource> resources = resourceManager.listResources("sky", Identifier -> Identifier.getNamespace().startsWith(NuitClient.MOD_ID) && Identifier.getPath().endsWith(".json"));
        resources.forEach((Identifier, resource) -> {
            try (InputStream inputStream = resource.open(); InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                skyboxManager.addSkybox(Identifier, json);
            } catch (Exception e) {
                NuitClient.getLogger().error("Error reading skybox {}", Identifier.toString(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor executor, PreparationBarrier preparationBarrier, Executor executor2) {
        return CompletableFuture.runAsync(() -> this.readFiles(sharedState.resourceManager()), executor2).thenCompose(preparationBarrier::wait);
    }
}
