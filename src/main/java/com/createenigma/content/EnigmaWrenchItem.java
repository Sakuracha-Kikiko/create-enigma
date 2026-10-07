package com.createenigma.content;

import net.minecraft.world.item.Item;

/**
 * 神秘扳手 / Enigma Wrench
 *
 * <p>Currently an ordinary item with no behaviour of its own: only its tooltip exists so far.
 * The class is here rather than using a plain {@link Item} because this is where the wrench's
 * actual behaviour will go, and an empty named class is a clearer placeholder than "some Item".
 *
 * <p>Its tooltip deliberately says nothing useful. See {@code EnigmaWrenchTooltip}.
 */
public class EnigmaWrenchItem extends Item {

    public EnigmaWrenchItem(Properties properties) {
        super(properties);
    }
}
