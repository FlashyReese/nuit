package me.flashyreese.mods.nuit.components;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.NuitClient;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.function.Function;

public class Blend {
    public static Codec<Blend> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(Blend::getType)
    ).apply(instance, Blend::new));

    private final String type;
    private final BlendFunction blendFunction;
    private final Function<Float, Vector4f> colorModifierFunc;

    public Blend(String type) {
        this.type = type;
        switch (type) {
            case "add" -> {
                this.blendFunction = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            case "subtract" -> {
                this.blendFunction = new BlendFunction(BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.ZERO);
                this.colorModifierFunc = (alpha) -> new Vector4f(alpha, alpha, alpha, 1.0F);
            }

            case "multiply" -> {
                this.blendFunction = new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE_MINUS_SRC_ALPHA);
                this.colorModifierFunc = (alpha) -> new Vector4f(alpha, alpha, alpha, alpha);
            }

            case "screen" -> {
                this.blendFunction = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR);
                this.colorModifierFunc = (alpha) -> new Vector4f(alpha, alpha, alpha, 1.0F);
            }

            case "replace" -> {
                this.blendFunction = new BlendFunction(BlendFactor.ZERO, BlendFactor.ONE);
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            case "", "alpha", "normal" -> {
                this.blendFunction = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            case "burn" -> {
                this.blendFunction = new BlendFunction(BlendFactor.ZERO, BlendFactor.ONE_MINUS_SRC_COLOR);
                this.colorModifierFunc = (alpha) -> new Vector4f(alpha, alpha, alpha, 1.0F);
            }

            case "dodge" -> {
                this.blendFunction = new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE);
                this.colorModifierFunc = (alpha) -> new Vector4f(alpha, alpha, alpha, 1.0F);
            }

            case "disable" -> {
                this.blendFunction = null;
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            case "decorations" -> {
                this.blendFunction = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            default -> {
                NuitClient.getLogger().error("Blend mode is set to an invalid or unsupported value.");

                this.blendFunction = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO);
                this.colorModifierFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }

    public static Blend normal() {
        return new Blend("normal");
    }

    public static Blend decorations() {
        return new Blend("decorations");
    }

    public Vector4f getColorModifier(float alpha) {
        return this.colorModifierFunc.apply(alpha);
    }

    public @Nullable BlendFunction getBlendFunction() {
        return this.blendFunction;
    }

    public String getType() {
        return this.type;
    }
}
