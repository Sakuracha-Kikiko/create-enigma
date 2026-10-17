package com.createenigma.client.ponder;

import com.createenigma.content.EnigmaCodeState;
import com.createenigma.registry.CEItems;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
}
