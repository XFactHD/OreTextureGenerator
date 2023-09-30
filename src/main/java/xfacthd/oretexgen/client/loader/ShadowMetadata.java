package xfacthd.oretexgen.client.loader;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import xfacthd.oretexgen.client.util.Utils;

/**
 * Represents shadow generation settings for an ore texture. {@code null} represents no shadow generation.
 *
 * @param paletteExpansion  background image palettes with a color distance of less than this value will be expanded
 * @param highlightStrength the strength of the highlight color
 * @param shadowStrength    the strength of the shadow color
 * @param uniformity        how uniform shaded areas are made before lightening and darkening
 */
public record ShadowMetadata(int paletteExpansion, int highlightStrength, int shadowStrength, float uniformity)
{
    public static final ShadowMetadata DEFAULT = new ShadowMetadata(125, 72, 72, 1F);
    public static final Codec<ShadowMetadata> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Utils.optionalFieldCodecOf(Codec.intRange(0, 255), "palette_expansion", DEFAULT.paletteExpansion)
                    .forGetter(ShadowMetadata::paletteExpansion),
            Utils.optionalFieldCodecOf(Codec.intRange(0, 255), "highlight_strength", DEFAULT.highlightStrength)
                    .forGetter(ShadowMetadata::highlightStrength),
            Utils.optionalFieldCodecOf(Codec.intRange(0, 255), "shadow_strength", DEFAULT.shadowStrength)
                    .forGetter(ShadowMetadata::shadowStrength),
            Utils.optionalFieldCodecOf(Codec.floatRange(0F, 1F), "uniformity", DEFAULT.uniformity)
                    .forGetter(ShadowMetadata::uniformity)
    ).apply(inst, ShadowMetadata::new));
}
