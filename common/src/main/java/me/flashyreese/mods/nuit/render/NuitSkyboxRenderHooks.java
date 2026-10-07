package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import me.flashyreese.mods.nuit.SkyboxManager;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix4fStack;

public final class NuitSkyboxRenderHooks {
    private NuitSkyboxRenderHooks() {
    }

    public static boolean allowCustomSkyPass(boolean renderSky) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        return renderSky || skyboxManager.isEnabled() && skyboxManager.hasActiveRenderableSkyboxes();
    }

    public static DimensionType.Skybox skyboxForPass(DimensionType.Skybox original, SkyRenderState skyRenderState) {
        if (original == DimensionType.Skybox.NONE && ((NuitSkyRenderStateHolder) skyRenderState).nuit$getRenderState() != null) {
            return DimensionType.Skybox.OVERWORLD;
        }
        return original;
    }

    public static boolean renderCustomSkyboxes(
            GpuBufferSlice fogParameters,
            SkyRenderer skyRenderer,
            SkyRenderState skyRenderState
    ) {
        NuitSkyRenderState renderState = ((NuitSkyRenderStateHolder) skyRenderState).nuit$getRenderState();
        if (renderState == null) {
            return false;
        }

        Matrix4fStack skyModelViewStack = new Matrix4fStack(16);
        skyModelViewStack.set(RenderSystem.getModelViewStack());
        skyModelViewStack.setTranslation(0.0F, 0.0F, 0.0F);
        SkyboxManager.getInstance().renderSkyboxes(skyRenderer, renderState, skyModelViewStack, fogParameters);
        return true;
    }
}
