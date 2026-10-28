package com.createenigma.content;

import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.createenigma.structure.EnigmaValidator;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Holds the formed/unformed state of the nearby machine and keeps it honest.
 *
 * <p><b>Why a periodic check rather than a block-break listener.</b> The machine is 82 blocks, none
 * of which belong to this mod, so there is nowhere on them to record "I am part of the Enigma" and
 * no event that reliably fires for all the ways a block can leave: mining, an explosion, a piston,
 * or a Create contraption carrying it off. Re-reading the shape on a timer catches every one of
 * those with the same code path, and 82 block state lookups once a second is not a cost worth
 * optimising away for a structure that is by definition a single showpiece.
 *
 * <p>The check runs on the server only, and it never loads a chunk to answer - see
 * {@link EnigmaValidator}.
 */
public class EnigmaCoreBlockEntity extends BlockEntity {

    /** Ticks to wait after something changed before looking; coalesces a burst of updates. */
    private static final int SETTLE_DELAY = 5;

    /** Ticks between checks once the answer is known. */
    private static final int RECHECK_INTERVAL = 20;

    private int ticksUntilCheck = SETTLE_DELAY;

    /**
     * How far the machine has been taken, or null when it is not assembled.
     *
     * <p>Recomputed with the formed state rather than stored, so it cannot go stale: the layer is
     * read from the motors that are actually installed at the moment of the check.
     */
    @Nullable
    private EnigmaValidator.Layer layer;

    public EnigmaCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The layer the machine is at, or null if it is not assembled. */
    @Nullable
    public EnigmaValidator.Layer layer() {
        return layer;
    }

    /** Asks for a check a few ticks from now. Safe to call in bursts; the delay absorbs them. */
    public void requestCheck() {
        if (ticksUntilCheck <= 0 || ticksUntilCheck > SETTLE_DELAY) {
            ticksUntilCheck = SETTLE_DELAY;
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (ticksUntilCheck > 0 && --ticksUntilCheck == 0) {
            check();
        }
    }

    /** Runs a check now and returns what it found, or null when the template could not be read. */
    public EnigmaValidator.Result check() {
        EnigmaStructure structure = EnigmaStructure.get();
        if (structure == null) {
            ticksUntilCheck = RECHECK_INTERVAL;
            return null;
        }

        // Validated against the assembled state - core present, six motors - not the complete
        // machine. The base machine runs on disguised motors, which is what makes it reachable in
        // survival; the seventh motor and the real ones are what the layers above are for, and they
        // are read by layer() rather than required here. Demanding all seven would deadlock the
        // whole progression: this machine is what produces the real motors it would be asking for.
        EnigmaValidator.Result result =
                EnigmaValidator.validate(level, worldPosition, structure, Generation.ASSEMBLED);

        switch (result.status()) {
            case FORMED -> {
                layer = EnigmaValidator.layer(level, worldPosition, structure);
                applyAssembled(true);
                ticksUntilCheck = RECHECK_INTERVAL;
            }
            case UNFORMED -> {
                layer = null;
                applyAssembled(false);
                ticksUntilCheck = RECHECK_INTERVAL;
            }
            case UNKNOWN -> {
                // Could not see enough of the machine to judge. Deliberately leave the state
                // exactly as it was: tearing the machine down because a chunk unloaded would be
                // far worse than showing a stale answer for a moment.
                ticksUntilCheck = SETTLE_DELAY;
            }
        }
        return result;
    }

    private void applyAssembled(boolean assembled) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof EnigmaCoreBlock)) {
            return;
        }
        if (state.getValue(EnigmaCoreBlock.ASSEMBLED) == assembled) {
            return;
        }
        // The block state is the client-visible half of this: vanilla syncs it for free, so no
        // custom packet is needed to make the core light up for everyone nearby.
        level.setBlock(worldPosition, state.setValue(EnigmaCoreBlock.ASSEMBLED, assembled), Block.UPDATE_ALL);

        if (assembled) {
            // Placed after the equality check above, so this fires on the transition and never on
            // the once-a-second re-check that follows it.
            awardWitnesses();
        }
    }

    /**
     * Gives the advancement to everyone close enough to have seen it happen.
     *
     * <p>Everyone in range rather than whoever placed the last block: the machine records nothing
     * about who built it, and a build like this is not one person placing one block - it is a
     * blueprint, a cannon, a stack of belts and a search for six motors. Whoever is standing there
     * when it lights up is who did it.
     *
     * <p>Sixteen blocks, which is about as far as the core's own presence reaches.
     */
    private void awardWitnesses() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                new AABB(worldPosition).inflate(16.0D))) {
            EnigmaAdvancements.grantMysticAge(player);
        }
    }
}
