package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public final class NuitRenderBackend {
    public static GpuBufferSlice createDynamicTransforms() {
        return createDynamicTransforms(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
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
            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = sequentialBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = sequentialBuffer.type();
            } else {
                indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            drawIndexed(pipeline, vertexBuffer, indexBuffer, indexType, meshData.drawState().indexCount(), dynamicTransforms, "Nuit draw for " + pipeline, configureRenderPass);
        }
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label) {
        drawIndexed(pipeline, vertexBuffer, indexBuffer, indexType, indexCount, dynamicTransforms, label, pass -> {
        });
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView colorTexture = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
        GpuTextureView depthTexture = renderTarget.useDepth ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView()) : null;

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, indexType);

            ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissorState.enabled()) {
                renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            configureRenderPass.accept(renderPass);
            renderPass.drawIndexed(0, 0, indexCount, 1);
        }
    }

    private static TextureBinding resolveTextureBinding(String samplerName, Identifier texture) {
        AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        return new TextureBinding(samplerName, abstractTexture.getTextureView(), abstractTexture.getSampler());
    }

    private record TextureBinding(String samplerName, GpuTextureView textureView, GpuSampler sampler) {
        private void bind(RenderPass renderPass) {
            renderPass.bindTexture(this.samplerName, this.textureView, this.sampler);
        }
    }
}
