package io.github.amerebagatelle.mods.nuit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import io.github.amerebagatelle.mods.nuit.api.NuitApi;
import io.github.amerebagatelle.mods.nuit.api.NuitPlatformHelper;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.components.Metadata;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.skybox.DefaultHandler;
import io.github.amerebagatelle.mods.nuit.skybox.SkyboxType;
import io.github.amerebagatelle.mods.nuit.skybox.TextureRegistrar;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.*;

public class SkyboxManager implements NuitApi {
    private static final SkyboxManager INSTANCE = new SkyboxManager();
    private final List<ResourceLocation> preloadedTextures = new ArrayList<>();
    private final Map<ResourceLocation, Skybox> skyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    /**
     * Stores a list of permanent skyboxes
     *
     * @see #addPermanentSkybox(ResourceLocation, Skybox)
     */
    private final Map<ResourceLocation, Skybox> permanentSkyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    private final List<Skybox> activeSkyboxes = new LinkedList<>();
    private Skybox currentSkybox = null;
    private boolean enabled = true;

    public static Optional<Skybox> parseSkyboxJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
        Metadata metadata;

        try {
            metadata = Metadata.CODEC.decode(JsonOps.INSTANCE, jsonObject).getOrThrow().getFirst();
        } catch (RuntimeException e) {
            NuitClient.getLogger().warn("Skipping invalid skybox {}", resourceLocation.toString(), e);
            NuitClient.getLogger().warn(jsonObject.toString());
            return Optional.empty();
        }

        Optional<Holder.Reference<SkyboxType<? extends Skybox>>> optionalType = NuitPlatformHelper.INSTANCE.getSkyboxTypeRegistry().get(metadata.type());
        if (optionalType.isEmpty()) {
            NuitClient.getLogger().warn("Skipping skybox {} with unknown type {}", resourceLocation.toString(), metadata.type().getPath().replace('_', '-'));
            return Optional.empty();
        }

        Holder.Reference<SkyboxType<? extends Skybox>> type = optionalType.get();
        try {
            return Optional.of(type.value().getCodec(metadata.schemaVersion()).decode(JsonOps.INSTANCE, jsonObject).getOrThrow().getFirst());
        } catch (RuntimeException e) {
            NuitClient.getLogger().warn("Skipping invalid skybox {}", resourceLocation.toString(), e);
            NuitClient.getLogger().warn(jsonObject.toString());
            return Optional.empty();
        }
    }

    public static SkyboxManager getInstance() {
        return INSTANCE;
    }

    public void addSkybox(ResourceLocation resourceLocation, JsonObject jsonObject) {
        Optional<Skybox> skybox = SkyboxManager.parseSkyboxJson(resourceLocation, jsonObject);
        if (skybox.isPresent()) {
            NuitClient.getLogger().info("Adding skybox {}", resourceLocation.toString());
            this.addSkybox(resourceLocation, skybox.get());
        }
    }

    public void addSkybox(ResourceLocation resourceLocation, Skybox skybox) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Preconditions.checkNotNull(skybox, "Skybox was null");
        DefaultHandler.addConditions(skybox);

        if (skybox instanceof TextureRegistrar textureRegistrar) {
            textureRegistrar.getTexturesToRegister().forEach((theResourceLocation) -> {
                Minecraft.getInstance().getTextureManager().registerAndLoad(theResourceLocation, new SimpleTexture(theResourceLocation));
                this.preloadedTextures.add(theResourceLocation);
            });
        }

        this.skyboxMap.put(resourceLocation, skybox);
    }

    /**
     * Permanent skyboxes are never cleared after a resource reload. This is
     * useful when adding skyboxes through code as resource reload listeners
     * have no defined order of being called.
     *
     * @param skybox the skybox to be added to the list of permanent skyboxes
     */
    public void addPermanentSkybox(ResourceLocation resourceLocation, Skybox skybox) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Preconditions.checkNotNull(skybox, "Skybox was null");
        DefaultHandler.addConditions(skybox);
        this.permanentSkyboxMap.put(resourceLocation, skybox);
    }

    @Internal
    public void clearSkyboxes() {
        DefaultHandler.clearConditionsExcept(this.permanentSkyboxMap.values());
        this.skyboxMap.clear();
        this.activeSkyboxes.clear();
        this.preloadedTextures.forEach(texture -> Minecraft.getInstance().getTextureManager().release(texture));
        this.preloadedTextures.clear();
    }

    @Internal
    public void renderSkyboxes(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters, Runnable fogCallback) {
        for (Skybox skybox : this.activeSkyboxes) {
            this.currentSkybox = skybox;
            skybox.render(skyRendererAccessor, poseStack, tickDelta, camera, bufferSource, fogParameters, fogCallback);
            bufferSource.endBatch();
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Skybox getCurrentSkybox() {
        return this.currentSkybox;
    }

    @Override
    public List<Skybox> getActiveSkyboxes() {
        return this.activeSkyboxes;
    }

    public void tick(ClientLevel level) {
        for (Skybox skybox : Iterables.concat(this.skyboxMap.values(), this.permanentSkyboxMap.values())) {
            skybox.tick(level);
        }

        this.activeSkyboxes.removeIf(skybox -> !skybox.isActive());

        // Add the skyboxes to a activeSkyboxes container so that they can be ordered
        for (Skybox skybox : Iterables.concat(this.skyboxMap.values(), this.permanentSkyboxMap.values())) {
            if (!this.activeSkyboxes.contains(skybox) && skybox.isActive()) {
                this.activeSkyboxes.add(skybox);
            }
        }
        this.activeSkyboxes.sort(Comparator.comparingInt(Skybox::getLayer));
    }

    public Map<ResourceLocation, Skybox> getSkyboxMap() {
        return this.skyboxMap;
    }
}