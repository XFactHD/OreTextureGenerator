package xfacthd.oretexgen.client.loader;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import xfacthd.oretexgen.OreTextureGenerator;
import xfacthd.oretexgen.client.shadow.ShadowGenerator;
import xfacthd.oretexgen.client.util.Utils;

import java.util.Optional;

public record OreTextureSource(ResourceLocation ore, ResourceLocation background, ShadowMetadata shadow) implements SpriteSource
{
    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation("minecraft:block/stone");
    private static SpriteSourceType TYPE = null;
    private static final Codec<OreTextureSource> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("ore").forGetter(s -> s.ore),
            Utils.optionalFieldCodecOf(ResourceLocation.CODEC, "background", DEFAULT_BACKGROUND).forGetter(s -> s.background),
            Utils.optionalFieldCodecOf(
                    new ExtraCodecs.EitherCodec<>(Codec.BOOL, ShadowMetadata.CODEC),
                    "shadow"
            ).xmap(
                    oe -> oe.flatMap(e -> e.left().map(b -> b ? ShadowMetadata.DEFAULT : null).or(e::right)),
                    os -> os.map(s -> s.equals(ShadowMetadata.DEFAULT) ? Either.left(true) : Either.right(s))
            ).forGetter(s -> Optional.ofNullable(s.shadow))
    ).apply(inst, (ore, bg, shadow) -> new OreTextureSource(ore, bg, shadow.orElse(null))));

    @Override
    public void run(ResourceManager manager, Output output)
    {
        ResourceLocation orePath = TEXTURE_ID_CONVERTER.idToFile(ore);
        Optional<Resource> optOre = manager.getResource(orePath);
        if (optOre.isEmpty())
        {
            OreTextureGenerator.LOGGER.warn("Missing ore sprite: {}", orePath);
            return;
        }

        ResourceLocation bgPath = TEXTURE_ID_CONVERTER.idToFile(background);
        Optional<Resource> optBg = manager.getResource(bgPath);
        if (optBg.isEmpty())
        {
            OreTextureGenerator.LOGGER.warn("Missing background sprite: {}", orePath);
            return;
        }

        Resource oreResource = optOre.get();
        Resource bgResource = optBg.get();
        LazyLoadedImage lazyOre = new LazyLoadedImage(orePath, oreResource, 1);
        LazyLoadedImage lazyBg = new LazyLoadedImage(bgPath, bgResource, 1);
        output.add(ore, new OreTextureSupplier(oreResource, lazyOre, bgPath, bgResource, lazyBg, shadow, ore));
    }

    @Override
    public SpriteSourceType type()
    {
        return Preconditions.checkNotNull(TYPE, "SpriteSourceType not registered");
    }



    private record OreTextureSupplier(
            Resource oreResource,
            LazyLoadedImage lazyOre,
            ResourceLocation backgroundPath,
            Resource backgroundResource,
            LazyLoadedImage lazyBackground,
            ShadowMetadata shadow,
            ResourceLocation outLoc
    ) implements SpriteSupplier
    {
        @Override
        public SpriteContents get()
        {
            try
            {
                AnimationMetadataSection bgAnim = backgroundResource.metadata()
                        .getSection(AnimationMetadataSection.SERIALIZER)
                        .orElse(AnimationMetadataSection.EMPTY);
                if (bgAnim != AnimationMetadataSection.EMPTY)
                {
                    throw new IllegalArgumentException(
                            "Ore background texture must not be animated but '" + backgroundPath + "' specifies an animation"
                    );
                }

                NativeImage background = lazyBackground.get();
                NativeImage image = lazyOre.get();

                AnimationMetadataSection oreAnim = oreResource.metadata()
                        .getSection(AnimationMetadataSection.SERIALIZER)
                        .orElse(AnimationMetadataSection.EMPTY);
                FrameSize size = oreAnim.calculateFrameSize(image.getWidth(), image.getHeight());

                int bgWidth = background.getWidth();
                int bgHeight = background.getHeight();
                int fgWidth = size.width();
                int fgHeight = size.height();
                if (!Utils.checkAspectRatio(bgWidth, bgHeight, fgWidth, fgHeight))
                {
                    throw new IllegalArgumentException(
                            "Aspect ratio of ore and background texture does not match for texture '" + outLoc + "'"
                    );
                }

                FrameSize resultSize = new FrameSize(Math.max(bgWidth, fgWidth), Math.max(bgHeight, fgHeight));
                int bgScale = fgWidth > bgWidth ? (fgWidth / bgWidth) : 1;
                int fgScale = bgWidth > fgWidth ? (bgWidth / fgWidth) : 1;
                background = Utils.scaleImage(background, bgScale);
                image = Utils.scaleImage(image, fgScale);
                return buildCombinedTexture(outLoc, resultSize, image, background, shadow, oreAnim);
            }
            catch (Exception e)
            {
                OreTextureGenerator.LOGGER.error("Failed to generate ore texture '{}'", outLoc, e);
            }
            finally
            {
                lazyOre.release();
                lazyBackground.release();
            }
            return MissingTextureAtlasSprite.create();
        }

        @Override
        public void discard()
        {
            lazyOre.release();
            lazyBackground.release();
        }
    }

    private static SpriteContents buildCombinedTexture(
            ResourceLocation name,
            FrameSize resultSize,
            NativeImage image,
            NativeImage background,
            @Nullable ShadowMetadata shadowMetadata,
            AnimationMetadataSection animation
    )
    {
        NativeImage resultImage = new NativeImage(image.format(), image.getWidth(), image.getHeight(), false);
        Utils.collectFrames(image, resultSize, animation).forEach(frame ->
        {
            int fx = frame.x();
            int fy = frame.y();

            background.copyRect(resultImage, 0, 0, fx, fy, resultSize.width(), resultSize.height(), false, false);

            if (shadowMetadata != null)
            {
                ShadowGenerator.generateShadow(resultImage, image, background, frame, resultSize, shadowMetadata);
            }

            Utils.copyRect(image, resultImage, fx, fy, fx, fy, resultSize.width(), resultSize.height());
        });

        background.close();
        image.close();

        return new SpriteContents(name, resultSize, resultImage, animation, null);
    }



    // TODO: replace with dedicated event when switching to Neo and the event is merged
    public static void register()
    {
        String name = OreTextureGenerator.MODID + ":ore_generator";
        TYPE = SpriteSources.register(name, CODEC);
    }
}
