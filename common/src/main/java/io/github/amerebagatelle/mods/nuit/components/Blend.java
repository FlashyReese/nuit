package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.function.Function;

public class Blend {
    public static Codec<Blend> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(Blend::getType),
            Blender.CODEC.optionalFieldOf("blender", Blender.normal()).forGetter(Blend::getBlender)
    ).apply(instance, Blend::new));

    private final String type;
    private final Blender blender;
    private final BlendFunction blendFunction;
    private final Function<Float, Vector4f> colorAndEquationFunc;

    public Blend(String type, Blender blender) {
        this.type = type;
        this.blender = blender;
        switch (type) {
            case "add" -> {
                this.blendFunction = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(1.0F, 1.0F, 1.0F, alpha);
                };
            }

            case "subtract" -> {
                this.blendFunction = new BlendFunction(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ZERO);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(alpha, alpha, alpha, 1.0F);
                };
            }

            case "multiply" -> {
                this.blendFunction = new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ONE_MINUS_SRC_ALPHA);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(alpha, alpha, alpha, alpha);
                };
            }

            case "screen" -> {
                this.blendFunction = new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_COLOR);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(alpha, alpha, alpha, 1.0F);
                };
            }

            case "replace" -> {
                this.blendFunction = new BlendFunction(SourceFactor.ZERO, DestFactor.ONE);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(1.0F, 1.0F, 1.0F, alpha);
                };
            }

            case "normal" -> {
                this.blendFunction = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(1.0F, 1.0F, 1.0F, alpha);
                };
            }

            case "burn" -> {
                this.blendFunction = new BlendFunction(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(alpha, alpha, alpha, 1.0F);
                };
            }

            case "dodge" -> {
                this.blendFunction = new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ONE);
                this.colorAndEquationFunc = (alpha) -> {
                    GL46C.glBlendEquation(Blender.Equation.ADD.value);
                    return new Vector4f(alpha, alpha, alpha, 1.0F);
                };
            }

            case "disable" -> {
                this.blendFunction = null;
                this.colorAndEquationFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }

            case "decorations" -> {
                this.blendFunction = Blender.decorations().getBlendFunction();
                this.colorAndEquationFunc = Blender.decorations()::applyEquationAndGetColor;
            }

            case "custom" -> {
                this.blendFunction = this.blender.getBlendFunction();
                this.colorAndEquationFunc = this.blender::applyEquationAndGetColor;
            }

            default -> {
                if (NuitClient.config().generalSettings.debugMode) {
                    NuitClient.logger().error("Blend mode is set to an invalid or unsupported value.");
                }

                this.blendFunction = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                this.colorAndEquationFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }

    public Vector4f applyEquationAndGetColor(float alpha) {
        return this.colorAndEquationFunc.apply(alpha);
    }

    public @Nullable BlendFunction getBlendFunction() {
        return this.blendFunction;
    }

    public String getType() {
        return this.type;
    }

    public Blender getBlender() {
        return this.blender;
    }

    public static Blend normal() {
        return new Blend("", Blender.normal());
    }

    public static Blend decorations() {
        return new Blend("decorations", Blender.decorations());
    }
}
