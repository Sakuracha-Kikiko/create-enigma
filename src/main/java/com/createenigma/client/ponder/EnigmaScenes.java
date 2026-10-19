package com.createenigma.client.ponder;

import com.createenigma.content.EnigmaCodeState;
import com.createenigma.registry.CEItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /** First wrench scene: calling on the machine, and being told what it wants first. */
    public static void callingTheMachine(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("enigma_wrench_1", "呼唤万机之神");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        text(scene, "若要呼唤伟大的万机之神……");
        scene.idle(80);

        scene.world().createItemEntity(new Vec3(2.5, 3.0, 2.5), new Vec3(0.0, 0.1, 0.0),
                new ItemStack(CEItems.ENIGMA_WRENCH.get()));
        scene.idle(10);

        scene.addKeyframe();
        text(scene, "……需要先取得祂的遗物，神秘扳手。");
        scene.idle(20);
    }

    /**
     * Second wrench scene: what the wrench is made of, and this world's three numbers.
     *
     * <p>The digits come from {@link EnigmaCodeSyncClient}, which the server fills in on login. They
     * are the fallback text itself rather than a translation argument: there is nothing about three
     * digits that a language could translate, and a lang entry would only be a second place for them
     * to go stale.
     */
    public static void theWrenchNumbers(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("enigma_wrench_2", "神秘扳手");
        scene.world().showSection(util.select().everywhere(), Direction.UP);
        scene.idle(20);

        scene.world().createItemEntity(new Vec3(2.5, 3.0, 2.5), new Vec3(0.0, 0.1, 0.0),
                new ItemStack(CEItems.ENIGMA_WRENCH.get()));
        scene.idle(10);

        text(scene, "你从低语中抓住了一些启示，");
        scene.idle(80);
        text(scene, "神秘扳手的基质与扳手并无不同，");
        scene.idle(80);
        text(scene, "但它拥有更精密的构件。");
        scene.idle(80);
        text(scene, "你还解读出了一些信息，");
        scene.idle(80);

        scene.addKeyframe();
        // The digits go through the lang entry's %s rather than being the displayed text: Ponder
        // always resolves the positional key and shows it when untranslated, so a line with no entry
        // prints "create_enigma.ponder.enigma_wrench_2.text_5" on screen. The first argument is a
        // fallback that Ponder does not render; it is passed here only to keep the call readable.
        scene.overlay().showText(60)
                .text(EnigmaCodeState.describe(null), EnigmaCodeState.describe(null))
                .pointAt(new Vec3(2.5, 1.0, 2.5))
                .placeNearTarget();
        scene.idle(80);

        text(scene, "……但那是什么意思？");
        scene.idle(20);
    }

    /** Every text line in these scenes is the same shape; this keeps them from drifting apart. */
    private static void text(SceneBuilder scene, String line) {
        scene.overlay().showText(60)
                .text(line)
                .pointAt(new Vec3(2.5, 1.0, 2.5))
                .placeNearTarget();
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
        // own later. Putting it in the structure rather than placing it at runtime is what makes the
        // reveal possible at all: a block set outside the schematic's bounds is quietly dropped.
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
        // The comparison gauge, revealed now rather than placed now: it was in the structure all
        // along, hidden by the subtract() above. This is the step that was missing when the gauge was
        // set at runtime instead - outside the schematic's bounds, the game drops it silently.
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
