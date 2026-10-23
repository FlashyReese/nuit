package me.flashyreese.mods.nuit.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import me.flashyreese.mods.nuit.IrisCompat;
import me.flashyreese.mods.nuit.NuitClient;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.pipeline.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class NuitRenderPipelines {
    private static final Identifier MONO_COLOR_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/mono_color_skybox");
    private static final Identifier TEXTURED_SKYBOX_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/textured_skybox");
    private static final Identifier SKY_OCCLUDER_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, "core/sky_occluder");
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
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .buildSnippet();

    public static RenderPipeline translucentSkyDisc(boolean hasDepthAttachment) {
        return sky(new SkyParameters(SkyType.TRANSLUCENT_SKY_DISC, BlendFunction.TRANSLUCENT, hasDepthAttachment));
    }

    /**
     * Vanilla's sky occluder, except that it fades with the color modulator's alpha.
     */
    public static RenderPipeline skyOccluder(boolean hasDepthAttachment) {
        return sky(new SkyParameters(SkyType.SKY_OCCLUDER, BlendFunction.TRANSLUCENT, hasDepthAttachment));
    }

    public static RenderPipeline monoColorSkybox(@Nullable BlendFunction blendFunction, boolean hasDepthAttachment) {
        return sky(new SkyParameters(SkyType.MONO_COLOR, blendFunction, hasDepthAttachment));
    }

    public static RenderPipeline texturedSkybox(@Nullable BlendFunction blendFunction, boolean hasDepthAttachment) {
        return sky(new SkyParameters(SkyType.TEXTURED, blendFunction, hasDepthAttachment));
    }

    public static RenderPipeline frameBlendedTexturedSkybox(@Nullable BlendFunction blendFunction,
                                                           boolean hasDepthAttachment) {
        return sky(new SkyParameters(SkyType.FRAME_BLENDED_TEXTURED, blendFunction, hasDepthAttachment));
    }

    /**
     * Returns the cached permutation for the requested sky type, blend and render-pass attachments.
     */
    public static RenderPipeline sky(SkyParameters parameters) {
        return RenderPipelines.dynamicPipeline(parameters, NuitClient.MOD_ID + ":sky",
                NuitRenderPipelines::buildSkyPipeline);
    }

    private static RenderPipeline buildSkyPipeline(SkyParameters parameters) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
        String location = switch (parameters.type()) {
            case TRANSLUCENT_SKY_DISC -> {
                builder.withBindGroupLayout(BindGroupLayouts.FOG)
                        .withVertexShader("core/sky")
                        .withFragmentShader("core/sky")
                        .withVertexBinding(0, DefaultVertexFormat.POSITION)
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN);
                yield "pipeline/translucent_sky_disc";
            }
            case SKY_OCCLUDER -> {
                builder.withBindGroupLayout(BindGroupLayouts.FOG)
                        .withBindGroupLayout(BindGroupLayouts.SKY_OCCLUDER_INFO)
                        .withVertexShader("core/sky_occluder")
                        .withFragmentShader(SKY_OCCLUDER_FRAGMENT_SHADER)
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
                yield "pipeline/sky_occluder";
            }
            case MONO_COLOR -> {
                builder.withVertexShader(MONO_COLOR_SKYBOX_SHADER)
                        .withFragmentShader(MONO_COLOR_SKYBOX_SHADER)
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS);
                yield "pipeline/mono_color_skybox";
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
                yield frameBlended ? "pipeline/textured_skybox_frame_blend" : "pipeline/textured_skybox";
            }
        };
        applyBlend(builder, parameters.blendFunction());
        location += "/" + pipelineSuffix(parameters.blendFunction());
        // The classic main pass has a depth attachment; the improved-transparency sky pass does not.
        if (parameters.hasDepthAttachment()) {
            builder.withDepthStencilState(DepthStencilState.OFF);
            location += "_with_depth_attachment";
        }

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

    public enum SkyType {
        MONO_COLOR,
        TEXTURED,
        FRAME_BLENDED_TEXTURED,
        TRANSLUCENT_SKY_DISC,
        SKY_OCCLUDER
    }

    public record SkyParameters(SkyType type, @Nullable BlendFunction blendFunction, boolean hasDepthAttachment) {
    }
}
