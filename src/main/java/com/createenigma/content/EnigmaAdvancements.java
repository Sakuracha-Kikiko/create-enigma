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
     * The core standing assembled for the first time - the machine actually working.
     *
     * <p>Last in the chain, and the only one that is not about seeing something. The three before it
     * are all "you have been shown"; this one is "you built it".
     */
    public static final ResourceLocation MYSTIC_AGE =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "mystic_age");

    /**
     * 万机之神的侧目 - crafting the Enigma Wrench.
     *
     * <p>Sits under {@code enigma}, and unlike the four before it this one is <b>not</b> awarded from
     * code: its file uses {@code minecraft:recipe_crafted} with
     * {@code recipe_id: create_enigma:enigma_wrench}, so the game fires it itself the moment that
     * recipe produces a result. No hook, no tick check, and it cannot fire for anything else.
     */
    public static final ResourceLocation GODS_GLANCE =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "gods_glance");

    /**
     * 神秘恩典 - obtaining the creative hand crank.
     *
     * <p>Sits under {@code mystic_age}. Also trigger-driven: {@code minecraft:inventory_changed}
     * watching for the item, because the crank has no recipe yet. Once one exists this can move to
     * {@code recipe_crafted} exactly like {@link #GODS_GLANCE}; the only thing that changes is the
     * criteria block in the json.
     */
    public static final ResourceLocation MYSTIC_GRACE =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "mystic_grace");

    /**
     * 鱼鱼熄灭 - eating the extinguished cake.
     *
     * <p>Hangs under {@link #GODS_GLANCE}, alongside {@code mystic_age}'s subtree. Trigger-driven:
     * {@code minecraft:consume_item} with an item predicate, fired by {@code LivingEntity.eat} for
     * any food. The cake is a plain {@code Item} with food properties, so eating it needs no code
     * and neither does this.
     */
    public static final ResourceLocation SNUFFED_OUT =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "snuffed_out");

    /**
     * 主将从现 - writing a blank blueprint into the Enigma blueprint.
     *
     * <p>Hangs under {@link #GODS_GLANCE}. This one <b>is</b> awarded from code, because the ritual
     * has no vanilla trigger to hang on: the wrench takes the right click from
     * {@code PlayerInteractEvent.RightClickBlock} before the placard is ever asked, so no
     * {@code item_used_on_block} fires. {@code BlueprintBlessing} is the only place a blank sheet
     * becomes a written one, so it is the only place that awards this.
     */
    public static final ResourceLocation SHAPE_REMEMBERED =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "shape_remembered");

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

    /**
     * Awards everything above, plus {@code mystic_age}, for a core that has just assembled.
     *
     * <p>The whole chain is awarded rather than just the new one. A player can assemble the machine
     * without ever having watched the scene that precedes it - they could have been told how, or
     * found the blueprint and worked it out - and a node whose parent is unawarded still renders,
     * but a tab whose root is unawarded does not. Awarding the chain keeps the tab visible and the
     * layout intact no matter which route the player took to get here.
     *
     * @return true when this call was the one that granted {@code mystic_age}
     */
    public static boolean grantMysticAge(ServerPlayer player) {
        award(player, PONDER_WATCHED);
        award(player, ENIGMA);
        return award(player, MYSTIC_AGE);
    }

    /**
     * Awards the tab and the two steps above {@code shape_remembered}, for a blueprint just written.
     *
     * <p>A player can receive the wrench from the creative inventory instead of crafting it, in which
     * case {@code gods_glance} was never granted and this node hangs off nothing. That is fine -
     * an unawarded parent still renders - but the tab itself depends on the root, so the chain above
     * is granted here for the same reason {@link #grantMysticAge} does it.
     *
     * <p>{@code gods_glance} is deliberately not granted: it is trigger-driven and awarding it from
     * code would hand it out for something the player did not do.
     *
     * @return true when this call was the one that granted {@code shape_remembered}
     */
    public static boolean grantShapeRemembered(ServerPlayer player) {
        award(player, PONDER_WATCHED);
        award(player, ENIGMA);
        return award(player, SHAPE_REMEMBERED);
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
