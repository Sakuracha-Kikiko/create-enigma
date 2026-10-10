package com.createenigma.structure;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Compares the world against the Enigma template.
 *
 * <p>The single most important rule here is the difference between <b>UNFORMED</b> and
 * <b>UNKNOWN</b>. A machine this wide can straddle chunk borders, and reading a block in an
 * unloaded chunk would force that chunk to load - so chunks are never touched unless they are
 * already loaded, and a position that could not be read makes the whole answer UNKNOWN rather than
 * UNFORMED. Callers must leave the formed state alone on UNKNOWN; otherwise walking past the
 * machine would tear it down and rebuilding it would be a lottery.
 */
public final class EnigmaValidator {

    private EnigmaValidator() {}

    public enum Status {
        /** Every block matches. */
        FORMED,
        /** At least one loaded block does not match, so the machine is definitely not intact. */
        UNFORMED,
        /** Part of the machine is in an unloaded chunk; nothing can be concluded right now. */
        UNKNOWN
    }

    /**
     * @param failingPos the first mismatching position, when the status is UNFORMED
     * @param detail     what was wrong there, for player feedback and test output
     */
    public record Result(Status status, @Nullable BlockPos failingPos, String detail) {

        public boolean isFormed() {
            return status == Status.FORMED;
        }
    }

    private static final Result FORMED = new Result(Status.FORMED, null, "");
    private static final Result UNKNOWN = new Result(Status.UNKNOWN, null, "part of the machine is not loaded");

    /**
     * The four horizontal orientations, tried in this order.
     *
     * <p>{@code NONE} is first because it is what the ponder shows and what
     * {@code /place template} produces, so the common case costs exactly one pass. A machine that
     * has been turned pays for the passes it fails, and each pass stops at its first mismatching
     * block - for a machine turned 90 degrees that is usually within the first few entries.
     */
    private static final Rotation[] ORIENTATIONS = {
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };

    /**
     * Checks the world against every horizontal orientation of the template, and reports FORMED if
     * any of them matches in full.
     *
     * <p><b>The machine is a building, and buildings get turned.</b> A 15x15 machine will not fit
     * in every spot, so a player who finds room for it only sideways has built a correct machine.
     * Refusing that would not just be unhelpful, it would be actively misleading: the mismatch
     * report names a position and a block, and for a turned machine those coordinates correspond to
     * nothing the player built. The mod's one diagnostic would turn into noise exactly when it is
     * needed.
     *
     * <p>The precedence between the two failure kinds is the same as before, extended across
     * orientations: UNKNOWN beats UNFORMED. A single orientation that could not be read in full
     * might have been the right one, and the existing rule is that uncertainty never tears a
     * machine down.
     */
    public static Result validate(Level level, BlockPos corePos, EnigmaStructure structure) {
        Result unknown = null;
        Result unformed = null;

        for (Rotation rotation : ORIENTATIONS) {
            Result result = validate(level, corePos, structure, rotation);
            if (result.isFormed()) {
                return result;
            }
            if (result.status() == Status.UNKNOWN) {
                if (unknown == null) {
                    unknown = result;
                }
            } else if (unformed == null) {
                // The first orientation to be definitely wrong, which is NONE when it is wrong at
                // all - so the coordinates in the message stay in the machine's own frame.
                unformed = result;
            }
        }

        return unknown != null ? unknown : unformed;
    }

    private static Result validate(Level level, BlockPos corePos, EnigmaStructure structure, Rotation rotation) {
        BlockPos origin = structure.originFor(corePos);
        boolean sawUnloaded = false;

        for (EnigmaStructure.Entry entry : structure.entries()) {
            BlockPos pos = origin.offset(structure.offsetFor(entry.offset(), rotation));

            // isLoaded does not load the chunk; getBlockState would.
            if (!level.isLoaded(pos)) {
                sawUnloaded = true;
                continue;
            }

            // The expected state has to turn with the machine, or a turned build would match on
            // position alone while every belt and gasket faced the wrong way.
            //
            // The level-aware overload rather than the one-argument one, which is deprecated
            // precisely because some blocks need their neighbours to answer this.
            BlockState expected = rotation == Rotation.NONE
                    ? entry.state()
                    : entry.state().rotate(level, pos, rotation);

            BlockState actual = level.getBlockState(pos);
            if (!BlockStateMatcher.matches(expected, actual)) {
                return new Result(Status.UNFORMED, pos.immutable(),
                        BlockStateMatcher.firstDifference(expected, actual));
            }
        }

        return sawUnloaded ? UNKNOWN : FORMED;
    }

    /**
     * Builds the machine into the world.
     *
     * <p>Goes through vanilla's own template placement rather than setting blocks from
     * {@link EnigmaStructure}, because only that path creates block entities and lets Create link
     * its belts and kinetic networks - which is exactly what the structure has to look like for the
     * validation above to be a fair test of it. It is also the same path {@code /place template}
     * takes, so the two cannot diverge.
     */
    public static boolean place(ServerLevel level, BlockPos corePos) {
        return place(level, corePos, Rotation.NONE);
    }

    /**
     * Builds the machine into the world, turned by the given amount.
     *
     * <p><b>{@code setRotationPivot} is what makes the core stay put.</b> Vanilla rotates around
     * {@code BlockPos.ZERO} unless told otherwise, so without it the whole machine orbits the
     * template's corner and the core lands somewhere else entirely - which is not a subtle failure:
     * the block that was supposed to be the core is simply air, and the validator has no anchor.
     *
     * <p>With the pivot at the core offset, the core maps to itself under every rotation (it is the
     * fixed point of the transform), so the origin computed for the unrotated template is also the
     * origin for every turned one, and no pivot arithmetic is needed here.
     *
     * <p>This has to agree exactly with {@link EnigmaStructure#offsetFor}, which is what the
     * validator uses. {@code transform}'s formulas reduce to the same three sign flips:
     * {@code (x, z) -> (-z, x)}, {@code (-x, -z)} and {@code (z, -x)}. {@code turnedMachinesAre}
     * {@code Recognised} and its two siblings place through vanilla and validate through ours
     * precisely so the two cannot drift apart unnoticed.
     */
    public static boolean place(ServerLevel level, BlockPos corePos, Rotation rotation) {
        StructureTemplateManager manager = level.getServer().getStructureManager();
        Optional<StructureTemplate> template = manager.get(EnigmaStructure.TEMPLATE_ID);
        if (template.isEmpty()) {
            return false;
        }

        EnigmaStructure structure = EnigmaStructure.get();
        if (structure == null) {
            return false;
        }

        BlockPos origin = structure.originFor(corePos);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setRotationPivot(structure.coreOffset())
                .setIgnoreEntities(true);

        RandomSource random = level.getRandom();
        return template.get().placeInWorld(level, origin, origin, settings, random, Block.UPDATE_ALL);
    }

    /** Convenience for callers that already hold a {@link ServerLevelAccessor}. */
    public static boolean place(ServerLevelAccessor level, BlockPos corePos) {
        return level.getLevel() instanceof ServerLevel serverLevel && place(serverLevel, corePos);
    }
}
