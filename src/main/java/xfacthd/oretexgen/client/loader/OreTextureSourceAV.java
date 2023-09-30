package xfacthd.oretexgen.client.loader;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import xfacthd.atlasviewer.client.api.*;

final class OreTextureSourceAV extends OreTextureSource implements IPackAwareSpriteSource
{
    private final SpriteSourceMeta meta = new SpriteSourceMeta();

    OreTextureSourceAV(ResourceLocation ore, ResourceLocation background, ShadowMetadata shadow)
    {
        super(ore, background, shadow);
    }

    @Override
    public SpriteSourceMeta atlasviewer$getMeta()
    {
        return meta;
    }

    @Override
    OreTextureSupplier createSupplier(Resource oreResource, LazyLoadedImage lazyOre, Resource bgResource, LazyLoadedImage lazyBg)
    {
        OreTextureSupplierAV supplier = new OreTextureSupplierAV(oreResource, lazyOre, background, bgResource, lazyBg, shadow, ore);
        supplier.atlasviewer$getMeta().readFromSpriteSourceMeta(this);
        return supplier;
    }



    static final class OreTextureSupplierAV extends OreTextureSupplier implements ISpriteSourcePackAwareSpriteSupplier
    {
        private final SpriteSupplierMeta meta = new SpriteSupplierMeta();

        OreTextureSupplierAV(
                Resource oreResource,
                LazyLoadedImage lazyOre,
                ResourceLocation backgroundPath,
                Resource backgroundResource,
                LazyLoadedImage lazyBackground,
                ShadowMetadata shadow,
                ResourceLocation outLoc
        )
        {
            super(oreResource, lazyOre, backgroundPath, backgroundResource, lazyBackground, shadow, outLoc);
        }

        @Override
        public SpriteSupplierMeta atlasviewer$getMeta()
        {
            return meta;
        }

        @Override
        SpriteContents postProcess(SpriteContents contents)
        {
            ((ISpriteSourcePackAwareSpriteContents) contents).atlasviewer$captureMetaFromSpriteSupplier(this, oreResource);
            return contents;
        }
    }
}
