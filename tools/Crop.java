import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Crops a rectangle and optionally scales it, for looking at a repair closely. */
public final class Crop {
    public static void main(String[] args) throws Exception {
        BufferedImage in = ImageIO.read(new File(args[0]));
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int w = Integer.parseInt(args[3]);
        int h = Integer.parseInt(args[4]);
        int scale = args.length > 5 ? Integer.parseInt(args[5]) : 1;

        x = Math.max(0, Math.min(in.getWidth() - 1, x));
        y = Math.max(0, Math.min(in.getHeight() - 1, y));
        w = Math.min(w, in.getWidth() - x);
        h = Math.min(h, in.getHeight() - y);

        BufferedImage out = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(in.getSubimage(x, y, w, h), 0, 0, w * scale, h * scale, null);
        g.dispose();
        ImageIO.write(out, "png", new File(args[6]));
        System.out.println("wrote " + args[6] + " " + out.getWidth() + "x" + out.getHeight());
    }
}
