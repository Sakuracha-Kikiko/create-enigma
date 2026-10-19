import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Prints pixel samples on a grid, so thresholds can be chosen from data. */
public final class Px {
    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File(args[0]));
        int w = image.getWidth();
        int h = image.getHeight();
        System.out.println(w + "x" + h);
        int step = Math.max(1, Integer.parseInt(args.length > 1 ? args[1] : "120"));
        for (int y = step / 2; y < h; y += step) {
            StringBuilder line = new StringBuilder(String.format("y=%4d ", y));
            for (int x = step / 2; x < w; x += step) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                line.append(String.format("%3d/%3d (b-r %3d)  ", r, b, b - r));
            }
            System.out.println(line);
        }
    }
}
