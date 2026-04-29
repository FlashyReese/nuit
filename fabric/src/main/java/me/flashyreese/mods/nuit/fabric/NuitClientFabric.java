package me.flashyreese.mods.nuit.fabric;

import com.mojang.serialization.Lifecycle;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import me.flashyreese.mods.nuit.screen.SkyboxDebugScreen;
import me.flashyreese.mods.nuit.skybox.SkyboxType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NuitClientFabric implements ClientModInitializer {
    public static final Registry<SkyboxType<? extends Skybox>> REGISTRY = FabricRegistryBuilder.from(new MappedRegistry<>(SkyboxType.SKYBOX_TYPE_REGISTRY_KEY, Lifecycle.stable())).buildAndRegister();

    @Override
    public void onInitializeClient() {
        SkyboxType.registerAll(skyboxType -> Registry.register(REGISTRY, skyboxType.getName(), skyboxType));
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "skybox_reader"), new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(SharedState sharedState, Executor executor, PreparationBarrier preparationBarrier, Executor executor2) {
                return NuitClient.skyboxResourceListener().reload(sharedState, executor, preparationBarrier, executor2);
            }
        });

        ClientTickEvents.END_WORLD_TICK.register(client -> SkyboxManager.getInstance().tick(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> NuitClient.config().getKeyBinding().tick(client));
        SkyboxDebugScreen screen = new SkyboxDebugScreen(Component.nullToEmpty("Skybox Debug Screen"));
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> screen.renderHud(drawContext));
        KeyBindingHelper.registerKeyBinding(NuitClient.config().getKeyBinding().toggleNuit);
        KeyBindingHelper.registerKeyBinding(NuitClient.config().getKeyBinding().toggleSkyboxDebugHud);
        NuitClient.init();
    }
}