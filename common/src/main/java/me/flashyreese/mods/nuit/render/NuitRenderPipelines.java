package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.NuitClient;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

public final class NuitRenderPipelines {
    private static final Identifier MONO_COLOR_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/mono_color_skybox");
    private static final Identifier TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/textured_skybox");
    private static final Identifier MULTI_TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/multi_textured_skybox");

    // fixme: 1.21.11 BufferBuilder has no generic custom float attribute setter
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
        builder.withDepthWrite(false);
        String location = switch (parameters.type()) {
            case MONO_COLOR -> {
                builder.withVertexShader(MONO_COLOR_SKYBOX_SHADER)
                        .withFragmentShader(MONO_COLOR_SKYBOX_SHADER)
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
                yield "pipeline/mono_color_skybox/" + pipelineSuffix(parameters.blendFunction());
            }
            case TEXTURED, FRAME_BLENDED_TEXTURED -> {
                boolean frameBlended = parameters.type() == SkyType.FRAME_BLENDED_TEXTURED;
                Identifier shader = frameBlended ? MULTI_TEXTURED_SKYBOX_SHADER : TEXTURED_SKYBOX_SHADER;
                VertexFormat vertexFormat = frameBlended
                        ? FRAME_BLENDED_TEXTURED_SKYBOX_VERTEX_FORMAT : DefaultVertexFormat.POSITION_TEX;
                builder.withVertexShader(shader)
                        .withFragmentShader(shader)
                        .withVertexFormat(vertexFormat, VertexFormat.Mode.QUADS)
                        .withCull(false)
                        .withSampler("Sampler0");
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
            builder.withBlend(blendFunction);
        } else {
            builder.withoutBlend();
        }
    }

    private static String pipelineSuffix(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            return "no_blend";
        }

        String colorBlend = factorName(blendFunction.sourceColor())
                + "_to_"
                + factorName(blendFunction.destColor());
        String alphaBlend = factorName(blendFunction.sourceAlpha())
                + "_to_"
                + factorName(blendFunction.destAlpha());

        if (colorBlend.equals(alphaBlend)) {
            return "blend_" + colorBlend;
        }

        return "blend_color_" + colorBlend + "_alpha_" + alphaBlend;
    }

    private static String factorName(Enum<?> factor) {
        return factor.name().toLowerCase(Locale.ROOT);
    }

    private enum SkyType {
        MONO_COLOR,
        TEXTURED,
        FRAME_BLENDED_TEXTURED
    }

    private record SkyParameters(SkyType type, @Nullable BlendFunction blendFunction) {
    }
}
