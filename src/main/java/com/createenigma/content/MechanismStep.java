package com.createenigma.content;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;

import net.minecraft.world.item.ItemStack;

/**
 * The step an incomplete precision mechanism is at, as the player reads it.
 *
 * <h2>The number the tooltip prints</h2>
 *
 * <p>Create's own tooltip reads {@code "组装进度：%s/%s"} with two integers: the second is
 * {@code sequence.size() * loops} - 15 for the precision mechanism - and the first is, verbatim,
 * {@code SequencedAssembly.step()}. Not a rounded percentage, not a bar width. This class returns
 * that same integer, so the recipe compares against exactly what the player is looking at.
 *
 * <h2>Why not {@code progress()}</h2>
 *
 * <p>The item carries two fields and only one of them is on screen. {@code progress()} is a float
 * that Create derives for its own bookkeeping, and it was once assumed here to run 0..13 with a +1
 * to reach the player's scale - a story invented from the durability bar's 13-pixel width. That is a
 * rendering detail. The consequence was silent and total: a mechanism reading {@code 1/15} had a
 * progress the old formula scored as {@code 2}, so any world whose numbers included a 1 could not
 * craft the wrench however carefully it was built. Reading the field the tooltip reads removes the
 * arithmetic, and with it the chance of that disagreement.
 */
public final class MechanismStep {

    /** What {@link #of} answers for a stack that is not a mechanism, matching no step. */
    public static final int NOT_A_MECHANISM = -1;

    private MechanismStep() {}

    /**
     * The step shown on this stack's tooltip, or {@link #NOT_A_MECHANISM}.
     *
     * <p>{@link #NOT_A_MECHANISM} is only for stacks that are not an incomplete precision mechanism
     * at all. A mechanism that somehow carries no assembly component reports <b>0</b>, because that
     * is what Create's tooltip would print for it - this method mirrors the tooltip rather than
     * improving on it, so that the two can never disagree.
     */
    public static int of(ItemStack stack) {
        if (!stack.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())) {
            return NOT_A_MECHANISM;
        }
        SequencedAssembly assembly = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        return assembly == null ? 0 : assembly.step();
    }
}
