package me.flashyreese.mods.nuit.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {
    @Accessor("SUN_SPRITE")
    static Identifier getSun() {
        throw new UnsupportedOperationException();
    }

    @Accessor("END_SKY_LOCATION")
    static Identifier getEndSky() {
        throw new UnsupportedOperationException();
    }

    @Invoker("renderStars")
    void invokeRenderStars(float tickDelta, PoseStack poseStack);
}
