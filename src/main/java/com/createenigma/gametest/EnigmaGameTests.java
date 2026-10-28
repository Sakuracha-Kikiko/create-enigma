package com.createenigma.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.createenigma.CreateEnigma;
import com.createenigma.content.BlueprintBlessing;
import com.createenigma.content.CreativeHandCrankBlockEntity;
import com.createenigma.content.EnigmaAdvancements;
import com.createenigma.content.EnigmaCodeState;
import com.createenigma.content.EnigmaCoreBlock;
import com.createenigma.registry.CEBlockEntities;
import com.createenigma.registry.CEBlocks;
import com.createenigma.registry.CEItems;
import com.createenigma.structure.BlockStateMatcher;
import com.createenigma.content.EnigmaOfferings;
import com.createenigma.content.EnigmaWrenchRecipe;
import com.createenigma.content.MechanismStep;
import com.createenigma.structure.EnigmaSchematic;
import com.createenigma.structure.EnigmaStructure;
import com.createenigma.structure.EnigmaStructure.Generation;
import com.createenigma.structure.EnigmaValidator;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.createenigma.content.ExtinguishedCakeItem;
import com.simibubi.create.content.decoration.placard.PlacardBlockEntity;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
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
     * <p>That mattered more here than it would elsewhere: belts are laid by hand after a blueprint
     * when the player builds without one, and the direction they end up with depends on which shaft
     * they clicked first. Comparing it would be asking them to guess, and the guess has no wrong
     * answer to learn from - only a machine that refuses to be finished.
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
     * The blueprint must contain the whole machine except the motors, and <b>change nothing else</b>.
     *
     * <p>Belts used to be filtered out here, with a bare shaft left at each belt end in their place.
     * That was done on the belief that the cannon could not place a belt run - a belief that was
     * never tested, and that the owner has since disproved in game: a schematic containing belts
     * prints correctly. The real culprit was belts carrying block entity data, which this blueprint
     * never writes. So the substitution is gone, and the test now guards the other direction: if a
     * filter ever comes back, it shows up here as a named difference.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void blueprintOmitsOnlyTheMotors(GameTestHelper helper) {
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

        // The machine must actually have belts in it, or this test proves nothing about them.
        int beltsInMachine = 0;
        for (EnigmaStructure.Entry entry : structure.entries(Generation.PRIMAL)) {
            if (entry.state().is(AllBlocks.BELT.get())) {
                beltsInMachine++;
            }
        }
        helper.assertTrue(beltsInMachine > 0, "the machine has no belts; this test proves nothing");

        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(built.keySet());
        allTypes.addAll(expected.keySet());

        StringBuilder differences = new StringBuilder();
        for (String type : allTypes) {
            int got = built.getOrDefault(type, 0);
            int want = expected.getOrDefault(type, 0);

            // The only types whose counts are supposed to move: the two kinds of motor.
            if (type.equals(BuiltInRegistries.BLOCK.getKey(AllBlocks.CREATIVE_MOTOR.get()).toString())
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

        helper.assertTrue(built.getOrDefault(beltType, 0) == beltsInMachine,
                "the blueprint dropped belts: it has " + built.getOrDefault(beltType, 0) + " of "
                        + beltsInMachine);
        helper.assertTrue(built.getOrDefault(
                        BuiltInRegistries.BLOCK.getKey(CEBlocks.DISGUISED_MOTOR.get()).toString(), 0) == 0,
                "the blueprint still contains disguised motors");

        // A belt run is a chain of blocks; dropping the middle of one leaves two stumps that can
        // never be joined again. That was the old failure, so the count above is the assertion that
        // matters, and the shaft count is left exactly where the machine put it.
        helper.assertTrue(built.getOrDefault(shaftType, 0) == expected.getOrDefault(shaftType, 0),
                "the blueprint invented shafts or lost them: blueprint has "
                        + built.getOrDefault(shaftType, 0) + ", machine has "
                        + expected.getOrDefault(shaftType, 0));

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

        // The shell plus the belts is not the machine - the motors are missing - and the gap is now
        // exactly the motors. That reading is what makes the blueprint's promise honest: the wrench
        // can tell the player that a substitute will finish the job, because it will.
        EnigmaValidator.Report afterBlueprint =
                EnigmaValidator.analyse(level, anchor, structure, Generation.PRIMAL);
        helper.assertTrue(!afterBlueprint.isComplete(),
                "the blueprint built the entire machine; the motors were supposed to be left to the "
                        + "player");
        helper.assertTrue(afterBlueprint.onlyMotorsMissing(),
                "after the blueprint the only gap left should be the motors, but the wrench reports "
                        + afterBlueprint.detail());

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
        AdvancementHolder glance = server.getAdvancements().get(EnigmaAdvancements.GODS_GLANCE);
        AdvancementHolder mysticAge = server.getAdvancements().get(EnigmaAdvancements.MYSTIC_AGE);
        AdvancementHolder snuffed = server.getAdvancements().get(EnigmaAdvancements.SNUFFED_OUT);
        AdvancementHolder shape = server.getAdvancements().get(EnigmaAdvancements.SHAPE_REMEMBERED);
        helper.assertTrue(root != null, "advancement " + EnigmaAdvancements.ROOT + " did not load");
        helper.assertTrue(watched != null,
                "advancement " + EnigmaAdvancements.PONDER_WATCHED + " did not load");
        helper.assertTrue(enigma != null, "advancement " + EnigmaAdvancements.ENIGMA + " did not load");
        helper.assertTrue(glance != null,
                "advancement " + EnigmaAdvancements.GODS_GLANCE + " did not load");
        helper.assertTrue(mysticAge != null,
                "advancement " + EnigmaAdvancements.MYSTIC_AGE + " did not load");
        helper.assertTrue(snuffed != null,
                "advancement " + EnigmaAdvancements.SNUFFED_OUT + " did not load");
        helper.assertTrue(shape != null,
                "advancement " + EnigmaAdvancements.SHAPE_REMEMBERED + " did not load");
        helper.assertTrue(root.value().isRoot(), EnigmaAdvancements.ROOT + " is not a root advancement");

        // The chain is what fixes the display order. Siblings under one parent are laid out in the
        // iteration order of an identity hash set, so two children of one parent would swap places
        // between launches; a chain has only one order. If a link broke, the advancement would
        // still be granted and simply displayed in the wrong place.
        //
        // gods_glance sits between enigma and mystic_age rather than beside mystic_age: hanging it
        // under enigma as well would have made the two of them siblings, which is the one arrangement
        // this chain exists to avoid.
        assertParent(helper, watched, EnigmaAdvancements.ROOT);
        assertParent(helper, enigma, EnigmaAdvancements.PONDER_WATCHED);
        assertParent(helper, glance, EnigmaAdvancements.ENIGMA);
        assertParent(helper, mysticAge, EnigmaAdvancements.GODS_GLANCE);
        assertParent(helper, snuffed, EnigmaAdvancements.GODS_GLANCE);
        assertParent(helper, shape, EnigmaAdvancements.GODS_GLANCE);

        // award() returns false for an unknown criterion rather than throwing, so this is the
        // assertion that stands between a typo and a feature that silently never appears.
        // snuffed_out is left out on purpose: it carries a real trigger, so it declares "ate".
        for (AdvancementHolder holder : new AdvancementHolder[] {root, watched, enigma, mysticAge, shape}) {
            helper.assertTrue(holder.value().criteria().containsKey(EnigmaAdvancements.CRITERION),
                    "an advancement does not declare the criterion '" + EnigmaAdvancements.CRITERION
                            + "'; it declares " + holder.value().criteria().keySet());
        }

        helper.succeed();
    }

    /**
     * No advancement is hidden, so the tab can teach.
     *
     * <p>{@code AdvancementVisibilityEvaluator} previews an unfinished node only when it is not
     * hidden, and only within two levels of a completed one. A {@code "hidden": true} display turns
     * that into "invisible until earned", which for a mod whose whole guidance lives in this tab
     * means a player who just watched the first ponder has nothing to go on.
     *
     * <p>This is a one-line-per-file mistake with no symptom other than "the tab looks empty", so it
     * is worth a test: every file must carry a display and none of them may be hidden.
     */
    @GameTest(template = TEMPLATE)
    public static void noAdvancementIsHidden(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getAdvancements();
        ResourceLocation[] all = {
                EnigmaAdvancements.ROOT,
                EnigmaAdvancements.PONDER_WATCHED,
                EnigmaAdvancements.ENIGMA,
                EnigmaAdvancements.GODS_GLANCE,
                EnigmaAdvancements.MYSTIC_AGE,
                EnigmaAdvancements.MYSTIC_GRACE,
                EnigmaAdvancements.SNUFFED_OUT,
                EnigmaAdvancements.SHAPE_REMEMBERED,
        };

        for (ResourceLocation id : all) {
            AdvancementHolder holder = manager.get(id);
            helper.assertTrue(holder != null, "advancement " + id + " did not load");
            var display = holder.value().display();
            helper.assertTrue(display.isPresent(), id + " has no display, so it can never be shown");
            helper.assertTrue(!display.get().isHidden(),
                    id + " is hidden, so the player never sees it coming and has nothing to aim at");
        }

        helper.succeed();
    }

    private static void assertParent(GameTestHelper helper, AdvancementHolder child, ResourceLocation parent) {
        helper.assertTrue(child.value().parent().isPresent()
                        && child.value().parent().get().equals(parent),
                child.id() + " does not declare " + parent + " as its parent");
    }

    /**
     * The two trigger-driven advancements, checked for the thing that makes them work.
     *
     * <p>Unlike the chain above them these are not awarded from code - they carry real vanilla
     * triggers in their json. Asserting only that the file loaded would pass even if the trigger were
     * misspelled, because an advancement with a bogus criteria key still parses. So this checks the
     * <b>trigger type</b>: a crafted recipe and an inventory change. Those are the two the mod relies
     * on, and a typo in {@code recipe_id} or in the item id shows up here rather than as an
     * advancement that silently never fires.
     */
    @GameTest(template = TEMPLATE)
    public static void triggerAdvancementsUseRealTriggers(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getAdvancements();

        AdvancementHolder glance = manager.get(EnigmaAdvancements.GODS_GLANCE);
        helper.assertTrue(glance != null, "advancement " + EnigmaAdvancements.GODS_GLANCE + " did not load");
        assertParent(helper, glance, EnigmaAdvancements.ENIGMA);
        helper.assertTrue(glance.value().criteria().containsKey("crafted"),
                "gods_glance does not declare the 'crafted' criterion");
        helper.assertTrue(
                glance.value().criteria().get("crafted").trigger()
                        instanceof net.minecraft.advancements.critereon.RecipeCraftedTrigger,
                "gods_glance does not use recipe_crafted; it would never fire on crafting the wrench");

        AdvancementHolder grace = manager.get(EnigmaAdvancements.MYSTIC_GRACE);
        helper.assertTrue(grace != null, "advancement " + EnigmaAdvancements.MYSTIC_GRACE + " did not load");
        assertParent(helper, grace, EnigmaAdvancements.MYSTIC_AGE);
        helper.assertTrue(grace.value().criteria().containsKey("obtained"),
                "mystic_grace does not declare the 'obtained' criterion");
        helper.assertTrue(
                grace.value().criteria().get("obtained").trigger()
                        instanceof net.minecraft.advancements.critereon.InventoryChangeTrigger,
                "mystic_grace does not use inventory_changed; it would never fire on obtaining the crank");

        AdvancementHolder snuffed = manager.get(EnigmaAdvancements.SNUFFED_OUT);
        helper.assertTrue(snuffed != null,
                "advancement " + EnigmaAdvancements.SNUFFED_OUT + " did not load");
        assertParent(helper, snuffed, EnigmaAdvancements.GODS_GLANCE);
        helper.assertTrue(snuffed.value().criteria().containsKey("ate"),
                "snuffed_out does not declare the 'ate' criterion");
        helper.assertTrue(
                snuffed.value().criteria().get("ate").trigger()
                        instanceof net.minecraft.advancements.critereon.ConsumeItemTrigger,
                "snuffed_out does not use consume_item; it would never fire on eating the cake");

        helper.succeed();
    }

    /**
     * {@code zh_cn} and {@code en_us} must declare exactly the same keys.
     *
     * <p>This exists because the same mistake was made twice: a line was added to a scene and to one
     * language file, and the other language fell back to whatever Ponder does with a missing entry.
     * For ponder scenes that is the <b>derived key printed on screen</b> - which is how a scene once
     * displayed {@code create_enigma.ponder.creative_hand_crank.text_1} in place of a sentence.
     * Nothing throws and nothing logs; the only symptom is a string nobody meant to show.
     *
     * <p>A set comparison catches both directions: a key added to one file only, and one removed from
     * one file only.
     */
    @GameTest(template = TEMPLATE)
    public static void langFilesDeclareTheSameKeys(GameTestHelper helper) {
        Set<String> zh = langKeys(helper, "zh_cn");
        Set<String> en = langKeys(helper, "en_us");

        helper.assertTrue(!zh.isEmpty(), "zh_cn.json parsed as empty");

        Set<String> missingInEn = new TreeSet<>(zh);
        missingInEn.removeAll(en);
        Set<String> missingInZh = new TreeSet<>(en);
        missingInZh.removeAll(zh);

        helper.assertTrue(missingInEn.isEmpty(), "keys only in zh_cn: " + missingInEn);
        helper.assertTrue(missingInZh.isEmpty(), "keys only in en_us: " + missingInZh);

        helper.succeed();
    }

    /**
     * 神之手 is exactly Create's brass hand recipe, rotated 180 degrees.
     *
     * <p>Compared against {@code create:brass_hand} cell by cell rather than spelled out here: the
     * point is that the two recipes are the same shape seen from the other side, so a copy of the
     * pattern in this test would only assert that two copies of the same thing agree. If Create ever
     * changes their recipe, this fails, which is the right moment to look at ours again.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void handOfGodIsTheBrassHandRecipeUpsideDown(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "hand_of_god");
        RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
        helper.assertTrue(holder != null, "recipe " + id + " did not load");
        helper.assertTrue(holder.value() instanceof ShapedRecipe,
                "expected a ShapedRecipe, found " + holder.value().getClass().getSimpleName());
        ShapedRecipe ours = (ShapedRecipe) holder.value();

        helper.assertTrue(ours.getWidth() == 3 && ours.getHeight() == 3,
                "the declared shape is " + ours.getWidth() + "x" + ours.getHeight() + ", expected 3x3");

        ItemStack result = ours.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(CEItems.HAND_OF_GOD.get()),
                "the recipe produces " + result + " instead of the Hand of God");

        // The path is part of the id: the file is data/create/recipe/crafting/kinetics/brass_hand.json,
        // so the recipe is create:crafting/kinetics/brass_hand, not create:brass_hand. Looking it up
        // by the latter reports "not present" and reads as if Create had removed the recipe.
        ResourceLocation originalId =
                ResourceLocation.fromNamespaceAndPath("create", "crafting/kinetics/brass_hand");
        RecipeHolder<?> originalHolder = recipes.byKey(originalId).orElse(null);
        helper.assertTrue(originalHolder != null,
                "recipe " + originalId + " is not present, so there is nothing to be the flip of");
        helper.assertTrue(originalHolder.value() instanceof ShapedRecipe,
                "create:brass_hand is not a ShapedRecipe any more");
        ShapedRecipe original = (ShapedRecipe) originalHolder.value();
        helper.assertTrue(original.getWidth() == 3 && original.getHeight() == 3,
                "create:brass_hand is no longer 3x3, so this mod's flip of it needs revisiting");

        // Row-major, so cell index is column + row * width; a 180-degree rotation maps (x, y) to
        // (2 - x, 2 - y).
        List<Ingredient> mine = ours.getIngredients();
        List<Ingredient> theirs = original.getIngredients();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Ingredient here = mine.get(column + row * 3);
                Ingredient rotated = theirs.get((2 - column) + (2 - row) * 3);
                helper.assertTrue(sameItems(here, rotated),
                        "cell " + column + "," + row + " holds " + here
                                + " but the rotated brass hand has " + rotated);
            }
        }

        helper.succeed();
    }

    /**
     * The offering rule: one flywheel, one extinguished cake and one Hand of God, and nothing else.
     *
     * <p>The cases that a looser rule would get wrong, and that this test exists for:
     *
     * <ul>
     *   <li>a chest with dirt and no offering is not "empty" - it is spoiled, and saying "you have
     *       not shown enough sincerity" to a player who filled it with junk would be a lie;</li>
     *   <li>two of the three is <b>not</b> acceptable either, even though it is nearly right - the
     *       machine asks for the set, and a partial set is read as no offering at all rather than as
     *       a wrong one;</li>
     *   <li>an offering <b>plus</b> dirt is spoiled; one item cannot be slipped past the rule by
     *       stacking it next to the right things;</li>
     *   <li>extras of the right things are fine and all of them are taken, because the rule accepts
     *       rather than counts.</li>
     * </ul>
     *
     * <p>Tested on a plain {@link SimpleContainer}: the rule is a function of contents, so it needs
     * no world, and this is the part that would otherwise only be checked by hand in game. The step
     * that acts on the verdict - consume, then replace the block - still needs a player, and
     * {@code makeMockServerPlayerInLevel} crashes this pack (Create's login event), so that step is
     * verified in game.
     */
    @GameTest(template = TEMPLATE)
    public static void offeringsMustBeOneOfEachAndNothingElse(GameTestHelper helper) {
        ItemStack flywheel = new ItemStack(AllBlocks.FLYWHEEL.get().asItem());
        ItemStack cake = new ItemStack(CEItems.EXTINGUISHED_CAKE.get());
        ItemStack hand = new ItemStack(CEItems.HAND_OF_GOD.get());

        SimpleContainer empty = new SimpleContainer(27);
        helper.assertTrue(EnigmaOfferings.judge(empty) == EnigmaOfferings.Verdict.EMPTY,
                "an empty chest is not EMPTY, it is " + EnigmaOfferings.judge(empty));

        SimpleContainer junk = new SimpleContainer(27);
        junk.setItem(4, new ItemStack(Items.DIRT, 12));
        helper.assertTrue(EnigmaOfferings.judge(junk) == EnigmaOfferings.Verdict.IMPURE,
                "a chest of dirt with no offering reads as " + EnigmaOfferings.judge(junk));

        SimpleContainer partial = new SimpleContainer(27);
        partial.setItem(0, flywheel.copy());
        partial.setItem(1, cake.copy());
        helper.assertTrue(EnigmaOfferings.judge(partial) == EnigmaOfferings.Verdict.EMPTY,
                "two of the three offerings read as " + EnigmaOfferings.judge(partial)
                        + " - an unfinished offering is not a spoiled one");

        SimpleContainer complete = new SimpleContainer(27);
        complete.setItem(0, flywheel.copy());
        complete.setItem(13, cake.copy());
        complete.setItem(26, hand.copy());
        helper.assertTrue(EnigmaOfferings.judge(complete) == EnigmaOfferings.Verdict.ACCEPTABLE,
                "one of each reads as " + EnigmaOfferings.judge(complete));

        SimpleContainer mixed = new SimpleContainer(27);
        mixed.setItem(0, flywheel.copy());
        mixed.setItem(1, cake.copy());
        mixed.setItem(2, hand.copy());
        mixed.setItem(9, new ItemStack(Items.DIRT));
        helper.assertTrue(EnigmaOfferings.judge(mixed) == EnigmaOfferings.Verdict.IMPURE,
                "an offering with dirt in the chest reads as " + EnigmaOfferings.judge(mixed));

        SimpleContainer surplus = new SimpleContainer(27);
        surplus.setItem(0, new ItemStack(AllBlocks.FLYWHEEL.get().asItem(), 3));
        surplus.setItem(1, cake.copy());
        surplus.setItem(2, new ItemStack(CEItems.HAND_OF_GOD.get(), 2));
        helper.assertTrue(EnigmaOfferings.judge(surplus) == EnigmaOfferings.Verdict.ACCEPTABLE,
                "surplus of the right things reads as " + EnigmaOfferings.judge(surplus));

        EnigmaOfferings.consume(surplus);
        helper.assertTrue(EnigmaOfferings.judge(surplus) == EnigmaOfferings.Verdict.EMPTY,
                "consume left something behind: " + EnigmaOfferings.judge(surplus));

        helper.succeed();
    }

    /**
     * 熄灭蛋糕 is Create's blaze cake, with water where the lava was.
     *
     * <p>Checked in both directions on purpose. "Accepts water" alone would still pass if the recipe
     * also accepted lava, which would make it a second way to craft a blaze cake - the one thing this
     * recipe must not be. The item side is checked too, because a filling recipe with the wrong input
     * still fills something.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void extinguishedCakeIsBlazeCakeFilledWithWater(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "extinguished_cake");

        RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
        helper.assertTrue(holder != null, "recipe " + id + " did not load");
        helper.assertTrue(holder.value() instanceof FillingRecipe,
                "expected a Create filling recipe, found " + holder.value().getClass().getSimpleName());
        helper.assertTrue(holder.value().getType() == AllRecipeTypes.FILLING.getType(),
                "the recipe is filed as " + holder.value().getType() + " instead of "
                        + AllRecipeTypes.FILLING.getType() + ", so no spout or basin will offer it");

        FillingRecipe filling = (FillingRecipe) holder.value();
        SizedFluidIngredient fluid = filling.getRequiredFluid();
        helper.assertTrue(fluid != null, "the filling recipe requires no fluid");
        helper.assertTrue(fluid.amount() == 250,
                "it fills " + fluid.amount() + " mB, expected 250 like a blaze cake");
        helper.assertTrue(fluid.test(new FluidStack(Fluids.WATER, 250)),
                "it does not accept water: " + fluid);
        helper.assertTrue(!fluid.test(new FluidStack(Fluids.LAVA, 250)),
                "it accepts lava as well, which would make it a second blaze cake recipe");

        var items = filling.getIngredients();
        helper.assertTrue(!items.isEmpty()
                        && items.get(0).test(new ItemStack(AllItems.BLAZE_CAKE_BASE.get())),
                "the item ingredient is not a blaze cake base: " + items);

        ItemStack result = filling.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(CEItems.EXTINGUISHED_CAKE.get()),
                "the recipe produces " + result + " instead of the Extinguished Cake");

        helper.succeed();
    }

    /** Whether two ingredients accept the same set of items; an empty cell matches an empty cell. */
    private static boolean sameItems(Ingredient a, Ingredient b) {
        Set<Item> first = new TreeSet<>(java.util.Comparator.comparing(Item::toString));
        for (ItemStack stack : a.getItems()) first.add(stack.getItem());
        Set<Item> second = new TreeSet<>(java.util.Comparator.comparing(Item::toString));
        for (ItemStack stack : b.getItems()) second.add(stack.getItem());
        return first.equals(second);
    }

    /**
     * Reads a json file from the jar's classpath, for the same reason {@link #langKeys} does: the
     * data is in the jar, and going through the server's resource manager would differ between the
     * two sides.
     */
    private static JsonObject readJson(String path) {
        try (InputStream in = EnigmaGameTests.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new AssertionError("missing " + path + " on the classpath");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("could not read " + path, e);
        }
    }

    private static Set<String> langKeys(GameTestHelper helper, String locale) {
        // Read from the classpath, not from the server's resource manager: lang files live under
        // assets/, and a server only mounts data packs - getResource would find nothing there. The
        // classpath holds the same file on both sides and inside the jar.
        String path = "/assets/" + CreateEnigma.MOD_ID + "/lang/" + locale + ".json";
        try (InputStream in = EnigmaGameTests.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new AssertionError("missing " + path + " on the classpath");
            }
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new TreeSet<>(json.keySet());
        } catch (IOException e) {
            throw new RuntimeException("could not read " + path, e);
        }
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

        helper.assertTrue(holder.value() instanceof EnigmaWrenchRecipe,
                "expected the Enigma Wrench recipe, found " + holder.value().getClass().getSimpleName());

        // The crafting table finds recipes through RecipeManager.getRecipeFor(RecipeType.CRAFTING,
        // ...), which reads an index keyed by each recipe's own getType(). If this recipe is filed
        // under anything else - or not filed at all - the grid is never even asked about it and
        // matches() is never called. From in front of the crafting table that looks exactly like a
        // wrong shape, which is why three rounds of fixing the shape changed nothing.
        var filedAsCrafting = recipes.getRecipes().stream()
                .filter(candidate -> candidate.value() instanceof EnigmaWrenchRecipe)
                .filter(candidate -> candidate.value().getType() == RecipeType.CRAFTING)
                .toList();
        helper.assertTrue(filedAsCrafting.size() == 1,
                "expected exactly one Enigma Wrench recipe filed as " + RecipeType.CRAFTING
                        + ", found " + filedAsCrafting.size() + "; getType() reports "
                        + holder.value().getType());

        ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(CEItems.ENIGMA_WRENCH.get()),
                "the recipe produces " + result + " instead of the Enigma Wrench");

        // JEI decides between a shaped grid and 无序 by asking whether the recipe is a ShapedRecipe -
        // its CraftingCategoryExtension#getWidth checks exactly that instanceof and returns 0
        // otherwise. A 0 makes JEI lay the ingredients out as shapeless, which is what the viewer was
        // showing. So this is the assertion that the shape is drawn at all.
        helper.assertTrue(holder.value() instanceof ShapedRecipe,
                "the recipe is not a ShapedRecipe, so JEI will draw it as 无序: "
                        + holder.value().getClass().getName());
        ShapedRecipe shaped = (ShapedRecipe) holder.value();
        helper.assertTrue(shaped.getWidth() == 2 && shaped.getHeight() == 3,
                "the declared shape is " + shaped.getWidth() + "x" + shaped.getHeight() + ", expected 2x3");

        // A two-by-three shape has six cells and JEI maps ingredient i to grid slot {0,1,3,4,6,7};
        // five ingredients put the rod in slot 6, the blank corner, while the table wants slot 7.
        // That mismatch is what made JEI's transfer button move the mechanisms into the wrong cells
        // and produce nothing, so the count and the blank cell are both load-bearing.
        var ingredients = holder.value().getIngredients();
        helper.assertTrue(ingredients.size() == 6,
                "expected six ingredients for a six-cell shape, got " + ingredients.size());

        // The skeleton JEI draws must name the intermediates by item alone. If a step ever appears in
        // getIngredients, the recipe viewer answers the puzzle the scene poses.
        //
        // Row-major over a two-wide pattern, ingredients 0, 1 and 2 are the cells (0,0), (1,0) and
        // (0,1) - the three cells EnigmaWrenchRecipe.MECHANISM_CELLS pins to the world's numbers.
        // Asserting them here is what keeps that hard-coded list honest if the json pattern changes.
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(
                    ingredients.get(i).test(new ItemStack(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())),
                    "ingredient " + i + " does not accept an incomplete precision mechanism");
        }
        helper.assertTrue(ingredients.get(3).test(new ItemStack(AllBlocks.COGWHEEL.get().asItem())),
                "the fourth ingredient does not accept a cogwheel");
        helper.assertTrue(ingredients.get(4).isEmpty(),
                "the fifth ingredient is not the blank bottom-left cell");
        helper.assertTrue(ingredients.get(5).test(new ItemStack(net.minecraft.world.item.Items.STICK)),
                "the sixth ingredient does not accept a wooden rod");

        // JEI skips recipes whose isSpecial is true, so this single word decides whether the
        // skeleton is visible at all.
        helper.assertTrue(!holder.value().isSpecial(),
                "the recipe reports itself as special, which hides it from JEI");

        helper.succeed();
    }

    // --------------------------------------------------------------------------------
    // The grid the scene tells the player to build, matched for real.
    //
    // The test above proves the recipe loaded, is filed, and is shaped the way we meant. It does
    // not prove that the pattern the scene describes is the pattern that matches, and every round
    // of "still cannot craft" has turned on exactly that gap. So this builds the grid out of
    // intermediates whose progress really is this world's numbers, and asks the recipe manager the
    // same question the crafting table asks - including the crop to the occupied cells, which is
    // the part that is easiest to get wrong and impossible to see from in front of the table.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrenchRecipeMatchesTheGridTheSceneDescribes(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        int[] wanted = EnigmaCodeState.progresses(helper.getLevel());
        LOGGER.info("Wrench test: this world's numbers are {} {} {}", wanted[0], wanted[1], wanted[2]);

        helper.assertTrue(wanted[0] != wanted[1] && wanted[1] != wanted[2] && wanted[0] != wanted[2],
                "the three numbers repeat: " + wanted[0] + " " + wanted[1] + " " + wanted[2]);

        // Row-major, exactly the layout a player builds in a crafting table:
        //   M M .
        //   M C .
        //   . S .
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        grid.set(0, mechanismAt(wanted[0]));
        grid.set(1, mechanismAt(wanted[1]));
        grid.set(3, mechanismAt(wanted[2]));
        grid.set(4, new ItemStack(AllBlocks.COGWHEEL.get().asItem()));
        grid.set(7, new ItemStack(Items.STICK));

        CraftingInput input = CraftingInput.of(3, 3, grid);

        // A table always hands over the cropped grid, so the recipe never sees the empty third
        // column. Asserting the crop means a future shape change fails here with a clear message
        // rather than in front of the table.
        helper.assertTrue(input.width() == 2 && input.height() == 3,
                "the crafting table hands over a " + input.width() + "x" + input.height()
                        + " grid, expected 2x3 after cropping");

        var found = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent(),
                "a grid holding " + wanted[0] + "/15, " + wanted[1] + "/15 and " + wanted[2] + "/15"
                        + " around a cogwheel and a stick matches no recipe at all");
        helper.assertTrue(found.get().value() instanceof EnigmaWrenchRecipe,
                "that grid matched " + found.get().id() + " instead of the Enigma Wrench");

        // The order matters, or the puzzle is just "any three mechanisms". Swapping the two top
        // ones must stop matching; the numbers are distinct, so the swap really changes the grid.
        List<ItemStack> swapped = new ArrayList<>(grid);
        swapped.set(0, mechanismAt(wanted[1]));
        swapped.set(1, mechanismAt(wanted[0]));
        helper.assertFalse(
                recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, swapped), helper.getLevel())
                        .isPresent(),
                "swapping the first two numbers still matched, so their order means nothing");

        // And so does the progress itself: one step off must not match, or any three mechanisms
        // would do.
        List<ItemStack> offByOne = new ArrayList<>(grid);
        offByOne.set(0, mechanismAt(stepOtherThan(wanted[0])));
        helper.assertFalse(
                recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, offByOne), helper.getLevel())
                        .isPresent(),
                "a mechanism one step away from " + wanted[0] + "/15 still matched");

        helper.succeed();
    }

    /**
     * An incomplete precision mechanism at a given step, as Create's tooltip would print it.
     *
     * <p>The step is the record's own {@code step} field - the one the tooltip shows - and the
     * progress is set to what Create would have there, so the stack is shaped like a real one. Only
     * the step is read by {@link MechanismStep}, which is the point: there is no arithmetic left to
     * get wrong.
     */
    private static ItemStack mechanismAt(int displayed) {
        ItemStack stack = new ItemStack(AllItems.INCOMPLETE_PRECISION_MECHANISM.get());
        stack.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                new SequencedAssemblyRecipe.SequencedAssembly(
                        ResourceLocation.fromNamespaceAndPath(CreateEnigma.MOD_ID, "gametest"),
                        displayed, displayed / 15f));
        return stack;
    }

    /** A legal progress that is not this one, for the negative case. */
    private static int stepOtherThan(int displayed) {
        return displayed == 1 ? 2 : 1;
    }

    /**
     * The step the recipe reads must be the step the tooltip prints.
     *
     * <p>This is the whole bug, pinned down. Every step from 0 to 15 round-trips, so an off-by-one at
     * either end fails here instead of in front of a crafting table. The recipe used to derive the
     * number from the item's float progress and scored a printed 1 as 2; that version would have
     * failed this loop on its first iteration, which is exactly why it is worth having.
     */
    @GameTest(template = TEMPLATE)
    public static void mechanismStepIsTheNumberOnTheTooltip(GameTestHelper helper) {
        for (int step = 0; step <= 15; step++) {
            int read = MechanismStep.of(mechanismAt(step));
            helper.assertTrue(read == step,
                    "a mechanism whose tooltip says " + step + "/15 reads as " + read);
        }

        helper.assertTrue(MechanismStep.of(new ItemStack(Items.STICK)) == MechanismStep.NOT_A_MECHANISM,
                "a stick reports a step of " + MechanismStep.of(new ItemStack(Items.STICK)));

        // Create's getStep answers 0 when the component is absent. Mirroring that rather than
        // answering -1 keeps "what the tooltip says" and "what the recipe reads" the same function.
        helper.assertTrue(
                MechanismStep.of(new ItemStack(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())) == 0,
                "a mechanism carrying no assembly component does not read as 0");

        helper.succeed();
    }

    /**
     * The creative hand crank differs from Create's by one number, and that number is the whole
     * feature - so it is asserted where the player's SU actually comes from.
     *
     * <h2>This test used to be useless, and the reason is worth keeping</h2>
     *
     * <p>It first asserted {@code BlockStressValues.getCapacity(ourBlock) == 16384}, which was true
     * from the start and stayed true while the block provided 256 SU. The value a player sees is not
     * that lookup: it is {@code calculateAddedStressCapacity()} on the block entity, and Create's
     * crank reaches the lookup through a <b>hardcoded</b> key that names its own two blocks and
     * nothing else. So the registration was correct, unread, and the test agreed with itself.
     *
     * <p>Both layers are asserted now. The registry one says the number is filed; the block entity
     * one says the number arrives - and it is the second that would have failed.
     */
    @GameTest(template = TEMPLATE)
    public static void creativeHandCrankProvidesCreativeStress(GameTestHelper helper) {
        HandCrankBlock ours = CEBlocks.CREATIVE_HAND_CRANK.get();
        HandCrankBlock theirs = AllBlocks.HAND_CRANK.get();

        // Layer one: the value is registered, and registering it did not disturb Create's.
        helper.assertTrue(BlockStressValues.getCapacity(ours) == 16384.0,
                "the creative hand crank registers " + BlockStressValues.getCapacity(ours)
                        + " su/RPM, expected 16384");
        helper.assertTrue(BlockStressValues.getCapacity(theirs) == 8.0,
                "Create's hand crank registers " + BlockStressValues.getCapacity(theirs)
                        + " su/RPM, expected 8");

        // Layer two: the value reaches the block entity. This is the assertion with teeth - it is
        // what the stress network reads, and what the first version of this test did not check.
        CreativeHandCrankBlockEntity creative = new CreativeHandCrankBlockEntity(
                CEBlockEntities.CREATIVE_HAND_CRANK.get(), BlockPos.ZERO, ours.defaultBlockState());
        helper.assertTrue(creative.calculateAddedStressCapacity() == 16384f,
                "the creative hand crank's block entity provides "
                        + creative.calculateAddedStressCapacity() + " su/RPM, expected 16384");

        HandCrankBlockEntity plain = new HandCrankBlockEntity(
                AllBlockEntityTypes.HAND_CRANK.get(), BlockPos.ZERO, theirs.defaultBlockState());
        helper.assertTrue(plain.calculateAddedStressCapacity() == 8f,
                "Create's hand crank's block entity provides " + plain.calculateAddedStressCapacity()
                        + " su/RPM, expected 8");

        // A block entity type lists the blocks it may appear on. Ours must list ours and only ours,
        // or placing the block creates no block entity and nothing turns.
        BlockEntityType<?> type = CEBlockEntities.CREATIVE_HAND_CRANK.get();
        helper.assertTrue(type.isValid(ours.defaultBlockState()),
                "the creative hand crank's block entity type does not accept its own block");
        helper.assertTrue(!type.isValid(theirs.defaultBlockState()),
                "the creative hand crank's block entity type also accepts Create's crank, so the two"
                        + " blocks are not distinguishable by it");

        // Everything else is meant to be Create's, unchanged.
        helper.assertTrue(ours.getRotationSpeed() == theirs.getRotationSpeed(),
                "the crank turns at " + ours.getRotationSpeed() + " RPM but Create's turns at "
                        + theirs.getRotationSpeed());

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

    /**
     * The core is a shell, not an item: no survival path may put one in a bag.
     *
     * <p>Three things have to agree for that, and two of them are data that a careless edit could
     * undo without any test failing: the block's own numbers (soft, and no tool requirement, so
     * there is no "right tool" to bring), and the loot table (a chest, not the core). The third -
     * the wrench dismantling it back into a chest - is code in {@link EnigmaCoreBlock} that needs a
     * player, so it is checked in game rather than here.
     */
    @GameTest(template = TEMPLATE)
    public static void theCoreIsNotAnItemYouCanKeep(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CEBlocks.ENIGMA_CORE.get().defaultBlockState());
        BlockState core = helper.getBlockState(pos);

        float hardness = core.getDestroySpeed(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(hardness == 3.0F, "the core's hardness is " + hardness + " instead of 3");
        helper.assertTrue(core.getBlock().getExplosionResistance() == 9.0F,
                "the core's blast resistance is " + core.getBlock().getExplosionResistance()
                        + " instead of 9");
        helper.assertFalse(core.requiresCorrectToolForDrops(),
                "the core asks for a specific tool, so a player without it would get nothing back");

        JsonObject loot = readJson("/data/create_enigma/loot_table/blocks/enigma_core.json");
        String drops = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject()
                .get("name").getAsString();
        helper.assertTrue("minecraft:chest".equals(drops),
                "the core's loot table drops " + drops + " instead of a chest");

        helper.succeed();
    }

    // --------------------------------------------------------------------------------
    // 熄灭蛋糕 puts a blaze burner out.
    //
    // The two fuel fields are protected, so the only way to set them up here is the same way the
    // item clears them: through the block entity's own NBT.
    // --------------------------------------------------------------------------------

    /** A lone blaze burner, well away from where the machine tests build. */
    private static final BlockPos BURNER = new BlockPos(2, 1, 2);

    private static BlazeBurnerBlockEntity placeBurner(GameTestHelper helper, HeatLevel heat) {
        helper.setBlock(BURNER,
                AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, heat));
        var be = helper.getBlockEntity(BURNER);
        helper.assertTrue(be instanceof BlazeBurnerBlockEntity,
                "no blaze burner at " + BURNER + ", got " + be);
        return (BlazeBurnerBlockEntity) be;
    }

    private static void putFuel(ServerLevel level, BlazeBurnerBlockEntity burner,
            BlazeBurnerBlockEntity.FuelType type, int burnTime, boolean creative) {
        net.minecraft.nbt.CompoundTag data = burner.saveWithoutMetadata(level.registryAccess());
        data.putInt("fuelLevel", type.ordinal());
        data.putInt("burnTimeRemaining", burnTime);
        if (creative)
            data.putBoolean("isCreative", true);
        burner.loadWithComponents(data, level.registryAccess());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void extinguishedCakePutsOutAKindledBurner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlazeBurnerBlockEntity burner = placeBurner(helper, HeatLevel.KINDLED);
        putFuel(level, burner, BlazeBurnerBlockEntity.FuelType.NORMAL, 5000, false);

        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.KINDLED,
                "the burner starts at " + burner.getHeatLevelFromBlock() + " instead of KINDLED");
        helper.assertTrue(ExtinguishedCakeItem.extinguish(level, helper.absolutePos(BURNER), burner),
                "the extinguished cake refused a burner that was heating");

        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.SMOULDERING,
                "the burner is " + burner.getHeatLevelFromBlock() + " instead of SMOULDERING");
        helper.assertTrue(burner.getActiveFuel() == BlazeBurnerBlockEntity.FuelType.NONE,
                "it still holds " + burner.getActiveFuel() + " fuel");
        helper.assertTrue(burner.getRemainingBurnTime() == 0,
                "it still has " + burner.getRemainingBurnTime() + " burn time left");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void extinguishedCakeRefusesACreativeBurner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlazeBurnerBlockEntity burner = placeBurner(helper, HeatLevel.SEETHING);
        putFuel(level, burner, BlazeBurnerBlockEntity.FuelType.SPECIAL, 5000, true);

        helper.assertTrue(!ExtinguishedCakeItem.extinguish(level, helper.absolutePos(BURNER), burner),
                "the extinguished cake put out a creative burner");
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.SEETHING,
                "the creative burner is now " + burner.getHeatLevelFromBlock());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void extinguishedCakeIgnoresAnIdleBurner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlazeBurnerBlockEntity burner = placeBurner(helper, HeatLevel.SMOULDERING);

        helper.assertTrue(!ExtinguishedCakeItem.extinguish(level, helper.absolutePos(BURNER), burner),
                "the extinguished cake acted on a burner that was not burning");
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.SMOULDERING,
                "the idle burner is now " + burner.getHeatLevelFromBlock());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void extinguishedCakePutsOutALowBurner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlazeBurnerBlockEntity burner = placeBurner(helper, HeatLevel.KINDLED);

        // Below 1.25% of 10000 ticks, which is 125, a lit burner reports FADING rather than KINDLED.
        // It is still burning, so the cake has to accept it: this is the state a player most wants
        // to put a burner out in.
        putFuel(level, burner, BlazeBurnerBlockEntity.FuelType.NORMAL, 50, false);
        burner.updateBlockState();
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.FADING,
                "a burner with 50 ticks left is " + burner.getHeatLevelFromBlock() + ", not FADING");

        helper.assertTrue(ExtinguishedCakeItem.extinguish(level, helper.absolutePos(BURNER), burner),
                "the extinguished cake refused a burner that was fading out");
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.SMOULDERING,
                "the fading burner is " + burner.getHeatLevelFromBlock() + " instead of SMOULDERING");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void extinguishedCakeAdvertisesItsSummary(GameTestHelper helper) {
        ItemStack cake = new ItemStack(CEItems.EXTINGUISHED_CAKE.get());

        // A dedicated server has no shift key, so this is the branch a player sees at rest: the name
        // and Create's own "Hold [Shift] for Summary" line, which has to be there in every state.
        List<net.minecraft.network.chat.Component> lines = cake.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.EMPTY, null,
                net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        helper.assertTrue(lines.size() == 2,
                "expected the item name and the summary hint, got " + lines.size() + " lines");
        boolean hint = lines.get(1).getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                && contents.getKey().equals("create.tooltip.holdForDescription");
        helper.assertTrue(hint, "the second line is not Create's summary hint: " + lines.get(1));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void extinguishedCakeIsFood(GameTestHelper helper) {
        ItemStack cake = new ItemStack(CEItems.EXTINGUISHED_CAKE.get());
        net.minecraft.world.food.FoodProperties food = cake.get(DataComponents.FOOD);
        helper.assertTrue(food != null, "the extinguished cake is not food");

        // One point five from the builder's saturation modifier is already doubled and multiplied by
        // the nutrition when the properties are built, so the record reports the total a full-hunger
        // player receives. Twelve is that total: five hunger times 1.2, doubled.
        helper.assertTrue(food.nutrition() == 5,
                "it restores " + food.nutrition() + " hunger instead of 5");
        helper.assertTrue(Math.abs(food.saturation() - 12.0F) < 1.0E-4F,
                "it restores " + food.saturation() + " saturation instead of 12");

        boolean fireResistance = food.effects().stream().anyMatch(possible ->
                possible.effect().getEffect().value() == net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE.value()
                        && possible.effect().getDuration() == 200);
        helper.assertTrue(fireResistance,
                "it grants no ten seconds of fire resistance: " + food.effects());
        helper.succeed();
    }

    // --------------------------------------------------------------------------------
    // The blueprint writes itself.
    //
    // The rule is small - a wrench, an empty blueprint, one page per click - so what is worth
    // pinning down is the part a later edit could break silently: the page has to be the *same*
    // stack the creative tab hands out, and the pile left on the ground has to stay a pile.
    // --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theWrenchWritesABlankBlueprintOnAPlacard(GameTestHelper helper) {
        BlockPos at = new BlockPos(10, 2, 10);
        helper.setBlock(at, AllBlocks.PLACARD.getDefaultState());

        PlacardBlockEntity placard = (PlacardBlockEntity) helper.getBlockEntity(at);
        placard.setHeldItem(new ItemStack(AllItems.EMPTY_SCHEMATIC.get()));

        helper.assertTrue(BlueprintBlessing.bless(placard),
                "the wrench wrote nothing on a blank blueprint hanging in a placard");

        ItemStack page = placard.getHeldItem();
        helper.assertTrue(page.is(AllItems.SCHEMATIC.get()),
                "the placard still holds " + page);
        helper.assertTrue(EnigmaSchematic.RELATIVE_PATH.equals(page.get(AllDataComponents.SCHEMATIC_FILE)),
                "the page points at the wrong structure: " + page.get(AllDataComponents.SCHEMATIC_FILE));
        helper.assertTrue(page.get(AllDataComponents.SCHEMATIC_BOUNDS) instanceof Vec3i,
                "the page has no bounds, which is what crashed the client last time");

        // Something that is not a blank blueprint is left alone.
        placard.setHeldItem(new ItemStack(Items.DIRT));
        helper.assertTrue(!BlueprintBlessing.bless(placard), "the wrench wrote on a block of dirt");
        helper.assertTrue(placard.getHeldItem().is(Items.DIRT),
                "the dirt was disturbed: " + placard.getHeldItem());

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void theWrenchWritesABlankBlueprint(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(6, 2, 6);

        ItemEntity blanks = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                new ItemStack(AllItems.EMPTY_SCHEMATIC.get(), 2));
        blanks.setPickUpDelay(10);
        level.addFreshEntity(blanks);

        helper.assertTrue(BlueprintBlessing.bless(blanks),
                "the wrench wrote nothing on a blank blueprint");

        helper.assertTrue(blanks.getItem().is(AllItems.EMPTY_SCHEMATIC.get()) && blanks.getItem().getCount() == 1,
                "the pile should have lost one sheet and kept the rest: " + blanks.getItem());

        List<ItemEntity> pages = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(at.getX(), at.getY(), at.getZ(), at.getX() + 1, at.getY() + 1, at.getZ() + 1)
                        .inflate(2.0),
                dropped -> dropped.getItem().is(AllItems.SCHEMATIC.get()));
        helper.assertTrue(pages.size() == 1,
                "expected exactly one written page on the ground, found " + pages.size());

        ItemStack page = pages.get(0).getItem();
        helper.assertTrue(EnigmaSchematic.RELATIVE_PATH.equals(page.get(AllDataComponents.SCHEMATIC_FILE)),
                "the page points at the wrong structure: " + page.get(AllDataComponents.SCHEMATIC_FILE));
        helper.assertTrue(page.get(AllDataComponents.SCHEMATIC_BOUNDS) instanceof Vec3i,
                "the page has no bounds, which is what crashed the client last time");

        ItemEntity dirt = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                new ItemStack(net.minecraft.world.item.Items.DIRT, 1));
        level.addFreshEntity(dirt);
        helper.assertTrue(!BlueprintBlessing.bless(dirt),
                "the wrench wrote on a block of dirt");

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
