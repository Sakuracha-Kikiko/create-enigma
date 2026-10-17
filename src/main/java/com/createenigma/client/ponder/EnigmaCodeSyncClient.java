package com.createenigma.client.ponder;

import com.createenigma.network.EnigmaCodeSync;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.foundation.PonderIndex;

import org.slf4j.Logger;

/**
 * Holds the three numbers on the client, and rebuilds the ponder index when they arrive.
 *
 * <h2>Why arriving has to trigger a rebuild</h2>
 *
 * <p>Ponder scenes are built once, at client startup, before any server connection exists. At that
 * moment this class holds nothing, so a scene built then would print the placeholder for the rest of
 * the session. The numbers only become known when the server sends them.
 *
 * <p>{@code PonderIndex.reload()} exists for exactly this shape of problem: it clears the scene and
 * tag registries, reopens registration, and re-runs every plugin. The mod already relies on it to
 * unlock content behind an advancement; this is the same mechanism with a different trigger.
 *
 * <h2>Why a static field rather than anything clever</h2>
 *
 * <p>The scene builder runs inside {@code registerScenes}, on the client, with no world and no
 * player - it can only read static state. A field set by the network handler is the whole design.
 */
public final class EnigmaCodeSyncClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Placeholder until the server answers. Never shown for long enough to matter. */
    private static final int[] UNKNOWN = {0, 1, 2};

    private static int[] code = UNKNOWN;

    private EnigmaCodeSyncClient() {}

    /**
     * Records what the server sent and rebuilds the ponder index.
     *
     * <p>Skipped when the numbers are unchanged - a reload throws away every scene on the client, so
     * doing it on a needless packet would be a visible stutter for nothing.
     */
    public static void accept(EnigmaCodeSync payload) {
        int[] received = {payload.first(), payload.second(), payload.third()};
        if (java.util.Arrays.equals(code, received)) {
            return;
        }

        code = received;
        LOGGER.info("Enigma ponder: this world's numbers are {}", payload.describe());
        PonderIndex.reload();
    }

    /** The three numbers, as the scene prints them. */
    public static String describe() {
        return code[0] + ", " + code[1] + ", " + code[2] + ".";
    }
}
