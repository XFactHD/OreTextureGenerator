package xfacthd.oretexgen.client.shadow;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.util.FastColor;
import xfacthd.oretexgen.client.loader.ShadowMetadata;
import xfacthd.oretexgen.client.util.FrameInfo;

public final class ShadowGenerator
{
    private static final int[] LOW_X = new int[] {1,0};
    private static final int[] LOW_Y = new int[] {0,1};
    private static final int[] HIGH_X = new int[] {-1,0};
    private static final int[] HIGH_Y = new int[] {0,-1};

    private static boolean safeCheckPixel(NativeImage image, int x, int y, FrameInfo frame, FrameSize size)
    {
        if (x < 0 || x >= size.width() || y < 0 || y >= size.height())
        {
            return false;
        }

        int px = frame.x() + x;
        int py = frame.y() + y;

        return FastColor.ARGB32.alpha(image.getPixelRGBA(px, py)) >= 128;
    }

    public static void generateShadow(NativeImage outputImage, NativeImage foreground, NativeImage background, FrameInfo frame, FrameSize size, ShadowMetadata shadowMetadata)
    {
        Palette palette = new Palette(background, shadowMetadata.paletteExpansion());

        int w = size.width();
        int h = size.height();
        for (int y = 0; y < h; y++)
        {
            int py = frame.y() + y;
            for (int x = 0; x < w; x++)
            {
                int px = frame.x() + x;

                boolean high = false;
                boolean low = false;

                for (int i = 0; i < LOW_X.length; i++)
                {
                    if (safeCheckPixel(foreground, x + LOW_X[i], y + LOW_Y[i], frame, size))
                    {
                        low = true;
                        break;
                    }
                }
                for (int i = 0; i < HIGH_X.length; i++)
                {
                    if (safeCheckPixel(foreground, x + HIGH_X[i], y + HIGH_Y[i], frame, size))
                    {
                        high = true;
                        break;
                    }
                }
                if (high && low)
                {
                    high = false;
                    low = false;
                }

                if (high || low)
                {
                    int oldColor = background.getPixelRGBA(px, py);
                    int index = palette.getIndex(oldColor);
                    index = (int) ((index + palette.average * shadowMetadata.uniformity()) / (1 + shadowMetadata.uniformity()));

                    if (high)
                    {
                        index += shadowMetadata.highlightStrength();
                    }
                    else
                    {
                        index -= shadowMetadata.shadowStrength();
                    }

                    int newColor = palette.getColor(index) | (oldColor & 0xFF000000);
                    outputImage.setPixelRGBA(px, py, newColor);
                }
            }
        }
    }
}
