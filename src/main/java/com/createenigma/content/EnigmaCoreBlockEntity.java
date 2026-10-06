package com.createenigma.content;

import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaValidator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

    public EnigmaCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

        EnigmaValidator.Result result = EnigmaValidator.validate(level, worldPosition, structure);
        switch (result.status()) {
            case FORMED -> {
                applyAssembled(true);
                ticksUntilCheck = RECHECK_INTERVAL;
            }
            case UNFORMED -> {
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
    }

    /** Tells the player what the machine currently looks like, and where it first disagrees. */
    public void report(Player player) {
        EnigmaValidator.Result result = check();

        if (result == null) {
            player.displayClientMessage(Component.translatable("create_enigma.enigma_core.no_template"), false);
            return;
        }

        switch (result.status()) {
            case FORMED -> player.displayClientMessage(
                    Component.translatable("create_enigma.enigma_core.formed"), false);
            case UNKNOWN -> player.displayClientMessage(
                    Component.translatable("create_enigma.enigma_core.unknown"), false);
            case UNFORMED -> {
                BlockPos pos = result.failingPos();
                player.displayClientMessage(Component.translatable("create_enigma.enigma_core.unformed",
                        pos.getX(), pos.getY(), pos.getZ(), result.detail()), false);
            }
        }
    }
}
