package com.createenigma.client.ponder;

import com.createenigma.CreateEnigma;
import com.createenigma.registry.CEItems;
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

    /**
     * The wrench's item, which is what this chapter's scenes hang off now.
     *
     * <p>It used to be the core. The scenes changed: the machine's own scene is hidden for the time
     * being, and what is left teaches how the wrench is made - which belongs on the wrench, not on
     * something you need the wrench to obtain.
     */
    private static final ResourceLocation ENIGMA_WRENCH =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma_wrench");

    /** The core's item. Kept for the scene that is currently hidden; nothing else refers to it. */
    private static final ResourceLocation ENIGMA_CORE =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma_core");

    /**
     * The small platform the wrench scenes are staged on.
     *
     * <p>{@code addStoryBoard} has no overload without a schematic - every scene is placed on some
     * structure, even one whose only content is a floating wrench and some text. This is the
     * five-by-ten platform the scenes were written against, taken from the exported scene set.
     *
     * <p>The path follows Ponder's own rule: {@code <namespace>:ponder/<path>.nbt}. Ponder appends
     * the {@code ponder/} prefix and the {@code .nbt} suffix itself, so neither appears here.
     */
    private static final ResourceLocation PLATFORM_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "basic");

    /**
     * The machine as it stands: the scene author's own copy of Create's {@code creative_motor_mojang},
     * fifteen wide, five tall, fifteen deep.
     *
     * <p>Our copy rather than Create's own file: the scenes were framed, lit and pointed at from this
     * one, and a structure that drifts away from the schematic a scene was composed against is the
     * kind of silent difference nobody notices until the machine is not where the text points.
     */
    private static final ResourceLocation MACHINE_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "creative_motor_mojang");

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

    /**
     * 创造手摇曲柄, whose scene is earned by obtaining the item.
     *
     * <p>Gated separately from the wrench scenes: those want the {@code enigma} advancement, this one
     * wants {@code mystic_grace}, which the game grants when the item reaches the inventory. Two
     * separate answers, so two separate watcher flags - a single flag would have made one of the two
     * scenes appear for the wrong reason.
     */
    private static final ResourceLocation CREATIVE_HAND_CRANK =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "creative_hand_crank");

    /** The crank's own five-by-four stage, shipped with the mod. */
    private static final ResourceLocation CRANK_SCHEMATIC =
            ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "creative_hand_crank");

    @Override
    public String getModId() {
        return CreateEnigma.MOD_ID;
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        if (!EnigmaPonderUnlockWatcher.isUnlocked()) {
            return;
        }
        // Registering the tag only creates the chapter - it files nothing. The icon is the wrench,
        // because the wrench is what the chapter is about now.
        helper.registerTag(ENIGMA_TAG)
                .addToIndex()
                .item(CEItems.ENIGMA_WRENCH.get(), true, false)
                .title("神秘机械")
                .description("源初的万机之神，我祈求您降下您的目光")
                .register();

        // This is what actually puts the item in the chapter.
        //
        // Two things that look like they would do it, and do not:
        //   TagBuilder.item(item, icon, main) - sets the tag's icon items and nothing else
        //   addStoryBoard(..., tag)           - that vararg is a UI highlight tag
        // Neither touches the registry's component-to-tag map, which is what the tag screen
        // reads. Only addTagToComponent - reached here through the addToTag(...).add(...)
        // builder - puts an item under a tag.
        helper.addToTag(ENIGMA_TAG).add(ENIGMA_WRENCH);

        // The crank joins the same chapter, but only once it has been obtained. Filing it here
        // rather than in registerScenes keeps the two gates in one place: an item with a storyboard
        // but no tag has nowhere to appear, so both halves have to agree.
        if (EnigmaPonderUnlockWatcher.isCrankUnlocked()) {
            helper.addToTag(ENIGMA_TAG).add(CREATIVE_HAND_CRANK);
        }
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        if (!EnigmaPonderUnlockWatcher.isUnlocked()) {
            LOGGER.info("Enigma ponder: locked, registering nothing");
            return;
        }
        LOGGER.info("Enigma ponder: unlocked, registering wrench scenes for {} under tag {}",
                ENIGMA_WRENCH, ENIGMA_TAG);

        // The six scenes, in the order they were authored in. Ponder lists them under the item in
        // registration order, so this is the reading order the chapter shows.
        helper.addStoryBoard(ENIGMA_WRENCH, MACHINE_SCHEMATIC, EnigmaScenes::theMachine, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_WRENCH, PLATFORM_SCHEMATIC, EnigmaScenes::callingTheMachine, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_WRENCH, PLATFORM_SCHEMATIC, EnigmaScenes::theWrenchNumbers, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_WRENCH, MACHINE_SCHEMATIC, EnigmaScenes::theMysticMachine, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_WRENCH, PLATFORM_SCHEMATIC, EnigmaScenes::theOfferings, ENIGMA_TAG);
        helper.addStoryBoard(ENIGMA_WRENCH, MACHINE_SCHEMATIC, EnigmaScenes::callingMysticMachine, ENIGMA_TAG);

        // Earned by obtaining the crank, so it has its own gate.
        if (EnigmaPonderUnlockWatcher.isCrankUnlocked()) {
            LOGGER.info("Enigma ponder: crank unlocked, registering its scene under tag {}", ENIGMA_TAG);
            helper.addStoryBoard(CREATIVE_HAND_CRANK, CRANK_SCHEMATIC,
                    EnigmaScenes::creativeHandCrank, ENIGMA_TAG);
        }

        // Hidden for now, at the user's request, while the wrench scenes take its place. To bring it
        // back, this is the only line needed - the scene itself is untouched:
        //
        //   helper.addStoryBoard(ENIGMA_CORE, MOJANG_SCHEMATIC, EnigmaScenes::firstOfAllMachines,
        //           ENIGMA_TAG);
        //
        // If it does come back, the core also needs filing under the tag again:
        //
        //   helper.addToTag(ENIGMA_TAG).add(ENIGMA_CORE);
    }
}
