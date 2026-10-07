package com.createenigma.client.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

/**
 * The machine as it was originally built, shown complete.
 *
 * <p>This is Create's {@code creativeMotorMojang} scene, step for step, with exactly one
 * difference: <b>the creative motor at (6,1,3) is revealed along with the belt run it drives.</b>
 *
 * <p>That motor is in the schematic - it is one of the two motors on the machine's main drive
 * network - but Create's scene never reveals it, because the selection that covers its neighbours
 * stops one cell short:
 *
 * <pre>fromTo(5, 1, 2, 7, 2, 1)   // x[5..7] y[1..2] z[1..2] - the motor is at z=3</pre>
 *
 * <p>It is not a mistake in the machine. Both Mojang's original trailer and Create's own trailer
 * show that motor, so the build is correct and the omission is in the scene. Rather than patch
 * Create's scene (which Ponder does not allow anyway), this scene shows the same machine with the
 * missing piece included.
 *
 * <p><b>Deliberately not spotlighted.</b> The motor is revealed in the same beat as the belt it
 * turns, with no separate pause and no text pointing at it. The scene shows a complete machine,
 * not a correction - the difference is there to be found by comparing, not to be announced. Adding
 * a beat here would turn a blueprint into an answer key.
 *
 * <p>Everything else - timings, camera turn, reveal order, the final idle - is copied from Create's
 * scene so the two read as the same machine.
 */
public final class EnigmaScenes {

    private EnigmaScenes() {}

    public static void firstOfAllMachines(SceneBuilder scene, SceneBuildingUtil util) {
        // Not localised through a lang key alone: the string below is the fallback, and
        // create_enigma.ponder.mojang_enigma_true.header is what actually gets displayed.
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
}
