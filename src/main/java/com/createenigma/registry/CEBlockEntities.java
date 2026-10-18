package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.DisguisedMotorBlockEntity;
import com.createenigma.content.EnigmaCoreBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankRenderer;
import com.simibubi.create.content.kinetics.crank.HandCrankVisual;
import com.simibubi.create.content.kinetics.motor.CreativeMotorRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * Block entities.
 */
public class CEBlockEntities {

    public static final BlockEntityEntry<EnigmaCoreBlockEntity> ENIGMA_CORE =
            CreateEnigma.registrate().blockEntity("enigma_core", EnigmaCoreBlockEntity::new)
                    .validBlocks(CEBlocks.ENIGMA_CORE)
                    .register();

    /**
     * The disguised motor reuses Create's renderer and visual outright.
     *
     * <p>Its block entity extends {@code CreativeMotorBlockEntity}, so it is exactly the type
     * {@code CreativeMotorRenderer} is written against - no adaptation needed. Borrowing both is
     * also what makes "identical appearance" true rather than approximate: a hand-written renderer
     * would have to reproduce the half-shaft, its rotation and its Flywheel visualisation, and any
     * difference would be visible - which for a block whose entire purpose is to be mistaken for
     * another one is the same as being broken.
     */
    public static final BlockEntityEntry<DisguisedMotorBlockEntity> DISGUISED_MOTOR =
            CreateEnigma.registrate().blockEntity("disguised_motor", DisguisedMotorBlockEntity::new)
                    .visual(() -> OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF), false)
                    .validBlocks(CEBlocks.DISGUISED_MOTOR)
                    .renderer(() -> CreativeMotorRenderer::new)
                    .register();

    /**
     * The creative hand crank, borrowing Create's whole visual chain.
     *
     * <p>{@code HandCrankBlockEntity} is used unchanged: the 16384 stress units do not live in the
     * block entity but in {@code BlockStressValues}, keyed by block, so the block entry supplies it
     * and the behaviour here is Create's original crank.
     *
     * <p>Both halves of the rendering are Create's, registered for our type. The arm is animated by
     * {@link HandCrankVisual} - a Flywheel visual, which is why it can turn without a block state
     * change - and {@link HandCrankRenderer} is the older path that draws the same arm through
     * {@code HandCrankBlockEntity.getRenderedHandle()}. Registering both mirrors what Create does for
     * its own crank and keeps the fallback intact.
     *
     * <p>On plain servers {@code HandCrankVisual} is client-only; it is only ever named inside the
     * supplier, which runs during client setup, and never while the block entity type is registered.
     */
    public static final BlockEntityEntry<HandCrankBlockEntity> CREATIVE_HAND_CRANK =
            CreateEnigma.registrate().blockEntity("creative_hand_crank", HandCrankBlockEntity::new)
                    .visual(() -> HandCrankVisual::new, false)
                    .validBlocks(CEBlocks.CREATIVE_HAND_CRANK)
                    .renderer(() -> HandCrankRenderer::new)
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
