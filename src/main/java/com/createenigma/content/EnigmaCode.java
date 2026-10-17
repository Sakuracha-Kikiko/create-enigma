package com.createenigma.content;

import java.util.Random;

/**
 * This world's numbers.
 *
 * <p>Every save gets its own answer to "which assembly step does the wrench want", derived from the
 * world seed. Two purposes, one source: the recipe that accepts three incomplete precision
 * mechanisms at those progresses, and the ponder scene that reveals them as three digits. If those
 * two ever disagreed, the scene would tell the player numbers that craft nothing, so both read from
 * here and nowhere else.
 *
 * <h2>Why {@code java.util.Random}</h2>
 *
 * <p>Not because it is good, but because its algorithm is <b>fixed by the Java specification</b>.
 * Minecraft's own {@code RandomSource} makes no such promise - a version bump may legitimately change
 * the sequence - and the failure would be silent and awful: a save that could craft the wrench today
 * could not tomorrow, with the scene still showing the old digits. A player would have no way to tell
 * that from a wrong guess.
 *
 * <h2>Why distinct</h2>
 *
 * <p>Three identical digits would make two of the three mechanisms interchangeable, turning a
 * three-part answer into a one-part one, and the scene's line would read as a single number repeated.
 * Rejection sampling keeps the three genuinely different. It also keeps the distribution uniform,
 * which simply taking {@code nextInt} three times would not: about one save in fourteen could
 * otherwise show a doubled digit.
 */
public final class EnigmaCode {

    /**
     * How many progress values an incomplete precision mechanism can hold.
     *
     * <p>Create stores the progress as a float and displays it as {@code round(progress * 13)}, so the
     * underlying steps are 0 through 13 - fourteen of them. The number a player ever sees is that plus
     * one: the first assembly step turns a gold sheet into an intermediate showing 1/15, and the
     * fifteenth resolves it into a finished mechanism, so 0/15 and 15/15 never exist in play.
     *
     * <p>Everything above this class therefore speaks in the <b>player's</b> numbers, 1 to 14. The
     * recipe subtracts the one back off before comparing against the stored progress. Getting this
     * backwards is silent: the scene would say 3 and the recipe would want 2, and nothing would ever
     * craft.
     */
    public static final int STEPS = 14;

    private EnigmaCode() {}

    /**
     * The three progresses this world's wrench recipe asks for, in the order the scene reads them
     * out.
     *
     * <p>Deterministic: the same seed always gives the same three numbers, forever.
     */
    public static int[] wrenchProgresses(long seed) {
        Random random = new Random(seed);

        int first = random.nextInt(STEPS);
        int second = nextDistinct(random, first);
        int third;
        do {
            third = random.nextInt(STEPS);
        } while (third == first || third == second);

        return new int[] {first + 1, second + 1, third + 1};
    }

    private static int nextDistinct(Random random, int differentFrom) {
        int value;
        do {
            value = random.nextInt(STEPS);
        } while (value == differentFrom);
        return value;
    }

    /** Convenience for the recipe and the scene: the same three numbers as one readable line. */
    public static String describe(long seed) {
        int[] progresses = wrenchProgresses(seed);
        return progresses[0] + ", " + progresses[1] + ", " + progresses[2] + ".";
    }
}
