package com.createenigma.structure;

import java.util.Optional;

import javax.annotation.Nullable;

import com.createenigma.registry.CEBlocks;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.simibubi.create.AllBlocks;

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
 *
 * <p>Two entry points sit on top of one scan: {@link #validate} answers "is it finished", and
 * {@link #analyse} answers "how far along is it" for the wrench, which has to say something useful
 * about a machine that is nowhere near finished.
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

    /**
     * How far along a machine is - for feedback rather than for a yes/no answer.
     *
     * @param placed    how many template positions hold the right block, in the best orientation
     * @param total     how many positions the template has
     * @param motorGaps how many of the missing ones are motor positions
     * @param firstGap  where the first missing block is, for the precise readout
     */
    public record Report(int placed, int total, int motorGaps, @Nullable BlockPos firstGap,
                         String detail) {

        public boolean isComplete() {
            return placed >= total;
        }

        /**
         * Nothing is wrong except the motors - the one thing a survival player cannot craft.
         *
         * <p>Worth telling apart from every other kind of unfinished, because it is the state where
         * the player has run out of things they can do rather than run out of blocks.
         */
        public boolean onlyMotorsMissing() {
            return !isComplete() && motorGaps > 0 && motorGaps == total - placed;
        }
    }

    private static final Result FORMED = new Result(Status.FORMED, null, "");

    /**
     * The four horizontal orientations, tried in this order.
     *
     * <p>{@code NONE} is first because it is what the ponder shows and what
     * {@code /place template} produces, so the common case costs exactly one pass. A machine that
     * has been turned pays for the passes it fails, and each pass stops counting only at the end -
     * but it stops <em>reading</em> at the first gap it can report.
     */
    private static final Rotation[] ORIENTATIONS = {
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };

    /** One pass over the template in one orientation. */
    private record Scan(boolean sawUnloaded, int placed, int motorGaps, @Nullable BlockPos firstGap,
                        String detail) {}

    public static Result validate(Level level, BlockPos anchorPos, EnigmaStructure structure) {
        return validate(level, anchorPos, structure, Generation.COMPLETE);
    }

    /**
     * Checks the world against every horizontal orientation of the template, and reports FORMED if
     * any of them matches in full.
     *
     * <p><b>The machine is a building, and buildings get turned.</b> A 15x15 machine will not fit in
     * every spot, so a player who finds room for it only sideways has built a correct machine.
     * Refusing that would not just be unhelpful, it would be actively misleading: the mismatch
     * report names a position and a block, and for a turned machine those coordinates correspond to
     * nothing the player built.
     *
     * <p>The precedence between the two failure kinds is the same as before, extended across
     * orientations: UNKNOWN beats UNFORMED. A single orientation that could not be read in full
     * might have been the right one, and the existing rule is that uncertainty never tears a
     * machine down.
     */
    public static Result validate(Level level, BlockPos anchorPos, EnigmaStructure structure,
                                  Generation generation) {
        Result unknown = null;
        Result unformed = null;
        int total = structure.entries(generation).size();

        for (Rotation rotation : ORIENTATIONS) {
            Scan scan = scan(level, anchorPos, structure, generation, rotation);

            if (!scan.sawUnloaded()) {
                if (scan.placed() == total) {
                    return FORMED;
                }
                if (unformed == null) {
                    // The first orientation to be definitely wrong, which is NONE when it is wrong
                    // at all - so the coordinates in the message stay in the machine's own frame.
                    unformed = new Result(Status.UNFORMED, scan.firstGap(), scan.detail());
                }
            } else if (unknown == null) {
                unknown = new Result(Status.UNKNOWN, null, "part of the machine is not loaded");
            }
        }

        return unknown != null ? unknown : unformed;
    }

    /**
     * How far along the machine is, in whichever orientation fits it best.
     *
     * <p>Used by the wrench, which has to say something about a machine that is nowhere near
     * finished - and "which block is wrong" is the wrong answer for someone who has laid five
     * blocks. The orientation with the most correct blocks wins, so a turned build reads as its
     * real progress rather than as a pile of mistakes.
     */
    public static Report analyse(Level level, BlockPos anchorPos, EnigmaStructure structure,
                                 Generation generation) {
        int total = structure.entries(generation).size();
        Scan best = null;

        for (Rotation rotation : ORIENTATIONS) {
            Scan scan = scan(level, anchorPos, structure, generation, rotation);
            if (best == null || scan.placed() > best.placed()) {
                best = scan;
            }
            if (best.placed() == total) {
                break;
            }
        }

        // There are four orientations, so best is never null here.
        return new Report(best.placed(), total, best.motorGaps(), best.firstGap(), best.detail());
    }

    /**
     * One full pass in one orientation.
     *
     * <p>A position counts as placed only when its block matches. A motor position that is empty or
     * wrong is additionally counted on its own, because "everything is here except the motors" is a
     * state the wrench reports differently from any other kind of unfinished - it is the point at
     * which a survival player has done all they can.
     *
     * <p>The whole template is walked rather than stopping at the first gap: the count is the
     * answer being asked for. Unloaded positions are skipped and flagged, never loaded.
     */
    private static Scan scan(Level level, BlockPos anchorPos, EnigmaStructure structure,
                             Generation generation, Rotation rotation) {
        BlockPos origin = structure.originFor(anchorPos);
        var entries = structure.entries(generation);

        boolean sawUnloaded = false;
        int placed = 0;
        int motorGaps = 0;
        BlockPos firstGap = null;
        String detail = "";

        for (EnigmaStructure.Entry entry : entries) {
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
            if (BlockStateMatcher.matches(expected, actual)) {
                placed++;
                continue;
            }

            if (isMotor(expected)) {
                motorGaps++;
            }
            if (firstGap == null) {
                firstGap = pos.immutable();
                detail = BlockStateMatcher.firstDifference(expected, actual);
            }
        }

        return new Scan(sawUnloaded, placed, motorGaps, firstGap, detail);
    }

    /**
     * Whether a template position is one of the motors.
     *
     * <p>Both kinds count: the complete template records Create's Creative Motor and the first
     * generation records this mod's disguised copy, and from the player's side they are the same
     * slot either way.
     */
    private static boolean isMotor(BlockState state) {
        return state.is(AllBlocks.CREATIVE_MOTOR.get()) || state.is(CEBlocks.DISGUISED_MOTOR.get());
    }

    /** Convenience for callers that already hold a {@link ServerLevelAccessor}. */
    public static boolean place(ServerLevelAccessor level, BlockPos corePos) {
        return level.getLevel() instanceof ServerLevel serverLevel && place(serverLevel, corePos);
    }

    public static boolean place(ServerLevel level, BlockPos corePos) {
        return place(level, corePos, Rotation.NONE);
    }

    /**
     * Builds the machine into the world, turned by the given amount.
     *
     * <p><b>{@code setRotationPivot} is what makes the anchor stay put.</b> Vanilla rotates around
     * {@code BlockPos.ZERO} unless told otherwise, so without it the whole machine orbits the
     * template's corner and the anchor lands somewhere else entirely - which is not a subtle
     * failure: the block that was supposed to be the core is simply air, and nothing has an anchor.
     *
     * <p>With the pivot at the anchor offset, the anchor maps to itself under every rotation (it is
     * the fixed point of the transform), so the origin computed for the unrotated template is also
     * the origin for every turned one, and no pivot arithmetic is needed here.
     *
     * <p>This has to agree exactly with {@link EnigmaStructure#offsetFor}, which is what the
     * validator uses. {@code transform}'s formulas reduce to the same three sign flips:
     * {@code (x, z) -> (-z, x)}, {@code (-x, -z)} and {@code (z, -x)}. The rotation tests place
     * through vanilla and validate through ours precisely so the two cannot drift apart unnoticed.
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
}
