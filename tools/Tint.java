import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Recolours block textures to the Enigma violet and writes a before/after sheet.
 *
 * <p>Different problem from the wrench, different method. The wrench's texture was a palette of warm
 * swatches, so rotating hue worked. These are real shaded textures, and neither of the two obvious
 * transforms reaches violet from all of them:
 *
 * <ul>
 *   <li><b>Hue rotation</b> does nothing to andesite casing, which is grey and has no saturation to
 *       rotate.</li>
 *   <li><b>Multiplying</b> by a violet works on grey, but not on the dark log: brown is red-dominant,
 *       so scaling green and blue down leaves red dominant and the wood comes out <em>maroon</em>.
 *       Measured, not guessed - the first attempt did exactly that.</li>
 * </ul>
 *
 * <p>So the colour is dropped entirely and rebuilt from brightness: each pixel's luminance is mapped
 * onto a dark-violet to light-violet ramp. Every material lands on the same hue family - which is the
 * point, since the aim is "clearly a creative item" rather than "surviving wood and surviving steel" -
 * while all the original shading, edges and highlights are preserved, because luminance is untouched.
 */
public final class Tint {

    /** Darkest and lightest ends of the violet ramp. */
    private static final int[] DARK = {0x15, 0x0a, 0x20};
    private static final int[] LIGHT = {0xb4, 0x8a, 0xd6};

    public static void main(String[] args) throws Exception {
        String outDir = args[0];
        String[] names = {"axis", "andesite_casing_short", "smooth_dark_log_top"};
        String[] labels = {"axis", "casing", "log_top"};

        BufferedImage[] originals = new BufferedImage[names.length];
        BufferedImage[] tinted = new BufferedImage[names.length];

        for (int i = 0; i < names.length; i++) {
            originals[i] = ImageIO.read(new File(args[1 + i]));
            tinted[i] = tint(originals[i]);
            ImageIO.write(tinted[i], "png", new File(outDir + "/" + names[i] + ".png"));
            System.out.println(names[i] + ": " + originals[i].getWidth() + "x" + originals[i].getHeight());
        }

        writeSheet(labels, originals, tinted, new File(outDir + "/tint_compare.png"));
        System.out.println("wrote tinted textures and tint_compare.png to " + outDir);
    }

    private static BufferedImage tint(BufferedImage in) {
        int w = in.getWidth();
        int h = in.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = in.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Rec. 601 luma: how bright the pixel is, which is the only thing kept.
                float l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
                int nr = lerp(DARK[0], LIGHT[0], l);
                int ng = lerp(DARK[1], LIGHT[1], l);
                int nb = lerp(DARK[2], LIGHT[2], l);
                out.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return out;
    }

    private static int lerp(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    private static void writeSheet(String[] names, BufferedImage[] before, BufferedImage[] after, File target)
            throws Exception {
        int scale = 6;
        int pad = 14;
        int labelH = 24;
        int rowH = before[0].getHeight() * scale;

        int cellW = 0;
        for (BufferedImage image : before) {
            cellW = Math.max(cellW, image.getWidth() * scale);
        }
        // one column per texture, two rows: original, tinted
        int sheetW = names.length * cellW + (names.length + 1) * pad;
        int sheetH = rowH * 2 + labelH * 2 + pad * 2;

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, sheetW, sheetH);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));

        g.setColor(Color.WHITE);
        g.drawString("original", pad, pad + 14);
        g.drawString("violet tint", pad, pad + labelH + rowH + 14);

        for (int i = 0; i < names.length; i++) {
            int ox = pad + i * (cellW + pad);
            blit(g, before[i], ox, pad + labelH, scale);
            blit(g, after[i], ox, pad + labelH + rowH + labelH, scale);
            g.setColor(Color.WHITE);
            g.drawString(names[i], ox, pad + 14);
        }

        g.dispose();
        ImageIO.write(sheet, "png", target);
    }

    private static void blit(Graphics2D g, BufferedImage image, int ox, int oy, int scale) {
        for (int y = 0; y < image.getHeight() * scale; y++) {
            for (int x = 0; x < image.getWidth() * scale; x++) {
                int argb = image.getRGB(x / scale, y / scale);
                if (((argb >>> 24) & 0xFF) == 0) {
                    continue;
                }
                g.setColor(new Color(argb, true));
                g.fillRect(ox + x, oy + y, 1, 1);
            }
        }
    }
}
