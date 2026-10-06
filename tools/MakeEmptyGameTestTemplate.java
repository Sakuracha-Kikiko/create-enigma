import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the empty arena a GameTest runs inside.
 *
 * <p>NeoForge prefixes a test's template with the lowercased test class name, so
 * {@code @GameTestHolder("create_enigma")} on {@code EnigmaGameTests} plus
 * {@code @GameTest(template = "empty")} resolves to the structure id
 * {@code create_enigma:enigmagametests.empty} - which is exactly this file's name.
 *
 * <p>The volume is air: the tests build the machine themselves, and the machine is
 * 15x4x15, so the arena only has to be slightly larger than that.
 *
 * <p>usage: MakeEmptyGameTestTemplate &lt;out.nbt&gt; &lt;sx&gt; &lt;sy&gt; &lt;sz&gt;
 */
public class MakeEmptyGameTestTemplate {

    static final int DATA_VERSION = 3955; // Minecraft 1.21.1

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        int sx = Integer.parseInt(args[1]);
        int sy = Integer.parseInt(args[2]);
        int sz = Integer.parseInt(args[3]);

        out.getParentFile().mkdirs();
        try (DataOutputStream o = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(out)))) {

            o.writeByte(10);            // root compound
            writeName(o, "");

            o.writeByte(3);             // DataVersion : int
            writeName(o, "DataVersion");
            o.writeInt(DATA_VERSION);

            o.writeByte(9);             // size : list<int>
            writeName(o, "size");
            o.writeByte(3);
            o.writeInt(3);
            o.writeInt(sx);
            o.writeInt(sy);
            o.writeInt(sz);

            o.writeByte(9);             // palette : list<compound>
            writeName(o, "palette");
            o.writeByte(10);
            o.writeInt(1);
            o.writeByte(8);             //   { Name: "minecraft:air" }
            writeName(o, "Name");
            writeName(o, "minecraft:air");
            o.writeByte(0);

            o.writeByte(9);             // blocks : list<compound>, empty
            writeName(o, "blocks");
            o.writeByte(10);
            o.writeInt(0);

            o.writeByte(9);             // entities : list<compound>, empty
            writeName(o, "entities");
            o.writeByte(10);
            o.writeInt(0);

            o.writeByte(0);             // end root
        }

        System.out.println("wrote " + out.getAbsolutePath() + "  size=" + sx + "x" + sy + "x" + sz
                + "  (" + out.length() + " bytes)");
    }

    static void writeName(DataOutputStream o, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        o.writeShort(b.length);
        o.write(b);
    }
}
