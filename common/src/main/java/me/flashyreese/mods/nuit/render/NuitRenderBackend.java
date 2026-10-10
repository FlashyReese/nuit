package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureHandle;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class NuitRenderBackend {
    public static final String SAMPLER0_NAME = "Sampler0";
    private static SkyRenderFrame activeSkyFrame;
    private static final List<GpuBuffer> PREVIOUS_FRAME_BUFFERS = new ArrayList<>();

    public static GpuBufferSlice createDynamicTransforms() {
        return createDynamicTransforms(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
    }

    public static GpuBufferSlice createDynamicTransforms(Matrix4f modelViewMatrix, Vector4f colorModulator) {
        return RenderSystem.getDynamicUniforms().writeTransform(modelViewMatrix, colorModulator, new Vector3f(), new Matrix4f());
    }

    /**
     * Begins collecting sky draws for a render pass owned by vanilla.
     *
     * @param renderPass          the open pass that vanilla's sky renderer would have drawn into
     * @param withDepthAttachment whether that pass has a depth attachment, which selects the pipeline variants
     */
    public static SkyRenderFrame beginSkyFrame(RenderPass renderPass, boolean withDepthAttachment) {
        if (activeSkyFrame != null) {
            throw new IllegalStateException("A Nuit sky render frame is already active");
        }

        releaseDeferredBuffers();
        activeSkyFrame = new SkyRenderFrame(renderPass, withDepthAttachment);
        return activeSkyFrame;
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
            SkyRenderFrame skyFrame = activeSkyFrame;
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Nuit vertex buffer for " + pipeline,
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
                    meshData.vertexBuffer()
            );
            GpuBuffer indexBuffer = null;
            boolean closeVertexBuffer = true;
            boolean closeIndexBuffer = false;

            try {
                MeshData.DrawState drawState = meshData.drawState();
                if (meshData.indexBuffer() == null) {
                    drawSequentialIndexed(
                            pipeline,
                            vertexBuffer,
                            drawState.primitiveTopology(),
                            drawState.indexCount(),
                            dynamicTransforms,
                            "Nuit draw for " + pipeline,
                            configureRenderPass
                    );
                } else {
                    indexBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "Nuit index buffer for " + pipeline,
                            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_INDEX,
                            meshData.indexBuffer()
                    );
                    closeIndexBuffer = true;
                    drawIndexed(
                            pipeline,
                            vertexBuffer,
                            indexBuffer,
                            drawState.indexType(),
                            drawState.indexCount(),
                            dynamicTransforms,
                            "Nuit draw for " + pipeline,
                            configureRenderPass
                    );
                }

                if (skyFrame != null) {
                    skyFrame.closeWithFrame(vertexBuffer);
                    closeVertexBuffer = false;
                    if (closeIndexBuffer) {
                        skyFrame.closeWithFrame(indexBuffer);
                        closeIndexBuffer = false;
                    }
                }
            } finally {
                if (closeVertexBuffer) {
                    vertexBuffer.close();
                }
                if (closeIndexBuffer && indexBuffer != null) {
                    indexBuffer.close();
                }
            }
        }
    }

    public static void draw(RenderPipeline pipeline, GpuBuffer vertexBuffer, int vertexCount, GpuBufferSlice dynamicTransforms, String label) {
        draw(pipeline, vertexBuffer, vertexCount, dynamicTransforms, label, _ -> {
        });
    }

    public static void draw(RenderPipeline pipeline, GpuBuffer vertexBuffer, int vertexCount, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        submitCommand(SkyDrawCommand.nonIndexed(
                pipeline,
                vertexBuffer,
                vertexCount,
                dynamicTransforms,
                label,
                configureRenderPass
        ));
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label) {
        drawIndexed(pipeline, vertexBuffer, indexBuffer, indexType, indexCount, dynamicTransforms, label, _ -> {
        });
    }

    public static void drawIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        submitCommand(SkyDrawCommand.indexed(
                pipeline,
                vertexBuffer,
                indexBuffer,
                indexType,
                indexCount,
                dynamicTransforms,
                label,
                configureRenderPass
        ));
    }

    public static void drawSequentialIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, PrimitiveTopology primitiveTopology, int indexCount, GpuBufferSlice dynamicTransforms, String label) {
        drawSequentialIndexed(pipeline, vertexBuffer, primitiveTopology, indexCount, dynamicTransforms, label, _ -> {
        });
    }

    public static void drawSequentialIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, PrimitiveTopology primitiveTopology, int indexCount, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        submitCommand(SkyDrawCommand.sequentialIndexed(
                pipeline,
                vertexBuffer,
                RenderSystem.getSequentialBuffer(primitiveTopology),
                indexCount,
                dynamicTransforms,
                label,
                configureRenderPass
        ));
    }

    /**
     * Closes the transient buffers kept alive from the previous sky frame. Called when a frame begins, and when skyboxes
     * are cleared so they are not kept around once Nuit stops rendering.
     */
    public static void releaseDeferredBuffers() {
        if (activeSkyFrame != null) {
            throw new IllegalStateException("Cannot release deferred buffers while a Nuit sky render frame is active");
        }

        Throwable closeFailure = null;
        for (GpuBuffer buffer : PREVIOUS_FRAME_BUFFERS) {
            try {
                if (!buffer.isClosed()) {
                    buffer.close();
                }
            } catch (RuntimeException | Error throwable) {
                if (closeFailure == null) {
                    closeFailure = throwable;
                } else {
                    closeFailure.addSuppressed(throwable);
                }
            }
        }
        PREVIOUS_FRAME_BUFFERS.clear();
        if (closeFailure != null) {
            SkyRenderFrame.rethrow(closeFailure);
        }
    }

    /**
     * Draws a full screen triangle generated in the vertex shader, without any vertex buffer.
     */
    public static void drawFullscreenTriangle(RenderPipeline pipeline, GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
        submitCommand(SkyDrawCommand.nonIndexed(
                pipeline,
                null,
                3,
                dynamicTransforms,
                label,
                configureRenderPass
        ));
    }

    private static void submitCommand(RenderCommand command) {
        if (activeSkyFrame == null) {
            throw new IllegalStateException("Nuit sky draws must be issued while a sky render frame is active: " + command.label());
        }

        activeSkyFrame.enqueue(command);
    }

    private static void renderCommands(List<RenderCommand> commands, RenderPass renderPass, boolean withDepthAttachment) {
        if (commands.isEmpty()) {
            return;
        }

        prepareSequentialIndexBuffers(commands);
        for (RenderCommand command : commands) {
            command.draw(renderPass, withDepthAttachment);
        }
    }

    private static void prepareSequentialIndexBuffers(List<RenderCommand> commands) {
        Map<RenderSystem.AutoStorageIndexBuffer, Integer> requiredIndexCounts = new IdentityHashMap<>();
        for (RenderCommand command : commands) {
            command.collectSequentialIndexBuffers(requiredIndexCounts);
        }

        requiredIndexCounts.forEach(RenderSystem.AutoStorageIndexBuffer::requestIndexCount);
        requiredIndexCounts.keySet().forEach(RenderSystem.AutoStorageIndexBuffer::resizeToRequestedIndexCount);
        for (RenderCommand command : commands) {
            command.resolveSequentialIndexBuffer();
        }
    }

    private static TextureBinding resolveTextureBinding(String samplerName, Identifier texture) {
        TextureHandle textureHandle = Minecraft.getInstance().getTextureManager().getTexture(texture);
        return new TextureBinding(samplerName, textureHandle.textureView(), textureHandle.sampler());
    }

    private record TextureBinding(String samplerName, GpuTextureView textureView, GpuSampler sampler) {
        private void bind(RenderPass renderPass) {
            renderPass.setUniform(this.samplerName, this.textureView, this.sampler);
        }
    }

    private record DefaultUniforms(GpuBufferSlice projection, GpuBufferSlice fog, GpuBuffer globals, GpuBufferSlice lighting) {
        private static DefaultUniforms capture() {
            return new DefaultUniforms(
                    RenderSystem.getProjectionMatrixBuffer(),
                    RenderSystem.getShaderFog(),
                    RenderSystem.getGlobalSettingsUniform(),
                    RenderSystem.getShaderLights()
            );
        }

        private void bind(RenderPass renderPass) {
            if (this.projection != null) {
                renderPass.setUniform("Projection", this.projection);
            }
            if (this.fog != null) {
                renderPass.setUniform("Fog", this.fog);
            }
            if (this.globals != null) {
                renderPass.setUniform("Globals", this.globals);
            }
            if (this.lighting != null) {
                renderPass.setUniform("Lighting", this.lighting);
            }
        }
    }

    private interface RenderCommand {
        String label();

        default void collectSequentialIndexBuffers(Map<RenderSystem.AutoStorageIndexBuffer, Integer> requiredIndexCounts) {
        }

        default void resolveSequentialIndexBuffer() {
        }

        void draw(RenderPass renderPass, boolean withDepthAttachment);
    }

    private static final class SkyDrawCommand implements RenderCommand {
        private final RenderPipeline pipeline;
        private final GpuBuffer vertexBuffer;
        private final RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer;
        private final int elementCount;
        private final GpuBufferSlice dynamicTransforms;
        private final String label;
        private final Consumer<RenderPass> configureRenderPass;
        private final DefaultUniforms defaultUniforms;
        private GpuBuffer indexBuffer;
        private IndexType indexType;

        private SkyDrawCommand(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, IndexType indexType,
                               RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer, int elementCount,
                               GpuBufferSlice dynamicTransforms, String label, Consumer<RenderPass> configureRenderPass) {
            this.pipeline = pipeline;
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.indexType = indexType;
            this.sequentialIndexBuffer = sequentialIndexBuffer;
            this.elementCount = elementCount;
            this.dynamicTransforms = dynamicTransforms;
            this.label = label;
            this.configureRenderPass = configureRenderPass;
            this.defaultUniforms = DefaultUniforms.capture();
        }

        private static SkyDrawCommand nonIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, int vertexCount,
                                                  GpuBufferSlice dynamicTransforms, String label,
                                                  Consumer<RenderPass> configureRenderPass) {
            return new SkyDrawCommand(pipeline, vertexBuffer, null, null, null, vertexCount, dynamicTransforms, label, configureRenderPass);
        }

        private static SkyDrawCommand indexed(RenderPipeline pipeline, GpuBuffer vertexBuffer, GpuBuffer indexBuffer,
                                               IndexType indexType, int indexCount, GpuBufferSlice dynamicTransforms,
                                               String label, Consumer<RenderPass> configureRenderPass) {
            return new SkyDrawCommand(pipeline, vertexBuffer, indexBuffer, indexType, null, indexCount, dynamicTransforms, label, configureRenderPass);
        }

        private static SkyDrawCommand sequentialIndexed(RenderPipeline pipeline, GpuBuffer vertexBuffer,
                                                         RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer,
                                                         int indexCount, GpuBufferSlice dynamicTransforms, String label,
                                                         Consumer<RenderPass> configureRenderPass) {
            return new SkyDrawCommand(pipeline, vertexBuffer, null, null, sequentialIndexBuffer, indexCount, dynamicTransforms, label, configureRenderPass);
        }

        @Override
        public String label() {
            return this.label;
        }

        @Override
        public void collectSequentialIndexBuffers(Map<RenderSystem.AutoStorageIndexBuffer, Integer> requiredIndexCounts) {
            if (this.sequentialIndexBuffer != null) {
                requiredIndexCounts.merge(this.sequentialIndexBuffer, this.elementCount, Math::max);
            }
        }

        @Override
        public void resolveSequentialIndexBuffer() {
            if (this.sequentialIndexBuffer != null) {
                this.indexBuffer = this.sequentialIndexBuffer.getBuffer();
                this.indexType = this.sequentialIndexBuffer.type();
            }
        }

        @Override
        public void draw(RenderPass renderPass, boolean withDepthAttachment) {
            renderPass.pushDebugGroup(() -> this.label);
            try {
                renderPass.setPipeline(RenderSystem.getCompiledPipeline(NuitRenderPipelines.forDepthAttachment(this.pipeline, withDepthAttachment)));
                if (this.vertexBuffer != null) {
                    renderPass.setVertexBuffer(0, this.vertexBuffer.slice());
                }
                if (this.indexBuffer != null) {
                    renderPass.setIndexBuffer(this.indexBuffer, this.indexType);
                }

                this.defaultUniforms.bind(renderPass);
                renderPass.setUniform("DynamicTransforms", this.dynamicTransforms);
                this.configureRenderPass.accept(renderPass);

                if (this.indexBuffer != null) {
                    renderPass.drawIndexed(this.elementCount, 1, 0, 0, 0);
                } else {
                    renderPass.draw(this.elementCount, 1, 0, 0);
                }
            } finally {
                renderPass.popDebugGroup();
            }
        }
    }

    public static final class SkyRenderFrame implements AutoCloseable {
        private final RenderPass renderPass;
        private final boolean withDepthAttachment;
        private final List<RenderCommand> commands = new ArrayList<>();
        private final List<GpuBuffer> buffersToClose = new ArrayList<>();
        private boolean submitting;
        private boolean finished;

        private SkyRenderFrame(RenderPass renderPass, boolean withDepthAttachment) {
            this.renderPass = renderPass;
            this.withDepthAttachment = withDepthAttachment;
        }

        private void enqueue(RenderCommand command) {
            if (this.finished) {
                throw new IllegalStateException("Nuit sky render frame is already finished");
            }
            if (this.submitting) {
                throw new IllegalStateException("Cannot enqueue a draw while submitting a Nuit sky render frame");
            }

            this.commands.add(command);
        }

        private void closeWithFrame(GpuBuffer buffer) {
            this.buffersToClose.add(buffer);
        }

        public void submit() {
            if (this.finished) {
                throw new IllegalStateException("Nuit sky render frame is already finished");
            }

            Throwable renderFailure = null;
            this.submitting = true;
            try {
                this.renderPass.pushDebugGroup(() -> "Nuit sky");
                try {
                    renderCommands(this.commands, this.renderPass, this.withDepthAttachment);
                } finally {
                    this.renderPass.popDebugGroup();
                }
            } catch (RuntimeException | Error throwable) {
                renderFailure = throwable;
                throw throwable;
            } finally {
                Throwable closeFailure = this.finish();
                if (activeSkyFrame == this) {
                    activeSkyFrame = null;
                }
                if (closeFailure != null) {
                    if (renderFailure != null) {
                        renderFailure.addSuppressed(closeFailure);
                    } else {
                        rethrow(closeFailure);
                    }
                }
            }
        }

        @Override
        public void close() {
            Throwable closeFailure = null;
            try {
                if (!this.finished) {
                    closeFailure = this.finish();
                }
            } finally {
                if (activeSkyFrame == this) {
                    activeSkyFrame = null;
                }
            }
            if (closeFailure != null) {
                rethrow(closeFailure);
            }
        }

        private Throwable finish() {
            this.finished = true;
            this.commands.clear();
            // Draws may already be recorded into vanilla's still-open pass, so defer closing to the next frame.
            PREVIOUS_FRAME_BUFFERS.addAll(this.buffersToClose);
            this.buffersToClose.clear();
            return null;
        }

        private static void rethrow(Throwable throwable) {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw (Error) throwable;
        }
    }
}
