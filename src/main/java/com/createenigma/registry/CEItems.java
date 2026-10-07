package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaWrenchItem;
import com.tterrag.registrate.util.entry.ItemEntry;

/**
 * The mod's items.
 *
 * <p>No model is generated: like the block, {@code enigma_wrench.json} is hand-written under
 * {@code src/main/resources}.
 */
public class CEItems {

    /** 神秘扳手 / Enigma Wrench. A tool, so it does not stack. */
    public static final ItemEntry<EnigmaWrenchItem> ENIGMA_WRENCH =
            CreateEnigma.registrate().item("enigma_wrench", EnigmaWrenchItem::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
