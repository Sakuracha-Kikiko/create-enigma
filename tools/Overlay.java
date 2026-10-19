import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Puts the wordmark over a background at logo size.
 *
 * <p>The mark goes exactly where the repair is, which is deliberate: the repair can only be an
 * approximation - the artwork's cells each carry their own shading, so the occluded area cannot be
 * restored to what it "was", only to something plausible - and the wordmark covers it.
 *
 * <p>Usage: java Overlay.java &lt;background.png&gt; &lt;wordmark.png&gt; &lt;out.png&gt; [size]
 */
public final class Overlay {

    public static void main(String[] args) throws Exception {
        BufferedImage background = ImageIO.read(new File(args[0]));
        BufferedImage source = ImageIO.read(new File(args[1]));
        File out = new File(args[2]);
        int size = args.length > 3 ? Integer.parseInt(args[3]) : 256;

        BufferedImage ink = ink(source);

        // Central square of the background, scaled to the logo size.
        int side = Math.min(background.getWidth(), background.getHeight());
        int x0 = (background.getWidth() - side) / 2;
        int y0 = (background.getHeight() - side) / 2;
        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = square.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(background.getSubimage(x0, y0, side, side), 0, 0, size, size, null);

        double target = size * 0.78;
        double scale = Math.min(target / ink.getWidth(), target / ink.getHeight());
        int dw = Math.max(1, (int) Math.round(ink.getWidth() * scale));
        int dh = Math.max(1, (int) Math.round(ink.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        sg.drawImage(ink, 0, 0, dw, dh, null);
        sg.dispose();

        g.drawImage(scaled, (size - dw) / 2, (size - dh) / 2, null);
        g.dispose();

        ImageIO.write(square, "png", out);
        System.out.println("wrote " + out + " " + size + "x" + size + " (" + out.length() + " bytes)");
    }

    /** Everything that differs from the corner colour, as white ink. */
    private static BufferedImage ink(BufferedImage source) {
        int bg = source.getRGB(0, 0);
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (differs(source.getRGB(x, y), bg)) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                out.setRGB(x - minX, y - minY, differs(source.getRGB(x, y), bg) ? 0xFFFFFFFF : 0);
            }
        }
        return out;
    }

    private static boolean differs(int argb, int bg) {
        int dr = Math.abs(((argb >> 16) & 0xFF) - ((bg >> 16) & 0xFF));
        int dg = Math.abs(((argb >> 8) & 0xFF) - ((bg >> 8) & 0xFF));
        int db = Math.abs((argb & 0xFF) - (bg & 0xFF));
        return dr + dg + db > 24;
    }
}
