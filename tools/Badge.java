import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Lays a circular red badge over the subject of a blueprint image.
 *
 * <p>The approach is the reverse of repairing the background: rather than reconstructing what was
 * behind the subject - which can only ever be plausible, because the artwork's cells each carry their
 * own shading - the badge <b>covers</b> it. Nothing is invented, so there is nothing to get wrong.
 *
 * <p>Both inputs are measured rather than assumed. The wordmark's bounding box comes from the colour
 * difference against the red field's corner pixel, so the mark can be centred in the disc; the
 * subject's bounding box comes from the same colour-and-darkness test that found its shadow, so the
 * disc can be sized to cover the shadow too.
 *
 * <p>Usage: java Badge.java &lt;red-logo.png&gt; &lt;blueprint.png&gt; &lt;out.png&gt; [diameterScale]
 */
public final class Badge {

    /** Badge diameter relative to the subject's bounding box diagonal. */
    private static final double COVERAGE = 1.02;
    /** How much of the disc the wordmark may fill, by width. */
    private static final double MARK_FILL = 0.80;
    /** Supersampling factor: the disc's edge is the only curved line in the result. */
    private static final int SUPERSAMPLE = 4;

    public static void main(String[] args) throws Exception {
        BufferedImage red = ImageIO.read(new File(args[0]));
        BufferedImage blueprint = ImageIO.read(new File(args[1]));
        File out = new File(args[2]);
        double scale = args.length > 3 ? Double.parseDouble(args[3]) : 1.0;

        int field = red.getRGB(0, 0);
        int[] mark = contentBox(red, field);
        System.out.println("wordmark " + (mark[2] - mark[0]) + "x" + (mark[3] - mark[1])
                + " at " + mark[0] + "," + mark[1]);

        int[] subject = subjectBox(blueprint);
        int subjectW = subject[2] - subject[0];
        int subjectH = subject[3] - subject[1];
        double diagonal = Math.sqrt(subjectW * subjectW + subjectH * subjectH);
        int diameter = (int) Math.round(diagonal * COVERAGE * scale);
        int centreX = (subject[0] + subject[2]) / 2;
        int centreY = (subject[1] + subject[3]) / 2;
        System.out.println("subject " + subjectW + "x" + subjectH + " at " + centreX + "," + centreY
                + " -> badge diameter " + diameter);

        BufferedImage badge = disc(red, field, mark, diameter,
                args.length > 6 && "ring".equals(args[6]));

        BufferedImage result = new BufferedImage(blueprint.getWidth(), blueprint.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(blueprint, 0, 0, null);
        g.drawImage(badge, centreX - diameter / 2, centreY - diameter / 2, null);
        g.dispose();

        ImageIO.write(result, "png", out);
        System.out.println("wrote " + out + " " + result.getWidth() + "x" + result.getHeight()
                + " (" + out.length() + " bytes)");

        // A square version, cropped around the disc: that is the shape a mod list wants, and the
        // blueprint's own frame is not part of the logo.
        if (args.length > 5) {
            int side = Math.min(Math.min(result.getWidth(), result.getHeight()),
                    diameter + Math.round(diameter * 0.55f));
            int x0 = Math.max(0, Math.min(result.getWidth() - side, centreX - side / 2));
            int y0 = Math.max(0, Math.min(result.getHeight() - side, centreY - side / 2));
            int size = Integer.parseInt(args[5]);
            BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = square.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            sg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            sg.drawImage(result.getSubimage(x0, y0, side, side), 0, 0, size, size, null);
            sg.dispose();
            File squareFile = new File(args[4]);
            ImageIO.write(square, "png", squareFile);
            System.out.println("wrote " + squareFile + " " + size + "x" + size
                    + " (" + squareFile.length() + " bytes)");
        }
    }

    /** A red disc with the wordmark centred in it, drawn supersampled and then reduced. */
    private static BufferedImage disc(BufferedImage red, int field, int[] mark, int diameter, boolean ring) {
        int big = diameter * SUPERSAMPLE;
        BufferedImage canvas = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(new Color(field, true));
        g.fill(new Ellipse2D.Double(0, 0, big, big));

        int markW = mark[2] - mark[0];
        int markH = mark[3] - mark[1];
        double target = big * MARK_FILL;
        double fit = Math.min(target / markW, target / markH);
        int dw = Math.max(1, (int) Math.round(markW * fit));
        int dh = Math.max(1, (int) Math.round(markH * fit));
        BufferedImage ink = ink(red, field, mark, dw, dh);
        g.drawImage(ink, (big - dw) / 2, (big - dh) / 2, null);

        if (ring) {
            // A thin white ring, as Create's own icon has: it reads as a badge rather than as a
            // pasted circle, because the edge separates the disc from whatever is behind it.
            float width = big * 0.012f;
            g.setColor(Color.WHITE);
            g.setStroke(new java.awt.BasicStroke(width));
            g.draw(new Ellipse2D.Double(width / 2.0, width / 2.0, big - width, big - width));
        }
        g.dispose();

        BufferedImage scaled = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        sg.drawImage(canvas, 0, 0, diameter, diameter, null);
        sg.dispose();
        return scaled;
    }

    /** The wordmark as white ink on transparency, already scaled to the disc. */
    private static BufferedImage ink(BufferedImage source, int field, int[] mark, int dw, int dh) {
        BufferedImage cropped = new BufferedImage(mark[2] - mark[0], mark[3] - mark[1],
                BufferedImage.TYPE_INT_ARGB);
        for (int y = mark[1]; y < mark[3]; y++) {
            for (int x = mark[0]; x < mark[2]; x++) {
                boolean on = differs(source.getRGB(x, y), field);
                cropped.setRGB(x - mark[0], y - mark[1], on ? 0xFFFFFFFF : 0);
            }
        }
        BufferedImage scaled = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cropped, 0, 0, dw, dh, null);
        g.dispose();
        return scaled;
    }

    private static boolean differs(int argb, int field) {
        int dr = Math.abs(((argb >> 16) & 0xFF) - ((field >> 16) & 0xFF));
        int dg = Math.abs(((argb >> 8) & 0xFF) - ((field >> 8) & 0xFF));
        int db = Math.abs((argb & 0xFF) - (field & 0xFF));
        return dr + dg + db > 60;
    }

    /** Bounding box of the wordmark: everything that is not the field colour. */
    private static int[] contentBox(BufferedImage image, int field) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (differs(image.getRGB(x, y), field)) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    /**
     * Bounding box of the subject, as the largest connected non-blue blob plus a margin.
     *
     * <p>The shadow is deliberately not detected here. A darkness threshold finds it, but it also
     * finds every bit of shading in the field - the first run returned 1712x956, essentially the whole
     * image - and the badge then covered everything. The shadow hugs the item, so a fixed margin
     * around the item's own box is both simpler and reliable.
     */
    private static int[] subjectBox(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[] mask = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int b = argb & 0xFF;
                mask[y * w + x] = (b - r) < 30 || (r - b) > 20;
            }
        }

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

        int margin = 40;
        return new int[]{Math.max(0, bestBox[0] - margin), Math.max(0, bestBox[1] - margin),
                Math.min(w, bestBox[2] + 1 + margin), Math.min(h, bestBox[3] + 1 + margin)};
    }

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
}
