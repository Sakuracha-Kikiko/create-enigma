import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Removes the subject from a blueprint-style logo, leaving the background.
 *
 * <h2>What does not work, and why</h2>
 *
 * <p><b>Shifting by one grid period.</b> The grid is not evenly spaced - the vertical lines sit at
 * 324, 574, 869, 1113, 1376 and 1640, gaps of 244 to 295 - so no shift aligns the pattern with
 * itself, and each attempt left a mismatched rectangle exactly where the subject had been.
 *
 * <p><b>Interpolating the field from the edges.</b> A Coons patch and inverse-distance weighting both
 * produce a smooth surface, but the artwork's field is <b>not</b> smooth: every cell carries its own
 * shading. The result was a broad tonal haze over the repair, which is more noticeable than the line
 * breaks it avoided.
 *
 * <h2>What is done instead</h2>
 *
 * <p>The grid lines are measured, which turns the background into a table of cells. Each pixel of the
 * repair is then taken from the <b>same relative position in a clean cell</b>, so that a cell's own
 * shading comes with it, and the grid lines crossing the repair are added back at their measured width
 * using the contrast of a line outside it.
 *
 * <p>The subject is found by two tests, not one: the item is not blue-dominant, and its <b>drop
 * shadow</b> - which is merely a darker blue - is caught by comparing against a heavily blurred copy
 * of the image. With only the colour test the shadow survived, and that was the halo visible around
 * the first repair.
 *
 * <p>Usage: java Rebuild.java &lt;in.png&gt; &lt;out.png&gt;
 */
public final class Rebuild {

    private static final int MARGIN = 30;
    private static final double LINE_PROMINENCE = 3.0;
    private static final int LINE_SEPARATION = 100;
    /** A peak brighter than this above the field is one of the wide bands, not a grid line. */
    private static final double BAND_PROMINENCE = 10.0;
    /** Half width of a restored grid line, in pixels. */
    private static final int LINE_HALF_WIDTH = 6;

