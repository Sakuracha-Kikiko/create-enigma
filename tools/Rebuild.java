import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Rebuilds a blueprint background behind a small subject.
 *
 * <p>The obvious approach - shift the image by one grid period and copy - does not work here, and it
 * took measuring to find out why: the grid is <b>not evenly spaced</b>. The vertical lines sit at
 * 324, 869, 1113, 1376 and 1640 - gaps of 244 to 295 - so there is no shift that lines the pattern up
 * with itself, and every attempt left a mismatched rectangle exactly where the subject had been.
 *
 * <p>What the measurement does give is where the lines actually are, and that is enough to rebuild:
 *
 * <ul>
 *   <li>the field is interpolated from the four edges of the repaired rectangle, which is smooth and
 *       therefore safe to reconstruct from its surroundings;</li>
 *   <li>each grid line crossing the rectangle is then added back, using the contrast measured on a
 *       line of the same orientation that is <em>outside</em> the rectangle, sampled row by row (or
 *       column by column) so the slow gradient is carried across with it.</li>
 * </ul>
 *
 * <p>Usage: java Rebuild.java &lt;in.png&gt; &lt;out.png&gt;
 */
public final class Rebuild {

    private static final int MARGIN = 26;
    private static final double LINE_PROMINENCE = 3.0;
    private static final int LINE_SEPARATION = 100;
    /** A peak brighter than this above the field is one of the wide bands, not a grid line. */
    private static final double BAND_PROMINENCE = 10.0;

