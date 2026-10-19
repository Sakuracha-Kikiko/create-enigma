import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Removes one block from a structure .nbt.
 *
 * <p>Reads and writes the tag tree without touching anything it was not asked to change, and in
 * particular keeps each value's <b>type</b>: a structure's "size" and each block's "pos" are lists of
 * ints, and writing them as int arrays makes the game read nothing and load an empty template - which
 * is silent, and looks like the file is fine.
 *
 * <p>The palette is left alone; an unused entry is harmless.
 *
 * <p>Usage: java RemoveBlock.java &lt;in.nbt&gt; &lt;x,y,z&gt; [expectedBlockId] &lt;out.nbt&gt;
 */
public final class RemoveBlock {

    static String readName(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    static Object readTag(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { int n = in.readInt(); byte[] b = new byte[n]; in.readFully(b); yield b; }
            case 8 -> readName(in);
            case 9 -> {
                int itemType = in.readUnsignedByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(readTag(in, itemType));
                yield list;
            }
            case 10 -> {
                Map<String, Object> map = new LinkedHashMap<>();
                while (true) {
                    int t = in.readUnsignedByte();
                    if (t == 0) break;
                    map.put(readName(in), readTag(in, t));
                }
                yield map;
            }
            case 11 -> { int n = in.readInt(); int[] a = new int[n];
                         for (int i = 0; i < n; i++) a[i] = in.readInt(); yield a; }
            case 12 -> { int n = in.readInt(); long[] a = new long[n];
                         for (int i = 0; i < n; i++) a[i] = in.readLong(); yield a; }
            default -> throw new IOException("unknown tag type " + type);
        };
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readRoot(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            int type = in.readUnsignedByte();
            readName(in);
            if (type != 10) throw new IOException("root is TAG" + type);
            return (Map<String, Object>) readTag(in, 10);
        }
    }

    static int typeOf(Object value) throws IOException {
        if (value instanceof Byte) return 1;
        if (value instanceof Short) return 2;
        if (value instanceof Integer) return 3;
        if (value instanceof Long) return 4;
        if (value instanceof Float) return 5;
        if (value instanceof Double) return 6;
        if (value instanceof byte[]) return 7;
        if (value instanceof String) return 8;
        if (value instanceof List) return 9;
        if (value instanceof Map) return 10;
        if (value instanceof int[]) return 11;
        if (value instanceof long[]) return 12;
        throw new IOException("cannot type " + value.getClass());
    }

    static void writeName(DataOutputStream out, String name) throws IOException {
        byte[] bytes = name.getBytes("UTF-8");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    static void writePayload(DataOutputStream out, Object value) throws IOException {
        if (value instanceof Byte v) { out.writeByte(v); return; }
        if (value instanceof Short v) { out.writeShort(v); return; }
        if (value instanceof Integer v) { out.writeInt(v); return; }
        if (value instanceof Long v) { out.writeLong(v); return; }
        if (value instanceof Float v) { out.writeFloat(v); return; }
        if (value instanceof Double v) { out.writeDouble(v); return; }
        if (value instanceof byte[] v) { out.writeInt(v.length); out.write(v); return; }
        if (value instanceof String v) { writeName(out, v); return; }
        if (value instanceof int[] v) { out.writeInt(v.length); for (int i : v) out.writeInt(i); return; }
        if (value instanceof long[] v) { out.writeInt(v.length); for (long l : v) out.writeLong(l); return; }
        if (value instanceof List<?> list) {
            out.writeByte(list.isEmpty() ? 0 : typeOf(list.get(0)));
            out.writeInt(list.size());
            for (Object element : list) writePayload(out, element);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                out.writeByte(typeOf(entry.getValue()));
                writeName(out, (String) entry.getKey());
                writePayload(out, entry.getValue());
            }
            out.writeByte(0);
            return;
        }
        throw new IOException("cannot write " + value.getClass());
    }

    static void writeRoot(File file, Map<String, Object> root) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeByte(10);
        out.writeShort(0);
        writePayload(out, root);
        out.flush();
        try (DataOutputStream fileOut =
                new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            fileOut.write(buffer.toByteArray());
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: RemoveBlock <in.nbt> <x,y,z> [blockId] <out.nbt>");
            return;
        }
        File in = new File(args[0]);
        String[] pos = args[1].split(",");
        int x = Integer.parseInt(pos[0].trim());
        int y = Integer.parseInt(pos[1].trim());
        int z = Integer.parseInt(pos[2].trim());
        String expected = args.length > 4 ? args[2] : null;
        File out = new File(args[args.length - 1]);

        Map<String, Object> root = readRoot(in);
        List<Object> palette = (List<Object>) root.get("palette");
        List<Object> blocks = (List<Object>) root.get("blocks");

        List<Object> kept = new ArrayList<>(blocks.size());
        String removed = null;
        for (Object element : blocks) {
            Map<String, Object> block = (Map<String, Object>) element;
            int[] at = triple(block.get("pos"));
            if (at[0] == x && at[1] == y && at[2] == z) {
                int state = ((Number) block.get("state")).intValue();
                removed = (String) ((Map<String, Object>) palette.get(state)).get("Name");
                continue;
            }
            kept.add(element);
        }

        if (removed == null) {
            System.out.println("no block at " + x + "," + y + "," + z + " - nothing written");
            return;
        }
        if (expected != null && !expected.equals(removed)) {
            System.out.println("refusing: the block at " + x + "," + y + "," + z + " is " + removed
                    + ", not " + expected);
            return;
        }

        root.put("blocks", kept);
        writeRoot(out, root);

        System.out.println("removed " + removed + " at " + x + "," + y + "," + z
                + "; " + blocks.size() + " -> " + kept.size() + " blocks");
        System.out.println("wrote " + out + " (" + out.length() + " bytes)");
    }

    /** Reads a position whether it was stored as an int array or as a list of ints. */
    static int[] triple(Object value) {
        if (value instanceof int[] array) return array;
        List<?> list = (List<?>) value;
        return new int[]{
                ((Number) list.get(0)).intValue(),
                ((Number) list.get(1)).intValue(),
                ((Number) list.get(2)).intValue()};
    }
}
