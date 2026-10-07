package com.createenigma.client.ponder;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaAdvancements;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import org.slf4j.Logger;

/**
 * Decides when the ponder scene should exist, and makes Ponder rebuild its index when that
 * answer changes.
 *
 * <h2>Why a poll</h2>
 *
 * <p>The condition is "the player has this advancement", and the client already holds the
 * player's advancement list - the server sends it - so there is nothing to request and no packet
 * to wait for. What there is no event for is "an advancement was just added to the client's
 * copy", so this compares the answer against the last one each tick. The comparison is a map
 * lookup and a boolean, which is the cheapest thing in the client tick.
 *
 * <h2>Why the reload is deferred</h2>
 *
 * <p>{@code PonderIndex.reload()} clears and rebuilds every registered scene in the game, not just
 * this mod's, so it is not something to run mid-frame while a ponder screen is open. The condition
 * in fact flips <em>while</em> the player is watching a ponder - that is the whole trigger - so
 * the reload waits for the screen to close.
 *
 * <p>There is also no harm in the wait: the index is only read when it is opened, and the player
 * is currently looking at something else.
 */
@EventBusSubscriber(modid = CreateEnigma.MOD_ID, value = Dist.CLIENT)
public final class EnigmaPonderUnlockWatcher {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Last observed answer, so the reload fires once per change rather than once per tick. */
    private static boolean unlocked;

    private EnigmaPonderUnlockWatcher() {}

    /**
     * Whether the scenes should exist. Read during plugin registration, which happens on the
     * client thread - the same thread that writes {@link #unlocked}.
     */
    static boolean isUnlocked() {
        return unlocked;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            unlocked = false;
            return;
        }

        boolean now = connection.getAdvancements().get(EnigmaAdvancements.ENIGMA) != null;
        if (now == unlocked) {
            return;
        }

        // A ponder screen is nearly always open at the moment this flips; rebuild once it is gone.
        if (minecraft.screen != null) {
            return;
        }

        unlocked = now;
        LOGGER.info("Enigma ponder: unlock state changed to {}; reloading the ponder index", now);
        PonderIndex.reload();
    }
}
