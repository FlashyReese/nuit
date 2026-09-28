package me.flashyreese.mods.nuit.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {
    @Accessor("celestialsAtlas")
    TextureAtlas getCelestialsAtlas();

    @Accessor("topSkyBuffer")
    GpuBuffer getTopSkyBuffer();

    @Accessor("bottomSkyBuffer")
    GpuBuffer getBottomSkyBuffer();

    @Accessor("starBuffer")
    GpuBuffer getStarBuffer();

    @Accessor("endFlashBuffer")
    GpuBuffer getEndFlashBuffer();

    @Accessor("starIndexCount")
    int getStarIndexCount();

    @Accessor("SUN_SPRITE")
    static Identifier getSun() {
        throw new UnsupportedOperationException();
    }

    @Accessor("END_SKY_LOCATION")
    static Identifier getEndSky() {
        throw new UnsupportedOperationException();
    }
}
