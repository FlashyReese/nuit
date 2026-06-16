package me.flashyreese.mods.nuit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import me.flashyreese.mods.nuit.api.NuitApi;
import me.flashyreese.mods.nuit.api.skyboxes.RenderableSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderAccess;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import me.flashyreese.mods.nuit.components.Metadata;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.DefaultHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4fStack;

import java.util.*;

public class SkyboxManager implements NuitApi {
    private static final SkyboxManager INSTANCE = new SkyboxManager();
    private final List<Identifier> preloadedTextures = new ArrayList<>();
    private final Map<Identifier, Skybox> skyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    /**
     * Stores a list of permanent skyboxes
     *
     * @see #addPermanentSkybox(Identifier, Skybox)
     */
    private final Map<Identifier, Skybox> permanentSkyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    private final List<Skybox> activeSkyboxes = new LinkedList<>();
    private Skybox currentSkybox = null;
    private boolean enabled = true;

    public static Optional<Skybox> parseSkyboxJson(Identifier resourceLocation, JsonObject jsonObject) {
        Metadata metadata;

        try {
            metadata = Metadata.CODEC.decode(JsonOps.INSTANCE, jsonObject).getOrThrow().getFirst();
        } catch (RuntimeException e) {
            NuitClient.getLogger().warn("Skipping invalid skybox {}", resourceLocation.toString(), e);
            NuitClient.getLogger().warn(jsonObject.toString());
            return Optional.empty();
        }

        Optional<SkyboxType<?>> optionalType = SkyboxType.get(metadata.type());
        if (optionalType.isEmpty()) {
            NuitClient.getLogger().warn("Skipping skybox {} with unknown type {}", resourceLocation.toString(), metadata.type().getPath().replace('_', '-'));
            return Optional.empty();
        }

        SkyboxType<?> type = optionalType.get();
        try {
            return Optional.of(type.getCodec(metadata.schemaVersion()).decode(JsonOps.INSTANCE, jsonObject).getOrThrow().getFirst());
        } catch (RuntimeException e) {
            NuitClient.getLogger().warn("Skipping invalid skybox {}", resourceLocation.toString(), e);
            NuitClient.getLogger().warn(jsonObject.toString());
            return Optional.empty();
        }
    }

    public static SkyboxManager getInstance() {
        return INSTANCE;
    }

    public void addSkybox(Identifier resourceLocation, JsonObject jsonObject) {
        Optional<Skybox> skybox = this.parseSkybox(resourceLocation, jsonObject);
        if (skybox.isPresent()) {
            NuitClient.getLogger().info("Adding skybox {}", resourceLocation.toString());
            this.addSkybox(resourceLocation, skybox.get());
        }
    }

    @Override
    public Optional<Skybox> parseSkybox(Identifier resourceLocation, JsonObject jsonObject) {
        return SkyboxManager.parseSkyboxJson(resourceLocation, jsonObject);
    }

    public void addSkybox(Identifier resourceLocation, Skybox skybox) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Preconditions.checkNotNull(skybox, "Skybox was null");
        DefaultHandler.addConditions(skybox);

