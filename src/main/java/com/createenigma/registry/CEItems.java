package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaWrenchItem;
import com.createenigma.content.ExtinguishedCakeItem;
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

    /**
     * 熄灭蛋糕 / Extinguished Cake: what a blaze cake base becomes when it is spouted with water.
     *
     * <p>Create's blaze cake is the base plus 250 mB of lava; this is that recipe with water, so the
     * two sit next to each other in JEI and differ by one ingredient.
     *
     * <p>It is not a plain meal, though it is food: eating it restores 5 hunger and 12 saturation
     * and grants ten seconds of fire resistance. See {@link ExtinguishedCakeItem#FOOD}.
     *
     * <p>Its texture is Create's blaze cake recoloured to water hues - the same sprite, the same
     * shading, with every warm hue remapped onto blue and cyan, so the two items read as the same
     * cake with the fire taken out of it.
     */
    public static final ItemEntry<ExtinguishedCakeItem> EXTINGUISHED_CAKE =
            CreateEnigma.registrate().item("extinguished_cake", ExtinguishedCakeItem::new)
                    .properties(p -> p.food(ExtinguishedCakeItem.FOOD))
                    .register();

    public static void register() {
        // Class loading performs the registration.
    }
}
