package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {
    @Accessor("SUN_LOCATION")
    static ResourceLocation getSun() {
        return ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    }

    @Accessor("MOON_LOCATION")
    static ResourceLocation getMoonPhases() {
        return ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    }

    @Invoker("renderStars")
    void invokeRenderStars(float tickDelta, PoseStack poseStack);
}