    public static void main(String[] args) throws Exception {
        BufferedImage in = ImageIO.read(new File(args[0]));
        int w = in.getWidth();
        int h = in.getHeight();

        boolean[] mask = subjectMask(in, w, h);
        int[] box = boundingBox(mask, w, h);
        int[] xs = boundaries(in, w, h, box, true);
        int[] ys = boundaries(in, w, h, box, false);
        System.out.println("box " + box[0] + "," + box[1] + " .. " + box[2] + "," + box[3]);
        System.out.println("cell x bounds " + java.util.Arrays.toString(xs));
        System.out.println("cell y bounds " + java.util.Arrays.toString(ys));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, in.getRGB(x, y));
            }
        }

        // One Coons patch over the whole repair, from its four edges.
        //
        // Per-cell patches were tried and are worse: a cell that the repair only partly covers has
        // corners underneath the subject, and a corner sampled from the subject drags its colour
        // across the whole cell - the result was a dark ghost of the wrench in the middle of the
        // blueprint. Sampling only the outer edges of the repair avoids that entirely.
        int pad = 8;
        int leftX = Math.max(0, box[0] - pad);
        int rightX = Math.min(w - 1, box[2] - 1 + pad);
        int topY = Math.max(0, box[1] - pad);
        int bottomY = Math.min(h - 1, box[3] - 1 + pad);
        int bl = in.getRGB(leftX, bottomY);
        int br = in.getRGB(rightX, bottomY);
        int tl = in.getRGB(leftX, topY);
        int tr = in.getRGB(rightX, topY);

        for (int y = box[1]; y < box[3]; y++) {
            double t = (y - box[1]) / (double) Math.max(1, box[3] - 1 - box[1]);
            for (int x = box[0]; x < box[2]; x++) {
                double s = (x - box[0]) / (double) Math.max(1, box[2] - 1 - box[0]);

                int bottomEdge = in.getRGB(x, bottomY);
                int topEdge = in.getRGB(x, topY);
                int leftEdge = in.getRGB(leftX, y);
                int rightEdge = in.getRGB(rightX, y);

                double[] total = new double[3];
                add(total, bottomEdge, 1 - t);
                add(total, topEdge, t);
                add(total, leftEdge, 1 - s);
                add(total, rightEdge, s);
                add(total, bl, -(1 - s) * (1 - t));
                add(total, br, -s * (1 - t));
                add(total, tl, -(1 - s) * t);
                add(total, tr, -s * t);

                out.setRGB(x, y, 0xFF000000
                        | clamp(total[0]) << 16 | clamp(total[1]) << 8 | clamp(total[2]));
            }
        }

        restoreLines(in, out, w, h, box, xs, ys, true);
        restoreLines(in, out, w, h, box, xs, ys, false);

        ImageIO.write(out, "png", new File(args[1]));
        System.out.println("wrote " + args[1] + " (" + new File(args[1]).length() + " bytes)");
    }

    private static void add(double[] total, int argb, double weight) {
        total[0] += weight * ((argb >> 16) & 0xFF);
        total[1] += weight * ((argb >> 8) & 0xFF);
        total[2] += weight * (argb & 0xFF);
    }

    private static int clampIndex(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Index of the cell containing {@code value}, i.e. the last boundary that is not past it. */
    private static int cellOf(int[] bounds, int value) {
        int index = 0;
        while (index < bounds.length - 2 && value >= bounds[index + 1]) index++;
        return index;
    }

    private static double fraction(int[] bounds, int cell, int value) {
        int span = Math.max(1, bounds[cell + 1] - bounds[cell]);
        return Math.max(0, Math.min(1, (value - bounds[cell]) / (double) span));
    }

    /**
     * A cell that the subject does not touch, preferring the one furthest from it so the donor is
     * never a partially repaired neighbour.
     */
    private static int[] donorCell(int[] xs, int[] ys, int[] box) {
        int bestX = 0;
        int bestY = 0;
        int bestDistance = -1;
        for (int i = 0; i < xs.length - 1; i++) {
            for (int j = 0; j < ys.length - 1; j++) {
                boolean clear = xs[i + 1] <= box[0] || xs[i] >= box[2] || ys[j + 1] <= box[1] || ys[j] >= box[3];
                if (!clear) continue;
                // Prefer large cells: more room to carry shading without stretching it.
                int area = (xs[i + 1] - xs[i]) * (ys[j + 1] - ys[j]);
                if (area > bestDistance) {
                    bestDistance = area;
                    bestX = i;
                    bestY = j;
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    /** Puts back the grid lines that cross the repair, at their measured width and contrast. */
    private static void restoreLines(BufferedImage in, BufferedImage out, int w, int h, int[] box,
            int[] xs, int[] ys, boolean vertical) {
        int[] bounds = vertical ? xs : ys;
        int start = vertical ? box[0] : box[1];
        int end = vertical ? box[2] : box[3];

        for (int k = 1; k < bounds.length - 1; k++) {
            int line = bounds[k];
            if (line < start || line >= end) continue;

            // A line outside the repair, of the same orientation, is the contrast reference. It has
            // to be well away from the frame: the first choice here was x=1643, a few pixels from the
            // right frame, and its contrast was contaminated by the frame's own ramp - the restored
            // line came out bright and misplaced.
            int reference = -1;
            int bestClearance = -1;
            for (int other = 1; other < bounds.length - 1; other++) {
                int candidate = bounds[other];
                if (candidate >= start && candidate < end && (vertical ? candidate >= box[0] && candidate < box[2]
                        : candidate >= box[1] && candidate < box[3])) {
                    continue;
                }
                int frameGap = Math.min(candidate - bounds[0], bounds[bounds.length - 1] - candidate);
                if (frameGap > bestClearance) {
                    bestClearance = frameGap;
                    reference = candidate;
                }
            }
            if (reference < 0) continue;
            System.out.println("restoring " + (vertical ? "vertical x=" + line + " from x=" + reference
                    : "horizontal y=" + line + " from y=" + reference) + " (clearance " + bestClearance + ")");

            for (int d = -LINE_HALF_WIDTH; d <= LINE_HALF_WIDTH; d++) {
                int at = line + d;
                if (at < start || at >= end) continue;
                for (int along = vertical ? box[1] : box[0]; along < (vertical ? box[3] : box[2]); along++) {
                    int refArgb = vertical ? in.getRGB(reference + d, along) : in.getRGB(along, reference + d);
                    int refBase = vertical
                            ? in.getRGB(reference + d + 20, along)
                            : in.getRGB(along, reference + d + 20);
                    int px = vertical ? at : along;
                    int py = vertical ? along : at;
                    int argb = out.getRGB(px, py);
                    out.setRGB(px, py, addDelta(argb, new int[]{
                            ((refArgb >> 16) & 0xFF) - ((refBase >> 16) & 0xFF),
                            ((refArgb >> 8) & 0xFF) - ((refBase >> 8) & 0xFF),
                            (refArgb & 0xFF) - (refBase & 0xFF)}));
                }
            }
        }
    }

    /** Cell boundaries: the frame's inner edge, every grid line, and the frame's other edge. */
    private static int[] boundaries(BufferedImage image, int w, int h, int[] box, boolean vertical) {
        int innerStart = innerFrame(image, w, h, true, vertical);
        int innerEnd = innerFrame(image, w, h, false, vertical);
        int[] lines = lines(image, w, h, box, vertical);

        List<Integer> all = new ArrayList<>();
        all.add(innerStart);
        for (int line : lines) {
            if (line > innerStart && line < innerEnd) all.add(line);
        }
        all.add(innerEnd);
        java.util.Collections.sort(all);
        return all.stream().mapToInt(Integer::intValue).toArray();
    }

    /** First non-white pixel walking in from the edge, i.e. where the blueprint itself starts. */
    private static int innerFrame(BufferedImage image, int w, int h, boolean fromStart, boolean vertical) {
        int depth = vertical ? w : h;
        int length = vertical ? h : w;
        for (int step = 0; step < depth; step++) {
            int fixed = fromStart ? step : depth - 1 - step;
            boolean white = true;
            for (int i = 0; i < length; i += 4) {
                int argb = vertical ? image.getRGB(fixed, i) : image.getRGB(i, fixed);
                if (luminanceAt(image, vertical ? fixed : i, vertical ? i : fixed) < 235) {
                    white = false;
                    break;
                }
            }
            if (!white) return fixed;
        }
        return fromStart ? 0 : depth - 1;
    }

    private static int[] lines(BufferedImage image, int w, int h, int[] box, boolean vertical) {
        int length = vertical ? w : h;
        int depth = vertical ? h : w;

        double[] profile = new double[length];
        for (int i = 0; i < length; i++) {
            double sum = 0;
            int n = 0;
            for (int j = 0; j < depth; j += 2) {
                boolean inside = vertical
                        ? (i >= box[0] && i < box[2] && j >= box[1] && j < box[3])
                        : (j >= box[0] && j < box[2] && i >= box[1] && i < box[3]);
                if (inside) continue;
                profile[i] += (vertical ? image.getRGB(i, j) : image.getRGB(j, i)) == 0 ? 0 : luminanceAt(image,
                        vertical ? i : j, vertical ? j : i);
                n++;
            }
            profile[i] = n == 0 ? 0 : profile[i] / n;
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

    /**
     * The subject's pixels: everything that is not the blue field, plus its shadow.
     *
     * <p>The item is grey and gold, so a colour test finds it. The <b>drop shadow is merely a darker
     * blue</b> and passes that test - it survived the first attempts and showed as a halo around the
     * repair - so a pixel also counts when it is clearly darker than a heavily blurred copy of the
     * image, which is what a shadow is and what a grid line is not.
     */
    private static boolean[] subjectMask(BufferedImage image, int w, int h) {
        double[] field = blur(luminance(image, w, h), w, h, 60);
        boolean[] mask = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int b = argb & 0xFF;
                boolean notBlue = (b - r) < 30 || (r - b) > 20;
                boolean shadow = luminanceAt(image, x, y) < field[y * w + x] - 4.0;
                mask[y * w + x] = notBlue || shadow;
            }
        }
        return mask;
    }

    private static double luminanceAt(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);
        return 0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF);
    }

    private static double[] luminance(BufferedImage image, int w, int h) {
        double[] lum = new double[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) lum[y * w + x] = luminanceAt(image, x, y);
        }
        return lum;
    }

    /** Separable box blur. */
    private static double[] blur(double[] in, int w, int h, int radius) {
        double[] temp = new double[w * h];
        double[] out = new double[w * h];
        for (int y = 0; y < h; y++) {
            double sum = 0;
            int n = 0;
            for (int x = -radius; x < w; x++) {
                int add = x + radius;
                int drop = x - radius - 1;
                if (add >= 0 && add < w) { sum += in[y * w + add]; n++; }
                if (drop >= 0) { sum -= in[y * w + drop]; n--; }
                if (x >= 0) temp[y * w + x] = sum / Math.max(1, n);
            }
        }
        for (int x = 0; x < w; x++) {
            double sum = 0;
            int n = 0;
            for (int y = -radius; y < h; y++) {
                int add = y + radius;
                int drop = y - radius - 1;
                if (add >= 0 && add < h) { sum += temp[add * w + x]; n++; }
                if (drop >= 0) { sum -= temp[drop * w + x]; n--; }
                if (y >= 0) out[y * w + x] = sum / Math.max(1, n);
            }
        }
        return out;
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

    private static int addDelta(int argb, int[] delta) {
        return 0xFF000000
                | clamp(((argb >> 16) & 0xFF) + delta[0]) << 16
                | clamp(((argb >> 8) & 0xFF) + delta[1]) << 8
                | clamp((argb & 0xFF) + delta[2]);
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
