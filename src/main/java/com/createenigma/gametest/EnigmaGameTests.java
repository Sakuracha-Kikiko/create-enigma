package com.createenigma.gametest;

import java.util.ArrayList;
import java.util.List;

import com.createenigma.content.EnigmaAdvancements;
import com.createenigma.content.EnigmaCoreBlock;
import com.createenigma.registry.CEBlocks;
import com.createenigma.structure.BlockStateMatcher;
import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaValidator;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * End-to-end checks that the machine is recognised in a real world.
 *
 * <p>These build the machine through vanilla's own structure placement rather than setting blocks
 * from the template: that is the only path that creates block entities and lets Create link its
 * belts and kinetic networks, so it is the only path that produces a fair test of the comparison
 * the mod actually performs.
 */
@GameTestHolder("create_enigma")
public class EnigmaGameTests {

    /**
     * NeoForge prefixes this with the lowercased class name, so it resolves to
     * {@code create_enigma:enigmagametests.empty}.
     */
    private static final String TEMPLATE = "empty";

    /** Where the core sits inside the template; the machine's origin follows from it. */
    private static final BlockPos CORE = new BlockPos(7, 2, 8);

    /** A spruce slab of the shed's floor - inert, and unambiguously part of the machine. */
    private static final BlockPos A_SHED_SLAB = new BlockPos(6, 2, 7);

    /** Long enough for the block entity's settle delay plus a periodic check. */
    private static final int SETTLE_TICKS = 40;

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void completeMachineAssemblesItsCore(GameTestHelper helper) {
        EnigmaStructure structure = EnigmaStructure.get();
        helper.assertTrue(structure != null, "structure template could not be read from the jar");
        helper.assertTrue(structure.entries().size() > 70,
                "template has only " + structure.entries().size() + " blocks; expected the full machine");

        ServerLevel level = helper.getLevel();
        helper.assertTrue(EnigmaValidator.place(level, helper.absolutePos(CORE)),
                "the structure template could not be placed");

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            BlockState core = helper.getBlockState(CORE);
            helper.assertTrue(core.getValue(EnigmaCoreBlock.ASSEMBLED),
                    "core did not assemble; validator says: " + describe(helper));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void removingOneBlockDisassemblesIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(EnigmaValidator.place(level, helper.absolutePos(CORE)),
                "the structure template could not be placed");

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                    "machine never assembled, so the disassembly half cannot be judged");

            helper.setBlock(A_SHED_SLAB, Blocks.AIR);

