package com.createenigma.content;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;

/**
 * Where the three numbers are read from, on whichever side is asking.
 *
 * <h2>Two sources, one answer</h2>
 *
 * <p>The recipe and the ponder scene must agree, or the scene would reveal numbers that craft
 * nothing. They also run in different places: the recipe's {@code matches} is asked on the server
 * (which has the seed) and on the client (which was told the numbers by {@link
 * com.createenigma.network.EnigmaCodeSync}). The scene is client-only and has no level at all.
 *
 * <p>So everything reads here. The server derives from the seed; the client and the scene use what
 * the server sent. There is deliberately no second implementation of the derivation anywhere.
 */
public final class EnigmaCodeState {

    /**
     * What a client shows before the server has answered.
     *
     * <p>Only ever visible in the first second after joining, and only in the scene - the recipe
     * cannot be satisfied with it by accident, because all three values would have to match a
     * player's actual mechanisms, and 0/1/2 is one guess in 2184.
     */
    private static final int[] PLACEHOLDER = {0, 1, 2};

    /** What the server last told this client. Null until then, and null on a server. */
    private static int[] synced;

    private EnigmaCodeState() {}

    /** Called by the network handler when the server reports this world's numbers. */
    public static void accept(int first, int second, int third) {
        synced = new int[] {first, second, third};
    }

    /** Whether the numbers already held are these three, so a redundant reload can be skipped. */
    public static boolean isSynced(int first, int second, int third) {
        return synced != null && synced[0] == first && synced[1] == second && synced[2] == third;
    }

    /**
     * The three progresses in force here.
     *
     * @param level the level asking, or null when there is none - the scene builder has no level
     */
    public static int[] progresses(@Nullable Level level) {
        if (level != null && level.getServer() != null) {
            // Authoritative. The overworld's seed is the save's seed; every dimension shares it.
            return EnigmaCode.wrenchProgresses(level.getServer().overworld().getSeed());
        }
        return synced != null ? synced : PLACEHOLDER;
    }

    /** "3, 2, 5." - what the scene prints. */
    public static String describe(@Nullable Level level) {
        int[] progresses = progresses(level);
        return progresses[0] + ", " + progresses[1] + ", " + progresses[2] + ".";
    }
}
