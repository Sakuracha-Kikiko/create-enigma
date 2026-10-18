import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Recolours block textures to the Enigma violet and writes a before/after sheet.
 *
 * <p>Usage: {@code java Tint.java <outDir> <in1.png> [in2.png ...]} - each input is written to
 * {@code outDir} under its own file name, so callers name their own outputs and this cannot silently
 * apply one texture's naming to another.
 *
 * <h2>Why this is not the wrench's transform</h2>
 *
 * <p>The wrench's texture was a palette of warm swatches, so rotating hue worked. These are real
 * shaded textures, and neither of the two obvious transforms reaches violet from all of them:
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
 *
 * <p>Alpha is carried through unchanged, which matters: several of these textures are cut out.
 */
public final class Tint {

    /** Darkest and lightest ends of the violet ramp. */
    private static final int[] DARK = {0x15, 0x0a, 0x20};
    private static final int[] LIGHT = {0xb4, 0x8a, 0xd6};

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: Tint <outDir> <in1.png> [in2.png ...]");
            return;
        }

        File outDir = new File(args[0]);
        outDir.mkdirs();

        List<String> names = new ArrayList<>();
        List<BufferedImage> originals = new ArrayList<>();
        List<BufferedImage> tinted = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            File in = new File(args[i]);
            String name = in.getName().replaceAll("\\.png$", "");
            BufferedImage source = ImageIO.read(in);
            BufferedImage result = tint(source);

            ImageIO.write(result, "png", new File(outDir, name + ".png"));
            System.out.println(name + ": " + source.getWidth() + "x" + source.getHeight());

            names.add(name);
            originals.add(source);
            tinted.add(result);
        }

        writeSheet(names, originals, tinted, new File(outDir, "tint_compare.png"));
        System.out.println("wrote " + names.size() + " textures and tint_compare.png to " + outDir);
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

    private static void writeSheet(List<String> names, List<BufferedImage> before, List<BufferedImage> after,
            File target) throws Exception {
        int scale = 6;
        int pad = 14;
        int labelH = 24;
        int rowH = 0;
        int cellW = 0;
        for (BufferedImage image : before) {
            rowH = Math.max(rowH, image.getHeight() * scale);
            cellW = Math.max(cellW, image.getWidth() * scale);
        }

        int sheetW = names.size() * cellW + (names.size() + 1) * pad;
        int sheetH = rowH * 2 + labelH * 2 + pad * 2;

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, sheetW, sheetH);
        g.setFont(new Font("Consolas", Font.PLAIN, 13));

        g.setColor(Color.WHITE);
        g.drawString("original", pad, pad + 13);
        g.drawString("violet", pad, pad + labelH + rowH + 13);

        for (int i = 0; i < names.size(); i++) {
            int ox = pad + i * (cellW + pad);
            blit(g, before.get(i), ox, pad + labelH, scale);
            blit(g, after.get(i), ox, pad + labelH + rowH + labelH, scale);
            g.setColor(Color.WHITE);
            g.drawString(names.get(i), ox, pad + 13);
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
