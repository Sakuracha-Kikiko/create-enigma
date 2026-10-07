package com.createenigma.content;

import com.createenigma.CreateEnigma;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants this mod's advancements.
 *
 * <p>Both the tab and the advancement use the {@code minecraft:impossible} trigger and are handed
 * out from code. That trigger never fires by itself, which is exactly what is wanted: nothing in
 * the world can earn these, only the one event this mod watches for.
 *
 * <p><b>The root is granted together with the advancement, and that is the whole point.</b> The
 * server only sends advancements a player can see (see {@code PlayerAdvancements}'s visibility
 * pass), and an ungranted root with no granted children is not visible. So before this fires, the
 * player has no idea the tab exists; the moment it does, the tab and its single entry appear
 * together. The reveal is the reward.
 */
public final class EnigmaAdvancements {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The tab. Never shown on its own; it exists so the advancement has somewhere to live. */
    public static final ResourceLocation ROOT =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "root");

    /** The first - and currently only - entry. */
    public static final ResourceLocation ENIGMA =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma");

    /**
     * The criterion key both advancement files declare.
     *
     * <p>Public so the GameTest can assert the files really declare it. {@code award()} returns
     * false for an unknown criterion instead of throwing, so a typo here would show up only as
     * "the advancement never appears" - the single most likely way to break this feature.
     */
    public static final String CRITERION = "impossible";

    private EnigmaAdvancements() {}

    /**
     * Awards both advancements. Idempotent: awarding an already-awarded criterion returns false
     * and changes nothing, so a player replaying the ponder is harmless.
     *
     * @return true when this call was the one that granted {@code enigma}
     */
    public static boolean grantEnigma(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerAdvancementManager manager = server.getAdvancements();
        AdvancementHolder root = manager.get(ROOT);
        AdvancementHolder enigma = manager.get(ENIGMA);

        if (root == null || enigma == null) {
            // A missing file here means a packaging mistake, and silently doing nothing would
            // look exactly like "the ponder hook does not work".
            LOGGER.error("Enigma advancements are missing from the datapack: root={} enigma={}",
                    root != null, enigma != null);
            return false;
        }

        player.getAdvancements().award(root, CRITERION);
        return player.getAdvancements().award(enigma, CRITERION);
    }
}
