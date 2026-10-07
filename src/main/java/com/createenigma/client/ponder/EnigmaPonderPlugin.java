package com.createenigma.client.ponder;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaAdvancements;
import com.createenigma.registry.CEBlocks;

import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;

/**
 * This mod's ponder content: one scene, showing the machine complete.
 *
 * <h2>It hangs off the Enigma Core, not off Create's Creative Motor</h2>
 *
 * <p>Attaching it to the Creative Motor would put it directly under Create's own scene in the
 * index, which is the one place it must not be: Ponder gives no way to show a scene to some
 * players and not others, so from the moment it existed every player would see a second, complete
 * version of the machine sitting next to the first one. That is the entire secret, given away on
 * the index screen before anyone has done anything.
 *
 * <p>Hanging it off this mod's own block makes it gatable - see {@link #indexExclusions}. The
 * cost is that it is no longer adjacent to the scene it answers, which is a real loss; the scene
 * has to stand on its own.
 *
 * <h2>Gating</h2>
 *
 * <p>{@code IndexExclusionHelper} takes a {@code Predicate<ItemLike>}, and those predicates are
 * evaluated when the index screen is built ({@code PonderIndexScreen.isItemIncluded} streams over
 * them), not when they are registered. A predicate can therefore read live client state - here,
 * whether the player has the Enigma advancement - which is what makes gating possible at all.
 *
 * <p>Only the <em>item</em> can be hidden, never a single scene; that is why the gate works by
 * hiding the core rather than by hiding the scene. The consequence is that a player who already
 * holds an Enigma Core can still ponder it directly even when it is hidden from the index - the
 * gate is a "don't spoil it" measure, not a lock.
 */
public class EnigmaPonderPlugin implements PonderPlugin {

    /** The core's item, which is what our scene hangs off. */
    private static final ResourceLocation ENIGMA_CORE =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma_core");

    /**
     * Create's schematic for the machine.
     *
     * <p>Note the path: <b>no {@code ponder/} prefix and no {@code .nbt} suffix.</b> Ponder builds
     * the real file path itself - {@code <namespace>:ponder/<path>.nbt} - and when it cannot find
     * the result it logs an error and returns an <em>empty</em> structure template rather than
     * throwing. Passing the full path therefore produces a scene with nothing in it but the
     * baseplate, and no visible sign of what went wrong.
     */
    private static final ResourceLocation MOJANG_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath("create", "creative_motor_mojang");

    @Override
    public String getModId() {
        return CreateEnigma.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(ENIGMA_CORE, MOJANG_SCHEMATIC, EnigmaScenes::firstOfAllMachines);
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        helper.exclude(item -> isEnigmaCore(item) && !hasWatchedTheEnigma());
    }

    private static boolean isEnigmaCore(net.minecraft.world.level.ItemLike item) {
        return item.asItem() == CEBlocks.ENIGMA_CORE.get().asItem();
    }

    /**
     * Whether the player has watched Create's scene for this machine.
     *
     * <p>The client is the only side that can answer this: Ponder is client-side, and the client
     * already holds the player's advancement list because the server sends it. No extra state and
     * no extra packet are needed.
     *
     * <p>Never called before a world is loaded - the predicate only runs when the index screen is
     * opened - so the null checks are for the main-menu edge case rather than for normal play.
     */
    private static boolean hasWatchedTheEnigma() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        return connection.getAdvancements().get(EnigmaAdvancements.ENIGMA) != null;
    }
}
