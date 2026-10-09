package com.createenigma.content;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Disguised Motor's behaviour: everything the real Creative Motor does, for a thousandth of the
 * stress capacity.
 *
 * <p>Speed and direction adjustment, the value panel, the network behaviour and the rendering are
 * all inherited. Only two things are overridden, and both are places where Create's implementation
 * is hardcoded to Create's own block.
 */
public class DisguisedMotorBlockEntity extends CreativeMotorBlockEntity {

    /**
     * 1 SU/RPM, against the real motor's 16384.
     *
     * <p>Set here rather than through Create's {@code CStress.setCapacity} transform: that helper
     * calls an internal {@code assertFromCreate} which throws {@code IllegalStateException} for any
     * block whose owner is not {@code create}. The block entity is the supported way in.
     */
    private static final float CAPACITY = 1.0F;

    public DisguisedMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Identical to the inherited implementation except for the first line.
     *
     * <p>Create's version begins {@code if (!AllBlocks.CREATIVE_MOTOR.has(getBlockState())) return 0;}
     * - a guard that makes this block permanently dead if it is not overridden, because a disguised
     * motor is by definition not {@code create:creative_motor}.
     */
    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed.getValue(),
                getBlockState().getValue(DisguisedMotorBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        // lastCapacityProvided is what the base implementation also sets, and what gets written to
        // the network tag and pushed to the kinetic network - so it has to be kept in step with the
        // returned value, not just returned.
        this.lastCapacityProvided = CAPACITY;
        return CAPACITY;
    }
}
