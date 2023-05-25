package xfacthd.oretexgen.client.generator;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import xfacthd.oretexgen.OreTextureGenerator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class OreTextureMetaProvider implements DataProvider
{
    protected static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(
            PackType.CLIENT_RESOURCES, ".png", "textures"
    );

    private final PackOutput output;
    private final String modid;
    private final ExistingFileHelper fileHelper;
    private final HashMap<ResourceLocation, MetaBuilder> builders = new HashMap<>();

    public OreTextureMetaProvider(PackOutput output, String modid, ExistingFileHelper fileHelper)
    {
        this.output = output;
        this.modid = modid;
        this.fileHelper = fileHelper;
    }

    protected abstract void registerMetadata();

    protected final MetaBuilder meta(ResourceLocation texture)
    {
        assertTextureExists(texture);
        texture = texture.withPrefix("texture/").withSuffix(".png.mcmeta");
        return builders.computeIfAbsent(texture, $ ->
        {
            MetaBuilder meta = new MetaBuilder(this);
            meta.section("forge").addEntry("loader", OreTextureGenerator.MODID + ":loader");
            return meta;
        });
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache)
    {
        builders.clear();
        registerMetadata();

        CompletableFuture<?>[] futures = new CompletableFuture<?>[builders.size()];
        int i = 0;
        for (Map.Entry<ResourceLocation, MetaBuilder> entry : builders.entrySet())
        {
            futures[i++] = saveMetadata(cache, output, entry.getKey(), entry.getValue());
        }
        return CompletableFuture.allOf(futures);
    }

    private void assertTextureExists(ResourceLocation texture)
    {
        Preconditions.checkState(
                fileHelper.exists(texture, TEXTURE),
                "Texture %s does not exist in any known resource pack", texture
        );
    }

    private static CompletableFuture<?> saveMetadata(CachedOutput cache, PackOutput output, ResourceLocation texture, MetaBuilder meta)
    {
        Path outputPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(texture.getNamespace())
                .resolve(texture.getPath());
        return DataProvider.saveStable(cache, meta.toJson(), outputPath);
    }

    @Override
    public final String getName()
    {
        return "OreTextureMeta: " + modid;
    }



    public static final class MetaBuilder
    {
        private final OreTextureMetaProvider parent;
        private final Map<String, SectionBuilder> sections = new HashMap<>();

        private MetaBuilder(OreTextureMetaProvider parent)
        {
            this.parent = parent;
        }

        public SimpleMetaSectionBuilder section(String sectionName)
        {
            return (SimpleMetaSectionBuilder) sections.computeIfAbsent(
                    sectionName, $ -> new SimpleMetaSectionBuilder()
            );
        }

        public OreTextureMetaSectionBuilder oreTextureSection()
        {
            return (OreTextureMetaSectionBuilder) sections.computeIfAbsent(
                    OreTextureGenerator.MODID, $ -> new OreTextureMetaSectionBuilder(parent)
            );
        }

        private JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            sections.forEach((name, section) -> json.add(name, section.toJson()));
            return json;
        }
    }

    public static abstract sealed class SectionBuilder permits OreTextureMetaSectionBuilder, SimpleMetaSectionBuilder
    {
        protected abstract JsonObject toJson();
    }

    public static final class OreTextureMetaSectionBuilder extends SectionBuilder
    {
        private ResourceLocation background = null;
        private Boolean genShadow = null;

        private final OreTextureMetaProvider parent;

        private OreTextureMetaSectionBuilder(OreTextureMetaProvider parent)
        {
            this.parent = parent;
        }

        public OreTextureMetaSectionBuilder background(ResourceLocation background)
        {
            parent.assertTextureExists(background);
            this.background = background;
            return this;
        }

        public OreTextureMetaSectionBuilder generateShadow(boolean genShadow)
        {
            this.genShadow = genShadow;
            return this;
        }

        @Override
        protected JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            if (background != null)
            {
                json.addProperty("background", background.toString());
            }
            if (genShadow != null)
            {
                json.addProperty("auto_shadow", genShadow);
            }
            return json;
        }
    }

    public static final class SimpleMetaSectionBuilder extends SectionBuilder
    {
        private final Map<String, Object> entries = new HashMap<>();

        public SimpleMetaSectionBuilder addEntry(String key, Object value)
        {
            entries.put(key, value);
            return this;
        }

        @Override
        protected JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            entries.forEach((key, value) ->
            {
                if (value instanceof Number num)
                {
                    json.addProperty(key, num);
                }
                else if (value instanceof String str)
                {
                    json.addProperty(key, str);
                }
                else if (value instanceof Boolean bool)
                {
                    json.addProperty(key, bool);
                }
                else if (value instanceof Character c)
                {
                    json.addProperty(key, c);
                }
                else if (value instanceof JsonElement elem)
                {
                    json.add(key, elem);
                }
                else
                {
                    throw new IllegalStateException(
                            "Entry '" + key + "' has unsupported type '" + value.getClass() + "'"
                    );
                }
            });
            return json;
        }
    }
}
