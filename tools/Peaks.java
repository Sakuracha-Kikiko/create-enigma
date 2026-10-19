import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Finds the light grid lines of a blueprint background, sampling only rows or columns that are clear
 * of the subject.
 *
 * <p>The lines are faint - about 8 luminance above the field, against 30 for the wide band - so a
 * plain peak picker latches onto the soft shading inside each cell instead. Two filters do the work:
 * the profile is high-passed with a radius smaller than a cell, which leaves the lines as the only
 * sharp features, and a peak has to clear a prominence threshold before it counts.
 *
 * <p>Usage: java Peaks.java &lt;in.png&gt; &lt;x|y&gt; &lt;bandFrom&gt; &lt;bandTo&gt;
 * (for x, the band is the row range to sample; for y, the column range)
 */
public final class Peaks {

    private static final double PROMINENCE = 3.0;
    private static final int MIN_SEPARATION = 100;

    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File(args[0]));
        boolean columns = "x".equals(args[1]);
        int bandFrom = Integer.parseInt(args[2]);
        int bandTo = Integer.parseInt(args[3]);

        int length = columns ? image.getWidth() : image.getHeight();
        int depth = columns ? image.getHeight() : image.getWidth();
        bandFrom = Math.max(0, Math.min(depth, bandFrom));
        bandTo = Math.max(bandFrom, Math.min(depth, bandTo));

        double[] profile = new double[length];
        for (int i = 0; i < length; i++) {
            double sum = 0;
            int n = 0;
            for (int j = bandFrom; j < bandTo; j++) {
                int argb = columns ? image.getRGB(i, j) : image.getRGB(j, i);
                sum += 0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF);
                n++;
            }
            profile[i] = n == 0 ? 0 : sum / n;
        }

        // High pass with a radius well below a cell, so cell shading is gone and lines remain.
        int radius = 30;
        double[] sharp = new double[length];
        for (int i = 0; i < length; i++) {
            double sum = 0;
            int n = 0;
            for (int k = Math.max(0, i - radius); k <= Math.min(length - 1, i + radius); k++) {
                sum += profile[k];
                n++;
            }
            sharp[i] = profile[i] - sum / n;
        }

        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < length - 1; i++) {
            if (sharp[i] < PROMINENCE) continue;
            if (sharp[i] < sharp[i - 1] || sharp[i] < sharp[i + 1]) continue;
            if (!peaks.isEmpty() && i - peaks.get(peaks.size() - 1) < MIN_SEPARATION) continue;
            peaks.add(i);
        }

        System.out.println((columns ? "vertical" : "horizontal") + " lines (band " + bandFrom + ".." + bandTo + "):");
        for (int i = 0; i < peaks.size(); i++) {
            int p = peaks.get(i);
            String gap = i == 0 ? "" : "  (+" + (p - peaks.get(i - 1)) + ")";
            System.out.printf("  x=%4d  brightness %6.1f  sharp %+5.2f%s%n", p, profile[p], sharp[p], gap);
        }
        if (peaks.size() >= 2) {
            List<Integer> gaps = new ArrayList<>();
            for (int i = 1; i < peaks.size(); i++) gaps.add(peaks.get(i) - peaks.get(i - 1));
            java.util.Collections.sort(gaps);
            System.out.println("median gap " + gaps.get(gaps.size() / 2));
        }
    }
}
