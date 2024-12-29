package io.github.amerebagatelle.mods.nuit.neoforge;

import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.screen.SkyboxDebugScreen;
import io.github.amerebagatelle.mods.nuit.skybox.SkyboxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@Mod(NuitClient.MOD_ID)
public final class NuitNeoForge {
    public static final Registry<SkyboxType<? extends Skybox>> REGISTRY = new RegistryBuilder<>(SkyboxType.SKYBOX_TYPE_REGISTRY_KEY).create();
    public final SkyboxDebugScreen screen = new SkyboxDebugScreen(Component.nullToEmpty("Skybox Debug Screen"));

    public NuitNeoForge(IEventBus bus) {
        bus.addListener(this::registerSkyTypeRegistry);
        bus.addListener(this::registerSkyTypes);
        bus.addListener(this::registerClientReloadListener);
        bus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::registerClientTick);
        NeoForge.EVENT_BUS.addListener(this::registerWorldTick);
        NeoForge.EVENT_BUS.addListener(this::registerHudRender);
        NuitClient.init();
    }

    @SubscribeEvent
    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(NuitClient.config().getKeyBinding().toggleNuit);
        event.register(NuitClient.config().getKeyBinding().toggleSkyboxDebugHud);
    }

    @SubscribeEvent
    public void registerClientTick(ClientTickEvent.Post event) {
        NuitClient.config().getKeyBinding().tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public void registerWorldTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ClientLevel level) {
            SkyboxManager.getInstance().tick(level);
        }
    }

    @SubscribeEvent
    public void registerHudRender(RenderGuiLayerEvent.Post event) {
        screen.renderHud(event.getGuiGraphics());
    }

    @SubscribeEvent
    public void registerSkyTypeRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    @SubscribeEvent
    public void registerSkyTypes(RegisterEvent event) {
        event.register(SkyboxType.SKYBOX_TYPE_REGISTRY_KEY, registry -> SkyboxType.register(skyboxType -> registry.register(skyboxType.createId(), skyboxType)));
    }

    @SubscribeEvent
    public void registerClientReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((pPreparationBarrier, pResourceManager, pPreparationsProfiler, pReloadProfiler) -> NuitClient.skyboxResourceListener().reload(pPreparationBarrier, pResourceManager, pPreparationsProfiler, pReloadProfiler));
    }
}