package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.blaze3d.vertex.PoseStack;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;

public interface Skybox {
    default int getLayer() {
        return 0;
    }

    void render(SkyRendererAccessor skyRendererAccessor, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters thickFog);

    void tick(ClientLevel clientLevel);

    boolean isActive();
}
