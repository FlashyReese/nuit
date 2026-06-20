package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.NuitClient;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class NuitRenderPipelines {
    private static final Identifier MONO_COLOR_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/mono_color_skybox");
    private static final Identifier TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/textured_skybox");
    private static final Identifier MULTI_TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/multi_textured_skybox");

    // fixme: 26.1 BufferBuilder has no generic custom float attribute setter
    // Reuse LINE_WIDTH as the frame blend carrier until the 26.2 vertex format API
    public static final VertexFormat FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("FrameBlend", VertexFormatElement.LINE_WIDTH)
            .build();

    private static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
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
        builder.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
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
            builder.withVertexFormat(FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT, VertexFormat.Mode.QUADS);
        } else {
            builder.withVertexShader(TEXTURED_SKYBOX_SHADER);
            builder.withFragmentShader(TEXTURED_SKYBOX_SHADER);
            builder.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS);
        }
        builder.withCull(false);
        applyBlend(builder, blendFunction);
        builder.withSampler("Sampler0");
        RenderPipeline pipeline = builder.build();
        IrisCompat.assignSkyTexturedPipeline(pipeline);
        return pipeline;
    }

    private static void applyBlend(RenderPipeline.Builder builder, @Nullable BlendFunction blendFunction) {
        if (blendFunction != null) {
            builder.withColorTargetState(new ColorTargetState(blendFunction));
        } else {
            builder.withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_ALL));
        }
    }

    private static String pipelineSuffix(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            return "no_blend";
        }

        return "blend_"
                + factorName(blendFunction.sourceColor()) + "_"
                + factorName(blendFunction.destColor()) + "_"
                + factorName(blendFunction.sourceAlpha()) + "_"
                + factorName(blendFunction.destAlpha());
    }

    private static String factorName(Enum<?> factor) {
        return factor.name().toLowerCase(Locale.ROOT);
    }
}
