package io.github.amerebagatelle.mods.nuit.fabric;

import com.mojang.serialization.Lifecycle;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.SkyboxManager;
import io.github.amerebagatelle.mods.nuit.api.skyboxes.Skybox;
import io.github.amerebagatelle.mods.nuit.screen.SkyboxDebugScreen;
import io.github.amerebagatelle.mods.nuit.skybox.SkyboxType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NuitClientFabric implements ClientModInitializer {
    public static final Registry<SkyboxType<? extends Skybox>> REGISTRY = FabricRegistryBuilder.from(new MappedRegistry<>(SkyboxType.SKYBOX_TYPE_REGISTRY_KEY, Lifecycle.stable())).buildAndRegister();

    @Override
    public void onInitializeClient() {
        SkyboxType.register(skyboxType -> Registry.register(REGISTRY, skyboxType.createId(), skyboxType));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2) {
                return NuitClient.skyboxResourceListener().reload(preparationBarrier, resourceManager, executor, executor2);
            }

            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.tryBuild(NuitClient.MOD_ID, "skybox_reader");
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