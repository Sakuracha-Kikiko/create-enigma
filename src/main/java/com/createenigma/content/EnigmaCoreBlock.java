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
     * The core answers exactly one interaction: a wrench, held while sneaking, takes it apart.
     *
     * <p>Same gesture and same sound as Create's wrench removing a kinetic component, because that
     * is the gesture players already know. What comes back is a chest, which is what this position
     * held before the machine was assembled - never the core itself. That is the whole point: the
     * core is not a survival item, so there is no path in the game that puts one in a bag.
     *
     * <p>Everything else passes through untouched: without a wrench, or without sneaking, right
     * click does nothing at all. The core stopped reporting the machine's state when this was added
     * - that readout belongs to the chest, which is the block the player places and can always get
     * back.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() || !stack.is(CEItems.ENIGMA_WRENCH.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            // The server removes the block and hands back the chest; the client only swings.
            return ItemInteractionResult.SUCCESS;
        }
        dismantle(level, pos, player);
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
