package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.Optional;
import java.util.function.Consumer;

public final class NuitRenderBackend {
    public static final String SAMPLER0_NAME = "Sampler0";

    public static GpuBufferSlice createDynamicTransforms() {
        return createDynamicTransforms(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
    }

    public static GpuBufferSlice createDynamicTransforms(Matrix4f modelViewMatrix, Vector4f colorModulator) {
        return RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, colorModulator, new Vector3f(), new Matrix4f());
    }

    public static void draw(RenderPipeline pipeline, MeshData meshData, GpuBufferSlice dynamicTransforms) {
        draw(pipeline, meshData, dynamicTransforms, _ -> {
        });
    }

    public static void drawTextured(RenderPipeline pipeline, MeshData meshData, GpuBufferSlice dynamicTransforms, String samplerName, Identifier texture) {
        TextureBinding textureBinding;
        try {
            textureBinding = resolveTextureBinding(samplerName, texture);
        } catch (Throwable throwable) {
            try {
                meshData.close();
            } catch (Throwable suppressed) {
                throwable.addSuppressed(suppressed);
            }
            throw throwable;
        }
        draw(pipeline, meshData, dynamicTransforms, textureBinding::bind);
    }

    public static void draw(RenderPipeline pipeline, MeshData meshData, GpuBufferSlice dynamicTransforms, Consumer<RenderPass> configureRenderPass) {
        try (meshData) {
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Nuit vertex buffer for " + pipeline,
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
                    meshData.vertexBuffer()
            );
            GpuBuffer indexBuffer = null;
            IndexType indexType;
            boolean closeIndexBuffer = false;

            try {
                if (meshData.indexBuffer() == null) {
                    RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().primitiveTopology());
                    indexBuffer = sequentialBuffer.getBuffer(meshData.drawState().indexCount());
                    indexType = sequentialBuffer.type();
                } else {
                    indexBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "Nuit index buffer for " + pipeline,
                            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX,
                            meshData.indexBuffer()
                    );
                    indexType = meshData.drawState().indexType();
                    closeIndexBuffer = true;
                }

                drawIndexed(pipeline, vertexBuffer, indexBuffer, indexType, meshData.drawState().indexCount(), dynamicTransforms, "Nuit draw for " + pipeline, configureRenderPass);
            } finally {
                vertexBuffer.close();
                if (closeIndexBuffer && indexBuffer != null) {
                    indexBuffer.close();
                }
            }
        }
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label) {
        drawIndexed(pipeline, vertexBuffer, indexBuffer, indexType, indexCount, dynamicTransforms, label, pass -> {
        });
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        withRenderPass(label, renderPass -> {
            renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            renderPass.setVertexBuffer(0, vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, indexType);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            configureRenderPass.accept(renderPass);
            renderPass.drawIndexed(indexCount, 1, 0, 0, 0);
        });
    }

    public static void withRenderPass(String label, Consumer<RenderPass> draw) {
        RenderTarget renderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.hasDepth() ? renderTarget.getDepthTextureView() : null;

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty())) {
            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            draw.accept(renderPass);
        }
    }

    private static TextureBinding resolveTextureBinding(String samplerName, Identifier texture) {
        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        return new TextureBinding(samplerName, abstractTexture.getTextureView(), abstractTexture.getSampler());
    }

    private record TextureBinding(String samplerName, GpuTextureView textureView, GpuSampler sampler) {
        private void bind(RenderPass renderPass) {
            renderPass.setUniform(this.samplerName, this.textureView, this.sampler);
        }
    }
}
