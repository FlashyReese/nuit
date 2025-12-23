package me.flashyreese.mods.nuit.api.skyboxes;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4fStack;

public interface Skybox extends AutoCloseable {
    default int getLayer() {
        return 0;
    }

    void render(SkyRendererAccessor skyRendererAccessor, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource);

    void tick(ClientLevel clientLevel);

    boolean isActive();
}
