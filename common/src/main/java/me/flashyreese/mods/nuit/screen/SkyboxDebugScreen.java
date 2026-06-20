package me.flashyreese.mods.nuit.screen;

import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.SkyboxManager;
import me.flashyreese.mods.nuit.api.skyboxes.Skybox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class SkyboxDebugScreen extends Screen {
    public SkyboxDebugScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float delta) {
        this.renderHud(graphicsExtractor);
    }

    public void renderHud(GuiGraphicsExtractor graphicsExtractor) {
        if (NuitClient.config().generalSettings.debugHud || Minecraft.getInstance().screen == this) {
            int yPadding = 2;
            for (Map.Entry<Identifier, Skybox> skyboxEntry : SkyboxManager.getInstance().getSkyboxMap().entrySet()) {
                Skybox activeSkybox = skyboxEntry.getValue();
                if (activeSkybox.isActive()) {
                    graphicsExtractor.text(Minecraft.getInstance().font, skyboxEntry.getKey() + activeSkybox.toString(), 2, yPadding, 0xffffffff, true);
                    yPadding += 14;
                }
            }
        }
    }
}
