package com.createenigma.content;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.createenigma.registry.CEItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * What the machine wants in the chest before it will assemble.
 *
 * <h2>The rule</h2>
 *
 * <p>The chest must contain <b>one of each</b> of the three things the machine values - a flywheel,
 * an extinguished cake and a Hand of God - and <b>nothing but</b> those three. Extra copies are
 * allowed and are consumed along with the rest: the machine is not counting, it is accepting.
 *
 * <p>Spelled as one rule rather than three checks, so the three verdicts the wrench has to tell
 * apart cannot disagree with each other:
 *
 * <ul>
 *   <li>nothing at all - no offering has been made;</li>
 *   <li>anything else, or only part of the set - the offering is spoiled, whether or not the
 *       missing pieces are "almost there";</li>
 *   <li>all three kinds, in any quantity - the offering is accepted and the chest is emptied.</li>
 * </ul>
 *
 * <p>A partial set is deliberately {@link Verdict#IMPURE} and not {@link Verdict#EMPTY}. A player who
 * has put in two of the three deserves to be told the offering is wrong, not that they have offered
 * nothing.
 *
 * <h2>Why a container and not a chest</h2>
 *
 * <p>This takes the vanilla {@link Container} so it can be tested without a world, and so a double
 * chest works: {@code ChestBlock.getContainer} hands back one combined view of both halves, whereas a
 * single block entity only exposes its own twenty-seven slots. Checking the entity directly would have
 * made an offering in the far half invisible - and, worse, silently acceptable in the near half.
 */
public final class EnigmaOfferings {

    /** The three things that count as part of an offering. */
    private enum Kind {
        FLYWHEEL,
        CAKE,
        HAND
    }

    /** What the chest in the machine's anchor position currently amounts to. */
    public enum Verdict {
        /** Nothing in it. Something has to be offered. */
        EMPTY,
        /** Something that is not an offering, or an offering with a piece still missing. */
        IMPURE,
        /** One of each, and nothing else. */
        ACCEPTABLE
    }

    private EnigmaOfferings() {}

    @Nullable
    private static Kind kindOf(ItemStack stack) {
        if (stack.is(AllBlocks.FLYWHEEL.get().asItem())) {
            return Kind.FLYWHEEL;
        }
        if (stack.is(CEItems.EXTINGUISHED_CAKE.get())) {
            return Kind.CAKE;
        }
        if (stack.is(CEItems.HAND_OF_GOD.get())) {
            return Kind.HAND;
        }
        return null;
    }

    public static Verdict judge(Container container) {
        Set<Kind> present = EnumSet.noneOf(Kind.class);
        boolean anything = false;
        boolean foreign = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            anything = true;

            Kind kind = kindOf(stack);
            if (kind == null) {
                foreign = true;
            } else {
                present.add(kind);
            }
        }

        if (!anything) {
            return Verdict.EMPTY;
        }
        if (foreign || present.size() < Kind.values().length) {
            return Verdict.IMPURE;
        }
        return Verdict.ACCEPTABLE;
    }

    /**
     * Empties the chest.
     *
     * <p>Only ever called after {@link Verdict#ACCEPTABLE}, which means every stack in it is part of
     * the offering. Written as "clear every offering slot" rather than "subtract one of each" on
     * purpose: the rule accepts extras, so the accepted surplus is the machine's, not the player's -
     * taking one of each and leaving the rest would quietly change what was agreed to.
     */
    public static void consume(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (kindOf(container.getItem(slot)) != null) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        container.setChanged();
    }
}
