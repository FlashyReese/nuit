package me.flashyreese.mods.nuit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import me.flashyreese.mods.nuit.api.NuitApi;
import me.flashyreese.mods.nuit.api.skyboxes.RenderableSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderAccess;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import me.flashyreese.mods.nuit.components.clock.ClockSource;
import me.flashyreese.mods.nuit.components.Metadata;
import me.flashyreese.mods.nuit.components.Rotation;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.render.NuitStarRenderer;
import me.flashyreese.mods.nuit.skybox.DefaultHandler;
import me.flashyreese.mods.nuit.skybox.decorations.DecorationBox;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4fStack;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

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
    private CelestialController celestialController = null;
    private boolean celestialControllerConflict;
    private boolean enabled = true;
    private ClientLevel currentLevel;

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
            previousSkybox.reset();
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
            previousSkybox.reset();
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

        skybox.reset();
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

        skybox.reset();
        this.activeSkyboxes.remove(skybox);
        this.clearCurrentSkyboxIfRemoved(skybox);
        this.releaseTextures(skybox);
        this.rebuildDefaultConditions();
        return true;
    }

    @Internal
    public void clearSkyboxes() {
        this.resetSkyboxes();
        DefaultHandler.clearConditionsExcept(this.permanentSkyboxMap.values());
        this.skyboxMap.values().forEach(this::releaseTextures);
        this.skyboxMap.clear();
        this.activeSkyboxes.clear();
        this.currentSkybox = null;
        this.celestialController = null;
        this.celestialControllerConflict = false;
        NuitStarRenderer.close();
    }

    @Internal
    public void renderSkyboxes(SkyRenderer skyRenderer, Matrix4fStack skyModelViewStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters) {
        SkyboxRenderContext context = new SkyboxRenderContext(createRenderAccess(skyRenderer), skyModelViewStack, tickDelta, camera, fogParameters);
        for (Skybox skybox : this.activeSkyboxes) {
            if (skybox instanceof RenderableSkybox renderableSkybox) {
                this.currentSkybox = skybox;
                renderableSkybox.render(context);
            }
        }
    }

    private static SkyboxRenderAccess createRenderAccess(SkyRenderer skyRenderer) {
        SkyRendererAccessor skyRendererAccessor = (SkyRendererAccessor) skyRenderer;
        return new SkyboxRenderAccess() {
            @Override
            public void renderSkyDisc(Vector4fc color) {
                GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(
                        RenderSystem.getModelViewMatrixCopy(),
                        new Vector4f(color)
                );
                NuitRenderBackend.withRenderPass(
                        "Nuit translucent sky disc",
                        renderPass -> {
                            renderPass.setPipeline(RenderSystem.getCompiledPipeline(NuitRenderPipelines.translucentSkyDisc()));
                            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                            renderPass.setVertexBuffer(0, skyRendererAccessor.getTopSkyBuffer().slice());
                            renderPass.draw(10, 1, 0, 0);
                        }
                );
            }

            @Override
            public void renderSkyDisc(Vector3fc color) {
                NuitRenderBackend.withRenderPass(
                        "Nuit vanilla sky disc",
                        renderPass -> skyRendererAccessor.invokeRenderSkyDisc(renderPass, color)
                );
            }

            @Override
            public void renderDarkDisc() {
                NuitRenderBackend.withRenderPass(
                        "Nuit vanilla dark sky disc",
                        skyRendererAccessor::invokeRenderDarkDisc
                );
            }

            @Override
            public void renderStars(float brightness, PoseStack poseStack) {
                NuitRenderBackend.withRenderPass(
                        "Nuit vanilla stars",
                        renderPass -> skyRendererAccessor.invokeRenderStars(renderPass, brightness, poseStack)
                );
            }

            @Override
            public void renderEndFlash(float intensity, float xAngle, float yAngle) {
                NuitRenderBackend.withRenderPass(
                        "Nuit vanilla End flash",
                        renderPass -> skyRendererAccessor.invokeRenderEndFlash(renderPass, new PoseStack(), intensity, xAngle, yAngle)
                );
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
        if (this.enabled && !enabled) {
            this.resetSkyboxes();
        }
        this.enabled = enabled;
    }

    /**
     * Called even outside a world so audio cannot survive a disconnect or dimension change.
     */
    public void updateLevel(ClientLevel level) {
        if (this.currentLevel != level) {
            this.resetSkyboxes();
            this.currentLevel = level;
        }
    }

    private void resetSkyboxes() {
        for (Skybox skybox : Iterables.concat(this.skyboxMap.values(), this.permanentSkyboxMap.values())) {
            skybox.reset();
        }
        this.activeSkyboxes.clear();
        this.currentSkybox = null;
        this.celestialController = null;
        this.celestialControllerConflict = false;
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

    /**
     * Returns the single coherent decoration rotation that may control global celestial effects.
     */
    @Internal
    public Optional<CelestialController> getCelestialController() {
        return Optional.ofNullable(this.celestialController);
    }

    public void tick(ClientLevel level) {
        this.updateLevel(level);
        if (!this.enabled) {
            return;
        }
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
        this.updateCelestialController();
    }

    private void updateCelestialController() {
        Set<CelestialController> controllers = new LinkedHashSet<>();
        for (Skybox skybox : this.activeSkyboxes) {
            if (skybox instanceof DecorationBox decorationBox) {
                celestialControllerFor(decorationBox).ifPresent(controllers::add);
            }
        }

        if (controllers.size() == 1) {
            this.celestialController = controllers.iterator().next();
            this.celestialControllerConflict = false;
            return;
        }

        this.celestialController = null;
        if (controllers.size() > 1 && !this.celestialControllerConflict) {
            NuitClient.getLogger().warn(
                    "Active sun decorations use conflicting celestial clocks or rotations; "
                            + "global sunrise and fog orientation will remain dimension-driven"
            );
        }
        this.celestialControllerConflict = controllers.size() > 1;
    }

    static Optional<CelestialController> celestialControllerFor(DecorationBox decorationBox) {
        Rotation rotation = decorationBox.getProperties().rotation();
        if (!decorationBox.isSunEnabled()
                || !rotation.skyboxRotation()
                || rotation.axis().isEmpty()
                || rotation.speed() == 0.0F) {
            return Optional.empty();
        }
        return Optional.of(new CelestialController(decorationBox.getProperties().clock(), rotation));
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

    public record CelestialController(ClockSource clock, Rotation rotation) {
        public CelestialController {
            Objects.requireNonNull(clock, "Celestial clock cannot be null");
            Objects.requireNonNull(rotation, "Celestial rotation cannot be null");
        }

        public double getSkyAngleDegrees(ClientLevel level, float tickDelta) {
            double angle = Utils.calculateRotation(
                    this.rotation.speed(),
                    true,
                    level,
                    this.clock,
                    tickDelta,
                    0.0D
            );
            return Mth.positiveModulo(angle + 270.0D, 360.0D);
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
