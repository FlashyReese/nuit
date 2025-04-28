package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.util.CodecUtils;

public class Fog extends RGB {
    public static final Codec<Fog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("red", 1.0F).forGetter(Fog::getRed),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("green", 1.0F).forGetter(Fog::getGreen),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("blue", 1.0F).forGetter(Fog::getBlue),
            Codec.BOOL.optionalFieldOf("modifyColors", false).forGetter(Fog::isModifyColors),
            Codec.BOOL.optionalFieldOf("modifyDensity", false).forGetter(Fog::isModifyDensity),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("density", 1.0F).forGetter(Fog::getDensity),
            Codec.BOOL.optionalFieldOf("showInDenseFog", true).forGetter(Fog::isShowInDenseFog)
    ).apply(instance, Fog::new));

    private final boolean modifyColors;
    private final boolean modifyDensity;
    private final float density;
    private final boolean showInDenseFog;

    public Fog(float red, float green, float blue, boolean modifyColors, boolean modifyDensity, float density, boolean renderInFoggy) {
        super(red, green, blue);
        this.modifyColors = modifyColors;
        this.modifyDensity = modifyDensity;
        this.density = density;
        this.showInDenseFog = renderInFoggy;
    }

    public static Fog of() {
        return new Fog(0F, 0F, 0F, false, false, 0F, true);
    }

    public boolean isModifyColors() {
        return this.modifyColors;
    }

    public boolean isModifyDensity() {
        return this.modifyDensity;
    }

    public float getDensity() {
        return this.density;
    }

    public boolean isShowInDenseFog() {
        return this.showInDenseFog;
    }
}
