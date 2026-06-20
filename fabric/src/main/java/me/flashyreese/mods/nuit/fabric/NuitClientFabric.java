package me.flashyreese.mods.nuit.fabric;

import com.mojang.serialization.Lifecycle;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.screen.SkyboxDebugScreen;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class NuitClientFabric implements ClientModInitializer {
    public static final Registry<SkyboxType<? extends Skybox>> REGISTRY = FabricRegistryBuilder.from(new MappedRegistry<>(SkyboxType.SKYBOX_TYPE_REGISTRY_KEY, Lifecycle.stable())).buildAndRegister();

    @Override
    public void onInitializeClient() {
        SkyboxType.registerAll(skyboxType -> Registry.register(REGISTRY, skyboxType.getName(), skyboxType));
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "skybox_reader"), NuitClient.skyboxResourceListener());
        ClientTickEvents.END_LEVEL_TICK.register(client -> SkyboxManager.getInstance().tick(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> NuitClient.config().getKeyBinding().tick(client));
        SkyboxDebugScreen screen = new SkyboxDebugScreen(Component.nullToEmpty("Skybox Debug Screen"));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "skybox_debug_hud"), (drawContext, tickCounter) -> screen.renderHud(drawContext));
        KeyMappingHelper.registerKeyMapping(NuitClient.config().getKeyBinding().toggleNuit);
        KeyMappingHelper.registerKeyMapping(NuitClient.config().getKeyBinding().toggleSkyboxDebugHud);
        NuitClient.init();
    }
}
