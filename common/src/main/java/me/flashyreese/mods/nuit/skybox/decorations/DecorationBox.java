package me.flashyreese.mods.nuit.skybox.decorations;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import me.flashyreese.mods.nuit.util.OverrideUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class DecorationBox extends AbstractSkybox implements SkyboxTextureProvider {
    private static final Identifier DEFAULT_SUN = Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
    private static final Identifier DEFAULT_MOON = Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png");
    private static final Identifier[] DEFAULT_MOON_PHASES = new Identifier[]{
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/waning_gibbous.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/third_quarter.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/waning_crescent.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/new_moon.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/waxing_crescent.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/first_quarter.png"),
            Identifier.withDefaultNamespace("textures/environment/celestial/moon/waxing_gibbous.png")
    };

    public static Codec<DecorationBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.decorations()).forGetter(DecorationBox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(DecorationBox::getConditions),
            Identifier.CODEC.optionalFieldOf("sun", DEFAULT_SUN).forGetter(DecorationBox::getSunTexture),
            Identifier.CODEC.optionalFieldOf("moon", DEFAULT_MOON).forGetter(DecorationBox::getMoonTexture),
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
    public void render(SkyboxRenderContext context) {
        context.applyFog();
        Camera camera = context.camera();
        float tickDelta = context.tickDelta();
        ClientLevel level = Objects.requireNonNull((ClientLevel) camera.entity().level());

        OverrideUtils.enableBlendingOverride(this.blend.getBlendFunction());
        try {
            Matrix4f decorationMatrix = this.properties.rotation().apply(new Matrix4f(context.skyModelViewStack()), level);
            Vector4f colorModifier = this.blend.getColorModifier(this.alpha);
            GpuBufferSlice dynamicTransforms = NuitRenderBackend.createDynamicTransforms(decorationMatrix, colorModifier);

            // poseStack.mulPose(Axis.YP.rotation(-90F));
            // poseStack.mulPose(Axis.YP.rotation(level.getTimeOfDay(tickDelta) * 360.0F));
            // Iris Compat
            // poseStack.mulPose(Axis.ZP.rotationDegrees(IrisCompat.getSunPathRotation()));
            // poseStack.mulPose(Axis.XP.rotationDegrees(level.getSunAngle(tickDelta) * 360.0F * this.properties.rotation().speed()));

            if (this.sunEnabled) {
                this.renderSun(dynamicTransforms);
            }

            if (this.moonEnabled) {
                this.renderMoon(camera.attributeProbe().getValue(EnvironmentAttributes.MOON_PHASE, tickDelta), dynamicTransforms);
            }

            if (this.starsEnabled) {
                PoseStack poseStack = new PoseStack();
                this.properties.rotation().apply(poseStack, level);
                context.renderStars(camera.attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, tickDelta), poseStack);
            }

        } finally {
            OverrideUtils.disableBlendingOverride();
        }
    }

    private void renderSun(GpuBufferSlice dynamicTransforms) {
        RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(this.blend.getBlendFunction());
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 4)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            builder.addVertex(-30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F);
            builder.addVertex(30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F);
            builder.addVertex(30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F);
            builder.addVertex(-30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F);

            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, "Sampler0", this.sunTexture);
        }
    }

    private void renderMoon(MoonPhase moonPhase, GpuBufferSlice dynamicTransforms) {
        boolean useDefaultMoonPhases = this.moonTexture.equals(DEFAULT_MOON);
        Identifier texture = useDefaultMoonPhases ? DEFAULT_MOON_PHASES[moonPhase.index()] : this.moonTexture;
        float startX = 0.0F;
        float startY = 0.0F;
        float endX = 1.0F;
        float endY = 1.0F;

        if (!useDefaultMoonPhases) {
            int xCoord = moonPhase.index() % 4;
            int yCoord = moonPhase.index() / 4 % 2;
            startX = xCoord / 4.0F;
            startY = yCoord / 2.0F;
            endX = (xCoord + 1) / 4.0F;
            endY = (yCoord + 1) / 2.0F;
        }

        RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(this.blend.getBlendFunction());
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(pipeline.getVertexFormat().getVertexSize() * 4)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            builder.addVertex(-20.0F, -100.0F, 20.0F).setUv(endX, endY);
            builder.addVertex(20.0F, -100.0F, 20.0F).setUv(startX, endY);
            builder.addVertex(20.0F, -100.0F, -20.0F).setUv(startX, startY);
            builder.addVertex(-20.0F, -100.0F, -20.0F).setUv(endX, startY);

            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), dynamicTransforms, "Sampler0", texture);
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

    @Override
    public Collection<Identifier> getTexturesToRegister() {
        Collection<Identifier> textures = new ArrayList<>();
        if (this.sunEnabled) {
            textures.add(this.sunTexture);
        }

        if (this.moonEnabled) {
            if (this.moonTexture.equals(DEFAULT_MOON)) {
                textures.addAll(List.of(DEFAULT_MOON_PHASES));
            } else {
                textures.add(this.moonTexture);
            }
        }

        return textures;
    }
}
