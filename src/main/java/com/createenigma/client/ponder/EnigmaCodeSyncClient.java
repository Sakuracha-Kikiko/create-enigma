package com.createenigma.client.ponder;

import com.createenigma.content.EnigmaCodeState;
import com.createenigma.network.EnigmaCodeSync;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.foundation.PonderIndex;

import org.slf4j.Logger;

/**
 * Receives this world's three numbers on the client and rebuilds the ponder index.
 *
 * <h2>Why arriving has to trigger a rebuild</h2>
 *
 * <p>Ponder scenes are built once, at client startup, before any server connection exists. At that
 * moment nothing has been received, so a scene built then would print the placeholder for the rest of
 * the session. The numbers only become known when the server sends them.
 *
 * <p>{@code PonderIndex.reload()} exists for exactly this shape of problem: it clears the scene and
 * tag registries, reopens registration, and re-runs every plugin. The mod already relies on it to
 * unlock content behind an advancement; this is the same mechanism with a different trigger.
 *
 * <p>The numbers themselves are stored in {@link EnigmaCodeState}, not here, because the recipe needs
 * to read them too and that code runs on both sides.
 */
public final class EnigmaCodeSyncClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EnigmaCodeSyncClient() {}

    /**
     * Records what the server sent and rebuilds the ponder index.
     *
     * <p>Skipped when the numbers are unchanged - a reload throws away every scene on the client, so
     * doing it on a needless packet would be a visible stutter for nothing.
     */
    public static void accept(EnigmaCodeSync payload) {
        if (EnigmaCodeState.isSynced(payload.first(), payload.second(), payload.third())) {
            return;
        }

        EnigmaCodeState.accept(payload.first(), payload.second(), payload.third());
        LOGGER.info("Enigma ponder: this world's numbers are {}", payload.describe());
        PonderIndex.reload();
    }
}
