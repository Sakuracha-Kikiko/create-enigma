import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Turns Create's Ponder set piece into a structure template this mod can ship.
 *
 * <p>Input : assets/create/ponder/creative_motor_mojang.nbt  (Create's own ponder capture)
 * <p>Output: data/create_enigma/structure/mojang_enigma.nbt  (a placable structure template)
 *
 * <p>Both files use the exact same NBT layout (verified against Create's own 1.21.1 structure
 * files: size = list&lt;Int&gt;[3], palette = list&lt;compound&gt;, blocks[].pos = list&lt;Int&gt;[3],
 * blocks[].state = int), so this is a pure transform, not a reformat.
 *
 * <p>What it changes, and why:
 * <ul>
 *   <li><b>Drops the y=0 layer</b> - 225 blocks of snow/white concrete checkerboard. That is
 *       Ponder's baseplate, not part of the machine; the ponder scene never reveals it either
 *       (it calls showBasePlate() and only ever shows y&gt;=1).</li>
 *   <li><b>Drops (8,3,9)</b> - a spruce slab the scene never reveals through any showSection
 *       call, so it is not part of the structure the player is shown.</li>
 *   <li><b>Shifts y down by one</b> so the machine sits flush on the ground.</li>
 *   <li><b>Replaces the chest at (7,3,8) with this mod's core block</b> - the anchor the
 *       multiblock hangs off. That cell is the machine's receiving end: the (7,2,8) funnel
 *       points up into it and the (8,3,8) hopper feeds west into it.</li>
 *   <li><b>Sanitises block entity data.</b> The ponder capture stores live kinetic network
 *       cross-references (Network id/size, Source, Controller, Length, Index, ...) that point at
 *       coordinates in the ponder's own virtual world. Those are recomputed on placement, so only
 *       the creative motors keep anything, and only their speed settings.</li>
 *   <li><b>Sets DataVersion to 3955</b> (Minecraft 1.21.1). The capture is 2975 (1.19-era); leaving
 *       it would send the palette through data fixers on load.</li>
 * </ul>
 */
public class PrepareEnigmaStructure {

    /** Where the core block goes, in the ORIGINAL ponder coordinates. */
    static final int CORE_X = 7, CORE_Y = 3, CORE_Z = 8;

    /**
     * There is no skip list.
     *
     * <p>An earlier revision dropped the spruce slab at (8,3,9), on the belief that the ponder
     * scene never reveals it. That was a misreading: the scene's step 19 is
     * {@code showSection(fromTo(7, 3, 9, 8, 3, 8))}, and {@code fromTo} is an inclusive box, so it
     * covers x in [7,8] x z in [8,9] - all four interior cells of the shed floor, the slab
     * included. Dropping it left a one-block hole in that floor for no reason at all.
     */

    /** DataVersion of Minecraft 1.21.1. */
    static final int DATA_VERSION = 3955;

    static final String CORE_BLOCK = "create_enigma:enigma_core";

    // ---------------------------------------------------------------- NBT reader

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

    static String readName(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, "UTF-8");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readRoot(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(f)))) {
            int type = in.readUnsignedByte();
            readName(in);
            if (type != 10) throw new IOException("root is TAG" + type + ", expected compound");
            return (Map<String, Object>) readTag(in, 10);
        }
    }

    // ---------------------------------------------------------------- NBT writer

    static int tagType(Object v) {
        if (v instanceof Byte) return 1;
        if (v instanceof Short) return 2;
        if (v instanceof Integer) return 3;
        if (v instanceof Long) return 4;
        if (v instanceof Float) return 5;
        if (v instanceof Double) return 6;
        if (v instanceof byte[]) return 7;
        if (v instanceof String) return 8;
        if (v instanceof List) return 9;
        if (v instanceof Map) return 10;
        if (v instanceof int[]) return 11;
        throw new IllegalArgumentException("unsupported payload " + v.getClass());
    }

    static void writeTag(DataOutputStream o, String name, Object v) throws IOException {
        int type = tagType(v);
        o.writeByte(type);
        writeName(o, name);
        writePayload(o, type, v);
    }

    static void writePayload(DataOutputStream o, int type, Object v) throws IOException {
        switch (type) {
            case 1 -> o.writeByte((Byte) v);
            case 2 -> o.writeShort((Short) v);
            case 3 -> o.writeInt((Integer) v);
            case 4 -> o.writeLong((Long) v);
            case 5 -> o.writeFloat((Float) v);
            case 6 -> o.writeDouble((Double) v);
            case 7 -> { byte[] b = (byte[]) v; o.writeInt(b.length); o.write(b); }
            case 8 -> writeName(o, (String) v);
            case 9 -> {
                List<?> list = (List<?>) v;
                if (list.isEmpty()) throw new IOException("cannot infer element type of an empty list");
                int elem = tagType(list.get(0));
                o.writeByte(elem);
                o.writeInt(list.size());
                for (Object e : list) writePayload(o, elem, e);
            }
            case 10 -> {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet())
                    writeTag(o, (String) e.getKey(), e.getValue());
                o.writeByte(0);
            }
            case 11 -> { int[] a = (int[]) v; o.writeInt(a.length); for (int i : a) o.writeInt(i); }
            default -> throw new IOException("cannot write tag type " + type);
        }
    }

    static void writeName(DataOutputStream o, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        o.writeShort(b.length);
        o.write(b);
    }

    static void writeRoot(File f, Map<String, Object> root) throws IOException {
        f.getParentFile().mkdirs();
        try (DataOutputStream o = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(f)))) {
            writeTag(o, "", root);
        }
    }

    // ---------------------------------------------------------------- helpers

    static int[] asIntTriple(Object o) {
        if (o instanceof int[] a) return a;
        if (o instanceof List<?> l) {
            int[] a = new int[l.size()];
            for (int i = 0; i < a.length; i++) a[i] = ((Number) l.get(i)).intValue();
            return a;
        }
        throw new IllegalArgumentException("not an int triple: " + o);
    }

    static String blockNameOf(Map<String, Object> state) {
        return String.valueOf(state.get("Name"));
    }

    /** The core block's state, as it would be written by a structure capture. */
    static Map<String, Object> coreState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("Name", CORE_BLOCK);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("assembled", "false");
        state.put("Properties", props);
        return state;
    }

    /**
     * Keeps a creative motor's speed settings and nothing else.
     *
     * <p>Everything else either is recomputed when the structure is placed (belt chain links,
     * kinetic network membership) or points at coordinates from the ponder's virtual world.
     */
    static Map<String, Object> sanitiseBlockEntity(Map<String, Object> beMap,
                                                   Map<String, Object> state) {
        if (beMap == null) return null;
        String name = blockNameOf(state);
        if (!name.equals("create:creative_motor")) return null;

        Map<String, Object> kept = new LinkedHashMap<>();
        for (String key : new String[]{"id", "Speed", "ScrollValue"})
            if (beMap.containsKey(key)) kept.put(key, beMap.get(key));
        kept.putIfAbsent("id", "create:motor");
        return kept;
    }

    static int intern(List<Map<String, Object>> palette, Map<String, Integer> index,
                      Map<String, Object> state) {
        String key = state.toString();
        Integer existing = index.get(key);
        if (existing != null) return existing;
        int id = palette.size();
        palette.add(state);
        index.put(key, id);
        return id;
    }

    // ---------------------------------------------------------------- main

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: PrepareEnigmaStructure <ponder.nbt> <out.nbt>");
            System.exit(2);
        }
        File in = new File(args[0]);
        File out = new File(args[1]);

        Map<String, Object> root = readRoot(in);
        int[] size = asIntTriple(root.get("size"));
        List<Map<String, Object>> palette = (List<Map<String, Object>>) root.get("palette");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) root.get("blocks");

        System.out.println("in : " + in.getName() + "  size=" + Arrays.toString(size)
                + "  palette=" + palette.size() + "  blocks=" + blocks.size()
                + "  DataVersion=" + root.get("DataVersion"));

        List<Map<String, Object>> newPalette = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        List<Map<String, Object>> newBlocks = new ArrayList<>();

        int droppedBase = 0, replaced = 0, motorsKept = 0;

        for (Map<String, Object> block : blocks) {
            int[] pos = asIntTriple(block.get("pos"));
            int x = pos[0], y = pos[1], z = pos[2];

            if (y == 0) { droppedBase++; continue; }

            Map<String, Object> state;
            Map<String, Object> be;
            if (x == CORE_X && y == CORE_Y && z == CORE_Z) {
                state = coreState();
                be = null;                       // a fresh block entity validates on placement
                replaced++;
            } else {
                state = palette.get(((Number) block.get("state")).intValue());
                be = sanitiseBlockEntity((Map<String, Object>) block.get("nbt"), state);
                if (be != null) motorsKept++;
            }

            Map<String, Object> written = new LinkedHashMap<>();
            written.put("pos", new ArrayList<>(List.of(x, y - 1, z)));
            written.put("state", intern(newPalette, index, state));
            if (be != null) written.put("nbt", be);
            newBlocks.add(written);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("size", new ArrayList<>(List.of(size[0], size[1] - 1, size[2])));
        result.put("palette", newPalette);
        result.put("blocks", newBlocks);
        result.put("DataVersion", DATA_VERSION);

        writeRoot(out, result);

        System.out.println("out: " + out.getAbsolutePath());
        System.out.println("  size        = " + Arrays.toString(new int[]{size[0], size[1] - 1, size[2]}));
        System.out.println("  dropped     = " + droppedBase + " baseplate (y=0)");
        System.out.println("  replaced    = " + replaced + " block -> " + CORE_BLOCK);
        System.out.println("  motor BEs   = " + motorsKept + " kept (id/Speed/ScrollValue only)");
        System.out.println("  new palette = " + newPalette.size() + "   new blocks = " + newBlocks.size());
    }
}
