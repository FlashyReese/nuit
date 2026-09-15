package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {
    @Accessor("topSkyBuffer")
    GpuBuffer getTopSkyBuffer();

    @Accessor("SUN_SPRITE")
    static Identifier getSun() {
        throw new UnsupportedOperationException();
    }

    @Accessor("END_SKY_LOCATION")
    static Identifier getEndSky() {
        throw new UnsupportedOperationException();
    }

    @Invoker("renderSkyDisc")
    void invokeRenderSkyDisc(RenderPass renderPass, Vector3fc color);

    @Invoker("renderDarkDisc")
    void invokeRenderDarkDisc(RenderPass renderPass);

    @Invoker("renderStars")
    void invokeRenderStars(RenderPass renderPass, float brightness, PoseStack poseStack);

    @Invoker("renderEndFlash")
    void invokeRenderEndFlash(RenderPass renderPass, PoseStack poseStack, float intensity, float xAngle, float yAngle);
}
