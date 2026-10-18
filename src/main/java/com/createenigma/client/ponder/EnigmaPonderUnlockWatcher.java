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

    /** Last observed answers, so the reload fires once per change rather than once per tick. */
    private static boolean unlocked;

    /** The crank's own gate: granted by obtaining the item, not by watching anything. */
    private static boolean crankUnlocked;

    private EnigmaPonderUnlockWatcher() {}

    /**
     * Whether the wrench scenes should exist. Read during plugin registration, which happens on the
     * client thread - the same thread that writes {@link #unlocked}.
     */
    static boolean isUnlocked() {
        return unlocked;
    }

    /** Whether the crank's scene should exist. Same rules as {@link #isUnlocked()}. */
    static boolean isCrankUnlocked() {
        return crankUnlocked;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            unlocked = false;
            crankUnlocked = false;
            return;
        }

        // Two independent gates. A single flag was how this started, and it cannot express "this
        // scene needs the wrench, that one needs the crank" - one of the two would appear early.
        boolean now = connection.getAdvancements().get(EnigmaAdvancements.ENIGMA) != null;
        boolean nowCrank = connection.getAdvancements().get(EnigmaAdvancements.MYSTIC_GRACE) != null;
        if (now == unlocked && nowCrank == crankUnlocked) {
            return;
        }

        // A ponder screen is nearly always open at the moment this flips; rebuild once it is gone.
        if (minecraft.screen != null) {
            return;
        }

        unlocked = now;
        crankUnlocked = nowCrank;
        LOGGER.info("Enigma ponder: unlock state changed to wrench={}, crank={}; reloading the ponder index",
                now, nowCrank);
        PonderIndex.reload();
    }
}
