import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Turns the Bugjump artwork into mod-list logos.
 *
 * <p>Two source variants, handled by asking the corner pixel what the background is:
 *
 * <ul>
 *   <li><b>transparent</b> - the mark's own bounding box is the content, and the square is left
 *       transparent;</li>
 *   <li><b>solid</b> - the red field is part of the design, so the content is everything that
 *       differs from the corner colour and the square is filled with that colour.</li>
 * </ul>
 *
 * <p>Either way the mark is re-laid onto a square canvas instead of being cropped: the source is
 * 1920x1080 with the mark about 1274 wide, so a centred 1080-square would slice the wordmark in half,
 * and simply scaling the whole 16:9 canvas would leave the mark a few pixels tall in a 64-pixel tile.
 *
 * <p>Downscaling happens in halvings. A single large bilinear step loses thin strokes, and this
 * wordmark is nothing but thin strokes.
 *
 * <p>Usage: java Logo.java &lt;in.png&gt; &lt;outDir&gt;
 */
public final class Logo {

    /** Fraction of the square the mark fills. Leaves the border the artwork already implies. */
    private static final double FILL = 0.90;

    /** How far a pixel has to be from the background colour to count as content. */
    private static final int THRESHOLD = 24;

    public static void main(String[] args) throws Exception {
        BufferedImage source = ImageIO.read(new File(args[0]));
        File outDir = new File(args[1]);
        outDir.mkdirs();

        int background = source.getRGB(0, 0);
        boolean transparent = ((background >>> 24) & 0xFF) == 0;
        System.out.println("source " + source.getWidth() + "x" + source.getHeight()
                + ", background " + (transparent ? "transparent" : colour(background)));

        int[] box = contentBox(source, background, transparent);
        int w = box[2] - box[0];
        int h = box[3] - box[1];
        System.out.println("content box " + box[0] + "," + box[1] + " .. " + box[2] + "," + box[3]
                + " = " + w + "x" + h + " (aspect " + String.format("%.2f", (double) w / h) + ")");

        BufferedImage cropped = source.getSubimage(box[0], box[1], w, h);

        for (int size : new int[]{256, 128}) {
            BufferedImage square = square(cropped, size, transparent ? 0 : background);
            File out = new File(outDir, size == 256 ? "logo.png" : "logo_" + size + ".png");
            ImageIO.write(square, "png", out);
            System.out.println("wrote " + out.getName() + " " + size + "x" + size + " (" + out.length() + " bytes)");
        }
    }

    private static int[] contentBox(BufferedImage image, int background, boolean transparent) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isContent(image.getRGB(x, y), background, transparent)) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) throw new IllegalStateException("no content found");
        return new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    private static boolean isContent(int argb, int background, boolean transparent) {
        int a = (argb >>> 24) & 0xFF;
        if (transparent) {
            return a > 8;
        }
        int dr = Math.abs(((argb >> 16) & 0xFF) - ((background >> 16) & 0xFF));
        int dg = Math.abs(((argb >> 8) & 0xFF) - ((background >> 8) & 0xFF));
        int db = Math.abs((argb & 0xFF) - (background & 0xFF));
        return dr + dg + db > THRESHOLD;
    }

    private static BufferedImage square(BufferedImage content, int size, int fillArgb) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (((fillArgb >>> 24) & 0xFF) != 0) {
            g.setColor(new Color(fillArgb, true));
            g.fillRect(0, 0, size, size);
        }

        double target = size * FILL;
        double scale = Math.min(target / content.getWidth(), target / content.getHeight());
        int dw = Math.max(1, (int) Math.round(content.getWidth() * scale));
        int dh = Math.max(1, (int) Math.round(content.getHeight() * scale));

        g.drawImage(halve(content, dw, dh), (size - dw) / 2, (size - dh) / 2, null);
        g.dispose();
        return out;
    }

    private static BufferedImage halve(BufferedImage image, int targetW, int targetH) {
        BufferedImage current = image;
        while (current.getWidth() / 2 >= targetW && current.getHeight() / 2 >= targetH) {
            current = resize(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        return resize(current, targetW, targetH);
    }

    private static BufferedImage resize(BufferedImage image, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    private static String colour(int argb) {
        return String.format("#%06x alpha %d", argb & 0xFFFFFF, (argb >>> 24) & 0xFF);
    }
}
