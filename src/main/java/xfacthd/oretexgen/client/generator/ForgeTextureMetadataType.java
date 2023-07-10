package xfacthd.oretexgen.client.generator;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraftforge.client.textures.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.Map;

public final class ForgeTextureMetadataType implements MetadataSectionType<ForgeTextureMetadata>
{
    public static final ForgeTextureMetadataType TYPE = new ForgeTextureMetadataType();

    private ForgeTextureMetadataType() { }

    @Override
    public String getMetadataSectionName()
    {
        return ForgeTextureMetadata.SERIALIZER.getMetadataSectionName();
    }

    @Override
    public ForgeTextureMetadata fromJson(JsonObject json)
    {
        return ForgeTextureMetadata.SERIALIZER.fromJson(json);
    }

    @Override
    public JsonObject toJson(ForgeTextureMetadata data)
    {
        JsonObject json = new JsonObject();
        if (data.getLoader() != null)
        {
            Map<ResourceLocation, ITextureAtlasSpriteLoader> loaderMap = ObfuscationReflectionHelper.getPrivateValue(
                    TextureAtlasSpriteLoaderManager.class,
                    null,
                    "LOADERS"
            );

            ResourceLocation name = loaderMap.entrySet()
                    .stream()
                    .filter(e -> e.getValue() == data.getLoader())
                    .findFirst()
                    .orElseThrow()
                    .getKey();
            json.addProperty("loader", name.toString());
        }
        return json;
    }
}
