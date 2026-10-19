import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Prints a brightness profile as a small text plot, so the real line positions can be read off.
 *
 * <p>Written after three attempts to infer the grid period failed: correlation settled on the edge of
 * its search range, and peak picking found the soft shading inside each cell instead of the lines.
 * Numbers on screen settle it immediately.
 *
 * <p>Usage: java Profile.java &lt;in.png&gt; &lt;x|y&gt; &lt;from&gt; &lt;to&gt;
 */
public final class Profile {
    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File(args[0]));
        boolean columns = "x".equals(args[1]);
        int from = Integer.parseInt(args[2]);
        int to = Integer.parseInt(args[3]);

        int length = columns ? image.getWidth() : image.getHeight();
        from = Math.max(0, from);
        to = Math.min(length, to);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double[] profile = new double[length];
        for (int i = 0; i < length; i++) {
            double sum = 0;
            int n = 0;
            for (int j = 0; j < (columns ? image.getHeight() : image.getWidth()); j += 2) {
                int argb = columns ? image.getRGB(i, j) : image.getRGB(j, i);
                sum += 0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF);
                n++;
            }
            profile[i] = sum / n;
            if (i >= from && i < to) {
                min = Math.min(min, profile[i]);
                max = Math.max(max, profile[i]);
            }
        }

        System.out.println((columns ? "column" : "row") + " profile " + from + ".." + to
                + "  min " + String.format("%.1f", min) + "  max " + String.format("%.1f", max));

        for (int i = from; i < to; i += 4) {
            StringBuilder bar = new StringBuilder();
            int width = 60;
            int filled = (int) Math.round((profile[i] - min) / Math.max(0.001, max - min) * width);
            for (int k = 0; k < width; k++) bar.append(k < filled ? '#' : '.');
            System.out.printf("%4d %6.1f %s%n", i, profile[i], bar);
        }
    }
}
