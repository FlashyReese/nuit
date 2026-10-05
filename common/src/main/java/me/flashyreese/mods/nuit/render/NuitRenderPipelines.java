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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * Pipelines drawn into a pass with a depth attachment (the main pass when improved transparency is off) must declare a
     * depth stencil state, while the sky target used with improved transparency has no depth attachment at all.
     */
    private static final Map<RenderPipeline, RenderPipeline> DEPTH_ATTACHMENT_VARIANTS = new IdentityHashMap<>(Map.of(
            RenderPipelines.SKY, RenderPipelines.SKY_WITH_DEPTH_ATTACHMENT,
            RenderPipelines.SKY_OCCLUDER, RenderPipelines.SKY_OCCLUDER_WITH_DEPTH_ATTACHMENT,
            RenderPipelines.END_SKY, RenderPipelines.END_SKY_WITH_DEPTH_ATTACHMENT,
            RenderPipelines.SUNRISE_SUNSET, RenderPipelines.SUNRISE_SUNSET_WITH_DEPTH_ATTACHMENT,
            RenderPipelines.STARS, RenderPipelines.STARS_WITH_DEPTH_ATTACHMENT,
            RenderPipelines.CELESTIAL, RenderPipelines.CELESTIAL_WITH_DEPTH_ATTACHMENT
    ));

    private static final Function<BlendFunction, RenderPipeline> MONO_COLOR_SKYBOX_BLEND_PIPELINE =
            Util.memoize(NuitRenderPipelines::buildMonoColorSkyboxPipeline);
    private static final Function<BlendFunction, RenderPipeline> TEXTURED_SKYBOX_BLEND_PIPELINE =
            Util.memoize(blend -> buildTexturedSkyboxPipeline(blend, false));
    private static final Function<BlendFunction, RenderPipeline> FRAME_BLENDED_TEXTURED_SKYBOX_BLEND_PIPELINE =
            Util.memoize(blend -> buildTexturedSkyboxPipeline(blend, true));

    private static RenderPipeline translucentSkyDiscPipeline;
    private static RenderPipeline skyOccluderPipeline;
    private static RenderPipeline monoColorSkyboxNoBlendPipeline;
    private static RenderPipeline texturedSkyboxNoBlendPipeline;
    private static RenderPipeline frameBlendedTexturedSkyboxNoBlendPipeline;

    public static RenderPipeline translucentSkyDisc() {
        if (translucentSkyDiscPipeline == null) {
            translucentSkyDiscPipeline = buildTranslucentSkyDiscPipeline();
        }

        return translucentSkyDiscPipeline;
    }

    /**
     * Vanilla's sky occluder, except that it fades with the color modulator's alpha.
     */
    public static RenderPipeline skyOccluder() {
        if (skyOccluderPipeline == null) {
            skyOccluderPipeline = buildSkyOccluderPipeline();
        }

        return skyOccluderPipeline;
    }

    public static RenderPipeline monoColorSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (monoColorSkyboxNoBlendPipeline == null) {
                monoColorSkyboxNoBlendPipeline = buildMonoColorSkyboxPipeline(null);
            }

            return monoColorSkyboxNoBlendPipeline;
        }

        return MONO_COLOR_SKYBOX_BLEND_PIPELINE.apply(blendFunction);
    }

    public static RenderPipeline texturedSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (texturedSkyboxNoBlendPipeline == null) {
                texturedSkyboxNoBlendPipeline = buildTexturedSkyboxPipeline(null, false);
            }

            return texturedSkyboxNoBlendPipeline;
        }

        return TEXTURED_SKYBOX_BLEND_PIPELINE.apply(blendFunction);
    }

    public static RenderPipeline frameBlendedTexturedSkybox(@Nullable BlendFunction blendFunction) {
        if (blendFunction == null) {
            if (frameBlendedTexturedSkyboxNoBlendPipeline == null) {
                frameBlendedTexturedSkyboxNoBlendPipeline = buildTexturedSkyboxPipeline(null, true);
            }

            return frameBlendedTexturedSkyboxNoBlendPipeline;
        }

        return FRAME_BLENDED_TEXTURED_SKYBOX_BLEND_PIPELINE.apply(blendFunction);
    }

    /**
     * Returns the variant of {@code pipeline} that matches the depth attachment of the render pass it is drawn into.
     */
    public static RenderPipeline forDepthAttachment(RenderPipeline pipeline, boolean withDepthAttachment) {
        if (!withDepthAttachment) {
            return pipeline;
        }

        RenderPipeline variant = DEPTH_ATTACHMENT_VARIANTS.get(pipeline);
        if (variant == null) {
            throw new IllegalStateException("No depth attachment variant registered for pipeline " + pipeline.getLocation());
        }

        return variant;
    }

    private static RenderPipeline buildTranslucentSkyDiscPipeline() {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withVertexShader("core/sky")
                .withFragmentShader("core/sky")
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        return buildWithDepthAttachmentVariant(builder, "pipeline/translucent_sky_disc", IrisCompat::assignSkyBasicPipeline);
    }

    private static RenderPipeline buildSkyOccluderPipeline() {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withBindGroupLayout(BindGroupLayouts.SKY_OCCLUDER_INFO)
                .withVertexShader("core/sky_occluder")
                .withFragmentShader(SKY_OCCLUDER_FRAGMENT_SHADER)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        return buildWithDepthAttachmentVariant(builder, "pipeline/sky_occluder", IrisCompat::assignSkyBasicPipeline);
    }

    private static RenderPipeline buildMonoColorSkyboxPipeline(@Nullable BlendFunction blendFunction) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
        builder.withVertexShader(MONO_COLOR_SKYBOX_SHADER);
        builder.withFragmentShader(MONO_COLOR_SKYBOX_SHADER);
        applyBlend(builder, blendFunction);
        builder.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR);
        builder.withPrimitiveTopology(PrimitiveTopology.QUADS);
        return buildWithDepthAttachmentVariant(builder, "pipeline/mono_color_skybox/" + pipelineSuffix(blendFunction), IrisCompat::assignSkyBasicPipeline);
    }

    private static RenderPipeline buildTexturedSkyboxPipeline(@Nullable BlendFunction blendFunction, boolean frameBlended) {
        RenderPipeline.Builder builder = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET);
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
        String location = (frameBlended ? "pipeline/textured_skybox_frame_blend/" : "pipeline/textured_skybox/") + pipelineSuffix(blendFunction);
        return buildWithDepthAttachmentVariant(builder, location, IrisCompat::assignSkyTexturedPipeline);
    }

    private static RenderPipeline buildWithDepthAttachmentVariant(RenderPipeline.Builder builder, String location, Consumer<RenderPipeline> irisAssignment) {
        RenderPipeline pipeline = builder
                .withLocation(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, location))
                .build();
        RenderPipeline depthAttachmentPipeline = builder
                .withLocation(Identifier.fromNamespaceAndPath(NuitClient.MOD_ID, location + "_with_depth_attachment"))
                .withDepthStencilState(DepthStencilState.OFF)
                .build();
        irisAssignment.accept(pipeline);
        irisAssignment.accept(depthAttachmentPipeline);
        DEPTH_ATTACHMENT_VARIANTS.put(pipeline, depthAttachmentPipeline);
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
