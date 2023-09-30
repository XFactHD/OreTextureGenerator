package xfacthd.oretexgen.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.util.FastColor;

import java.util.*;

public final class Utils
{
    public static boolean checkAspectRatio(int bgWidth, int bgHeight, int fgWidth, int fgHeight)
    {
        int widthFactor = Math.max(bgWidth, fgWidth) / Math.min(bgWidth, fgWidth);
        int heightFactor = Math.max(bgHeight, fgHeight) / Math.min(bgHeight, fgHeight);
        return widthFactor == heightFactor;
    }

    public static NativeImage scaleImage(NativeImage source, int scale)
    {
        if (scale <= 1)
        {
            return source;
        }

        NativeImage scaled = new NativeImage(source.format(), source.getWidth() * scale, source.getHeight() * scale, false);
        scaled.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), source);
        source.close();
        return scaled;
    }

    public static List<FrameInfo> collectFrames(NativeImage image, FrameSize size, AnimationMetadataSection animation)
    {
        List<FrameInfo> frames = new ArrayList<>();
        int rowCount = image.getWidth() / size.width();
        animation.forEachFrame((idx, time) ->
        {
            int frameX = (idx % rowCount) * size.width();
            int frameY = (idx / rowCount) * size.height();
            frames.add(new FrameInfo(idx, frameX, frameY));
        });
        if (frames.isEmpty())
        {
            int frameCount = rowCount * (image.getHeight() / size.height());
            for (int idx = 0; idx < frameCount; idx++)
            {
                int frameX = (idx % rowCount) * size.width();
                int frameY = (idx / rowCount) * size.height();
                frames.add(new FrameInfo(idx, frameX, frameY));
            }
        }
        return frames;
    }

    public static void copyRect(NativeImage src, NativeImage dest, int srcX, int srcY, int destX, int destY, int width, int height)
    {
        for (int y = 0; y < width; y++)
        {
            for (int x = 0; x < height; x++)
            {
                int rgba = src.getPixelRGBA(srcX + x, srcY + y);
                int alpha = FastColor.ARGB32.alpha(rgba);
                if (alpha == 255)
                {
                    dest.setPixelRGBA(destX + x, destY + y, rgba);
                }
                else if (alpha > 0)
                {
                    dest.blendPixel(destX + x, destY + y, rgba);
                }
            }
        }
    }

    public static <T> MapCodec<T> optionalFieldCodecOf(Codec<T> elementCodec, String key, T defaultValue)
    {
        return optionalFieldCodecOf(elementCodec, key).xmap(
                opt -> opt.orElse(defaultValue),
                val -> Objects.equals(val, defaultValue) ? Optional.empty() : Optional.of(val)
        );
    }

    public static <T> MapCodec<Optional<T>> optionalFieldCodecOf(Codec<T> elementCodec, String key)
    {
        return new BetterOptionalFieldCodec<>(key, elementCodec);
    }



    private Utils() { }
}
