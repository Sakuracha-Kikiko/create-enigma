package com.createenigma.content;

import com.createenigma.client.EnigmaPartialModels;
import com.createenigma.registry.CEBlocks;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The block entity behind 创造手摇曲柄, and the only reason it exists is one hardcoded lookup.
 *
 * <h2>Why registering the stress value was not enough</h2>
 *
 * <p>{@code BlockStressValues.CAPACITIES} is keyed by block, so registering 16384 for our block was
 * correct and sufficient in principle. It was also invisible in practice: the value is never read by
 * looking up the block being asked about. Create's crank does this instead -
 *
 * <pre>
 *   protected Block getStressConfigKey() {
 *       return AllBlocks.HAND_CRANK.has(getBlockState()) ? AllBlocks.HAND_CRANK.get()
 *                                                        : AllBlocks.COPPER_VALVE_HANDLE.get();
 *   }
 * </pre>
 *
 * <p>- so a crank that is neither of those two answers with the copper valve handle's value. Our
 * block went in at 16384 su/RPM and came out providing 256 SU at 32 RPM, which is exactly 8 x 32:
 * Create's number, quietly, because the question "how much stress is this block worth" was never
 * asked about <em>this block</em>.
 *
 * <p>Overriding the key is the whole fix. {@code calculateAddedStressCapacity()} is inherited and
 * stays Create's - it is one line, {@code BlockStressValues.getCapacity(getStressConfigKey())} - so
 * the registered 16384 flows through Create's own formula rather than being restated here.
 *
 * <h2>Why the test did not catch it</h2>
 *
 * <p>Because it asserted the registration instead of the behaviour: {@code getCapacity(ourBlock)},
 * which was 16384 and had been from the start. The number a player sees comes from the block entity,
 * one indirection further along. The test now calls {@code calculateAddedStressCapacity()} on a real
 * block entity, which is the value the stress network actually receives.
 */
public class CreativeHandCrankBlockEntity extends HandCrankBlockEntity {

    public CreativeHandCrankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Names this block, so the registered capacity is the one that applies.
     *
     * <p>Returning our block is what makes {@code BlockStressValues.getCapacity} answer 16384 here -
     * the superclass would have answered with a Create crank's value for any crank that is not its
     * own two.
     */
    @Override
    protected Block getStressConfigKey() {
        return CEBlocks.CREATIVE_HAND_CRANK.get();
    }

    /**
     * The arm as drawn by the block entity renderer, which is the path a Ponder scene uses.
     *
     * <h2>Why this is needed, and why the block looked like Create's even when it was ours</h2>
     *
     * <p>{@code HandCrankBlock.getRenderShape} returns {@code ENTITYBLOCK_ANIMATED}, so the block
     * model is never drawn - the arm and base come from a renderer. There are two of them:
     *
     * <ul>
     *   <li>in a real world, Flywheel visualisation is available, {@code HandCrankRenderer.renderSafe}
     *       returns early, and {@code CreativeHandCrankVisual} draws everything;</li>
     *   <li>in a Ponder scene Flywheel is not driving anything, so the early return does not fire and
     *       {@code HandCrankRenderer} draws the arm through this method.</li>
     * </ul>
     *
     * <p>The inherited version returns Create's own {@code HAND_CRANK_HANDLE}, which names Create's
     * texture - so a Ponder scene showed the vanilla crank no matter which block was placed, and even
     * replacing the block inside the scene changed nothing. That is the whole mystery: the renderer
     * was never asking the block what it looked like.
     *
     * <p>{@code @OnlyIn(Dist.CLIENT)} is required and is what keeps this safe: the method is only
     * called on a client, and NeoForge's dist cleaner allows client types inside a method marked this
     * way. The dedicated-server gametest is the check that it stays that way.
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public SuperByteBuffer getRenderedHandle() {
        BlockState state = getBlockState();
        Direction facing = state.getOptionalValue(HandCrankBlock.FACING).orElse(Direction.UP);
        return CachedBuffers.partialFacing(EnigmaPartialModels.CRANK_HANDLE, state, facing.getOpposite());
    }
}
