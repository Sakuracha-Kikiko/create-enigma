package com.createenigma.gametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaAdvancements;
import com.createenigma.content.EnigmaCoreBlock;
import com.createenigma.registry.CEBlocks;
import com.createenigma.registry.CEItems;
import com.createenigma.structure.BlockStateMatcher;
import com.createenigma.structure.EnigmaSchematic;
import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.createenigma.structure.EnigmaValidator;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.schematics.SchematicItem;

import org.slf4j.Logger;

import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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

    private static final Logger LOGGER = LogUtils.getLogger();

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

    // --------------------------------------------------------------------------------
    // Rotation.
    //
    // This is the test that matters for the turning support, and it is not about tidiness: the
    // offsets in EnigmaStructure.offsetFor are hand-written sign flips that have to agree with
    // vanilla's own rotation convention. Get one backwards and nothing fails loudly - a machine
    // placed turned one way simply never assembles. Placing through vanilla and then validating
    // through our own maths is what forces the two to agree.
    //
    // All three non-trivial orientations are checked, not just one: 90 and 270 are the pair most
    // likely to be swapped, and 180 would still pass if the two were.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void turnedMachinesAreRecognised(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(CORE);

        helper.assertTrue(EnigmaValidator.place(level, core, Rotation.CLOCKWISE_90),
                "the structure template could not be placed turned 90 degrees");

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                    "a machine turned 90 degrees was not recognised; validator says: " + describe(helper));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void machinesTurnedUpsideDownInPlanAreRecognised(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(CORE);

        helper.assertTrue(EnigmaValidator.place(level, core, Rotation.CLOCKWISE_180),
                "the structure template could not be placed turned 180 degrees");

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                    "a machine turned 180 degrees was not recognised; validator says: " + describe(helper));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void machinesTurnedCounterclockwiseAreRecognised(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(CORE);

        helper.assertTrue(EnigmaValidator.place(level, core, Rotation.COUNTERCLOCKWISE_90),
                "the structure template could not be placed turned 270 degrees");

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(CORE).getValue(EnigmaCoreBlock.ASSEMBLED),
                    "a machine turned 270 degrees was not recognised; validator says: " + describe(helper));
            helper.succeed();
        });
    }

    // --------------------------------------------------------------------------------
    // The first generation, which is what a survival player can actually build.
    //
    // It is derived from the complete template rather than shipped as a second file, so these
    // tests are also the check that the derivation says what it is supposed to: a chest, six
    // motors, and the disputed one gone.
    //
    // The disguised motor does not have to be placed here. Real Creative Motors are accepted in
    // its position - that is what "it is not picky" means, and it is what lets a machine built
    // either way read the same.
    // --------------------------------------------------------------------------------

    /** The disputed motor's position, relative to the test's own origin. */
    private static BlockPos disputedMotorPos(EnigmaStructure structure) {
        return CORE.offset(EnigmaStructure.DISPUTED_MOTOR.subtract(structure.coreOffset()));
    }

    /** Turns a freshly placed complete machine into the first generation, in the world. */
    private static void downgradeToFirstGeneration(GameTestHelper helper, EnigmaStructure structure) {
        helper.setBlock(CORE, Blocks.CHEST);
        helper.setBlock(disputedMotorPos(structure), Blocks.AIR);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void firstGenerationReadsAsComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(CORE);
        EnigmaStructure structure = EnigmaStructure.get();

        helper.assertTrue(EnigmaValidator.place(level, anchor), "template could not be placed");
        downgradeToFirstGeneration(helper, structure);

        EnigmaValidator.Report report =
                EnigmaValidator.analyse(level, anchor, structure, Generation.PRIMAL);

        helper.assertTrue(report.isComplete(),
                "the first generation did not read as complete: " + report.placed() + "/" + report.total()
                        + ", motors missing " + report.motorGaps());
        helper.assertTrue(EnigmaValidator.validate(level, anchor, structure, Generation.PRIMAL).isFormed(),
                "the first generation did not validate as formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void missingMotorsAreReportedSeparately(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(CORE);
        EnigmaStructure structure = EnigmaStructure.get();

        helper.assertTrue(EnigmaValidator.place(level, anchor), "template could not be placed");
        downgradeToFirstGeneration(helper, structure);

        // Everything a survival player can obtain is now in place; only the motors are absent.
        //
        // GameTest positions are relative to the test's own origin, while template offsets are
        // relative to the template's corner, so each entry has to be rebased onto the core.
        int removed = 0;
        for (EnigmaStructure.Entry entry : structure.entries(Generation.PRIMAL)) {
            BlockState state = entry.state();
            if (state.is(CEBlocks.DISGUISED_MOTOR.get()) || state.is(AllBlocks.CREATIVE_MOTOR.get())) {
                helper.setBlock(CORE.offset(entry.offset().subtract(structure.coreOffset())), Blocks.AIR);
                removed++;
            }
        }
        helper.assertTrue(removed == 6, "expected six motors in the first generation, found " + removed);

        EnigmaValidator.Report report = EnigmaValidator.analyse(level, anchor, structure, Generation.PRIMAL);

        helper.assertTrue(!report.isComplete(), "the machine should not read as complete with no motors");
        helper.assertTrue(report.onlyMotorsMissing(),
                "expected the only gaps to be motors; got " + report.placed() + "/" + report.total()
                        + " with " + report.motorGaps() + " motor gaps");
        helper.succeed();
    }

    /**
     * A belt run laid the other way round is still the right belt.
     *
     * <p>Reported from play: the wrench said a block "should face south but faces north", and the
     * block was a belt. Create does not let a builder pick a belt's direction - you click one shaft,
     * then the other, and which end you started from decides both {@code facing} and whether each
     * block is START or END. On a flat run both outcomes are valid belts along the same line.
     *
     * <p>That mattered more here than it would elsewhere: the belts were deliberately left out of
     * the blueprint, so a builder has nothing to copy the direction from. Comparing it would be
     * asking them to guess, and the guess has no wrong answer to learn from - only a machine that
     * refuses to be finished.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void aBeltRunLaidTheOtherWayRoundStillCounts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(CORE);
        EnigmaStructure structure = EnigmaStructure.get();

        helper.assertTrue(EnigmaValidator.place(level, anchor), "template could not be placed");
        downgradeToFirstGeneration(helper, structure);

        int flipped = 0;
        for (EnigmaStructure.Entry entry : structure.entries(Generation.PRIMAL)) {
            BlockState state = entry.state();
            if (!state.is(AllBlocks.BELT.get())) {
                continue;
            }

            // Everything that encodes which way items travel: the horizontal direction, which end
            // is START, and - on a diagonal run - whether it reads as going up or down.
            BlockState other = state.setValue(BeltBlock.HORIZONTAL_FACING,
                    state.getValue(BeltBlock.HORIZONTAL_FACING).getOpposite());
            other = switch (other.getValue(BeltBlock.PART)) {
                case START -> other.setValue(BeltBlock.PART, BeltPart.END);
                case END -> other.setValue(BeltBlock.PART, BeltPart.START);
                default -> other;
            };
            other = switch (other.getValue(BeltBlock.SLOPE)) {
                case UPWARD -> other.setValue(BeltBlock.SLOPE, BeltSlope.DOWNWARD);
                case DOWNWARD -> other.setValue(BeltBlock.SLOPE, BeltSlope.UPWARD);
                default -> other;
            };

            helper.setBlock(CORE.offset(entry.offset().subtract(structure.coreOffset())), other);
            flipped++;
        }
        helper.assertTrue(flipped > 0, "there were no belts to lay the other way round");

        EnigmaValidator.Result result =
                EnigmaValidator.validate(level, anchor, structure, Generation.PRIMAL);
        helper.assertTrue(result.isFormed(),
                "a belt run laid the other way round was rejected: " + result.failingPos() + " "
                        + result.detail() + " (" + flipped + " belts flipped)");
        helper.succeed();
    }

    // --------------------------------------------------------------------------------
    // The blueprint.
    //
    // This is what keeps the generated file honest. It is built from the same list the validator
    // checks against, so the two cannot disagree by construction - but "cannot by construction" is
    // exactly the kind of claim that quietly stops being true after a refactor. Placing the
    // generated NBT and reading it back through the wrench's own path makes it a fact rather than
    // an intention.
    // --------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------
    // The blueprint item.
    //
    // This test exists because of a crash. The first version of the blueprint set two data
    // components - the file name and the owner - on the reasonable-sounding theory that a blueprint
    // is just a file reference. It is not: the client reads the extents from SCHEMATIC_BOUNDS to
    // draw the outline and place the ghost, and SchematicHandler passes that straight to a tool
    // that calls getCenter() on it. Holding the item crashed the game inside Create's tick loop.
    //
    // So the oracle is Create's own factory rather than a list written out by hand. Whatever a real
    // blueprint carries, ours has to carry; if a future Create version adds an eighth component,
    // this fails rather than waiting for another crash.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void blueprintCarriesEverythingARealBlueprintCarries(GameTestHelper helper) {
        // Requires the extracted file to exist, which is the point: it also proves the extraction
        // put it where the server looks.
        ItemStack reference = SchematicItem.create(helper.getLevel(),
                EnigmaSchematic.RELATIVE_PATH, EnigmaSchematic.OWNER);
        ItemStack ours = EnigmaSchematic.blueprintStack();

        Set<DataComponentType<?>> missing = new LinkedHashSet<>(reference.getComponents().keySet());
        missing.removeAll(ours.getComponents().keySet());

        helper.assertTrue(missing.isEmpty(),
                "the blueprint is missing components a real blueprint carries: " + missing);

        // The one deliberate addition: a name. Every Create blueprint is called "Blueprint", which
        // is fine until one of them is a specific thing the player is meant to recognise. item_name
        // rather than custom_name, because custom_name renders in italics.
        Set<DataComponentType<?>> extra = new LinkedHashSet<>(ours.getComponents().keySet());
        extra.removeAll(reference.getComponents().keySet());
        helper.assertTrue(extra.equals(Set.of(DataComponents.ITEM_NAME)),
                "the blueprint carries components a real one does not: " + extra);
        helper.assertTrue(ours.getHoverName().getString()
                        .equals(Component.translatable("item.create_enigma.enigma_blueprint").getString()),
                "the blueprint's name is not the mod's own: " + ours.getHoverName().getString());

        // The one that crashed, checked by value rather than by presence.
        helper.assertTrue(reference.get(AllDataComponents.SCHEMATIC_BOUNDS) instanceof Vec3i bounds
                        && bounds.equals(ours.get(AllDataComponents.SCHEMATIC_BOUNDS)),
                "bounds differ: real " + reference.get(AllDataComponents.SCHEMATIC_BOUNDS)
                        + ", ours " + ours.get(AllDataComponents.SCHEMATIC_BOUNDS));
        helper.succeed();
    }

    /**
     * Counts a placed structure by block registry name, so the two sides can be compared as
     * dictionaries rather than by eyeballing a schematic.
     */
    private static Map<String, Integer> tally(ServerLevel level, BlockPos origin, Vec3i size) {
        Map<String, Integer> counts = new TreeMap<>();
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockState state = level.getBlockState(origin.offset(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }
                    String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    // The test harness's own structure block sits inside the volume. It is not
                    // part of the machine and never appears in the template, so counting it would
                    // report a difference that means nothing.
                    if (name.equals("minecraft:structure_block")) {
                        continue;
                    }
                    counts.merge(name, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static Map<String, Integer> tally(List<EnigmaStructure.Entry> entries) {
        Map<String, Integer> counts = new TreeMap<>();
        for (EnigmaStructure.Entry entry : entries) {
            counts.merge(BuiltInRegistries.BLOCK.getKey(entry.state().getBlock()).toString(), 1,
                    Integer::sum);
        }
        return counts;
    }

    /**
     * The blueprint must omit the belts and the motors, leave a shaft behind for every belt that had
     * an end block, and <b>change nothing else</b>.
     *
     * <p>Written because the first version of this feature was reported as taking the shafts out
     * with the belts. That is the failure mode worth guarding against: a filter on "belt" quietly
     * catching something a belt depends on leaves a machine that looks almost right and cannot be
     * finished by hand without guesswork.
     *
     * <p>Comparing full type tallies rather than spot-checking a few blocks means any future
     * over-reach shows up as a named difference, whatever it is.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void blueprintOmitsBeltsAndMotorsAndNothingElse(GameTestHelper helper) {
        EnigmaStructure structure = EnigmaStructure.get();
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(CORE);

        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), EnigmaSchematic.build(structure));

        BlockPos origin = structure.originFor(anchor);
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(),
                Block.UPDATE_ALL);

        Map<String, Integer> built = tally(level, origin, structure.size());
        Map<String, Integer> expected = tally(structure.entries(Generation.PRIMAL));

        String shaftType = BuiltInRegistries.BLOCK.getKey(AllBlocks.SHAFT.get()).toString();
        String beltType = BuiltInRegistries.BLOCK.getKey(AllBlocks.BELT.get()).toString();

        // Every belt block that was an end of a run becomes a shaft; middles leave a gap.
        int endBelts = 0;
        for (EnigmaStructure.Entry entry : structure.entries(Generation.PRIMAL)) {
            if (entry.state().is(AllBlocks.BELT.get())
                    && entry.state().getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
                endBelts++;
            }
        }
        helper.assertTrue(endBelts > 0, "the machine has no belt ends; this test proves nothing");

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(built.keySet());
        allTypes.addAll(expected.keySet());

        StringBuilder differences = new StringBuilder();
        for (String type : allTypes) {
            int got = built.getOrDefault(type, 0);
            int want = expected.getOrDefault(type, 0);

            // The three types whose counts are supposed to move.
            if (type.equals(beltType)) {
                want = 0;
            } else if (type.equals(shaftType)) {
                want += endBelts;
            } else if (type.equals(BuiltInRegistries.BLOCK.getKey(AllBlocks.CREATIVE_MOTOR.get()).toString())
                    || type.equals(BuiltInRegistries.BLOCK.getKey(CEBlocks.DISGUISED_MOTOR.get()).toString())) {
                want = 0;
            }

            if (got != want) {
                differences.append('\n').append("  ").append(type)
                        .append(": blueprint ").append(got).append(", expected ").append(want);
            }
        }

        helper.assertTrue(differences.isEmpty(),
                "the blueprint does not match the machine minus the player's part:" + differences);

        helper.assertTrue(built.getOrDefault(beltType, 0) == 0,
                "the blueprint still contains belts");
        helper.assertTrue(built.getOrDefault(
                        BuiltInRegistries.BLOCK.getKey(CEBlocks.DISGUISED_MOTOR.get()).toString(), 0) == 0,
                "the blueprint still contains disguised motors");

        // What a belt sits on survives: every end block left a shaft, and none went missing.
        helper.assertTrue(built.getOrDefault(shaftType, 0) == endBelts + expected.getOrDefault(shaftType, 0),
                "shafts did not survive the belts being dropped: blueprint has "
                        + built.getOrDefault(shaftType, 0) + ", expected "
                        + (endBelts + expected.getOrDefault(shaftType, 0))
                        + " (" + endBelts + " belt ends plus the machine's own)");

        // Logged rather than asserted: the exact contents are useful to read back when someone asks
        // "what did the blueprint actually leave me", and a passing test prints nothing otherwise.
        LOGGER.info("Blueprint vs machine, by block type:");
        for (String type : allTypes) {
            LOGGER.info("  {}: blueprint {}, machine {}", type, built.getOrDefault(type, 0),
                    expected.getOrDefault(type, 0));
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void blueprintBuildsTheShellAndThePlayerFinishesIt(GameTestHelper helper) {
        EnigmaStructure structure = EnigmaStructure.get();
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(CORE);

        // Exactly what Create's cannon does with a schematic: read the NBT into a template, then
        // place it. None of this goes through our own placement code.
        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), EnigmaSchematic.build(structure));

        BlockPos origin = structure.originFor(anchor);
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(),
                Block.UPDATE_ALL);

        helper.assertTrue(helper.getBlockState(CORE).is(Blocks.CHEST),
                "the blueprint did not put a chest at the anchor; it put "
                        + helper.getBlockState(CORE).getBlock());

        // The shell on its own is not the machine, and - this is the part worth asserting - it must
        // not read as "only the motors are missing" either. The belts are outstanding too, so the
        // wrench has to keep asking for more rather than promising that a substitute motor will
        // finish the job.
        EnigmaValidator.Report afterBlueprint =
                EnigmaValidator.analyse(level, anchor, structure, Generation.PRIMAL);
        helper.assertTrue(!afterBlueprint.isComplete(),
                "the blueprint built the entire machine; the belts and motors were supposed to be "
                        + "left to the player");
        helper.assertTrue(!afterBlueprint.onlyMotorsMissing(),
                "the blueprint alone must not read as \"only motors missing\" - the belts are "
                        + "outstanding too, and that hint would be a lie here");

        // The player's half: everything the blueprint left out, placed as the template records it.
        int placedByHand = 0;
        for (EnigmaStructure.Entry entry : structure.entries(Generation.PRIMAL)) {
            if (!EnigmaSchematic.isPlayersOwnWork(entry.state())) {
                continue;
            }
            helper.setBlock(CORE.offset(entry.offset().subtract(structure.coreOffset())), entry.state());
            placedByHand++;
        }
        helper.assertTrue(placedByHand > 0, "the blueprint omitted nothing; nothing was left to place");

        EnigmaValidator.Report finished = EnigmaValidator.analyse(level, anchor, structure, Generation.PRIMAL);
        helper.assertTrue(finished.isComplete(),
                "shell plus the player's " + placedByHand + " blocks is still not complete: "
                        + finished.placed() + "/" + finished.total() + ", first gap "
                        + finished.firstGap() + " " + finished.detail());
        helper.succeed();
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
        AdvancementHolder watched = server.getAdvancements().get(EnigmaAdvancements.PONDER_WATCHED);
        AdvancementHolder enigma = server.getAdvancements().get(EnigmaAdvancements.ENIGMA);
        helper.assertTrue(root != null, "advancement " + EnigmaAdvancements.ROOT + " did not load");
        helper.assertTrue(watched != null,
                "advancement " + EnigmaAdvancements.PONDER_WATCHED + " did not load");
        helper.assertTrue(enigma != null, "advancement " + EnigmaAdvancements.ENIGMA + " did not load");
        helper.assertTrue(root.value().isRoot(), EnigmaAdvancements.ROOT + " is not a root advancement");

        // The chain is what fixes the display order. Siblings under one parent are laid out in the
        // iteration order of an identity hash set, so two children of the root would swap places
        // between launches; root -> watched -> enigma has only one order. If either link broke,
        // the advancement would still be granted and simply displayed in the wrong place.
        assertParent(helper, watched, EnigmaAdvancements.ROOT);
        assertParent(helper, enigma, EnigmaAdvancements.PONDER_WATCHED);

        // award() returns false for an unknown criterion rather than throwing, so this is the
        // assertion that stands between a typo and a feature that silently never appears.
        for (AdvancementHolder holder : new AdvancementHolder[] {root, watched, enigma}) {
            helper.assertTrue(holder.value().criteria().containsKey(EnigmaAdvancements.CRITERION),
                    "an advancement does not declare the criterion '" + EnigmaAdvancements.CRITERION
                            + "'; it declares " + holder.value().criteria().keySet());
        }

        helper.succeed();
    }

    private static void assertParent(GameTestHelper helper, AdvancementHolder child, ResourceLocation parent) {
        helper.assertTrue(child.value().parent().isPresent()
                        && child.value().parent().get().equals(parent),
                child.id() + " does not declare " + parent + " as its parent");
    }

    // --------------------------------------------------------------------------------
    // The wrench, which so far is only a recipe and a tooltip.
    //
    // The tooltip is client-side and cannot be checked here. The recipe can: a malformed one is
    // logged and then simply absent, so "no error in the log" is not the same as "the recipe is
    // what we meant". This asserts it loaded, is shaped, and yields this mod's wrench.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrenchRecipeLoadsAndProducesTheWrench(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "enigma_wrench");

        RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
        helper.assertTrue(holder != null, "recipe " + id + " did not load");

        helper.assertTrue(holder.value() instanceof ShapedRecipe,
                "expected a shaped recipe, found " + holder.value().getClass().getSimpleName());

        ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(CEItems.ENIGMA_WRENCH.get()),
                "the recipe produces " + result + " instead of the Enigma Wrench");

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
                "belt slope must NOT be ignored wholesale - flat against sloped is a different build");
        helper.assertFalse(BlockStateMatcher.matches(belt, cycle(belt, "casing")),
                "belt casing must NOT be ignored - an encased belt is a different build");

        // Facing and part ARE ignored, and this assertion used to say the opposite.
        //
        // It was an assumption, not something checked: "a belt's part is a real difference" sounded
        // right. It is not. Create derives both from which end of the run the builder clicked first,
        // and the other outcome is the same belt along the same line with START and END swapped -
        // indistinguishable in the world. Since the belts are not in the blueprint either, the
        // original's choice is not something a builder could look up.
        helper.assertTrue(BlockStateMatcher.matches(belt, cycle(belt, "part")),
                "belt part must be ignored - reversing a run swaps START and END");
        helper.assertTrue(BlockStateMatcher.matches(belt, cycle(belt, "facing")),
                "belt facing must be ignored - it follows from which end the run was started at");

        // The diagonal slopes are the same run seen from the other end. Reported from play: "should
        // be downward but is upward", fixed by re-placing the belt the other way round.
        BlockState flat = belt.setValue(BeltBlock.SLOPE, BeltSlope.HORIZONTAL);
        helper.assertTrue(BlockStateMatcher.matches(
                        flat.setValue(BeltBlock.SLOPE, BeltSlope.UPWARD),
                        flat.setValue(BeltBlock.SLOPE, BeltSlope.DOWNWARD)),
                "upward and downward are the same diagonal run and must match");
        helper.assertFalse(BlockStateMatcher.matches(flat,
                        flat.setValue(BeltBlock.SLOPE, BeltSlope.UPWARD)),
                "a flat belt must still not match a diagonal one");

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
