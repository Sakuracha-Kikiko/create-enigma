package com.createenigma.client.ponder;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEBlocks;
import com.mojang.logging.LogUtils;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;

/**
 * This mod's ponder content: one tag, and the scenes filed under it.
 *
 * <h2>Why a tag is not optional</h2>
 *
 * <p>The ponder UI files items under tags, not the other way round. {@code PonderTagScreen.init}
 * builds its list from {@code PonderIndex.getTagAccess().getItems(tag)} - the items explicitly
 * filed under that tag - so <b>an item whose storyboards carry no tag has nowhere to appear.</b>
 * That is why the Enigma Core was invisible in the index despite its scene registering correctly:
 * the scene was never the problem, the filing was.
 *
 * <p>Every future scene in this mod belongs under {@link #ENIGMA_TAG}, so that the whole mod
 * reads as one chapter rather than as loose entries scattered through Create's categories.
 *
 * <h2>What this costs</h2>
 *
 * <p>Being in a tag makes the scene visible to everyone, always. There is no supported way to
 * show a ponder scene to some players and not others: scenes are registered globally at client
 * startup, and the one filter that exists ({@code IndexExclusionHelper}) is <b>item-level and only
 * applied by {@code PonderIndexScreen}</b> - {@code PonderTagScreen} has no such filter at all.
 * A gate built on it would therefore hide the item on one screen and not the other, which is worse
 * than no gate. So there is none, and the scene is visible from the start.
 *
 * <p>Making it conditional needs a mixin on the ponder UI. That is a deliberate decision to patch
 * someone else's screen, and it should be taken knowing it can break silently on a Ponder update.
 */
public class EnigmaPonderPlugin implements PonderPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The chapter every scene in this mod is filed under. */
    public static final ResourceLocation ENIGMA_TAG =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma");

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
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(ENIGMA_TAG)
                .addToIndex()
                .item(CEBlocks.ENIGMA_CORE.get(), true, false)
                .title("神秘机械")
                .description("源初的万机之神，我祈求您降下您的目光")
                .register();
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Logged because a scene that never registers looks exactly like a scene that registered
        // but whose item is not filed anywhere - both leave the index empty.
        LOGGER.info("Enigma ponder: registering scene for {} under tag {}, schematic {}",
                ENIGMA_CORE, ENIGMA_TAG, MOJANG_SCHEMATIC);
        helper.addStoryBoard(ENIGMA_CORE, MOJANG_SCHEMATIC, EnigmaScenes::firstOfAllMachines, ENIGMA_TAG);
    }
}
