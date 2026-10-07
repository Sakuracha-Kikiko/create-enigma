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
 * <h2>Registration is a gate, because it can be run again</h2>
 *
 * <p>Both methods below return early unless the scene has been earned. That is not a trick: the
 * two registration callbacks are re-run every time {@code PonderIndex.reload()} is called, so
 * "should this scene exist" can be answered fresh each time, from live client state.
 *
 * <p>{@code PonderIndex.reload()} is public API and does exactly this:
 *
 * <pre>
 * LOCALIZATION.clearShared();
 * SCENES.clearRegistry();   // clears the multimap AND sets allowRegistration = true
 * TAGS.clearRegistry();
 * registerAll();            // re-runs every plugin's registerScenes / registerTags
 * gatherSharedText();
 * </pre>
 *
 * <p>Without that, a scene could only ever be registered once, at client startup, when no world
 * exists and no player state can be read - which is why this was thought impossible earlier.
 * {@link EnigmaPonderUnlockWatcher} decides when to call it.
 *
 * <h2>Why a tag is not optional</h2>
 *
 * <p>The ponder UI files items under tags, not the other way round. {@code PonderTagScreen.init}
 * builds its list from {@code PonderIndex.getTagAccess().getItems(tag)} - the items explicitly
 * filed under that tag - so <b>an item whose storyboards carry no tag has nowhere to appear.</b>
 * That is why the Enigma Core was once invisible in the index despite its scene registering
 * correctly: the scene was never the problem, the filing was.
 *
 * <p>The tag itself is gated too. Right now the scene is the only thing in this mod, so an
 * unconditional chapter would announce that something exists while its contents stayed hidden -
 * the tag's mere presence is a hint. When a scene that should always be visible is added, the tag
 * registration moves out of the gate and stays.
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
        if (!EnigmaPonderUnlockWatcher.isUnlocked()) {
            return;
        }
        helper.registerTag(ENIGMA_TAG)
                .addToIndex()
                .item(CEBlocks.ENIGMA_CORE.get(), true, false)
                .title("神秘机械")
                .description("源初的万机之神，我祈求您降下您的目光")
                .register();
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        if (!EnigmaPonderUnlockWatcher.isUnlocked()) {
            LOGGER.info("Enigma ponder: locked, registering nothing");
            return;
        }
        LOGGER.info("Enigma ponder: unlocked, registering scene for {} under tag {}",
                ENIGMA_CORE, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_CORE, MOJANG_SCHEMATIC, EnigmaScenes::firstOfAllMachines, ENIGMA_TAG);
    }
}
