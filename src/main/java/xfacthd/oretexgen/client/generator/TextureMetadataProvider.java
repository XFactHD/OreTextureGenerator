package xfacthd.oretexgen.client.generator;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.minecraftforge.client.textures.TextureAtlasSpriteLoaderManager;
import net.minecraftforge.common.data.ExistingFileHelper;
import xfacthd.oretexgen.client.loader.OreTextureLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class TextureMetadataProvider implements DataProvider
{
    protected static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(
            PackType.CLIENT_RESOURCES, ".png", "textures"
    );

    private final PackOutput output;
    private final String modid;
    private final ExistingFileHelper fileHelper;
    private final HashMap<ResourceLocation, MetaBuilder> builders = new HashMap<>();

    public TextureMetadataProvider(PackOutput output, String modid, ExistingFileHelper fileHelper)
    {
        this.output = output;
        this.modid = modid;
        this.fileHelper = fileHelper;
    }

    protected abstract void registerMetadata();

    /**
     * {@return a new {@link MetaBuilder} for the given texture}
     */
    protected final MetaBuilder meta(ResourceLocation texture)
    {
        assertTextureExists(texture);
        return builders.computeIfAbsent(
                texture.withPrefix("textures/").withSuffix(".png.mcmeta"),
                $ -> new MetaBuilder()
        );
    }

    /**
     * {@return a new {@link MetaBuilder} for the given texture with the custom sprite loader metadata already attached}
     */
    protected final MetaBuilder oreMeta(ResourceLocation texture)
    {
        return meta(texture).section(
                ForgeTextureMetadataType.TYPE,
                new ForgeTextureMetadata(TextureAtlasSpriteLoaderManager.get(OreTextureLoader.NAME))
        );
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
        private final Map<String, Supplier<JsonElement>> sections = new HashMap<>();

        private MetaBuilder() { }

        public <T> MetaBuilder section(MetadataSectionType<T> type, T value)
        {
            sections.put(type.getMetadataSectionName(), () -> type.toJson(value));
            return this;
        }

        private JsonObject toJson()
        {
            JsonObject json = new JsonObject();
            sections.forEach((name, section) -> json.add(name, section.get()));
            return json;
        }
    }
}
