package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.NuitClient;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

public final class NuitRenderPipelines {
    private static final Identifier MONO_COLOR_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/mono_color_skybox");
    private static final Identifier TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/textured_skybox");
    private static final Identifier MULTI_TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/multi_textured_skybox");
    public static final String NEXT_UV_SEMANTIC_NAME = "NextUV";
    public static final String FRAME_BLEND_SEMANTIC_NAME = "FrameBlend";

    public static final VertexFormat FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT = VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(NEXT_UV_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(FRAME_BLEND_SEMANTIC_NAME, GpuFormat.R32_FLOAT)
            .build();

    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .buildSnippet();

    private static final Function<SkyParameters, RenderPipeline> SKY_PIPELINES =
            Util.memoize(NuitRenderPipelines::buildSkyPipeline);

    public static RenderPipeline monoColorSkybox(@Nullable BlendFunction blendFunction) {
        return SKY_PIPELINES.apply(new SkyParameters(SkyType.MONO_COLOR, blendFunction));
    }

    public static RenderPipeline texturedSkybox(@Nullable BlendFunction blendFunction) {
        return SKY_PIPELINES.apply(new SkyParameters(SkyType.TEXTURED, blendFunction));
    }

    public static RenderPipeline frameBlendedTexturedSkybox(@Nullable BlendFunction blendFunction) {
        return SKY_PIPELINES.apply(new SkyParameters(SkyType.FRAME_BLENDED_TEXTURED, blendFunction));
    }

    private static RenderPipeline buildSkyPipeline(SkyParameters parameters) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
        String location = switch (parameters.type()) {
            case MONO_COLOR -> {
                builder.withVertexShader(MONO_COLOR_SKYBOX_SHADER)
                        .withFragmentShader(MONO_COLOR_SKYBOX_SHADER)
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS);
                yield "pipeline/mono_color_skybox/" + pipelineSuffix(parameters.blendFunction());
            }
            case TEXTURED, FRAME_BLENDED_TEXTURED -> {
                boolean frameBlended = parameters.type() == SkyType.FRAME_BLENDED_TEXTURED;
                Identifier shader = frameBlended ? MULTI_TEXTURED_SKYBOX_SHADER : TEXTURED_SKYBOX_SHADER;
                VertexFormat vertexFormat = frameBlended
                        ? FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT : DefaultVertexFormat.POSITION_TEX;
                builder.withVertexShader(shader)
                        .withFragmentShader(shader)
                        .withVertexBinding(0, vertexFormat)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withCull(false)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0);
                String path = frameBlended ? "pipeline/textured_skybox_frame_blend/" : "pipeline/textured_skybox/";
                yield path + pipelineSuffix(parameters.blendFunction());
            }
        };
        applyBlend(builder, parameters.blendFunction());

        RenderPipeline pipeline = builder
                .withLocation(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, location))
                .build();
        if (parameters.type() == SkyType.TEXTURED || parameters.type() == SkyType.FRAME_BLENDED_TEXTURED) {
            IrisCompat.assignSkyTexturedPipeline(pipeline);
        } else {
            IrisCompat.assignSkyBasicPipeline(pipeline);
        }
        return pipeline;
    }

    private static void applyBlend(RenderPipeline.Builder builder, @Nullable BlendFunction blendFunction) {
        if (blendFunction != null) {
            builder.withColorTargetState(new ColorTargetState(blendFunction));
        } else {
            builder.withColorTargetState(ColorTargetState.DEFAULT);
        }
    }

    private static String pipelineSuffix(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            return "no_blend";
        }

        String colorBlend = factorName(blendFunction.color().sourceFactor())
                + "_to_"
                + factorName(blendFunction.color().destFactor());
        String alphaBlend = factorName(blendFunction.alpha().sourceFactor())
                + "_to_"
                + factorName(blendFunction.alpha().destFactor());

        if (colorBlend.equals(alphaBlend)) {
            return "blend_" + colorBlend;
        }

        return "blend_color_" + colorBlend + "_alpha_" + alphaBlend;
    }

    private static String factorName(Enum<?> factor) {
        return factor.name().toLowerCase(Locale.ROOT);
    }

    public static VertexFormat vertexFormat(RenderPipeline pipeline) {
        return pipeline.getVertexFormatBinding(0);
    }

    public static ByteBufferBuilder byteBufferBuilder(RenderPipeline pipeline, int vertexCount) {
        return new ByteBufferBuilder(vertexFormat(pipeline).getVertexSize() * vertexCount);
    }

    public static BufferBuilder bufferBuilder(ByteBufferBuilder byteBufferBuilder, RenderPipeline pipeline) {
        return new BufferBuilder(byteBufferBuilder, pipeline.getPrimitiveTopology(), vertexFormat(pipeline));
    }

    private enum SkyType {
        MONO_COLOR,
        TEXTURED,
        FRAME_BLENDED_TEXTURED
    }

    private record SkyParameters(SkyType type, @Nullable BlendFunction blendFunction) {
    }
}
