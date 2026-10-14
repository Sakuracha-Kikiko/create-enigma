package com.createenigma.structure;

import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Decides whether a block standing in the world satisfies a block recorded in the structure
 * template.
 *
 * <p><b>Why this cannot be a plain {@code equals}.</b> Several of the properties stored in the
 * template are not choices the builder made - they are derived by the game from the surrounding
 * blocks, or they change on their own:
 *
 * <ul>
 *   <li>Create computes a belt's {@code part}/{@code slope}/{@code facing} from the pulley shafts
 *       around it, and pane connection booleans from neighbouring panes;</li>
 *   <li>{@code waterlogged} depends on whether it rained, {@code powered}/{@code enabled} on
 *       whether a redstone signal happens to be present, and {@code snowy} on the biome.</li>
 * </ul>
 *
 * <p>Comparing every property would therefore make the multiblock refuse to form for reasons the
 * player cannot see or control. This class compares the block itself plus every property except an
 * explicit ignore set.
 */
public final class BlockStateMatcher {

    private BlockStateMatcher() {}

    /**
     * Properties that never describe the build, only the moment. Ignored for every block.
     *
     * <p>{@code powered} and {@code enabled} flip with redstone; {@code waterlogged} flips with rain
     * and with neighbouring water.
     */
    private static final Set<String> ALWAYS_IGNORED = Set.of("waterlogged", "powered", "enabled");

    /**
     * Extra per-block exceptions, keyed by registry name so this class does not need to touch
     * Create's {@code AllBlocks} (and cannot be broken by a constant being renamed).
     *
     * <p>Kept deliberately short: every entry here is a property this mod has decided does not have
     * to match, and each one is a place where a correct build could otherwise be rejected.
     */
    private static final Map<String, Set<String>> EXTRA_IGNORED = Map.of(
            // snow appears and melts on its own
            "minecraft:grass_block", Set.of("snowy"),
            // a double chest reports type=left/right, and facing only says which way the lid opens
            "minecraft:chest", Set.of("type", "facing"),
            // shape is recomputed from neighbours, and is cosmetic on a straight run
            "minecraft:spruce_stairs", Set.of("shape"),
            // pane connections are recomputed from neighbouring panes and are purely cosmetic
            "create:oak_window_pane", Set.of("north", "south", "east", "west"),
            // A belt's direction is not the player's choice - and since the belts came out of the
            // blueprint, demanding one would be demanding a guess. Create derives facing and part
            // (START/MIDDLE/END/PULLEY) when a run is placed, and which end you click first decides
            // both; the two outcomes are the same belt running the same line, mirrored. A builder
            // has no way to tell which one the original used.
            //
            // Slope stays compared. A belt on a slope in a flat slot is a different machine.
            "create:belt", Set.of("facing", "part"),
            // the core's own state - we are standing on it, and it is what we are computing
            "create_enigma:enigma_core", Set.of("assembled"));

    /**
     * Blocks this mod treats as interchangeable, keyed by registry name.
     *
     * <p><b>"It is not picky."</b> The machine was built with Create's Creative Motors, which are
     * unobtainable in survival. This mod ships a disguised copy that behaves the same and looks the
     * same, and the whole conceit is that the machine cannot tell the difference - so the validator
     * must not be able to either.
     *
     * <p>Both directions are listed rather than special-casing one, because both templates need it:
     * the first-generation template records the disguised motor and the complete one records the
     * real Creative Motor, and a player may legitimately end up with either block in either place.
     *
     * <p>Only the block is substituted; the properties are still compared, and the two blocks carry
     * the same {@code facing} property, so a motor pointing the wrong way is still rejected.
     */
    private static final Map<String, String> SUBSTITUTES = Map.of(
            "create_enigma:disguised_motor", "create:creative_motor",
            "create:creative_motor", "create_enigma:disguised_motor");

    public static boolean matches(BlockState expected, BlockState actual) {
        if (!blockMatches(expected, actual)) {
            return false;
        }

        Set<String> extra = EXTRA_IGNORED.getOrDefault(registryNameOf(expected), Set.of());

        for (Property<?> property : expected.getProperties()) {
            String name = property.getName();
            if (ALWAYS_IGNORED.contains(name) || extra.contains(name)) {
                continue;
            }
            if (!propertyMatches(expected, actual, property)) {
                return false;
            }
        }
        return true;
    }

    private static boolean blockMatches(BlockState expected, BlockState actual) {
        if (expected.getBlock() == actual.getBlock()) {
            return true;
        }
        // get() returns null for the overwhelming majority of blocks - every block not in the
        // substitution table - so this is the common path, not an edge case.
        String substitute = SUBSTITUTES.get(registryNameOf(expected));
        return substitute != null && substitute.equals(registryNameOf(actual));
    }

    /** Names the first property that differs, for diagnostics. Empty when the states match. */
    public static String firstDifference(BlockState expected, BlockState actual) {
        if (!blockMatches(expected, actual)) {
            return "block is " + registryNameOf(actual) + ", expected " + registryNameOf(expected);
        }

        Set<String> extra = EXTRA_IGNORED.getOrDefault(registryNameOf(expected), Set.of());
        for (Property<?> property : expected.getProperties()) {
            String name = property.getName();
            if (ALWAYS_IGNORED.contains(name) || extra.contains(name)) {
                continue;
            }
            if (!propertyMatches(expected, actual, property)) {
                return property.getName() + " is " + actual.getValue(property)
                        + ", expected " + expected.getValue(property);
            }
        }
        return "";
    }

    /**
     * Comparing through a generic helper is what lets a {@code Property<?>} be read: the wildcard is
     * captured into {@code T} here, so {@code getValue} type-checks on both states.
     */
    private static <T extends Comparable<T>> boolean propertyMatches(BlockState expected,
                                                                    BlockState actual,
                                                                    Property<T> property) {
        return actual.hasProperty(property) && expected.getValue(property).equals(actual.getValue(property));
    }

    private static String registryNameOf(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
