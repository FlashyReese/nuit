package me.flashyreese.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.mixin.SkyRendererAccessor;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.BufferUploader;
import me.flashyreese.mods.nuit.util.OverrideUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL46C;

import java.util.Objects;

public class DecorationBox extends AbstractSkybox {
    private static final Identifier MOON_PHASES = Identifier.withDefaultNamespace("textures/environment/moon_phases.png");

    public static Codec<DecorationBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.decorations()).forGetter(DecorationBox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(DecorationBox::getConditions),
            Identifier.CODEC.optionalFieldOf("sun", SkyRendererAccessor.getSun()).forGetter(DecorationBox::getSunTexture),
            Identifier.CODEC.optionalFieldOf("moon", MOON_PHASES).forGetter(DecorationBox::getMoonTexture),
            Codec.BOOL.optionalFieldOf("showSun", false).forGetter(DecorationBox::isSunEnabled),
            Codec.BOOL.optionalFieldOf("showMoon", false).forGetter(DecorationBox::isMoonEnabled),
            Codec.BOOL.optionalFieldOf("showStars", false).forGetter(DecorationBox::isStarsEnabled),
            Blend.CODEC.optionalFieldOf("blend", Blend.decorations()).forGetter(DecorationBox::getBlend)
    ).apply(instance, DecorationBox::new));

    private final Identifier sunTexture;
    private final Identifier moonTexture;
    private final boolean sunEnabled;
    private final boolean moonEnabled;
    private final boolean starsEnabled;
    private final Blend blend;

    public DecorationBox(Properties properties, Conditions conditions, Identifier sun, Identifier moon, boolean sunEnabled, boolean moonEnabled, boolean starsEnabled, Blend blend) {
        this.properties = properties;
        this.conditions = conditions;
        this.sunTexture = sun;
        this.moonTexture = moon;
        this.sunEnabled = sunEnabled;
        this.moonEnabled = moonEnabled;
        this.starsEnabled = starsEnabled;
        this.blend = blend;
    }

    @Override
    public void render(SkyRendererAccessor skyRendererAccessor, Matrix4fStack matrix4fStack, float tickDelta, Camera camera, GpuBufferSlice fogParameters, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        RenderSystem.setShaderFog(fogParameters);
        ClientLevel level = Objects.requireNonNull((ClientLevel) camera.entity().level());

        OverrideUtils.enableBlendingOverride(this.blend.getBlendFunction());

        matrix4fStack.pushMatrix();
        this.properties.rotation().apply(matrix4fStack, level);

        // poseStack.mulPose(Axis.YP.rotation(-90F));
        // poseStack.mulPose(Axis.YP.rotation(level.getTimeOfDay(tickDelta) * 360.0F));
        // Iris Compat
        // poseStack.mulPose(Axis.ZP.rotationDegrees(IrisCompat.getSunPathRotation()));
        // poseStack.mulPose(Axis.XP.rotationDegrees(level.getSunAngle(tickDelta) * 360.0F * this.properties.rotation().speed()));

        if (this.sunEnabled) {
            this.renderSun(bufferSource, poseStack);
        }

        if (this.moonEnabled) {
            this.renderMoon(level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE), bufferSource, poseStack);
        }

        if (this.sunEnabled || this.moonEnabled) {
            bufferSource.endBatch();
        }

        if (this.starsEnabled) {
            //PoseStack poseStack = new PoseStack();
            poseStack.mulPose(matrix4fStack);
            skyRendererAccessor.invokeRenderStars(level.environmentAttributes().getDimensionValue(EnvironmentAttributes.STAR_BRIGHTNESS), poseStack);
        }

        matrix4fStack.popMatrix();
        OverrideUtils.disableBlendingOverride();
        GL46C.glBlendEquation(GL46C.GL_FUNC_ADD);
    }

    private void renderSun(MultiBufferSource multiBufferSource, PoseStack poseStack) {
        int i = ARGB.white(this.alpha);
        Matrix4f matrix4f = poseStack.last().pose();

        final RenderPipeline pipeline = RenderPipelines.CELESTIAL;
        try (final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 4)) {
            final BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            builder.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F).setColor(i);
            builder.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F).setColor(i);
            builder.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F).setColor(i);
            builder.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F).setColor(i);

            final GpuTextureView sunTextureView = Minecraft.getInstance().getTextureManager().getTexture(this.sunTexture).getTextureView();
            BufferUploader.drawWithShader(pipeline, builder.buildOrThrow(), (pass) -> {
                pass.bindTexture("Sampler0", sunTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            });
        }
    }

    private void renderMoon(MoonPhase moonPhase, MultiBufferSource multiBufferSource, PoseStack poseStack) {
        int xCoord = moonPhase.index() % 4;
        int yCoord = moonPhase.index() / 4 % 2;
        float startX = xCoord / 4.0F;
        float startY = yCoord / 2.0F;
        float endX = (xCoord + 1) / 4.0F;
        float endY = (yCoord + 1) / 2.0F;

        final RenderPipeline pipeline = RenderPipelines.CELESTIAL;
        try (final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 4)) {
            final BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());

            int p = ARGB.white(this.alpha);
            Matrix4f matrix4f = poseStack.last().pose();
            builder.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(endX, endY).setColor(p);
            builder.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(startX, endY).setColor(p);
            builder.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(startX, startY).setColor(p);
            builder.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(endX, startY).setColor(p);

            final GpuTextureView moonTextureView = Minecraft.getInstance().getTextureManager().getTexture(this.moonTexture).getTextureView();
            BufferUploader.drawWithShader(pipeline, builder.buildOrThrow(), (pass) -> {
                pass.bindTexture("Sampler0", moonTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            });
        }
    }

    public Identifier getSunTexture() {
        return this.sunTexture;
    }

    public Identifier getMoonTexture() {
        return this.moonTexture;
    }

    public boolean isSunEnabled() {
        return this.sunEnabled;
    }

    public boolean isMoonEnabled() {
        return this.moonEnabled;
    }

    public boolean isStarsEnabled() {
        return this.starsEnabled;
    }

    public Blend getBlend() {
        return this.blend;
    }
}