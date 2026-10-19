package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaWrenchItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;

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

    /**
     * The icon of the advancement tab: Create's brass hand, turned upside down.
     *
     * <p>An advancement's icon has to be an item, and a rotation is a property of the model, so the
     * rotation cannot be expressed on {@code create:brass_hand} itself - it is a flat
     * {@code item/generated} model with default transforms. This item exists only to carry a model
     * that is the same texture with {@code display.gui.rotation} set to 180 degrees about Z, which is
     * what the advancement screen renders through.
     *
     * <p>It is not added to the creative tab, and the registry's default tab is null, so it never
     * appears as a usable item.
     */
    public static final ItemEntry<Item> TAB_ICON =
            CreateEnigma.registrate().item("enigma_tab_icon", Item::new).register();

    public static void register() {
        // Class loading performs the registration.
    }
}
