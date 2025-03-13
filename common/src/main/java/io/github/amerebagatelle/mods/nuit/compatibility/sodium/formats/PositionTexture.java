package io.github.amerebagatelle.mods.nuit.compatibility.sodium.formats;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import org.joml.Matrix4f;

public class PositionTexture {
    public static final VertexFormat FORMAT;
    public static final int STRIDE = 20;
    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_TEXTURE = 12;

    public static void put(long ptr, Matrix4f matrix, float x, float y, float z, float u, float v) {
        float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
        float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);
        put(ptr, xt, yt, zt, u, v);
    }


    public static void put(long ptr, float x, float y, float z, float u, float v) {
        PositionAttribute.put(ptr + OFFSET_POSITION, x, y, z);
        TextureAttribute.put(ptr + OFFSET_TEXTURE, u, v);
    }

    static {
        FORMAT = DefaultVertexFormat.POSITION_TEX;
    }
}
