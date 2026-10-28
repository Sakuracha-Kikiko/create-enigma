package com.createenigma.content;

import javax.annotation.Nullable;

import com.createenigma.registry.CEBlockEntities;
import com.createenigma.registry.CEItems;
import com.simibubi.create.AllSoundEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 谜之核心 / Enigma Core - the anchor of the multiblock.
 *
 * <p>It replaces the chest the original machine had at this spot, which is the cell the machine
 * actually feeds: the funnel below it points up into it and the hopper beside it points into it.
 * Making that cell the anchor means the thing a player interacts with is the thing the machine
 * delivers to, which leaves room for a purpose to be attached later without moving anything.
 *
 * <p><b>{@code assembled} is a block state, not a block entity field.</b> Vanilla syncs block
 * states to every nearby client for free, so the core can change appearance when the machine forms
 * without this mod owning a single packet.
 */
public class EnigmaCoreBlock extends Block implements EntityBlock {

    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    public EnigmaCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASSEMBLED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnigmaCoreBlockEntity(CEBlockEntities.ENIGMA_CORE.get(), pos, state);
    }

    /**
     * Server-only ticker. The block entity starts its own countdown, so a freshly placed core
     * checks itself without this hook having to do anything.
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof EnigmaCoreBlockEntity core) {
                core.serverTick();
            }
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            requestCheck(level, pos);
        }
    }

    /**
     * Only ever fires for the six blocks touching the core, so this is a fast path for edits right
     * next to it. Everything further away is caught by the block entity's own periodic check.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            requestCheck(level, pos);
        }
    }

    /**
     * The wrench is the core's only interface, and it reads the same in both hands: a plain right
     * click asks the machine what it looks like, a sneaking right click takes it apart.
     *
     * <p>That split is Create's own wrench - right click turns a component, sneaking right click
     * removes it - so the gesture is already in the player's hands by the time they meet this
     * block. What the dismantle gives back is a chest, which is what this position held before the
     * machine was assembled, never the core itself: no path in the game puts one in a bag.
     *
     * <p>Anything other than the wrench passes straight through, so building against the core still
     * works. Empty-handed right click does nothing - the readout is something the tool does, not
     * something the block says to whoever walks past it.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (!stack.is(CEItems.ENIGMA_WRENCH.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            // The server answers and sends the message, or removes the block; the client only swings.
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            dismantle(level, pos, player);
        } else if (level.getBlockEntity(pos) instanceof EnigmaCoreBlockEntity core) {
            core.report(player);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Removes the core and puts a chest in the player's inventory.
     *
     * <p>The block is cleared with {@code setBlock} rather than broken with drops on purpose: the
     * loot table already says a chest, and letting the block drop its own loot here as well would
     * hand out two. Clearing it directly also keeps the removal silent to neighbours other than
     * the one block update, which is all the machine needs to notice it is gone.
     */
    private static void dismantle(Level level, BlockPos pos, Player player) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHEST));
        AllSoundEvents.WRENCH_REMOVE.playOnServer(level, pos);
    }

    private static void requestCheck(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnigmaCoreBlockEntity core) {
            core.requestCheck();
        }
    }
}
