import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws a blueprint background from parameters measured on a reference image.
 *
 * <p>Written after repairing a reference image turned into a losing game. The repair was always going
 * to be an approximation - the artwork's cells each carry their own shading, so an occluded area can be
 * made plausible but never restored - and each attempt traded one artifact for another. Redrawing it
 * from measurement produces the same look with nothing to repair hidden in it.
 *
 * <p>Everything here is read off the reference, not invented: the colour at each corner of the field,
 * the position and width of every grid line, and the two wide bands of light.
 *
 * <p>Usage: java Blueprint.java &lt;reference.png&gt; &lt;out.png&gt; [size]
 */
public final class Blueprint {

    /** Where the reference's grid lines are, as fractions of its width and height. */
    private static final double[] LINE_X = {324 / 1728.0, 602 / 1728.0, 861 / 1728.0,
            1113 / 1728.0, 1377 / 1728.0, 1643 / 1728.0};
    private static final double[] LINE_Y = {219 / 972.0, 474 / 972.0, 750 / 972.0};
    /** The wide bands, as a fraction of the axis and as a fraction of the shorter side for width. */
    private static final double BAND_X = 596 / 1728.0;
    private static final double BAND_Y = 205 / 972.0;
    private static final double BAND_W = 52 / 1728.0;
    private static final double BAND_H = 44 / 972.0;

    public static void main(String[] args) throws Exception {
        BufferedImage reference = ImageIO.read(new File(args[0]));
        File out = new File(args[1]);
        int size = args.length > 2 ? Integer.parseInt(args[2]) : 256;

        int[] corners = sampleField(reference);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // The field: bilinear between the four corner colours of the reference's field.
        for (int y = 0; y < size; y++) {
            double t = y / (double) (size - 1);
            for (int x = 0; x < size; x++) {
                double s = x / (double) (size - 1);
                image.setRGB(x, y, bilinear(corners, s, t));
            }
        }

        // Grid lines: one pixel, a fixed amount lighter, which is all they are in the reference.
        g.setColor(new Color(255, 255, 255, 30));
        g.setStroke(new BasicStroke(1f));
        for (double fx : LINE_X) {
            int x = (int) Math.round(fx * size);
            g.drawLine(x, 0, x, size);
        }
        for (double fy : LINE_Y) {
            int y = (int) Math.round(fy * size);
            g.drawLine(0, y, size, y);
        }

        // The two wide bands, crossing near the upper left as in the reference.
        g.setColor(new Color(255, 255, 255, 26));
        g.fillRect((int) Math.round((BAND_X - BAND_W / 2) * size), 0,
                Math.max(1, (int) Math.round(BAND_W * size)), size);
        g.fillRect(0, (int) Math.round((BAND_Y - BAND_H / 2) * size),
                size, Math.max(1, (int) Math.round(BAND_H * size)));

        g.dispose();
        ImageIO.write(image, "png", out);
        System.out.printf("field corners: #%06x #%06x #%06x #%06x%n",
                corners[0] & 0xFFFFFF, corners[1] & 0xFFFFFF, corners[2] & 0xFFFFFF, corners[3] & 0xFFFFFF);
        System.out.println("wrote " + out + " " + size + "x" + size + " (" + out.length() + " bytes)");
    }

    /** Colours a little inside each corner of the reference's field, averaged over a small patch. */
    private static int[] sampleField(BufferedImage reference) {
        int w = reference.getWidth();
        int h = reference.getHeight();
        int[] result = new int[4];
        int[][] points = {{90, 60}, {w - 90, 60}, {90, h - 60}, {w - 90, h - 60}};
        for (int i = 0; i < 4; i++) {
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int dy = -8; dy <= 8; dy++) {
                for (int dx = -8; dx <= 8; dx++) {
                    int x = Math.max(0, Math.min(w - 1, points[i][0] + dx));
                    int y = Math.max(0, Math.min(h - 1, points[i][1] + dy));
                    int argb = reference.getRGB(x, y);
                    r += (argb >> 16) & 0xFF;
                    g += (argb >> 8) & 0xFF;
                    b += argb & 0xFF;
                    n++;
                }
            }
            result[i] = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
        }
        return result;
    }

    private static int bilinear(int[] corners, double s, double t) {
        double r = lerp(lerp((corners[0] >> 16) & 0xFF, (corners[1] >> 16) & 0xFF, s),
                lerp((corners[2] >> 16) & 0xFF, (corners[3] >> 16) & 0xFF, s), t);
        double g = lerp(lerp((corners[0] >> 8) & 0xFF, (corners[1] >> 8) & 0xFF, s),
                lerp((corners[2] >> 8) & 0xFF, (corners[3] >> 8) & 0xFF, s), t);
        double b = lerp(lerp(corners[0] & 0xFF, corners[1] & 0xFF, s),
                lerp(corners[2] & 0xFF, corners[3] & 0xFF, s), t);
        return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