            helper.runAfterDelay(SETTLE_TICKS, () -> {
                helper.assertFalse(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                        "core stayed assembled after " + A_SHED_SLAB + " was removed");
                helper.succeed();
            });
        });
    }

    /**
     * Runs the exact command the README tells players to run.
     *
     * <p>Written after shipping a README that said {@code /place structure}: that subcommand only
     * accepts <b>worldgen</b> structures from the {@code structure} registry, so it never offered
     * this mod's template and the machine could not be placed by the documented route at all.
     * Template placement itself had been verified through the structure manager, which is exactly
     * why the mistake survived - the entry point a player actually types was never exercised.
     *
     * <p>So this test asserts the two things a player depends on: that the id shows up in tab
     * completion, and that running the command really builds a machine that then assembles.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void theDocumentedCommandPlacesTheMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        // Tab completion after "/place template " is StructureTemplateManager.listTemplates().
        String id = EnigmaStructure.TEMPLATE_ID.toString();
        helper.assertTrue(
                server.getStructureManager().listTemplates().anyMatch(t -> t.toString().equals(id)),
                "/place template will not suggest " + id + " - it is not in the template list");

        // Exactly the documented command, run through the real dispatcher.
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        String command = "place template " + id + " "
                + origin.getX() + " " + origin.getY() + " " + origin.getZ();

        CommandSourceStack source = server.createCommandSourceStack()
                .withLevel(level)
                .withPosition(Vec3.atLowerCornerOf(origin))
                .withSuppressedOutput();

        // performPrefixedCommand returns void, so the assertion is on the effect rather than on a
        // result code - which is the stronger check anyway: a rejected command simply builds
        // nothing, and the machine then fails to assemble.
        server.getCommands().performPrefixedCommand(source, command);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                    "the documented command /" + command + " did not build an assembled machine");
            helper.succeed();
        });
    }

    // --------------------------------------------------------------------------------
    // The advancement the ponder unlocks.
    //
    // The trigger itself cannot be tested here: it lives in a client-side mixin on Ponder, and a
    // GameTest has no client. What is testable is the whole server half - the two files loading,
    // the child hanging off the root, and the criterion key matching what the code awards.
    //
    // NOT covered: the award() call itself. Getting a ServerPlayer in a GameTest means
    // makeMockServerPlayerInLevel(), and that crashes this pack - the fake login fires Create's
    // PlayerLoggedInEvent, which tries to send a network payload to a player with no connection
    // and throws. So the assertion below stops one step short of the call, at the thing that
    // would actually be wrong: a criterion key that does not match.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void ponderAdvancementLoadsAndDeclaresTheAwardedCriterion(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();

        AdvancementHolder root = server.getAdvancements().get(EnigmaAdvancements.ROOT);
        AdvancementHolder enigma = server.getAdvancements().get(EnigmaAdvancements.ENIGMA);
        helper.assertTrue(root != null, "advancement " + EnigmaAdvancements.ROOT + " did not load");
        helper.assertTrue(enigma != null, "advancement " + EnigmaAdvancements.ENIGMA + " did not load");
        helper.assertTrue(root.value().isRoot(), EnigmaAdvancements.ROOT + " is not a root advancement");

        // The root is what creates the tab. If the parent link were wrong the advancement would
        // still be granted but would be displayed nowhere - a failure nothing else would catch.
        helper.assertTrue(enigma.value().parent().isPresent()
                        && enigma.value().parent().get().equals(EnigmaAdvancements.ROOT),
                EnigmaAdvancements.ENIGMA + " does not declare " + EnigmaAdvancements.ROOT + " as its parent");

        // award() returns false for an unknown criterion rather than throwing, so this is the
        // assertion that stands between a typo and a feature that silently never appears.
        helper.assertTrue(enigma.value().criteria().containsKey(EnigmaAdvancements.CRITERION),
                EnigmaAdvancements.ENIGMA + " does not declare the criterion '"
                        + EnigmaAdvancements.CRITERION + "'; it declares " + enigma.value().criteria().keySet());
        helper.assertTrue(root.value().criteria().containsKey(EnigmaAdvancements.CRITERION),
                EnigmaAdvancements.ROOT + " does not declare the criterion '"
                        + EnigmaAdvancements.CRITERION + "'");

        helper.succeed();
    }

    private static String describe(GameTestHelper helper) {
        EnigmaStructure structure = EnigmaStructure.get();
        if (structure == null) {
            return "(no template)";
        }
        EnigmaValidator.Result result = EnigmaValidator.validate(
                helper.getLevel(), helper.absolutePos(CORE), structure);
        return result.status() + " at " + result.failingPos() + " (" + result.detail() + ")";
    }

    // --------------------------------------------------------------------------------
    // The comparison policy itself.
    //
    // This is the single decision the whole mod rests on, and the one most likely to be got
    // wrong in a way that only shows up in play: too strict and a correct build is rejected for
    // reasons the player cannot see, too lenient and any pile of blocks counts as the machine.
    // These assertions are pure logic, so they need no world and cannot be flaky.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void matcherIgnoresDerivedPropertiesButNotRealChanges(GameTestHelper helper) {
        BlockState drySlab = Blocks.SPRUCE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false);

        helper.assertTrue(BlockStateMatcher.matches(drySlab,
                        drySlab.setValue(BlockStateProperties.WATERLOGGED, true)),
                "waterlogged must be ignored - it flips with rain and neighbouring water");
        helper.assertFalse(BlockStateMatcher.matches(drySlab,
                        drySlab.setValue(SlabBlock.TYPE, SlabType.TOP)),
                "slab type must NOT be ignored - a top slab is a different build");

        BlockState bareGrass = Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyDirtBlock.SNOWY, false);
        helper.assertTrue(BlockStateMatcher.matches(bareGrass,
                        bareGrass.setValue(SnowyDirtBlock.SNOWY, true)),
                "snowy must be ignored - it depends on the biome");

        helper.assertFalse(BlockStateMatcher.matches(drySlab, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                "a different block must never match");

        // Create's own blocks, where the derived-state problem is worst.
        BlockState belt = AllBlocks.BELT.get().defaultBlockState();
        helper.assertFalse(BlockStateMatcher.matches(belt, cycle(belt, "slope")),
                "belt slope must NOT be ignored - it is the shape of the machine");
        helper.assertFalse(BlockStateMatcher.matches(belt, cycle(belt, "part")),
                "belt part must NOT be ignored");
        helper.assertFalse(BlockStateMatcher.matches(belt, cycle(belt, "casing")),
                "belt casing must NOT be ignored - an encased belt is a different build");

        // Pane connections are recomputed from neighbouring panes and are purely cosmetic.
        BlockState pane = AllPaletteBlocks.OAK_WINDOW_PANE.get().defaultBlockState();
        helper.assertTrue(BlockStateMatcher.matches(pane, cycle(pane, "north")),
                "window pane connections must be ignored");

        // The invariant that keeps the machine from disassembling itself: the template stores the
        // core as assembled=false, but once formed the world holds assembled=true. If this
        // property were compared, every check after the first would tear the machine down again.
        BlockState core = CEBlocks.ENIGMA_CORE.get().defaultBlockState();
        helper.assertTrue(BlockStateMatcher.matches(core,
                        core.setValue(EnigmaCoreBlock.ASSEMBLED, true)),
                "the core's own assembled property must be ignored");

        helper.succeed();
    }

    /** Advances a property to its next possible value, whatever its type. */
    private static BlockState cycle(BlockState state, String propertyName) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(propertyName);
        if (property == null) {
            throw new IllegalStateException("block " + state.getBlock() + " has no property " + propertyName);
        }
        return cycle(state, property);
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        T next = values.get((values.indexOf(state.getValue(property)) + 1) % values.size());
        return state.setValue(property, next);
    }
}
