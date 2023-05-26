package xfacthd.oretexgen.client.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import xfacthd.oretexgen.OreTextureGenerator;

import java.io.IOException;

public final class OreMetadata
{
    private static final String DEFAULT_BACKGROUND = "minecraft:block/stone";
    private static final OreMetadata DEFAULT = new OreMetadata(DEFAULT_BACKGROUND, null);
    private static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation background;

    @Nullable
    private final ShadowMetadata shadowMetadata;

    private OreMetadata(String background, @Nullable ShadowMetadata shadowMetadata)
    {
        this.background = new ResourceLocation(background);
        this.shadowMetadata = shadowMetadata;
    }

    public ResourceLocation getBackground()
    {
        return background;
    }

    @Nullable
    public ShadowMetadata getShadowMetadata()
    {
        return shadowMetadata;
    }



    public static OreMetadata fromResource(Resource resource) throws IOException
    {
        return resource.metadata().getSection(SERIALIZER).orElse(DEFAULT);
    }

    /**
     * Represents shadow generation settings for an ore texture. {@code null} represents no shadow generation.
     * @param paletteExpansion background image palettes with a color distance of less than this value will be expanded
     * @param highlightStrength the strength of the highlight color
     * @param shadowStrength the strength of the shadow color
     * @param uniformity how uniform shaded areas are made before lightening and darkening
     */
    public record ShadowMetadata(int paletteExpansion, int highlightStrength, int shadowStrength, float uniformity)
    {
        public static ShadowMetadata fromJson(JsonObject json)
        {
            int paletteExpansion = 125;
            if (json.has("palette_expansion"))
            {
                paletteExpansion = json.get("palette_expansion").getAsInt();
            }
            int highlightStrength = 72;
            if (json.has("highlight_strength"))
            {
                highlightStrength = json.get("highlight_strength").getAsInt();
            }
            int shadowStrength = 72;
            if (json.has("shadow_strength"))
            {
                shadowStrength = json.get("shadow_strength").getAsInt();
            }
            float uniformity = 1.0f;
            if (json.has("uniformity"))
            {
                uniformity = json.get("uniformity").getAsFloat();
            }
            return new ShadowMetadata(paletteExpansion, highlightStrength, shadowStrength, uniformity);
        }
    }

    private static class Serializer implements MetadataSectionSerializer<OreMetadata>
    {
        @Override
        public String getMetadataSectionName()
        {
            return OreTextureGenerator.MODID;
        }

        @Override
        public OreMetadata fromJson(JsonObject json)
        {
            String background = DEFAULT_BACKGROUND;
            if (json.has("background"))
            {
                background = json.get("background").getAsString();
            }
            ShadowMetadata shadow = null;
            if (json.has("shadow"))
            {
                JsonElement shadowElement = json.get("shadow");
                if (shadowElement.isJsonObject())
                {
                    shadow = ShadowMetadata.fromJson(shadowElement.getAsJsonObject());
                }
                else if (shadowElement.getAsBoolean())
                {
                    shadow = ShadowMetadata.fromJson(new JsonObject());
                }
            }
            return new OreMetadata(background, shadow);
        }
    }
}