    public static void main(String[] args) throws Exception {
        BufferedImage in = ImageIO.read(new File(args[0]));
        int w = in.getWidth();
        int h = in.getHeight();

        boolean[] mask = subjectMask(in, w, h);
        int[] box = boundingBox(mask, w, h);
        System.out.println("box " + box[0] + "," + box[1] + " .. " + box[2] + "," + box[3]);

        int[] vertical = lines(in, w, h, mask, box, true);
        int[] horizontal = lines(in, w, h, mask, box, false);
        System.out.println("vertical lines   " + java.util.Arrays.toString(vertical));
        System.out.println("horizontal lines " + java.util.Arrays.toString(horizontal));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, in.getRGB(x, y));
            }
        }

        // Field: a Coons patch over the four edges. Not inverse-distance weighting - that pulls the
        // middle toward the average of the edges and shows as a soft bulge exactly where the subject
        // was. The Coons form matches the boundary exactly and interpolates smoothly between it.
        int pad = 8;
        int x0 = box[0];
        int y0 = box[1];
        int x1 = box[2] - 1;
        int y1 = box[3] - 1;
        int leftX = Math.max(0, x0 - pad);
        int rightX = Math.min(w - 1, x1 + pad);
        int topY = Math.max(0, y0 - pad);
        int bottomY = Math.min(h - 1, y1 + pad);

        int bl = in.getRGB(leftX, bottomY);
        int br = in.getRGB(rightX, bottomY);
        int tl = in.getRGB(leftX, topY);
        int tr = in.getRGB(rightX, topY);

        for (int y = y0; y <= y1; y++) {
            double t = (y - y0) / (double) Math.max(1, y1 - y0);
            for (int x = x0; x <= x1; x++) {
                double s = (x - x0) / (double) Math.max(1, x1 - x0);
                int bottomEdge = in.getRGB(x, bottomY);
                int topEdge = in.getRGB(x, topY);
                int leftEdge = in.getRGB(leftX, y);
                int rightEdge = in.getRGB(rightX, y);

                double[] total = new double[3];
                add(total, bottomEdge, (1 - t));
                add(total, topEdge, t);
                add(total, leftEdge, (1 - s));
                add(total, rightEdge, s);
                add(total, bl, -(1 - s) * (1 - t));
                add(total, br, -s * (1 - t));
                add(total, tl, -(1 - s) * t);
                add(total, tr, -s * t);

                out.setRGB(x, y, 0xFF000000
                        | clamp(total[0]) << 16 | clamp(total[1]) << 8 | clamp(total[2]));
            }
        }

        // Feather the join: the field is approximate, so a hard rectangle edge would still read.
        feather(out, box, 18);

        // Then the lines that cross the box, using the contrast of a line outside it.
        for (int lineX : vertical) {
            if (lineX < box[0] || lineX >= box[2]) continue;
            int reference = nearestOutside(vertical, box[0], box[2]);
            if (reference < 0) continue;
            System.out.println("restoring vertical line x=" + lineX + " (contrast from x=" + reference + ")");
            for (int y = box[1]; y < box[3]; y++) {
                out.setRGB(lineX, y, addDelta(out.getRGB(lineX, y), delta(in, reference, y, 10, true, w, h)));
            }
        }
        for (int lineY : horizontal) {
            if (lineY < box[1] || lineY >= box[3]) continue;
            int reference = nearestOutside(horizontal, box[1], box[3]);
            if (reference < 0) continue;
            System.out.println("restoring horizontal line y=" + lineY + " (contrast from y=" + reference + ")");
            for (int x = box[0]; x < box[2]; x++) {
                out.setRGB(x, lineY, addDelta(out.getRGB(x, lineY), delta(in, reference, x, 10, false, w, h)));
            }
        }

        ImageIO.write(out, "png", new File(args[1]));
        System.out.println("wrote " + args[1] + " (" + new File(args[1]).length() + " bytes)");
    }

    /** A line outside the box, used as the contrast reference. */
    private static int nearestOutside(int[] lines, int from, int to) {
        int best = -1;
        for (int line : lines) {
            if (line < from || line >= to) best = line;
        }
        return best;
    }

    /** How much brighter the reference line is than its neighbourhood, at that row or column. */
    private static int[] delta(BufferedImage image, int line, int along, int offset, boolean vertical,
            int w, int h) {
        int argb = vertical
                ? image.getRGB(line, along)
                : image.getRGB(along, line);
        int neighbour = vertical
                ? image.getRGB(Math.min(w - 1, line + offset), along)
                : image.getRGB(along, Math.min(h - 1, line + offset));
        return new int[]{
                ((argb >> 16) & 0xFF) - ((neighbour >> 16) & 0xFF),
                ((argb >> 8) & 0xFF) - ((neighbour >> 8) & 0xFF),
                (argb & 0xFF) - (neighbour & 0xFF)};
    }

    private static int addDelta(int argb, int[] delta) {
        int r = clamp(((argb >> 16) & 0xFF) + delta[0]);
        int g = clamp(((argb >> 8) & 0xFF) + delta[1]);
        int b = clamp((argb & 0xFF) + delta[2]);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int blend4(int l, int r, int t, int b,
            double wl, double wr, double wt, double wb) {
        int rr = clamp(wl * ((l >> 16) & 0xFF) + wr * ((r >> 16) & 0xFF) + wt * ((t >> 16) & 0xFF) + wb * ((b >> 16) & 0xFF));
        int gg = clamp(wl * ((l >> 8) & 0xFF) + wr * ((r >> 8) & 0xFF) + wt * ((t >> 8) & 0xFF) + wb * ((b >> 8) & 0xFF));
        int bb = clamp(wl * (l & 0xFF) + wr * (r & 0xFF) + wt * (t & 0xFF) + wb * (b & 0xFF));
        return 0xFF000000 | rr << 16 | gg << 8 | bb;
    }

    private static void add(double[] total, int argb, double weight) {
        total[0] += weight * ((argb >> 16) & 0xFF);
        total[1] += weight * ((argb >> 8) & 0xFF);
        total[2] += weight * (argb & 0xFF);
    }

    /** Averages a ramp just inside the rectangle's edge, so the patch does not end in a step. */
    private static void feather(BufferedImage image, int[] box, int width) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int d = Math.min(Math.min(x - box[0], box[2] - 1 - x), Math.min(y - box[1], box[3] - 1 - y));
                copy.setRGB(x, y, argb);
                if (d < 0 || d >= width) continue;
                int sr = 0, sg = 0, sb = 0, n = 0;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        int px = Math.min(w - 1, Math.max(0, x + dx));
                        int py = Math.min(h - 1, Math.max(0, y + dy));
                        int p = image.getRGB(px, py);
                        sr += (p >> 16) & 0xFF;
                        sg += (p >> 8) & 0xFF;
                        sb += p & 0xFF;
                        n++;
                    }
                }
                double t = 1.0 - d / (double) width;
                copy.setRGB(x, y, 0xFF000000
                        | clamp(((argb >> 16) & 0xFF) * (1 - t) + (sr / (double) n) * t) << 16
                        | clamp(((argb >> 8) & 0xFF) * (1 - t) + (sg / (double) n) * t) << 8
                        | clamp((argb & 0xFF) * (1 - t) + (sb / (double) n) * t));
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, copy.getRGB(x, y));
            }
        }
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    private static boolean[] subjectMask(BufferedImage image, int w, int h) {
        boolean[] mask = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                mask[y * w + x] = (b - r) < 30 || (r - b) > 20;
            }
        }
        return mask;
    }

    private static int[] boundingBox(boolean[] mask, int w, int h) {
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[w * h];
        int bestArea = 0;
        int[] bestBox = null;

        for (int start = 0; start < w * h; start++) {
            if (!mask[start] || seen[start]) continue;
            int top = 0;
            stack[top++] = start;
            seen[start] = true;
            int area = 0;
            boolean border = false;
            int minX = w, minY = h, maxX = -1, maxY = -1;
            while (top > 0) {
                int index = stack[--top];
                int x = index % w;
                int y = index / w;
                area++;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border = true;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int n = ny * w + nx;
                        if (mask[n] && !seen[n]) { seen[n] = true; stack[top++] = n; }
                    }
                }
            }
            if (border) continue;
            if (area > bestArea) { bestArea = area; bestBox = new int[]{minX, minY, maxX, maxY}; }
        }
        if (bestBox == null) throw new IllegalStateException("no subject found");
        return new int[]{Math.max(0, bestBox[0] - MARGIN), Math.max(0, bestBox[1] - MARGIN),
                Math.min(w, bestBox[2] + 1 + MARGIN), Math.min(h, bestBox[3] + 1 + MARGIN)};
    }

    /** Grid line positions, excluding the wide bands and anything inside the subject's box. */
    private static int[] lines(BufferedImage image, int w, int h, boolean[] mask, int[] box, boolean vertical) {
        int length = vertical ? w : h;
        int depth = vertical ? h : w;

        double[] profile = new double[length];
        for (int i = 0; i < length; i++) {
            double sum = 0;
            int n = 0;
            for (int j = 0; j < depth; j += 2) {
                // Skip anything inside the box, so the subject cannot bias the profile.
                boolean inside = vertical
                        ? (i >= box[0] && i < box[2] && j >= box[1] && j < box[3])
                        : (j >= box[0] && j < box[2] && i >= box[1] && i < box[3]);
                if (inside) continue;
                int argb = vertical ? image.getRGB(i, j) : image.getRGB(j, i);
                sum += 0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF);
                n++;
            }
            profile[i] = n == 0 ? 0 : sum / n;
        }

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

        List<Integer> found = new ArrayList<>();
        for (int i = 1; i < length - 1; i++) {
            if (sharp[i] < LINE_PROMINENCE || sharp[i] > BAND_PROMINENCE) continue;
            if (sharp[i] < sharp[i - 1] || sharp[i] < sharp[i + 1]) continue;
            if (!found.isEmpty() && i - found.get(found.size() - 1) < LINE_SEPARATION) continue;
            found.add(i);
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }
}
