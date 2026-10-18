package com.createenigma.content;

import com.createenigma.registry.CEBlockEntities;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 创造手摇曲柄 / Creative Hand Crank
 *
 * <p>Create's hand crank with one number changed: it provides 16384 instead of 8 stress units per
 * RPM. Mechanically nothing else differs - same 32 RPM, same right-click-to-turn, same placement,
 * shapes, waterlogging and shaft connection, all inherited.
 *
 * <h2>Why this is a subclass rather than a plain {@code new HandCrankBlock(properties)}</h2>
 *
 * <p>Two reasons, and only two:
 *
 * <ul>
 *   <li>A block entity type lists the blocks it may appear on. Create's {@code HAND_CRANK} type lists
 *       only Create's own block, so placing this one would fail to create a block entity at all.
 *       {@link #getBlockEntityType()} names a type of our own that lists this block instead.</li>
 *   <li>The stress value is looked up <b>by block</b> ({@code BlockStressValues}). A different block
 *       is a different key, which is exactly what makes the 16384 possible - see
 *       {@code CEBlocks.CREATIVE_HAND_CRANK}.</li>
 * </ul>
 *
 * <p>Everything visible is Create's: the blockstate and models point at Create's own models, and the
 * rotation is drawn by Create's Flywheel visual and block entity renderer, which are registered for
 * our block entity type. Nothing here reproduces their work, which is also why it cannot drift from
 * theirs.
 */
public class CreativeHandCrankBlock extends HandCrankBlock {

    public CreativeHandCrankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
        return CEBlockEntities.CREATIVE_HAND_CRANK.get();
    }
}
