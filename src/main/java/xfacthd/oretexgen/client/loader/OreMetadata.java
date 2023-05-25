package xfacthd.oretexgen.client.loader;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import xfacthd.oretexgen.OreTextureGenerator;

import java.io.IOException;

public final class OreMetadata
{
    private static final String DEFAULT_BACKGROUND = "minecraft:block/stone";
    private static final OreMetadata DEFAULT = new OreMetadata(DEFAULT_BACKGROUND, false);
    private static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation background;
    private final boolean generateShadow;

    private OreMetadata(String background, boolean generateShadow)
    {
        this.background = new ResourceLocation(background);
        this.generateShadow = generateShadow;
    }

    public ResourceLocation getBackground()
    {
        return background;
    }

    public boolean shouldGenerateShadow()
    {
        return generateShadow;
    }



    public static OreMetadata fromResource(Resource resource) throws IOException
    {
        return resource.metadata().getSection(SERIALIZER).orElse(DEFAULT);
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
            boolean generateShadow = false;
            if (json.has("auto_shadow"))
            {
                generateShadow = json.get("auto_shadow").getAsBoolean();
            }
            return new OreMetadata(background, generateShadow);
        }
    }
}
