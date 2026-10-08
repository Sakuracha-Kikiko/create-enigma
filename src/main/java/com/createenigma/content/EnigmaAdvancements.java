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
 * <p>All of them use the {@code minecraft:impossible} trigger and are handed out from code. That
 * trigger never fires by itself, which is exactly what is wanted: nothing in the world can earn
 * these, only the events this mod watches for.
 *
 * <p><b>The root is granted together with the first advancement, and that is the whole point.</b>
 * The server only sends advancements a player can see (see {@code PlayerAdvancements}'s visibility
 * pass), and an ungranted root with no granted children is not visible. So before this fires, the
 * player has no idea the tab exists; the moment it does, the tab and its entry appear together.
 * The reveal is the reward.
 *
 * <h2>Why the three form a chain rather than three siblings</h2>
 *
 * <p>{@code root -> ponder_watched -> enigma}. The chain is not about unlock order - advancements
 * are awarded from code and a parent never gates anything - it is about <em>layout</em>.
 *
 * <p>{@code TreeNodePosition} lays out an advancement's children by iterating
 * {@code AdvancementNode.children()}, which is a {@code ReferenceOpenHashSet}: an unordered set
 * keyed on object identity. Siblings therefore come out in hash order, which is neither authorable
 * from a datapack nor even stable between game launches. Two siblings under the root would swap
 * places at random; a chain has only one possible order.
 */
public final class EnigmaAdvancements {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The tab. Never shown on its own; it exists so the other two have somewhere to live. */
    public static final ResourceLocation ROOT =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "root");

    /** Watching any ponder scene at all. Comes first, immediately under the tab. */
    public static final ResourceLocation PONDER_WATCHED =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "ponder_watched");

    /** Watching Create's own scene about the machine. */
    public static final ResourceLocation ENIGMA =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma");

    /**
     * The criterion key every advancement file declares.
     *
     * <p>Public so the GameTest can assert the files really declare it. {@code award()} returns
     * false for an unknown criterion instead of throwing, so a typo here would show up only as
     * "the advancement never appears" - the single most likely way to break this feature.
     */
    public static final String CRITERION = "impossible";

    private EnigmaAdvancements() {}

    /**
     * Awards the tab and {@code ponder_watched}. Idempotent: awarding an already-awarded criterion
     * returns false and changes nothing, so a player replaying a ponder is harmless.
     *
     * @return true when this call was the one that granted {@code ponder_watched}
     */
    public static boolean grantPonderWatched(ServerPlayer player) {
        return award(player, PONDER_WATCHED);
    }

    /**
     * Awards the tab, {@code ponder_watched} and {@code enigma}.
     *
     * <p>Watching the Enigma's scene is also watching a ponder, so the earlier advancement is
     * awarded here too rather than being left to a second packet. The two would otherwise race.
     *
     * @return true when this call was the one that granted {@code enigma}
     */
    public static boolean grantEnigma(ServerPlayer player) {
        award(player, PONDER_WATCHED);
        return award(player, ENIGMA);
    }

    private static boolean award(ServerPlayer player, ResourceLocation id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerAdvancementManager manager = server.getAdvancements();
        AdvancementHolder root = manager.get(ROOT);
        AdvancementHolder target = manager.get(id);

        if (root == null || target == null) {
            // A missing file here means a packaging mistake, and silently doing nothing would
            // look exactly like "the ponder hook does not work".
            LOGGER.error("Enigma advancements are missing from the datapack: root={} target={} ({})",
                    root != null, target != null, id);
            return false;
        }

        player.getAdvancements().award(root, CRITERION);
        return player.getAdvancements().award(target, CRITERION);
    }
}
