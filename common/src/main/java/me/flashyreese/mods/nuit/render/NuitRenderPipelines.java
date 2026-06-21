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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    private static final Map<BlendFunction, RenderPipeline> MONO_COLOR_SKYBOX_BLEND_PIPELINES = new HashMap<>();
    private static final Map<BlendFunction, RenderPipeline> TEXTURED_SKYBOX_BLEND_PIPELINES = new HashMap<>();
    private static final Map<BlendFunction, RenderPipeline> FRAME_BLENDED_TEXTURED_SKYBOX_BLEND_PIPELINES = new HashMap<>();

    private static RenderPipeline monoColorSkyboxNoBlendPipeline;
    private static RenderPipeline texturedSkyboxNoBlendPipeline;
    private static RenderPipeline frameBlendedTexturedSkyboxNoBlendPipeline;

    public static RenderPipeline monoColorSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (monoColorSkyboxNoBlendPipeline == null) {
                monoColorSkyboxNoBlendPipeline = buildMonoColorSkyboxPipeline(null);
            }

            return monoColorSkyboxNoBlendPipeline;
        }

        return MONO_COLOR_SKYBOX_BLEND_PIPELINES.computeIfAbsent(blendFunction, NuitRenderPipelines::buildMonoColorSkyboxPipeline);
    }

    public static RenderPipeline texturedSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (texturedSkyboxNoBlendPipeline == null) {
                texturedSkyboxNoBlendPipeline = buildTexturedSkyboxPipeline(null, false);
            }

            return texturedSkyboxNoBlendPipeline;
        }

        return TEXTURED_SKYBOX_BLEND_PIPELINES.computeIfAbsent(blendFunction, blend -> buildTexturedSkyboxPipeline(blend, false));
    }

    public static RenderPipeline frameBlendedTexturedSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (frameBlendedTexturedSkyboxNoBlendPipeline == null) {
                frameBlendedTexturedSkyboxNoBlendPipeline = buildTexturedSkyboxPipeline(null, true);
            }

            return frameBlendedTexturedSkyboxNoBlendPipeline;
        }

        return FRAME_BLENDED_TEXTURED_SKYBOX_BLEND_PIPELINES.computeIfAbsent(blendFunction, blend -> buildTexturedSkyboxPipeline(blend, true));
    }

    private static RenderPipeline buildMonoColorSkyboxPipeline(@Nullable BlendFunction blendFunction) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
        builder.withLocation(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "pipeline/mono_color_skybox/" + pipelineSuffix(blendFunction)));
        builder.withVertexShader(MONO_COLOR_SKYBOX_SHADER);
        builder.withFragmentShader(MONO_COLOR_SKYBOX_SHADER);
        applyBlend(builder, blendFunction);
        builder.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR);
        builder.withPrimitiveTopology(PrimitiveTopology.QUADS);
        RenderPipeline pipeline = builder.build();
        IrisCompat.assignSkyBasicPipeline(pipeline);
        return pipeline;
    }

    private static RenderPipeline buildTexturedSkyboxPipeline(@Nullable BlendFunction blendFunction, boolean frameBlended) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
        builder.withLocation(Identifier.fromNamespaceAndPath(
                NuitClient.MOD_ID,
                (frameBlended ? "pipeline/textured_skybox_frame_blend/" : "pipeline/textured_skybox/") + pipelineSuffix(blendFunction)
        ));
        if (frameBlended) {
            builder.withVertexShader(MULTI_TEXTURED_SKYBOX_SHADER);
            builder.withFragmentShader(MULTI_TEXTURED_SKYBOX_SHADER);
            builder.withVertexBinding(0, FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT);
            builder.withPrimitiveTopology(PrimitiveTopology.QUADS);
        } else {
            builder.withVertexShader(TEXTURED_SKYBOX_SHADER);
            builder.withFragmentShader(TEXTURED_SKYBOX_SHADER);
            builder.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX);
            builder.withPrimitiveTopology(PrimitiveTopology.QUADS);
        }
        builder.withCull(false);
        applyBlend(builder, blendFunction);
        builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        RenderPipeline pipeline = builder.build();
        IrisCompat.assignSkyTexturedPipeline(pipeline);
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
}
