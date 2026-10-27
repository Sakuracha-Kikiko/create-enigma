package com.createenigma.client.ponder;

import com.createenigma.content.EnigmaCodeState;
import com.createenigma.content.EnigmaCoreBlock;
import com.createenigma.registry.CEBlocks;
import com.createenigma.registry.CEItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * The machine as it was originally built, and the two scenes that teach how to make the wrench.
 *
 * <h2>How Ponder localises text</h2>
 *
 * <p>The string handed to {@code text(...)} is a <b>fallback</b>, not a key. Ponder derives the key
 * itself from the scene's title id and the line's position in it:
 *
 * <pre>
 * scene.title("enigma_wrench_1", "...")   -&gt;  create_enigma.ponder.enigma_wrench_1.header
 * first .text("...")                      -&gt;  create_enigma.ponder.enigma_wrench_1.text_1
 * second                                  -&gt;  ...text_2
 * </pre>
 *
 * <p>Passing a translation key as the fallback therefore prints the key on screen and nothing else,
 * which is exactly what happened once. Write the sentence; put the translation in the lang file
 * under the derived key if one is wanted, and it wins over the fallback.
 *
 * <p><b>Which copy is on screen:</b> the lang file. {@code PonderLocalization.getSpecific} resolves
 * {@code I18n.get(create_enigma.ponder.<sceneId>.<key>)} and only falls back to the registered
 * string when nothing is registered for it, so once a line has a lang entry, editing the Java string
 * changes nothing at all. Every scene line is therefore kept in three places - the authored DSL, this
 * file, and {@code lang/zh_cn.json} + {@code lang/en_us.json} - and the two lang files are the ones
 * that must move when the wording does.
 *
 * <p>The digits are the exception: they differ per world, so there is nothing to translate. They are
 * passed as the fallback themselves, which means the line reads correctly with no lang entry at all.
 */
public final class EnigmaScenes {

    private EnigmaScenes() {}

