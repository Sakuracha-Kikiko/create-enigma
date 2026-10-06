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

    public static Result validate(Level level, BlockPos corePos, EnigmaStructure structure) {
        BlockPos origin = structure.originFor(corePos);
        boolean sawUnloaded = false;

        for (EnigmaStructure.Entry entry : structure.entries()) {
            BlockPos pos = origin.offset(entry.offset());

            // isLoaded does not load the chunk; getBlockState would.
            if (!level.isLoaded(pos)) {
                sawUnloaded = true;
                continue;
            }

            BlockState actual = level.getBlockState(pos);
            if (!BlockStateMatcher.matches(entry.state(), actual)) {
                return new Result(Status.UNFORMED, pos.immutable(),
                        BlockStateMatcher.firstDifference(entry.state(), actual));
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
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(true);

        RandomSource random = level.getRandom();
        return template.get().placeInWorld(level, origin, origin, settings, random, Block.UPDATE_ALL);
    }

    /** Convenience for callers that already hold a {@link ServerLevelAccessor}. */
    public static boolean place(ServerLevelAccessor level, BlockPos corePos) {
        return level.getLevel() instanceof ServerLevel serverLevel && place(serverLevel, corePos);
    }
}
