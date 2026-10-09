package com.createenigma.content;

import com.createenigma.registry.CEBlockEntities;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 伪装创造马达 / Disguised Creative Motor
 *
 * <p>Behaves exactly like Create's Creative Motor - same placement, same shape, same rotation axis,
 * same value panel for speed and direction - and looks identical, because it points at Create's own
 * block models. The only difference is in {@link DisguisedMotorBlockEntity}: it produces 1 SU/RPM
 * instead of 16384.
 *
 * <p><b>Why this mirrors {@code CreativeMotorBlock} instead of extending it.</b> That class declares
 * {@code implements IBE<CreativeMotorBlockEntity>} and returns {@code Class<CreativeMotorBlockEntity>}
 * from {@code getBlockEntityClass()}. A subclass cannot narrow that to its own block entity type -
 * generics are invariant - so extending it would mean either keeping Create's block entity or
 * fighting the type system. Copying six small overrides is the shorter path.
 *
 * <p>{@code FACING} comes from {@link DirectionalKineticBlock} and is
 * {@code BlockStateProperties.FACING} itself, so it is the <em>same</em> property instance Create's
 * code reads. That matters: the inherited value box in {@code CreativeMotorBlockEntity} looks up
 * {@code CreativeMotorBlock.FACING}, and would throw on a block that used a different instance.
 */
public class DisguisedMotorBlock extends DirectionalKineticBlock implements IBE<DisguisedMotorBlockEntity> {

    public DisguisedMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.MOTOR_BLOCK.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if ((context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) || preferred == null) {
            return super.getStateForPlacement(context);
        }
        return defaultBlockState().setValue(FACING, preferred);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /** It is a source, so it has no stress impact to report - same as the real one. */
    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<DisguisedMotorBlockEntity> getBlockEntityClass() {
        return DisguisedMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DisguisedMotorBlockEntity> getBlockEntityType() {
        return CEBlockEntities.DISGUISED_MOTOR.get();
    }
}
