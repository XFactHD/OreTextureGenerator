package xfacthd.oretexgen.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.util.FastColor;
import xfacthd.oretexgen.client.loader.OreMetadata;

import java.util.*;

public class ShadowGenerator
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

    public static void generateShadow(NativeImage outputImage, NativeImage foreground, NativeImage background, FrameInfo frame, FrameSize size, OreMetadata.ShadowMetadata shadowMetadata)
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

    public static class Palette
    {
        private final List<Integer> colors;
        public final int average;

        public Palette(NativeImage image, int paletteExpansion)
        {
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++)
            {
                for (int x = 0; x < image.getWidth(); x++)
                {
                    int color = image.getPixelRGBA(x, y);
                    if (FastColor.ARGB32.alpha(color) == 255)
                    {
                        colors.add(color & 0x00FFFFFF);
                    }
                }
            }

            this.colors = new ArrayList<>(colors.stream().sorted(Comparator.comparing(ShadowGenerator::value)).toList());
            float averageF = this.colors.size() / 2f;

            // Expand the palette if it is two narrow (looking at you, stone)
            int paletteSize = value(this.colors.get(this.colors.size()-1)) - value(this.colors.get(0));
            if (paletteSize < paletteExpansion)
            {
                int count = 0;
                int cap = 2 * this.colors.size() / 5;
                boolean below = true;
                while (paletteSize < 150)
                {
                    count += 1;
                    // Increase the size by at most 2/5
                    if (count > cap)
                    {
                        break;
                    }

                    int oldColor = this.colors.get(below ? 0 : this.colors.size()-1);
                    int r = FastColor.ARGB32.red(oldColor);
                    int g = FastColor.ARGB32.green(oldColor);
                    int b = FastColor.ARGB32.blue(oldColor);
                    int sw = value(oldColor) / 3;
                    if (below)
                    {
                        sw -= paletteSize / (3 * 2 * cap);
                    }
                    else
                    {
                        sw += paletteSize / (3 * 2 * cap);
                    }
                    sw = Math.min(255, Math.max(0, sw));

                    r = (r*2 + sw) / 3;
                    g = (g*2 + sw) / 3;
                    b = (b*2 + sw) / 3;

                    int newColor = FastColor.ARGB32.color(0, r, g, b);

                    if (below)
                    {
                        this.colors.add(0, newColor);
                        averageF += 1;
                    }
                    else
                    {
                        this.colors.add(newColor);
                    }

                    paletteSize = value(this.colors.get(this.colors.size()-1)) - value(this.colors.get(0));
                    below = !below;
                }
            }

            this.average = (int) (averageF / this.colors.size() * 255);

        }

        /**
         * Takes a 0-255 index and returns the corresponding ARGB32 palette color.
         */
        public int getColor(int index)
        {
            if (index < 0)
            {
                return colors.get(0);
            }
            else if (index > 255)
            {
                return colors.get(colors.size()-1);
            }
            index = index * colors.size() / 255;
            return colors.get(index);
        }

        /**
         * Takes an ARGB32 palette color and returns the corresponding 0-255 index, or 0 if the color is not in the
         * palette.
         */
        public int getIndex(int color)
        {
            color = color & 0x00FFFFFF;

            int index = colors.indexOf(color);
            if (index == -1)
            {
                index = 0;
            }

            return index * 255 / colors.size();
        }
    }

    private static int value(int color)
    {
        return (color & 0xFF) + (color >> 8 & 0xFF) + (color >> 16 & 0xFF);
    }
}
