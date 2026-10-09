package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.DisguisedMotorBlockEntity;
import com.createenigma.content.EnigmaCoreBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
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

    public static void register() {
        // Class loading performs the registration.
    }
}
