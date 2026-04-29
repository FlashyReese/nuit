package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

/**
 * Encapsulates the location of a texture, the
 * minimum u coordinate, maximum u coordinate,
 * minimum v coordinate and maximum v coordinate.
 */
public class Texture {
    public static final Codec<Texture> CODEC = Identifier.CODEC.xmap(Texture::new, Texture::getTextureId);
    private final Identifier textureId;
    private final UVRange uvRange;

    public Texture(Identifier textureId, UVRange uvRange) {
        this.textureId = textureId;
        this.uvRange = uvRange;
    }

    public Texture(Identifier textureId) {
        this(textureId, new UVRange(0.0F, 0.0F, 1.0F, 1.0F));
    }

    public Identifier getTextureId() {
        return this.textureId;
    }

    public UVRange getUvRange() {
        return uvRange;
    }
}