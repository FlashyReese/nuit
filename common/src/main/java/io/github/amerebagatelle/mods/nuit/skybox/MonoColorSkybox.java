package io.github.amerebagatelle.mods.nuit.skybox;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.components.Blend;
import io.github.amerebagatelle.mods.nuit.components.Conditions;
import io.github.amerebagatelle.mods.nuit.components.Properties;
import io.github.amerebagatelle.mods.nuit.components.RGBA;
import io.github.amerebagatelle.mods.nuit.mixin.RenderPipelinesAccessor;
import io.github.amerebagatelle.mods.nuit.mixin.SkyRendererAccessor;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;

public class MonoColorSkybox extends AbstractSkybox implements AutoCloseable {
    public static Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            RGBA.CODEC.optionalFieldOf("color", RGBA.of()).forGetter(MonoColorSkybox::getColor),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(MonoColorSkybox::getBlend)
    ).apply(instance, MonoColorSkybox::new));
    public RGBA color;
    public Blend blend;

    private static final Function<BlendFunction, RenderPipeline> MONO_COLOR_SKYBOX_PIPELINE_CONSUMER = (blendFunction) -> {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesAccessor.getMatricesColorSnippet());
        builder.withLocation(NuitClient.withId("pipeline/mono_color_skybox"));
        builder.withVertexShader("core/position_color");
        builder.withFragmentShader("core/position_color");
        builder.withDepthWrite(false);
        if (blendFunction != null) {
            builder.withBlend(blendFunction);
        } else {
            builder.withoutBlend();
        }
        builder.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
        return builder.build();
    };

    public MonoColorSkybox(Properties properties, Conditions conditions, RGBA color, Blend blend) {
        super(properties, conditions);
        this.color = color;
        this.blend = blend;
        buildSky();
    }

    private int indexCount = 0;
    private RenderSystem.AutoStorageIndexBuffer skyIndices;
    private GpuBuffer vertexBuffer = null;

    private void buildSky() {
        PoseStack poseStack = new PoseStack();

        VertexFormat vertexFormat = DefaultVertexFormat.POSITION_COLOR;
        VertexFormat.Mode vertexFormatMode = VertexFormat.Mode.QUADS;

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 24);
        BufferBuilder builder = new BufferBuilder(byteBufferBuilder, vertexFormatMode, vertexFormat);
        for (int face = 0; face < 6; ++face) {
            poseStack.pushPose();
            Utils.rotateSkyBoxByFace(poseStack, face);
            Matrix4f matrix4f = poseStack.last().pose();
            builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            poseStack.popPose();
        }

        skyIndices = RenderSystem.getSequentialBuffer(vertexFormatMode);
        MeshData meshData = builder.build();
        if (meshData != null) {
            indexCount = meshData.drawState().indexCount();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Mono color skybox", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
        }
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        if (this.alpha > 0) {
            Vector4f colorModifier = this.blend.applyEquationAndGetColor(this.alpha);
            RenderSystem.setShaderColor(colorModifier.x, colorModifier.y, colorModifier.z, colorModifier.w);
            if (vertexBuffer != null) {
                RenderPipeline pipeline = MONO_COLOR_SKYBOX_PIPELINE_CONSUMER.apply(this.blend.getBlendFunction());
                RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
                try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {
                    renderPass.setPipeline(pipeline);
                    renderPass.setVertexBuffer(0, vertexBuffer);
                    renderPass.setIndexBuffer(skyIndices.getBuffer(indexCount), skyIndices.type());
                    renderPass.drawIndexed(0, indexCount);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL46C.glBlendEquation(GL46C.GL_FUNC_ADD);
        }
    }

    public RGBA getColor() {
        return this.color;
    }

    public Blend getBlend() {
        return this.blend;
    }

    @Override
    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
