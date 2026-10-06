package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaCoreBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * Block entities. No renderer: the core is an ordinary full-cube block, so it has nothing to draw
 * beyond its model.
 */
public class CEBlockEntities {

    public static final BlockEntityEntry<EnigmaCoreBlockEntity> ENIGMA_CORE =
            CreateEnigma.registrate().blockEntity("enigma_core", EnigmaCoreBlockEntity::new)
                    .validBlocks(CEBlocks.ENIGMA_CORE)
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
