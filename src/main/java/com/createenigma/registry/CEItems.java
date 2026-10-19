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
     * 神之手 / Hand of God: the icon of the advancement tab, and an item in its own right.
     *
     * <p>It exists first as the tab's icon. An advancement's icon has to be an item and a rotation is
     * a property of the model, so the rotation cannot be expressed on {@code create:brass_hand} - that
     * is a flat {@code item/generated} model with default transforms. This item carries a model that
     * is the same texture with {@code display.gui.rotation} set to 180 degrees about Z, and that is
     * the transform the advancements screen renders icons through.
     *
     * <p>It is craftable as well, from Create's brass hand recipe turned upside down: same
     * ingredients, same tags, the shape rotated half a turn - so it is the hand pointing down, which
     * is what the icon shows. A gametest compares the two recipes cell by cell so the two cannot
     * drift apart.
     */
    public static final ItemEntry<Item> HAND_OF_GOD =
            CreateEnigma.registrate().item("hand_of_god", Item::new).register();

    public static void register() {
        // Class loading performs the registration.
    }
}
