package me.flashyreese.mods.nuit.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class BufferUploader {
    public static void drawWithShader(RenderPipeline pipeline, MeshData meshData, Consumer<RenderPass> renderPassConsumer) {
        try {
            GpuBuffer gpuBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer gpuBuffer2;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                gpuBuffer2 = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                gpuBuffer2 = pipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : renderTarget.getColorTextureView();
            GpuTextureView gpuTextureView2 = renderTarget.useDepth ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView()) : null;

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Immediate draw for " + pipeline, gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer2, indexType);
                ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissorState.enabled()) {
                    renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                }

                renderPassConsumer.accept(renderPass);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
            }
        } catch (Throwable var17) {
            if (meshData != null) {
                try {
                    meshData.close();
                } catch (Throwable var14) {
                    var17.addSuppressed(var14);
                }
            }

            throw var17;
        }

        if (meshData != null) {
            meshData.close();
        }
    }
}