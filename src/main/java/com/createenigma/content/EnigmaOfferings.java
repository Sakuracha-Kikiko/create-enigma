package com.createenigma.content;

import com.createenigma.registry.CEItems;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * What the machine wants in the chest before it will assemble.
 *
 * <h2>The rule</h2>
 *
 * <p>The chest must contain <b>nothing but</b> the Hand of God, in any quantity. That single rule
 * covers the three cases the wrench has to distinguish, and keeping it as one rule rather than three
 * is what makes them impossible to disagree:
 *
 * <ul>
 *   <li>nothing at all - no offering has been made;</li>
 *   <li>anything else, with or without a Hand - the offering is spoiled;</li>
 *   <li>Hands only, one or more - the offering is accepted and the Hands are consumed.</li>
 * </ul>
 *
 * <p>The quantity is deliberately not fixed yet. Whatever number sits in the chest is what is taken;
 * if it should be a specific count, that check belongs in {@link #judge(Container)} and nowhere else.
 *
 * <h2>Why a container and not a chest</h2>
 *
 * <p>This takes the vanilla {@link Container} so it can be tested without a world, and so a double
 * chest works: {@code ChestBlock.getContainer} hands back one combined view of both halves, whereas a
 * single block entity only exposes its own twenty-seven slots. Checking the entity directly would have
 * made an offering in the far half invisible - and, worse, silently acceptable in the near half.
 */
public final class EnigmaOfferings {

    /** What the chest in the machine's anchor position currently amounts to. */
    public enum Verdict {
        /** Nothing in it. Something has to be offered. */
        EMPTY,
        /** Something that is not the Hand of God, possibly alongside one. */
        IMPURE,
        /** Nothing but the Hand of God, at least one. */
        ACCEPTABLE
    }

    private EnigmaOfferings() {}

    public static Verdict judge(Container container) {
        int hands = 0;
        boolean other = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(CEItems.HAND_OF_GOD.get())) {
                hands += stack.getCount();
            } else {
                other = true;
            }
        }

        if (other) {
            return Verdict.IMPURE;
        }
        return hands == 0 ? Verdict.EMPTY : Verdict.ACCEPTABLE;
    }

    /**
     * Empties the chest.
     *
     * <p>Only ever called after {@link Verdict#ACCEPTABLE}, which means every stack in it is a Hand
     * of God. Written as "clear every slot" rather than "subtract N of the Hand" on purpose: with the
     * quantity still undecided, taking everything is the behaviour that cannot drift from the rule
     * above.
     */
    public static void consume(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(CEItems.HAND_OF_GOD.get())) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
    }
}
