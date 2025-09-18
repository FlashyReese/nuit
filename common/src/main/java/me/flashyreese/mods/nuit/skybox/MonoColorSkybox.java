package me.flashyreese.mods.nuit.skybox;

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
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.components.RGBA;
import me.flashyreese.mods.nuit.mixin.RenderPipelinesAccessor;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.util.Utils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;

public class MonoColorSkybox extends AbstractSkybox {
    private static final Function<BlendFunction, RenderPipeline> MONO_COLOR_SKYBOX_PIPELINE_CONSUMER = (blendFunction) -> {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesAccessor.getMatricesColorSnippet());
        builder.withLocation(ResourceLocation.tryBuild(NuitClient.MOD_ID, "pipeline/mono_color_skybox"));
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
    public static Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(AbstractSkybox::getConditions),
            RGBA.CODEC.optionalFieldOf("color", RGBA.of()).forGetter(MonoColorSkybox::getColor),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(MonoColorSkybox::getBlend)
    ).apply(instance, MonoColorSkybox::new));
    public RGBA color;
    public Blend blend;
    private int indexCount = 0;
    private RenderSystem.AutoStorageIndexBuffer skyIndices;
    private GpuBuffer vertexBuffer = null;

    public MonoColorSkybox(Properties properties, Conditions conditions, RGBA color, Blend blend) {
        super(properties, conditions);
        this.color = color;
        this.blend = blend;
        this.buildSky();
    }

    private void buildSky() {
        VertexFormat vertexFormat = DefaultVertexFormat.POSITION_COLOR;
        VertexFormat.Mode vertexFormatMode = VertexFormat.Mode.QUADS;

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(vertexFormat.getVertexSize() * 24);
        BufferBuilder builder = new BufferBuilder(byteBufferBuilder, vertexFormatMode, vertexFormat);
        for (int face = 0; face < 6; ++face) {
            Matrix4f matrix4f = Utils.getMatrixForRotatedFace(face);
            builder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
            builder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
        }

        this.skyIndices = RenderSystem.getSequentialBuffer(vertexFormatMode);
        try (MeshData meshData = builder.build()) {
            if (meshData != null) {
                this.indexCount = meshData.drawState().indexCount();
                this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Mono color skybox", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            }
        }
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccess, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        RenderSystem.setShaderFog(fogParameters);
        if (this.alpha > 0 && this.vertexBuffer != null) {
            Vector4f colorModifier = this.blend.applyEquationAndGetColor(this.alpha);
            RenderSystem.setShaderColor(colorModifier.x, colorModifier.y, colorModifier.z, colorModifier.w);

            RenderPipeline pipeline = MONO_COLOR_SKYBOX_PIPELINE_CONSUMER.apply(this.blend.getBlendFunction());
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, this.vertexBuffer);
                renderPass.setIndexBuffer(this.skyIndices.getBuffer(this.indexCount), this.skyIndices.type());
                renderPass.drawIndexed(0, this.indexCount);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL46C.glBlendEquation(GL46C.GL_FUNC_ADD); // fixme: ?????? we should avoid calling gl calls outside of blaze3d
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
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }
}
