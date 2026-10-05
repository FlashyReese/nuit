package me.flashyreese.mods.nuit.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.util.CodecUtils;

import java.util.Optional;

public class Fog extends RGB {
    public static final Codec<Fog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("red", 1.0F).forGetter(Fog::getRed),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("green", 1.0F).forGetter(Fog::getGreen),
            CodecUtils.getClampedFloat(0.0F, 1.0F).optionalFieldOf("blue", 1.0F).forGetter(Fog::getBlue),
            Codec.BOOL.optionalFieldOf("modifyColors", false).forGetter(Fog::isModifyColors),
            Codec.BOOL.optionalFieldOf("showInDenseFog", true).forGetter(Fog::isShowInDenseFog),
            // Removed in 26.4: accepted (with any value) so existing packs still load, but never written back.
            Codec.PASSTHROUGH.optionalFieldOf("modifyDensity").forGetter(fog -> Optional.empty()),
            Codec.PASSTHROUGH.optionalFieldOf("density").forGetter(fog -> Optional.empty())
    ).apply(instance, Fog::decode));

    private static boolean warnedAboutDensity;

    private final boolean modifyColors;
    private final boolean showInDenseFog;

    public Fog(float red, float green, float blue, boolean modifyColors, boolean showInDenseFog) {
        super(red, green, blue);
        this.modifyColors = modifyColors;
        this.showInDenseFog = showInDenseFog;
    }

    /**
     * @deprecated fog density is no longer supported; {@code modifyDensity} and {@code density} are ignored.
     */
    @Deprecated(forRemoval = true)
    public Fog(float red, float green, float blue, boolean modifyColors, boolean modifyDensity, float density, boolean showInDenseFog) {
        this(red, green, blue, modifyColors, showInDenseFog);
    }

    private static Fog decode(float red, float green, float blue, boolean modifyColors, boolean showInDenseFog,
                              Optional<Dynamic<?>> modifyDensity, Optional<Dynamic<?>> density) {
        if ((modifyDensity.isPresent() || density.isPresent()) && !warnedAboutDensity) {
            warnedAboutDensity = true;
            NuitClient.getLogger().warn("Skybox fog 'density' and 'modifyDensity' are no longer supported and will be ignored: "
                    + "since Minecraft 26.4, terrain fog no longer uses the fog color's alpha.");
        }

        return new Fog(red, green, blue, modifyColors, showInDenseFog);
    }

    public static Fog of() {
        return new Fog(0F, 0F, 0F, false, true);
    }

    public boolean isModifyColors() {
        return this.modifyColors;
    }

    public boolean isShowInDenseFog() {
        return this.showInDenseFog;
    }
}
