package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

/**
 * Encapsulates the location of a texture, the
 * minimum u coordinate, maximum u coordinate,
 * minimum v coordinate and maximum v coordinate.
 */
public class Texture {
    public static final Codec<Texture> CODEC = ResourceLocation.CODEC.xmap(Texture::new, Texture::getTextureId);
    private final ResourceLocation textureId;
    private final UVRange uvRange;

    public Texture(ResourceLocation textureId, UVRange uvRange) {
        this.textureId = textureId;
        this.uvRange = uvRange;
    }

    public Texture(ResourceLocation textureId) {
        this(textureId, new UVRange(0.0F, 0.0F, 1.0F, 1.0F));
    }

    public ResourceLocation getTextureId() {
        return this.textureId;
    }

    public UVRange getUvRange() {
        return uvRange;
    }
}