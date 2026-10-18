import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Regenerates this mod's Enigma Wrench texture from Create's own wrench texture.
 *
 * <p>Not shipped in the jar - it lives here so the derived asset can be reproduced or retuned rather
 * than being an unexplained binary someone has to reverse.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *   # 1. take the source texture out of Create's jar
 *   jar xf create-1.21.1-6.0.10.jar assets/create/textures/item/wrench.png
 *   # 2. produce the 16x16 result and a 16x nearest-neighbour preview
 *   java RecolourWrenchTexture.java assets/create/textures/item/wrench.png \
 *        out/enigma_wrench.png out/preview.png
 * </pre>
 *
 * <h2>Why a recolour at all</h2>
 *
 * <p>Create's wrench texture is a 16x16 <b>palette</b>, not a picture: the 3D shape lives entirely in
 * the model's boxes and the texture only supplies a colour for each UV island. Recolouring therefore
 * keeps every UV island exactly where the model expects it - only the hue changes - which is why this
 * works at all without touching geometry.
 *
 * <p>The mapping rewrites the original's warm span (gold into brown, around 0.04-0.17 of the hue
 * circle) onto violet and magenta-violet, preserving each pixel's relative position inside that span
 * so shading, highlights and the darker recesses survive. Near-grey pixels are left alone, so the
 * metal stays metal instead of going purple.
 */
public final class RecolourWrenchTexture {

    /** The original's warm span: gold plate, darker gold, brown. */
    private static final float WARM_LOW = 0.04f;
    private static final float WARM_HIGH = 0.17f;

    /** Violet to magenta-violet - the Enigma core's range, and the creative-item look. */
    private static final float TARGET_LOW = 0.72f;
    private static final float TARGET_HIGH = 0.86f;

    /** Below this saturation a pixel counts as metal and is left untouched. */
    private static final float METAL_SATURATION = 0.15f;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: RecolourWrenchTexture <source.png> <result.png> <preview.png>");
            return;
        }

        BufferedImage in = ImageIO.read(new File(args[0]));
        int width = in.getWidth();
        int height = in.getHeight();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int recoloured = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = in.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    out.setRGB(x, y, 0);
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
                if (hsb[1] >= METAL_SATURATION) {
                    float span = Math.min(1f, Math.max(0f, (hsb[0] - WARM_LOW) / (WARM_HIGH - WARM_LOW)));
                    float hue = TARGET_LOW + span * (TARGET_HIGH - TARGET_LOW);
                    int rgb = java.awt.Color.HSBtoRGB(hue, hsb[1], hsb[2]);
                    out.setRGB(x, y, (alpha << 24) | (rgb & 0xFFFFFF));
                    recoloured++;
                } else {
                    out.setRGB(x, y, argb);
                }
            }
        }

        File resultFile = new File(args[1]);
        if (resultFile.getParentFile() != null) {
            resultFile.getParentFile().mkdirs();
        }
        ImageIO.write(out, "png", resultFile);

        // 16x16 is too small to judge on screen; nearest-neighbour keeps the pixels honest.
        int scale = 16;
        BufferedImage preview = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height * scale; y++) {
            for (int x = 0; x < width * scale; x++) {
                preview.setRGB(x, y, out.getRGB(x / scale, y / scale));
            }
        }
        ImageIO.write(preview, "png", new File(args[2]));

        System.out.println("read " + width + "x" + height + ", recoloured " + recoloured + " pixels");
    }
}
