package me.flashyreese.mods.nuit.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;

public final class DynamicTransformsBuilder {
    private Optional<Matrix4f> modelViewMatrix = Optional.empty();
    private Optional<Vector4f> colorModulator = Optional.empty();
    private Optional<Vector3f> modelOffset = Optional.empty();
    private Optional<Matrix4f> textureMatrix = Optional.empty();
    private Optional<Float> lineWidth = Optional.empty();

    public static DynamicTransformsBuilder of() {
        return new DynamicTransformsBuilder();
    }

    public DynamicTransformsBuilder withModelViewMatrix(Matrix4f matrix4f) {
        this.modelViewMatrix = Optional.of(matrix4f);
        return this;
    }

    public DynamicTransformsBuilder withShaderColor(Vector4f vector4f) {
        this.colorModulator = Optional.of(vector4f);
        return this;
    }

    public DynamicTransformsBuilder withShaderColor(Vector3f vector3f) {
        return this.withShaderColor(new Vector4f(vector3f, 1.0F));
    }

    public DynamicTransformsBuilder withShaderColor(float red, float green, float blue, float alpha) {
        return this.withShaderColor(new Vector4f(red, green, blue, alpha));
    }

    public DynamicTransformsBuilder withShaderColor(float red, float green, float blue) {
        return this.withShaderColor(new Vector3f(red, green, blue));
    }

    public DynamicTransformsBuilder withModelOffset(Vector3f vector3f) {
        this.modelOffset = Optional.of(vector3f);
        return this;
    }

    public DynamicTransformsBuilder withTextureMatrix(Matrix4f matrix4f) {
        this.textureMatrix = Optional.of(matrix4f);
        return this;
    }

    public DynamicTransformsBuilder withLineWidth(float lineWidth) {
        this.lineWidth = Optional.of(lineWidth);
        return this;
    }

    public com.mojang.blaze3d.buffers.GpuBufferSlice build() {
        return RenderSystem.getDynamicUniforms().writeTransform(this.modelViewMatrix.orElse(RenderSystem.getModelViewMatrix()), this.colorModulator.orElse(new Vector4f(1.0F, 1.0F, 1.0F, 1.0F)), this.modelOffset.orElse(new Vector3f()), this.textureMatrix.orElse(new Matrix4f()));
    }
}