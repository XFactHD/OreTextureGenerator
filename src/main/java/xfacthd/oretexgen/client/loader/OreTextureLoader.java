package xfacthd.oretexgen.client.loader;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.client.textures.ForgeTextureMetadata;
import net.minecraftforge.client.textures.ITextureAtlasSpriteLoader;
import org.jetbrains.annotations.Nullable;
import xfacthd.oretexgen.OreTextureGenerator;
import xfacthd.oretexgen.client.shadow.ShadowGenerator;
import xfacthd.oretexgen.client.util.Utils;

import java.io.IOException;
import java.io.InputStream;

public final class OreTextureLoader implements ITextureAtlasSpriteLoader
{
    @Override
    public SpriteContents loadContents(
            ResourceLocation name, Resource resource, FrameSize size, NativeImage image, AnimationMetadataSection animation, ForgeTextureMetadata forgeMeta
    )
    {
        OreMetadata meta;
        try
        {
            meta = OreMetadata.fromResource(resource);
        }
        catch (IOException e)
        {
            String msg = "Failed to load texture metadata for texture '" + name + "'";
            return fallback(name, size, image, animation, msg, e);
        }

        ResourceLocation bgName = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(meta.getBackground());
        Resource bgResource;
        try
        {
            bgResource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(bgName);
        }
        catch (IOException e)
        {
            String msg = "Failed to get ore background texture '" + bgName + "' for texture '" + name + "'";
            return fallback(name, size, image, animation, msg, e);
        }

        try
        {
            AnimationMetadataSection bgAnim = bgResource.metadata()
                    .getSection(AnimationMetadataSection.SERIALIZER)
                    .orElse(AnimationMetadataSection.EMPTY);
            if (bgAnim != AnimationMetadataSection.EMPTY)
            {
                String msg = "Ore background texture must not be animated but '" + bgName + "' specifies an animation";
                return fallback(name, size, image, animation, msg, null);
            }
        }
        catch (IOException e)
        {
            String msg = "Failed to check ore background texture '" + bgName + "' for an animation";
            return fallback(name, size, image, animation, msg, e);
        }

        NativeImage background;
        try (InputStream stream = bgResource.open())
        {
            background = NativeImage.read(stream);
        }
        catch (IOException e)
        {
            String msg = "Failed to load ore background texture '" + meta.getBackground() + "' for texture '" + name + "'";
            return fallback(name, size, image, animation, msg, null);
        }

        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        int fgWidth = size.width();
        int fgHeight = size.height();
        if (!Utils.checkAspectRatio(bgWidth, bgHeight, fgWidth, fgHeight))
        {
            String msg = "Aspect ratio of ore and background texture does not match for texture '" + name + "'";
            return fallback(name, size, image, animation, msg, null);
        }

        FrameSize resultSize = new FrameSize(Math.max(bgWidth, fgWidth), Math.max(bgHeight, fgHeight));
        int bgScale = fgWidth > bgWidth ? (fgWidth / bgWidth) : 1;
        int fgScale = bgWidth > fgWidth ? (bgWidth / fgWidth) : 1;
        background = Utils.scaleImage(background, bgScale);
        image = Utils.scaleImage(image, fgScale);

        return buildCombinedTexture(name, resultSize, image, background, meta.getShadowMetadata(), animation, forgeMeta);
    }

    private static SpriteContents buildCombinedTexture(
            ResourceLocation name, FrameSize resultSize, NativeImage image, NativeImage background, @Nullable OreMetadata.ShadowMetadata shadowMetadata, AnimationMetadataSection animation, ForgeTextureMetadata forgeMeta
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

        return new SpriteContents(name, resultSize, resultImage, animation, forgeMeta);
    }

    private static SpriteContents fallback(
            ResourceLocation name, FrameSize size, NativeImage image, AnimationMetadataSection animation, String message, Exception e
    )
    {
        OreTextureGenerator.LOGGER.error(message);
        if (e != null)
        {
            e.printStackTrace();
        }
        // Remove loader to fully fall back to vanilla
        return new SpriteContents(name, size, image, animation, null);
    }

    @Override
    public TextureAtlasSprite makeSprite(
            ResourceLocation name, SpriteContents contents, int atlasWidth, int atlasHeight, int spriteX, int spriteY, int mipmapLevel
    )
    {
        return new TextureAtlasSprite(name, contents, atlasWidth, atlasHeight, spriteX, spriteY) {};
    }
}