    /**
     * The machine complete, with the motor Create's own scene leaves out.
     *
     * <p><b>Currently not registered.</b> The user asked for it to be hidden for now while the wrench
     * scenes take its place; the code is kept because it is the only record of the machine's original
     * arrangement, and it is one line to bring back.
     *
     * <p>It is Create's {@code creativeMotorMojang} scene, step for step, with one difference: the
     * creative motor at (6,1,3) is revealed along with the belt run it drives. That motor is in the
     * schematic - one of the two on the main drive network - but Create's scene never reveals it,
     * because the selection covering its neighbours stops one cell short:
     *
     * <pre>fromTo(5, 1, 2, 7, 2, 1)   // x[5..7] y[1..2] z[1..2] - the motor is at z=3</pre>
     *
     * <p>It is not a mistake in the machine. Both Mojang's trailer and Create's own show that motor,
     * so the build is correct and the omission is in the scene. It is revealed in the same beat as
     * the belt it turns, with no separate pause and no text pointing at it: the difference is there
     * to be found by comparing, not to be announced.
     */
    public static void firstOfAllMachines(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("mojang_enigma_true", "神的作品，万机之首，厄尼格默");
        scene.setNextUpEnabled(false);
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(.55f);
        scene.showBasePlate();
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(12, 1, 7, 12, 1, 2), Direction.WEST);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(5, 1, 2, 7, 2, 1), Direction.EAST);
        scene.idle(3);
        // The one change: Create's scene reveals only the belt here.
        scene.world().showSection(util.select().fromTo(7, 1, 3, 7, 1, 8)
                .add(util.select().position(6, 1, 3)), Direction.NORTH);
        scene.idle(3);
        scene.world().showSection(util.select().position(7, 2, 8), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(4, 1, 6), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(3, 1, 10), Direction.SOUTH);
        scene.idle(3);
        scene.world().showSection(util.select().position(1, 1, 11), Direction.EAST);
        scene.idle(3);
        scene.world().showSection(util.select().position(11, 1, 3), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(11, 2, 3, 11, 2, 7), Direction.NORTH);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(8, 1, 2, 10, 1, 2), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(11, 1, 2), Direction.SOUTH);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(6, 1, 8, 5, 1, 8), Direction.EAST);
        scene.rotateCameraY(-90);
        scene.idle(3);
        scene.world().showSection(util.select().position(12, 1, 10), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(11, 1, 12), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(8, 1, 8, 11, 1, 8), Direction.WEST);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(5, 2, 8, 5, 3, 8), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(8, 1, 5, 8, 2, 7), Direction.WEST);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(7, 3, 9, 8, 3, 8), Direction.UP);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(6, 3, 7, 9, 3, 7)
                .add(util.select().fromTo(6, 3, 8, 6, 3, 10))
                .add(util.select().fromTo(7, 3, 10, 9, 3, 10))
                .add(util.select().fromTo(9, 3, 7, 9, 3, 9)), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(10, 4, 7, 6, 4, 10), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(8, 1, 13, 8, 2, 11), Direction.NORTH);
        scene.idle(3);
        scene.idle(20);
    }

    // ---------------------------------------------------------------------------------------
    // 神秘机械 — the six ported scenes
    //
    // Hand-ported from the Ponderer export of 2026-10-27 (config/ponderer/scripts/enigma_wrench.json:
    // six scenes, 94 steps). The DSL file is what the author writes and what playback runs, so it -
    // not the generated Java module - is the specification these methods follow: step order,
    // positions, colours, durations and NBT are reproduced one for one.
    //
    // Two things in the export have no counterpart in Ponder's API:
    //
    //   · per-block entrance animations. Every placed block carries
    //     "entranceAnimation": "down"; Ponder places a block in a single tick and has no per-block
    //     reveal - only a whole section can fade in through showSection. The offerings appear at once.
    //   · the X component of rotate_camera_y ("degreesX"). Ponder's scene camera turns about Y only.
    //
    // Ponderer stays the authoring tool: change the scene there, re-export, and diff against these
    // methods - the shape of the diff is the instruction.
    // ---------------------------------------------------------------------------------------

    /** Where the machine's own scenes look: above its core, at (7, 3, 8) in a 15 × 5 × 15 structure. */
    private static final Vec3 HUB = new Vec3(7.5, 3.0, 8.5);

    /** Where the wrench scenes look: the middle of the 5 × 10 × 5 platform, one block above its face. */
    private static final Vec3 PLATFORM = new Vec3(2.5, 1.0, 2.5);

    /** Scene 0: looking at the machine, and failing to name it. */
    public static void theMachine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("enigma_machine", "Enigma");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        framed(scene, .5f, 270f);
        scene.idle(20);

        line(scene, 60, "在令人惊恐的低语之中，一幅神秘的画面涌入你的脑中", HUB);
        scene.idle(80);
        line(scene, 60, "……这是什么？", HUB);
        scene.idle(80);
        line(scene, 40, "不，这根本不是机械动力", HUB);
        scene.idle(50);
        line(scene, 40, "这太没有逻辑了", HUB);
        scene.idle(50);

        // The export's rotate_camera_y step, which turns on both axes: Ponderer stores degrees and
        // degreesX separately, and this scene has 180 in each. RotateSceneInstruction takes x first,
        // and is the only public way in - WorldInstructions offers rotateCameraY alone, which is why
        // this looked like a missing feature.
        scene.addInstruction(new RotateSceneInstruction(180f, 180f, true));
        scene.idle(20);

        line(scene, 60, "但你会明白祂的。", new Vec3(7.5, 0.0, 7.5), PonderPalette.RED).attachKeyFrame();
        scene.idle(70);
        line(scene, 60, "万机之神的作品，不容任何人玷污。", null, PonderPalette.RED);
        scene.idle(20);
    }

    /** Scene 1: the call, and the relic it requires. */
    public static void callingTheMachine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("enigma_wrench_1", "呼唤万机之神");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        line(scene, 60, "若要呼唤伟大的万机之神……", PLATFORM);
        scene.idle(80);

        tossWrench(scene);

        line(scene, 60, "……需要先取得祂的遗物，神秘扳手", PLATFORM).attachKeyFrame();
        scene.idle(20);
    }

    /**
     * Second wrench scene: what the wrench is made of, and this world's three numbers.
     *
     * <p>The digits come from {@link EnigmaCodeState}, which the server fills in on login. They are
     * the fallback text itself rather than a translation argument: there is nothing about three
     * digits that a language could translate, and a lang entry would only be a second place for them
     * to go stale.
     */
    public static void theWrenchNumbers(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("enigma_wrench_2", "神秘扳手");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        tossWrench(scene);

        line(scene, 60, "你从低语中抓住了一些启示，", PLATFORM);
        scene.idle(80);
        line(scene, 60, "神秘扳手的基质与扳手并无不同，", PLATFORM);
        scene.idle(80);
        line(scene, 60, "但它拥有更精密的构件", PLATFORM);
        scene.idle(80);
        line(scene, 60, "你还解读出了一些信息，", PLATFORM);
        scene.idle(80);

        // The digits go through the lang entry's %s rather than being the displayed text: Ponder
        // always resolves the positional key and shows it when untranslated, so a line with no entry
        // prints "create_enigma.ponder.enigma_wrench_2.text_5" on screen. The first argument is a
        // fallback that Ponder does not render; it is passed here only to keep the call readable.
        scene.overlay().showText(60)
                .text(EnigmaCodeState.describe(null), EnigmaCodeState.describe(null))
                .pointAt(PLATFORM)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        line(scene, 60, "……但那是什么意思？", PLATFORM);
        scene.idle(20);
    }

    /** Scene 3: what the machine is called, and what it is for. */
    public static void theMysticMachine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("mystic_machine", "神秘机械");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        framed(scene, .5f, 270f);
        scene.idle(20);

        line(scene, 60, "这是被称作\u201c神秘机械\u201d之物", HUB);
        scene.idle(80);
        line(scene, 60, "你并不知晓它从何而来，它又究竟是什么", HUB);
        scene.idle(80);
        line(scene, 60, "但你有预感，它将为你揭示一个神秘的新世界", HUB, PonderPalette.GREEN);
        scene.idle(80);
    }

    /**
     * Scene 4: the three offerings.
     *
     * <p>The gear train is placed at the coordinates the export gives it, z = 5 inclusive - one block
     * past the platform's far edge, since the structure is five deep (z = 0..4). That is allowed, but
     * it is not free: the scene's bounding box has to cover it first, which is what
     * {@link #preScanBounds} does at the top of the scene. Without that, the blocks are written into
     * the level and then never drawn - the same instruction sequence Create uses to show stress
     * gauges sitting off the edge of a schematic.
     */
    public static void theOfferings(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("mystic_offerings", "神秘之物");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        preScanBounds(scene, new BlockPos(1, 0, 1), new BlockPos(3, 2, 5));
        scene.idle(20);

        line(scene, 60, "据说神秘机械喜欢一些神秘的物品", PLATFORM);
        scene.idle(80);
        line(scene, 60, "这些神秘之物对新一代工程师们毫无用处，早已遗落在历史之中", PLATFORM);
        scene.idle(80);
        line(scene, 60, "但没关系，神秘的启示早已给予你一切", PLATFORM);
        scene.idle(80);
        line(scene, 40, "比如说……", PLATFORM).attachKeyFrame();
        scene.idle(40);

        offering(scene, util, new BlockPos(1, 0, 5),
                AllBlocks.LARGE_COGWHEEL.get().defaultBlockState(), 16f);
        offering(scene, util, new BlockPos(2, 1, 5), AllBlocks.COGWHEEL.get().defaultBlockState(), -32f);
        offering(scene, util, new BlockPos(3, 2, 5),
                AllBlocks.LARGE_COGWHEEL.get().defaultBlockState(), 16f);
        offering(scene, util, new BlockPos(3, 2, 4), AllBlocks.FLYWHEEL.get().defaultBlockState(), 16f);

        line(scene, 40, "象征旋转与静止的飞轮…", new Vec3(3.0, 2.0, 4.0));
        scene.idle(20);

        // A blaze burner, lit regardless of what the machine is made of, then extinguished by the cake.
        place(scene, util, new BlockPos(3, 1, 1), AllBlocks.BLAZE_BURNER.get().defaultBlockState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),
                tag -> tag.putBoolean("isCreative", true));
        scene.idle(10);

        scene.overlay().showControls(new Vec3(3.5, 2.0, 1.5), Pointing.DOWN, 20)
                .rightClick()
                .withItem(new ItemStack(CEItems.EXTINGUISHED_CAKE.get()));
        scene.idle(10);

        scene.world().modifyBlock(new BlockPos(3, 1, 1),
                state -> state.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING),
                true);
        scene.idle(10);

        line(scene, 40, "象征燃烧与熄灭的熄灭蛋糕…", new Vec3(3.5, 2.0, 1.5));
        scene.idle(20);

        scene.world().createItemEntity(new Vec3(1.5, 2.5, 2.5), new Vec3(0.0, 0.1, 0.0),
                new ItemStack(CEItems.HAND_OF_GOD.get()));
        scene.idle(30);

        line(scene, 40, "…以及象征机械与工具的神之手。", new Vec3(1.5, 1.0, 2.5));
        scene.idle(20);
    }

    /** Scene 5: what to do once the offerings have been accepted. */
    public static void callingMysticMachine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("mystic_call", "呼唤神秘机械");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        preScanBounds(scene, new BlockPos(7, 3, 8), new BlockPos(7, 3, 8));
        framed(scene, .5f, 270f);
        scene.idle(20);

        line(scene, 60, "若想取得神秘机械的认可……", HUB);
        scene.idle(80);
        line(scene, 60, "……需要先向祂献出你的诚意", HUB);
        scene.idle(80);
        line(scene, 60, "待你足够虔诚之后……", HUB);
        scene.idle(65);

        scene.overlay().showControls(new Vec3(7.5, 4.0, 8.5), Pointing.DOWN, 20)
                .rightClick()
                .withItem(new ItemStack(CEItems.ENIGMA_WRENCH.get()));
        scene.idle(25);

        scene.world().replaceBlocks(util.select().position(new BlockPos(7, 3, 8)),
                CEBlocks.ENIGMA_CORE.get().defaultBlockState()
                        .setValue(EnigmaCoreBlock.ASSEMBLED, true), true);
        scene.idle(5);

        line(scene, 60, "……使用神秘扳手来呼唤祂", HUB);
        scene.idle(20);
    }

    // ---------------------------------------------------------------------------------------
    // Shared step shapes
    // ---------------------------------------------------------------------------------------

    /**
     * The export's opening camera: half scale, turned a quarter past the front.
     *
     * <p>Ponderer places this step with duration 0, so the scene <em>starts</em> framed. Ponder's
     * public {@code rotateCameraY} eases the turn over the ticks that follow instead, which would
     * make the first second of every machine scene a sweep. Setting the current value and the chase
     * target together leaves nothing to ease.
     */
    private static void framed(SceneBuilder scene, float scale, float degrees) {
        scene.scaleSceneView(scale);
        scene.addInstruction(ps -> {
            LerpedFloat yRotation = ps.getTransform().yRotation;
            yRotation.startWithValue(yRotation.getChaseTarget() + degrees);
        });
    }

    /**
     * Grows the scene's bounding box to include a region before anything is placed in it.
     *
     * <p>This is Ponderer's {@code preScanBounds}, and it is how a scene shows a block that is not in
     * its schematic. Both write paths are gated on the bounds, in
     * {@code net.createmod.ponder.foundation.instruction}:
     *
     * <pre>
     * // ReplaceBlocksInstruction.runModification (line 29)
     * selection.forEach(pos -&gt; {
     *     if (!level.getBounds().isInside(pos))
     *         return;                                  // silently dropped
     *     level.setBlockAndUpdate(pos, stateToUse.apply(prevState));
     * });
     *
     * // BlockEntityDataInstruction.runModification (line 29) opens with the same guard
     * </pre>
     *
     * <p>So a placement past the edge is not drawn off-screen or clipped - it is never written at
     * all, and the section shown over it is empty; the NBT would be lost the same way.
     * {@code SchematicLevel.setBlock} does grow the box ({@code bounds = BBHelper.encapsulate(bounds,
     * pos)}, line 206), but that line is only reached after the guard passes, which is why growing it
     * here first is what makes the difference.
     *
     * <p>Rendering itself has no bounds check: {@code PonderScene}'s only {@code isInside} calls are
     * in {@code rayTraceScene}, for tooltips. {@code DebugScenes.offScreenScene} - "Out of bounds /
     * configureBasePlate", line 144 - shows blocks outside the base plate on purpose, captioned
     * "Blocks outside of the base plate do not affect scaling". Out-of-plate blocks display fine; it
     * is writing them that needs the box.
     */
    private static void preScanBounds(SceneBuilder scene, BlockPos from, BlockPos to) {
        scene.addInstruction(ps -> {
            ps.getWorld().getBounds().encapsulate(from);
            ps.getWorld().getBounds().encapsulate(to);
        });
    }

    /** One text line, pointing at {@code point} when there is one. */
    private static TextElementBuilder line(SceneBuilder scene, int duration, String text, Vec3 point) {
        return line(scene, duration, text, point, null);
    }

    /** The same, in a colour. Red is the god speaking; green is an answer that is not a warning. */
    private static TextElementBuilder line(SceneBuilder scene, int duration, String text, Vec3 point,
            PonderPalette colour) {
        TextElementBuilder builder = scene.overlay().showText(duration).text(text);
        if (point != null) {
            builder.pointAt(point);
        }
        if (colour != null) {
            builder.colored(colour);
        }
        return builder.placeNearTarget();
    }

    /** The wrench, thrown onto the platform from above, as both wrench scenes start. */
    private static void tossWrench(SceneBuilder scene) {
        scene.world().createItemEntity(new Vec3(2.5, 3.0, 2.5), new Vec3(0.0, 0.1, 0.0),
                new ItemStack(CEItems.ENIGMA_WRENCH.get()));
        scene.idle(10);
    }

    /** One offering: gear or flywheel, axled across the platform, already turning. */
    private static void offering(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos,
            BlockState state, float speed) {
        place(scene, util, pos, state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z),
                tag -> tag.putFloat("Speed", speed));
        scene.idle(2);
    }

    /**
     * One {@code set_block} out of the export: written without a redraw, patched, then revealed as a
     * section from above - the section is what carries the ten-tick drop into place.
     *
     * <p>The drop does not block the timeline. In the authored scene the {@code idle} after a
     * {@code set_block} is two ticks, while its {@code entranceDuration} is ten, so a row of
     * placements starts one after another and their drops overlap. Pausing for the whole ten ticks
     * here made each gear land complete before the next appeared, which both slowed the cascade by
     * five and hid the drop entirely - it read as blocks that had always been there.
     *
     * <p>{@code setBlock} on its own lands the block at full size in a single tick, which reads as a
     * different cut; that was one of the things that made this scene look unlike the authored one.
     * The NBT goes in here rather than in the placement because the gear model is chosen from the
     * speed its block entity is told about, so without the redraw it appears and then stands still.
     */
    private static void place(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos, BlockState state,
            Consumer<CompoundTag> nbt) {
        Selection selection = util.select().position(pos);
        scene.world().setBlock(pos, state, false);
        if (nbt != null) {
            scene.world().modifyBlockEntityNBT(selection, BlockEntity.class, nbt, true);
        }
        scene.world().showSection(selection, Direction.DOWN);
    }

    // ---------------------------------------------------------------------------------------
    // 创造手摇曲柄
    //
    // Written by hand from a Ponderer export rather than shipping the generated module: that
    // export needed a 99 KB, 2186-line runtime support library for one 60-line scene, and the file
    // header says it is overwritten on every export, which makes it a piece of the repository that
    // can never be maintained. The exported scene was used as the specification instead - its step
    // order, positions, durations and NBT are reproduced below.
    //
    // Ponderer is still the authoring tool. If the scene changes there, re-export and diff against
    // this method; the shape of the diff is the instruction.
    //
    // Two things the export could express and Ponder's own API cannot:
    //   · the layered structure reveal (Create's original reveals base, body and arm in turn).
    //     Here the plate and the remaining layers are shown in two beats, which is close enough
    //     that the arm still appears last on its own.
    //   · kinetic speed. Ponder has no setKineticSpeed; the crank is driven by writing its
    //     InUse/Backwards, which is the same state a player produces by holding right-click.
    // ---------------------------------------------------------------------------------------

    /**
     * 创造手摇曲柄: the crank, turning faster than anything hand-driven should.
     *
     * <p>{@code InUse} is a countdown in ticks and is what makes the crank a generator; while it is
     * positive {@code getGeneratedSpeed} returns ±32 and the whole network runs. {@code Backwards}
     * picks the direction. That is why no speed is written on the crank itself - only on the shafts
     * and the gauge column, which have nothing of their own.
     *
     * <p>The gauge column runs at twice the crank's speed and opposite to it, as in Create's own
     * scene, and carries a {@code Network} tag because a gauge's needle is computed from
     * stress/capacity rather than from speed.
     */
    public static void creativeHandCrank(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("creative_hand_crank", "用创造手摇曲柄产生旋转力");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        // Everything above the plate except the stressometer. That one is part of the structure too,
        // but it must not be visible yet - it is the comparison at the end - so it is revealed on its
        // own later. Being in the structure is what gives it a section: a block placed at runtime
        // arrives at full size with no section to slide in.
        scene.world().showSection(
                util.select().layersFrom(1).substract(util.select().position(1, 4, 3)), Direction.DOWN);
        scene.idle(10);

        Vec3 crank = new Vec3(2.5, 2.5, 2.5);
        Vec3 side = new Vec3(2.0, 2.5, 2.5);

        scene.overlay().showText(70)
                .text("创造手摇曲柄可以让玩家手动施加旋转力")
                .pointAt(new Vec3(2.5, 2.5, 2.0))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(crank, Pointing.DOWN, 40).rightClick();
        scene.idle(7);

        // Counter-clockwise: Backwards = false.
        crankNbt(scene, util, true);
        scene.effects().indicateSuccess(new BlockPos(1, 3, 3));
        scene.idle(10);

        scene.addKeyframe();
        scene.overlay().showText(50)
                .text("按住右键，让它逆时针旋转")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(35);

        // Stop before showing the other direction.
        stopNbt(scene, util);
        scene.idle(15);

        // Clockwise: sneak and hold right-click.
        scene.overlay().showControls(crank, Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(7);

        crankNbt(scene, util, false);
        scene.effects().indicateSuccess(new BlockPos(1, 3, 3));
        scene.idle(10);

        scene.addKeyframe();
        scene.overlay().showText(50)
                .text("潜行并按住右键，让它顺时针旋转")
                .pointAt(new Vec3(2.5, 2.5, 2.0))
                .placeNearTarget();
        scene.idle(35);

        stopNbt(scene, util);
        scene.idle(25);

        // The comparison: a second gauge on top of the column.
        // Revealed now rather than placed now: it was in the structure all along, hidden by the
        // subtract() above, so it can slide in as a section.
        scene.world().showSection(util.select().position(1, 4, 3), Direction.DOWN);
        scene.overlay().showText(60)
                .text("和手摇曲柄相比，创造手摇曲柄能提供大得多的旋转力")
                .pointAt(new Vec3(1.5, 4.0, 3.5))
                .placeNearTarget();
        scene.idle(70);

        scene.overlay().showControls(crank, Pointing.DOWN, 40).rightClick();
        scene.idle(7);

        crankNbt(scene, util, true);
        scene.effects().indicateSuccess(new BlockPos(1, 3, 3));
        scene.effects().indicateSuccess(new BlockPos(1, 4, 3));
        scene.idle(10);

        scene.addKeyframe();
        scene.overlay().showText(50)
                .text("……不过，它还是需要你自己来摇")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(35);

        stopNbt(scene, util);
        scene.idle(20);
    }

    /** Crank driving the network, shafts following it, gauge column at twice the speed, reversed. */
    private static void crankNbt(SceneBuilder scene, SceneBuildingUtil util, boolean counterClockwise) {
        float speed = counterClockwise ? 32f : -32f;
        modify(scene, util.select().fromTo(2, 2, 2, 2, 2, 2), tag -> {
            tag.putInt("InUse", 600);
            tag.putBoolean("Backwards", !counterClockwise);
            tag.putFloat("Speed", speed);
        });
        modify(scene, util.select().fromTo(2, 1, 2, 2, 1, 2), tag -> tag.putFloat("Speed", speed));
        // Create's own scene runs this column at f * -2.
        modify(scene, util.select().fromTo(1, 1, 3, 1, 3, 3), tag -> {
            tag.putFloat("Speed", speed * -2f);
            network(tag);
        });
    }

    /** Everything back to rest, so the next direction starts from a still machine. */
    private static void stopNbt(SceneBuilder scene, SceneBuildingUtil util) {
        modify(scene, util.select().fromTo(2, 2, 2, 2, 2, 2), tag -> {
            tag.putInt("InUse", 0);
            tag.putFloat("Speed", 0f);
        });
        modify(scene, util.select().fromTo(2, 1, 2, 2, 1, 2), tag -> tag.putFloat("Speed", 0f));
        modify(scene, util.select().fromTo(1, 1, 3, 1, 3, 3), tag -> {
            tag.putFloat("Speed", 0f);
            network(tag);
        });
    }

    /**
     * A plausible network for the gauges to read.
     *
     * <p>{@code Stress 0} of {@code Capacity 256} is the hand crank's own figure at 32 RPM, so a
     * stressometer shows a sensible needle instead of nothing. All members share one {@code Id}, or
     * they are not one network.
     */
    private static void network(CompoundTag tag) {
        CompoundTag network = new CompoundTag();
        network.putLong("Id", 1L);
        network.putFloat("Stress", 0.0f);
        network.putFloat("Capacity", 256.0f);
        network.putInt("Size", 4);
        tag.put("Network", network);
    }

    /**
     * Writes block entity data, with redraw on.
     *
     * <p>Redraw is what makes a changed {@code Speed} visible: the crank's arm recomputes its angle
     * every frame, but a shaft is a Flywheel instance that is only set up again when asked. Without
     * it the numbers change and the shafts keep turning as they were - or stand still.
     */
    private static void modify(SceneBuilder scene, Selection selection, Consumer<CompoundTag> patch) {
        scene.world().modifyBlockEntityNBT(selection, BlockEntity.class, patch, true);
    }
}