        this.registerTextures(skybox);
        Skybox previousSkybox = this.skyboxMap.put(resourceLocation, skybox);
        if (previousSkybox != null) {
            this.activeSkyboxes.remove(previousSkybox);
            this.clearCurrentSkyboxIfRemoved(previousSkybox);
            this.releaseTextures(previousSkybox);
            this.rebuildDefaultConditions();
        }
    }

    /**
     * Permanent skyboxes are never cleared after a resource reload. This is
     * useful when adding skyboxes through code as resource reload listeners
     * have no defined order of being called.
     *
     * @param skybox the skybox to be added to the list of permanent skyboxes
     */
    public void addPermanentSkybox(Identifier resourceLocation, Skybox skybox) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Preconditions.checkNotNull(skybox, "Skybox was null");
        DefaultHandler.addConditions(skybox);
        this.registerTextures(skybox);
        Skybox previousSkybox = this.permanentSkyboxMap.put(resourceLocation, skybox);
        if (previousSkybox != null) {
            this.activeSkyboxes.remove(previousSkybox);
            this.clearCurrentSkyboxIfRemoved(previousSkybox);
            this.releaseTextures(previousSkybox);
            this.rebuildDefaultConditions();
        }
    }

    @Override
    public boolean removeSkybox(Identifier resourceLocation) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Skybox skybox = this.skyboxMap.remove(resourceLocation);
        if (skybox == null) {
            return false;
        }

        this.activeSkyboxes.remove(skybox);
        this.clearCurrentSkyboxIfRemoved(skybox);
        this.releaseTextures(skybox);
        this.rebuildDefaultConditions();
        return true;
    }

    @Override
    public boolean removePermanentSkybox(Identifier resourceLocation) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Skybox skybox = this.permanentSkyboxMap.remove(resourceLocation);
        if (skybox == null) {
            return false;
        }

        this.activeSkyboxes.remove(skybox);
        this.clearCurrentSkyboxIfRemoved(skybox);
        this.releaseTextures(skybox);
        this.rebuildDefaultConditions();
        return true;
    }

    @Internal
    public void clearSkyboxes() {
        DefaultHandler.clearConditionsExcept(this.permanentSkyboxMap.values());
        this.skyboxMap.values().forEach(this::releaseTextures);
        this.skyboxMap.clear();
        this.activeSkyboxes.clear();
        this.currentSkybox = null;
    }

    @Internal
    public void renderSkyboxes(SkyRenderer skyRenderer, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters) {
        SkyboxRenderContext context = new SkyboxRenderContext(createRenderAccess(skyRenderer), matrix4fStack, tickDelta, camera, fogParameters);
        for (Skybox skybox : this.activeSkyboxes) {
            if (skybox instanceof RenderableSkybox renderableSkybox) {
                this.currentSkybox = skybox;
                renderableSkybox.render(context);
            }
        }
        //RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static SkyboxRenderAccess createRenderAccess(SkyRenderer skyRenderer) {
        SkyRendererAccessor skyRendererAccessor = (SkyRendererAccessor) skyRenderer;
        return new SkyboxRenderAccess() {
            @Override
            public void renderSkyDisc(int color) {
                skyRenderer.renderSkyDisc(color);
            }

            @Override
            public void renderDarkDisc() {
                skyRenderer.renderDarkDisc();
            }

            @Override
            public void renderStars(float brightness, PoseStack poseStack) {
                skyRendererAccessor.invokeRenderStars(brightness, poseStack);
            }

            @Override
            public void renderEndFlash(float intensity, float xAngle, float yAngle) {
                skyRenderer.renderEndFlash(new PoseStack(), intensity, xAngle, yAngle);
            }

            @Override
            public Identifier endSkyTexture() {
                return SkyRendererAccessor.getEndSky();
            }
        };
    }

    public boolean hasActiveRenderableSkyboxes() {
        return this.activeSkyboxes.stream().anyMatch(RenderableSkybox.class::isInstance);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Optional<Skybox> getCurrentSkybox() {
        return Optional.ofNullable(this.currentSkybox);
    }

    @Override
    public Optional<Skybox> getSkybox(Identifier resourceLocation) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Skybox skybox = this.skyboxMap.get(resourceLocation);
        if (skybox == null) {
            skybox = this.permanentSkyboxMap.get(resourceLocation);
        }
        return Optional.ofNullable(skybox);
    }

    @Override
    public Map<Identifier, Skybox> getSkyboxes() {
        return Collections.unmodifiableMap(this.skyboxMap);
    }

    @Override
    public List<Skybox> getActiveSkyboxes() {
        return Collections.unmodifiableList(this.activeSkyboxes);
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

    public Map<Identifier, Skybox> getSkyboxMap() {
        return Collections.unmodifiableMap(this.skyboxMap);
    }

    private void registerTextures(Skybox skybox) {
        if (skybox instanceof SkyboxTextureProvider textureProvider) {
            textureProvider.getTexturesToRegister().forEach((theIdentifier) -> {
                Minecraft.getInstance().getTextureManager().registerAndLoad(theIdentifier, new SimpleTexture(theIdentifier));
                this.preloadedTextures.add(theIdentifier);
            });
        }
    }

    private void releaseTextures(Skybox skybox) {
        if (skybox instanceof SkyboxTextureProvider textureProvider) {
            textureProvider.getTexturesToRegister().forEach(texture -> {
                if (this.preloadedTextures.remove(texture) && !this.preloadedTextures.contains(texture)) {
                    Minecraft.getInstance().getTextureManager().release(texture);
                }
            });
        }
    }

    private void clearCurrentSkyboxIfRemoved(Skybox skybox) {
        if (this.currentSkybox == skybox) {
            this.currentSkybox = null;
        }
    }

    private void rebuildDefaultConditions() {
        List<Skybox> skyboxes = new ArrayList<>(this.permanentSkyboxMap.values());
        skyboxes.addAll(this.skyboxMap.values());
        DefaultHandler.clearConditionsExcept(skyboxes);
    }
}
