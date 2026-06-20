package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class NuitStarRenderer {
    private static final int STAR_COUNT = 1500;
    private static final float STAR_DISTANCE = 100.0F;
    private static final long STAR_SEED = 10842L;

    private static GpuBuffer starBuffer;
    private static int starIndexCount;

    public static void render(Matrix4f modelViewMatrix, Vector4f colorModifier, @Nullable BlendFunction blendFunction) {
        ensureStarsBuilt();

        RenderPipeline pipeline = NuitRenderPipelines.monoColorSkybox(blendFunction);
        RenderSystem.AutoStorageIndexBuffer quadIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = quadIndices.getBuffer(starIndexCount);
        GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(modelViewMatrix, colorModifier);
        NuitRenderBackend.drawIndexed(
                pipeline,
                starBuffer,
                indexBuffer,
                quadIndices.type(),
                starIndexCount,
                dynamicTransforms,
                "Nuit stars"
        );
    }

    public static void close() {
        if (starBuffer != null) {
            if (!starBuffer.isClosed()) {
                starBuffer.close();
            }
            starBuffer = null;
            starIndexCount = 0;
        }
    }

    private static void ensureStarsBuilt() {
        if (starBuffer != null && !starBuffer.isClosed()) {
            return;
        }

        VertexFormat format = DefaultVertexFormat.POSITION_COLOR;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(format.getVertexSize() * STAR_COUNT * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, format);
            RandomSource random = RandomSource.createThreadLocalInstance(STAR_SEED);
            int starColor = ARGB.white(1.0F);

            for (int i = 0; i < STAR_COUNT; i++) {
                float x = random.nextFloat() * 2.0F - 1.0F;
                float y = random.nextFloat() * 2.0F - 1.0F;
                float z = random.nextFloat() * 2.0F - 1.0F;
                float starSize = 0.15F + random.nextFloat() * 0.1F;
                float lengthSquared = Mth.lengthSquared(x, y, z);
                if (lengthSquared <= 0.010000001F || lengthSquared >= 1.0F) {
                    continue;
                }

                Vector3f starCenter = new Vector3f(x, y, z).normalize(STAR_DISTANCE);
                float zRotation = (float) (random.nextDouble() * Mth.TWO_PI);
                Matrix3f rotation = new Matrix3f()
                        .rotateTowards(new Vector3f(starCenter).negate(), new Vector3f(0.0F, 1.0F, 0.0F))
                        .rotateZ(-zRotation);

                bufferBuilder.addVertex(new Vector3f(starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setColor(starColor);
                bufferBuilder.addVertex(new Vector3f(starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setColor(starColor);
                bufferBuilder.addVertex(new Vector3f(-starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setColor(starColor);
                bufferBuilder.addVertex(new Vector3f(-starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setColor(starColor);
            }

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                starIndexCount = meshData.drawState().indexCount();
                starBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Nuit stars vertex buffer",
                        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
                        meshData.vertexBuffer()
                );
            }
        }
    }
}
