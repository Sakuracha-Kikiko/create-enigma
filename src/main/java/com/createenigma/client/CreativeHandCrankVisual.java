package com.createenigma.client;

import java.util.function.Consumer;

import com.createenigma.CreateEnigma;
import com.createenigma.content.CreativeHandCrankBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.Quaternionf;

/**
 * Draws the creative hand crank's arm, in this mod's colours.
 *
 * <h2>This is a copy of {@code HandCrankVisual}, and it has to be</h2>
 *
 * <p>The arm is a <b>second</b> model, drawn only by a Flywheel visual. There is no override point:
 * {@code HandCrankVisual} builds its instance from {@code AllPartialModels.HAND_CRANK_HANDLE} inside a
 * private field, and that field cannot be reached or replaced from outside. The block entity renderer
 * is not an alternative either - {@code HandCrankRenderer.renderSafe} returns early whenever Flywheel
 * supports visualisation, so on any client with Create installed that renderer never draws the arm at
 * all, and {@code HandCrankBlockEntity.getRenderedHandle()} is dead code there.
 *
 * <p>What is copied is the <i>behaviour</i>: the same two instances, the same rotation, the same
 * lifetime and light handling. What is not copied is any appearance - the geometry is still Create's,
 * inherited by name from {@code create:block/hand_crank/handle} and only rebound to this mod's
 * textures by a three-line model json.
 *
 * <h2>Only the arm, for now</h2>
 *
 * <p>{@link #BASE} still points at Create's own base model, so the static shaft stub keeps Create's
 * colours. Switching it to a model of our own is the only change needed for a full recolour - which is
 * why this class exists as the foundation rather than as a one-off.
 */
public class CreativeHandCrankVisual extends KineticBlockEntityVisual<CreativeHandCrankBlockEntity>
        implements SimpleDynamicVisual {

    /** The arm: Create's geometry, this mod's textures. */
    private static final PartialModel HANDLE = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "block/creative_hand_crank/handle"));

    /**
     * The rotating base, still Create's.
     *
     * <p>This is the single line the full recolour would change: point it at
     * {@code create_enigma:block/creative_hand_crank/block} and add the matching model json.
     */
    private static final PartialModel BASE = AllPartialModels.HAND_CRANK_BASE;

    private final RotatingInstance rotatingModel;
    private final TransformedInstance crank;

    public CreativeHandCrankVisual(VisualizationContext context, CreativeHandCrankBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);

        crank = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(HANDLE)).createInstance();

        rotateCrank(partialTick);

        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(BASE)).createInstance();

        rotatingModel.setup(this.blockEntity)
                .setPosition(getVisualPosition())
                .rotateToFace(blockState.getValue(BlockStateProperties.FACING))
                .setChanged();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        rotateCrank(context.partialTick());
    }

    /**
     * Turns the arm by the crank's own angle.
     *
     * <p>Kept identical to Create's arithmetic. The angle is {@code getIndependentAngle}, not the
     * network speed - a hand crank's kinetic speed is zero until somebody turns it, and this is the
     * separate "chasing" angle that gives the arm its weight. The quaternion at the end tips the arm
     * from its modelled orientation onto the face it was placed against.
     */
    private void rotateCrank(float partialTick) {
        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        float angle = AngleHelper.rad(blockEntity.getIndependentAngle(partialTick));

        crank.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotate(angle, Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis()))
                .rotate(new Quaternionf().rotateTo(0, 0, -1, facing.getStepX(), facing.getStepY(), facing.getStepZ()))
                .uncenter()
                .setChanged();
    }

    @Override
    protected void _delete() {
        crank.delete();
        rotatingModel.delete();
    }

    @Override
    public void update(float partialTick) {
        rotatingModel.setup(blockEntity)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(crank, rotatingModel);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(crank);
        consumer.accept(rotatingModel);
    }
}
