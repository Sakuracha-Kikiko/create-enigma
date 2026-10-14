package com.createenigma.structure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.createenigma.registry.CEBlocks;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.CreatePaths;

import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Writes this mod's blueprint out to disk, where Create's own schematic system can find it.
 *
 * <h2>Why a file at all</h2>
 *
 * <p>{@code SchematicItem.loadSchematic} resolves an item's schematic by name against the game
 * directory - {@code <game>/schematics/<name>} on the client, {@code <game>/schematics/uploaded/
 * <owner>/<name>} on the server. A blueprint is only ever a name plus an owner; there is no way to
 * carry the structure inside the item. So a mod that wants to hand out a blueprint has to put the
 * file where Create looks.
 *
 * <h2>Why the subdirectory</h2>
 *
 * <p>{@link #RELATIVE_PATH} deliberately includes a directory. The schematic table builds its list
 * with {@code Files.list} - one level, not a walk - and drops anything that is a directory, so a
 * file below the root is <b>invisible in the table</b> while still resolving perfectly. That is the
 * difference between "a blueprint the player finds" and "an entry sitting in the list next to their
 * own builds".
 *
 * <p>This is not a workaround around a check: Create's own resolution explicitly permits
 * subdirectories. It only refuses paths that escape the schematics folder.
 *
 * <h2>Why it is generated rather than shipped as a file</h2>
 *
 * <p>The structure is written from {@link EnigmaStructure#entries(Generation)} - <b>the same list
 * the validator checks against</b>. A second .nbt shipped in the jar could drift from the template,
 * and the failure would be a blueprint that builds a machine the wrench refuses to read. Generated
 * from the one source, they cannot disagree.
 *
 * <p>Block entity data is deliberately omitted. The blueprint's job is to place the right blocks in
 * the right places, and that is all the validator looks at; the motors come up at their default
 * speed and the player tunes them, which is the same thing they would do after building by hand.
 */
public final class EnigmaSchematic {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Where the file goes, relative to the schematics folder. The directory is the hiding place. */
    public static final String RELATIVE_PATH = "enigma/first_generation.nbt";

    /**
     * The owner recorded on the blueprint item.
     *
     * <p>Normally a player name - schematics loaded through the table are copied into a folder named
     * after whoever uploaded them. Using the mod id instead means the server resolves straight to
     * our file without anything ever being uploaded.
     */
    public static final String OWNER = "create_enigma";

    private EnigmaSchematic() {}

    /**
     * Builds the blueprint into both locations Create reads from.
     *
     * <p>Both, because the two sides look in different places: the server prints the machine from
     * its copy, and the client draws the preview from its own. Writing only one produces a cannon
     * that either builds nothing visible or shows a ghost it cannot place. In single player both
     * paths are on this machine anyway, so doing both unconditionally is simpler than branching.
     *
     * <p>Overwritten every launch, so a change to the machine reaches existing saves.
     */
    public static void extract() {
        EnigmaStructure structure = EnigmaStructure.get();
        if (structure == null) {
            LOGGER.error("Enigma blueprint not written: the structure template could not be read");
            return;
        }

        CompoundTag schematic = build(structure);

        write(CreatePaths.SCHEMATICS_DIR.resolve(RELATIVE_PATH), schematic);
        write(CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(OWNER).resolve(RELATIVE_PATH), schematic);
    }

    /** The blueprint as NBT, in the shape vanilla's {@code StructureTemplate} reads. */
    public static CompoundTag build(EnigmaStructure structure) {
        List<EnigmaStructure.Entry> entries = structure.entries(Generation.PRIMAL);

        // One palette entry per distinct state, in first-seen order, so the file is deterministic
        // and two runs produce byte-identical output.
        Map<BlockState, Integer> seen = new LinkedHashMap<>();
        ListTag palette = new ListTag();
        ListTag blocks = new ListTag();

        for (EnigmaStructure.Entry entry : entries) {
            if (isPlayersOwnWork(entry.state())) {
                continue;
            }

            Integer index = seen.get(entry.state());
            if (index == null) {
                index = palette.size();
                seen.put(entry.state(), index);
                palette.add(NbtUtils.writeBlockState(entry.state()));
            }

            CompoundTag block = new CompoundTag();
            block.put("pos", intList(entry.offset().getX(), entry.offset().getY(), entry.offset().getZ()));
            block.putInt("state", index);
            blocks.add(block);
        }

        CompoundTag root = new CompoundTag();
        // Must match what the game expects for 1.21.1, or the structure is quietly rejected.
        root.putInt("DataVersion", 3955);
        root.put("size", intList(structure.size().getX(), structure.size().getY(), structure.size().getZ()));
        root.put("palette", palette);
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        return root;
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    /**
     * Blocks the blueprint deliberately leaves out, for the player to place themselves.
     *
     * <p>Two kinds, for two different reasons:
     *
     * <ul>
     *   <li><b>Belts.</b> Create's own cannon cannot place belt runs correctly - a known limitation
     *       of the schematic system, not of this machine. Shipping them would leave a machine full
     *       of subtly wrong belts, and fixing those by hand is worse than placing them by hand in
     *       the first place.</li>
     *   <li><b>Motors.</b> The machine needs six of them in the first generation and a survival
     *       player cannot obtain a real one. A blueprint showing a row of disguised motors would
     *       hand over the entire conceit at once: that they exist, that they go there, and that
     *       they stand in for something else. Left out, the machine gets to say it in its own
     *       voice - the wrench reports that nothing is missing except the motors, and adds that it
     *       is not picky any more.</li>
     * </ul>
     *
     * <p>Both are still required by {@link EnigmaStructure#entries(Generation)}: the blueprint
     * omits them, the validator does not. That gap is the player's work.
     */
    public static boolean isPlayersOwnWork(BlockState state) {
        return state.is(AllBlocks.BELT.get())
                || state.is(AllBlocks.CREATIVE_MOTOR.get())
                || state.is(CEBlocks.DISGUISED_MOTOR.get());
    }

    private static void write(Path target, CompoundTag schematic) {
        try {
            Files.createDirectories(target.getParent());
            NbtIo.writeCompressed(schematic, target);
        } catch (IOException e) {
            // Not fatal: everything except the blueprint keeps working without this file.
            LOGGER.error("Could not write the Enigma blueprint to {}", target, e);
        }
    }

    /** For diagnostics and tests: the block count the blueprint will contain. */
    public static List<EnigmaStructure.Entry> contents() {
        EnigmaStructure structure = EnigmaStructure.get();
        return structure == null ? new ArrayList<>() : structure.entries(Generation.PRIMAL);
    }

    /**
     * A blueprint item for this mod's machine, carrying every component Create's own factory sets.
     *
     * <p><b>All seven, not just the name.</b> A blueprint is not "a file reference" - the client
     * reads the extents from {@code SCHEMATIC_BOUNDS} to draw the outline and to place the ghost,
     * and {@code SchematicHandler} hands that value straight to a tool that calls
     * {@code getCenter()} on it. Setting only the file and the owner produced an item whose bounds
     * were null, and holding it crashed the game inside Create's tick loop before any of our code
     * ran again.
     *
     * <p>Built here rather than by calling {@code SchematicItem.create}, because that method needs
     * a {@code Level} to read the file for its size - and the creative tab builds its stacks with
     * no level available. The size is already known: it is the template's own.
     */
    public static ItemStack blueprintStack() {
        EnigmaStructure structure = EnigmaStructure.get();
        Vec3i size = structure == null ? Vec3i.ZERO : structure.size();

        ItemStack stack = new ItemStack(AllItems.SCHEMATIC.get());
        stack.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        stack.set(AllDataComponents.SCHEMATIC_OWNER, OWNER);
        stack.set(AllDataComponents.SCHEMATIC_FILE, RELATIVE_PATH);
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO);
        stack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
        stack.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE);
        stack.set(AllDataComponents.SCHEMATIC_BOUNDS, size);
        return stack;
    }
}
