package io.github.amerebagatelle.mods.nuit.components;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.amerebagatelle.mods.nuit.NuitClient;
import io.github.amerebagatelle.mods.nuit.util.Utils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46C;

import java.util.Arrays;
import java.util.function.Function;

public class Blender {
    public static Codec<Blender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("separateFunction", false).forGetter(Blender::isSeparateFunction),
            Codec.INT.optionalFieldOf("sourceFactor", GL46C.GL_SRC_ALPHA).forGetter(Blender::getSourceFactor),
            Codec.INT.optionalFieldOf("destinationFactor", GL46C.GL_ONE).forGetter(Blender::getDestinationFactor),
            Codec.INT.optionalFieldOf("equation", GL46C.GL_FUNC_ADD).forGetter(Blender::getEquation),
            Codec.INT.optionalFieldOf("sourceFactorAlpha", GL46C.GL_ZERO).forGetter(Blender::getSourceFactorAlpha),
            Codec.INT.optionalFieldOf("destinationFactorAlpha", GL46C.GL_ZERO).forGetter(Blender::getDestinationFactorAlpha),
            Codec.BOOL.optionalFieldOf("redAlphaEnabled", false).forGetter(Blender::isRedAlphaEnabled),
            Codec.BOOL.optionalFieldOf("greenAlphaEnabled", false).forGetter(Blender::isGreenAlphaEnabled),
            Codec.BOOL.optionalFieldOf("blueAlphaEnabled", false).forGetter(Blender::isBlueAlphaEnabled),
            Codec.BOOL.optionalFieldOf("alphaEnabled", true).forGetter(Blender::isAlphaEnabled)
    ).apply(instance, Blender::new));

    private final boolean separateFunction;

    private final int sourceFactor;
    private final int destinationFactor;
    private final int equation;

    private final int sourceFactorAlpha;
    private final int destinationFactorAlpha;

    private final boolean redAlphaEnabled;
    private final boolean greenAlphaEnabled;
    private final boolean blueAlphaEnabled;
    private final boolean alphaEnabled;

    private final BlendFunction blendFunction;
    private final Function<Float, Vector4f> colorAndEquationFunc;

    public Blender(boolean separateFunction, int sourceFactor, int destinationFactor, int equation, int sourceFactorAlpha, int destinationFactorAlpha, boolean redAlphaEnabled, boolean greenAlphaEnabled, boolean blueAlphaEnabled, boolean alphaEnabled) {
        this.separateFunction = separateFunction;
        this.sourceFactor = sourceFactor;
        this.destinationFactor = destinationFactor;
        this.equation = equation;
        this.sourceFactorAlpha = sourceFactorAlpha;
        this.destinationFactorAlpha = destinationFactorAlpha;
        this.redAlphaEnabled = redAlphaEnabled;
        this.greenAlphaEnabled = greenAlphaEnabled;
        this.blueAlphaEnabled = blueAlphaEnabled;
        this.alphaEnabled = alphaEnabled;
        if ((this.separateFunction && this.isValidFactor(sourceFactor) && this.isValidFactor(destinationFactor) && this.isValidFactor(sourceFactorAlpha) && this.isValidFactor(destinationFactorAlpha) && this.isValidEquation(equation)) || (this.isValidFactor(sourceFactor) && this.isValidFactor(destinationFactor) && this.isValidEquation(equation))) {
            if (this.separateFunction) {
                this.blendFunction = new BlendFunction(Utils.toSourceFactor(this.sourceFactor), Utils.toDestFactor(this.destinationFactor), Utils.toSourceFactor(this.sourceFactorAlpha), Utils.toDestFactor(this.destinationFactorAlpha));
            } else {
                this.blendFunction = new BlendFunction(Utils.toSourceFactor(this.sourceFactor), Utils.toDestFactor(this.destinationFactor));
            }

            this.colorAndEquationFunc = (alpha) -> {
                GL46C.glBlendEquation(this.equation);
                return new Vector4f(this.redAlphaEnabled ? alpha : 1.0F, this.greenAlphaEnabled ? alpha : 1.0F, this.blueAlphaEnabled ? alpha : 1.0F, this.alphaEnabled ? alpha : 1.0F);
            };
        } else {
            if (NuitClient.config().generalSettings.debugMode) {
                NuitClient.logger().error("Invalid custom blender values!");
            }

            this.blendFunction = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            this.colorAndEquationFunc = (alpha) -> new Vector4f(1.0F, 1.0F, 1.0F, alpha);
        }
    }

    public Vector4f applyEquationAndGetColor(float alpha) {
        return this.colorAndEquationFunc.apply(alpha);
    }

    public @Nullable BlendFunction getBlendFunction() {
        return this.blendFunction;
    }

    public boolean isSeparateFunction() {
        return this.separateFunction;
    }

    public int getSourceFactor() {
        return this.sourceFactor;
    }

    public int getDestinationFactor() {
        return this.destinationFactor;
    }

    public int getEquation() {
        return this.equation;
    }

    public int getSourceFactorAlpha() {
        return this.sourceFactorAlpha;
    }

    public int getDestinationFactorAlpha() {
        return this.destinationFactorAlpha;
    }

    public boolean isRedAlphaEnabled() {
        return this.redAlphaEnabled;
    }

    public boolean isGreenAlphaEnabled() {
        return this.greenAlphaEnabled;
    }

    public boolean isBlueAlphaEnabled() {
        return this.blueAlphaEnabled;
    }

    public boolean isAlphaEnabled() {
        return this.alphaEnabled;
    }

    public boolean isValidFactor(int factor) {
        return Arrays.stream(SourceFactor.values()).filter(factor1 -> factor == GlConst.toGl(factor1)).count() == 1;
    }

    public boolean isValidEquation(int equation) {
        return Arrays.stream(Equation.values()).filter(equation1 -> equation == equation1.value).count() == 1;
    }

    public static Blender normal() {
        return new Blender(
                false,
                GL46C.GL_SRC_ALPHA,
                GL46C.GL_ONE,
                Equation.ADD.value,
                GL46C.GL_ZERO,
                GL46C.GL_ZERO,
                false,
                false,
                false,
                true);
    }

    public static Blender decorations() {
        return new Blender(
                true,
                GL46C.GL_SRC_ALPHA,
                GL46C.GL_ONE,
                Equation.ADD.value,
                GL46C.GL_ONE,
                GL46C.GL_ZERO,
                false,
                false,
                false,
                true);
    }

    public enum Equation {
        ADD(GL46C.GL_FUNC_ADD),
        SUBTRACT(GL46C.GL_FUNC_SUBTRACT),
        REVERSE_SUBTRACT(GL46C.GL_FUNC_REVERSE_SUBTRACT),
        MIN(GL46C.GL_MIN),
        MAX(GL46C.GL_MAX);

        public final int value;

        Equation(int value) {
            this.value = value;
        }
    }
}
