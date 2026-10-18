package com.createenigma.content;

import java.util.concurrent.atomic.AtomicInteger;

import com.createenigma.registry.CEItems;
import com.createenigma.registry.CERecipes;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.slf4j.Logger;

/**
 * The Enigma Wrench, crafted in the Wrench's own pattern out of three precision mechanism
 * intermediates whose assembly progresses are this world's numbers.
 *
 * <pre>
 *   G G
 *   G P
 *     S
 * </pre>
 *
 * <p>G is an incomplete precision mechanism at a specific step, P a cogwheel, S a wooden rod - the
 * vanilla wrench exactly, with gold sheets replaced by the thing that looks like it was made by
 * something that knew what it was doing.
 *
 * <h2>Declared loosely, matched strictly</h2>
 *
 * <p>{@link #getIngredients()} says "any incomplete precision mechanism". That is what JEI draws, so
 * the viewer shows the shape of the recipe without ever showing which progress each one needs.
 * {@link #matches} then checks the real progresses. The two are allowed to differ, and that gap is
 * the whole feature.
 *
 * <p>Nothing is given for a near miss. A wrong progress simply fails to match, so the result slot
 * stays empty. The three numbers are written plainly in the ponder scene, and reading them is the
 * puzzle.
 *
 * <h2>{@code CraftingInput.getItem}'s parameters are misnamed</h2>
 *
 * <p>The storage built by {@code ofPositioned} is row-major - {@code column + row * width} - but the
 * accessor reads {@code row + column * width}, which is column-major:
 *
 * <pre>
 * ofPositioned:  list.add(items.get(k1 + i + (k2 + k) * width));   // k1 = column, k2 = row
 * getItem:       return this.items.get(row  +  column * this.width);
 * </pre>
 *
 * <p>So the parameter called {@code row} is really the column. Every read in this class goes through
 * {@link #at}, which says so once instead of at seven call sites. Getting this backwards is silent:
 * the recipe reads a transposed grid and never matches, and the grid it prints in a log is
 * transposed too, so the mistake looks like the player's.
 */
public class EnigmaWrenchRecipe implements CraftingRecipe {

    /** Width and height of the shape. The bottom row is " S", so the shape is two wide. */
    private static final int WIDTH = 2;
    private static final int HEIGHT = 3;

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Numbers the diagnostic lines, so one crafting attempt can be told from the next.
     *
     * <p>Deliberately a running counter rather than a per-grid fingerprint: what matters is whether
     * a click in the table produces <em>any</em> call at all, and then what that call was looking at.
     * Remove every user of this alongside the surrounding log lines once the recipe works.
     */
    private static final AtomicInteger CALLS = new AtomicInteger();

    /** Create's tag for wooden rods, the same one the vanilla wrench recipe names. */
    private static final TagKey<Item> WOODEN_RODS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods/wooden"));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int call = CALLS.incrementAndGet();
        int[] wanted = EnigmaCodeState.progresses(level);
        LOGGER.info("Enigma recipe #{}: asked on {} for a {}x{} grid, wanted {} {} {}",
                call, level.isClientSide() ? "client" : "server",
                input.width(), input.height(), wanted[0], wanted[1], wanted[2]);
        logGrid(call, input);

        // A shaped recipe may sit anywhere in a grid, so every offset is tried. Note that the grid
        // handed in is already cropped to the cells that hold something, so a 3x3 table showing this
        // pattern arrives as 2x3.
        for (int x = 0; x + WIDTH <= input.width(); x++) {
            for (int y = 0; y + HEIGHT <= input.height(); y++) {
                if (matchesAt(input, level, call, x, y)) {
                    LOGGER.info("Enigma recipe #{}: MATCHED at offset {},{}", call, x, y);
                    return true;
                }
            }
        }
        LOGGER.info("Enigma recipe #{}: no offset matched", call);
        return false;
    }

    private boolean matchesAt(CraftingInput input, Level level, int call, int x, int y) {
        int[] wanted = EnigmaCodeState.progresses(level);

        // Each check is named so a refusal says which cell was wrong and what was in it, instead of
        // leaving the next reader to guess again.
        if (progressAt(input, x, y) != wanted[0]) {
            return refuse(call, x, y, "top left", progress(progressAt(input, x, y)), progress(wanted[0]));
        }
        if (progressAt(input, x + 1, y) != wanted[1]) {
            return refuse(call, x, y, "top right", progress(progressAt(input, x + 1, y)), progress(wanted[1]));
        }
        if (progressAt(input, x, y + 1) != wanted[2]) {
            return refuse(call, x, y, "middle left", progress(progressAt(input, x, y + 1)), progress(wanted[2]));
        }
        if (!at(input, x + 1, y + 1).is(AllBlocks.COGWHEEL.get().asItem())) {
            return refuse(call, x, y, "middle right", describeCell(input, x + 1, y + 1), "a cogwheel");
        }
        if (!at(input, x + 1, y + 2).is(WOODEN_RODS)) {
            return refuse(call, x, y, "bottom right", describeCell(input, x + 1, y + 2), "a wooden rod");
        }
        return restIsEmpty(input, x, y) || refuseExtras(call, x, y, input);
    }

    /** "12/15", or "nothing at all" for a cell that is not a mechanism. */
    private static String progress(int progress) {
        return progress < 0 ? "nothing at all" : progress + "/15";
    }

    /** Logs one refusal and answers false, so a check can read as {@code return refuse(...)}. */
    private static boolean refuse(int call, int x, int y, String where, String found, String wanted) {
        LOGGER.info("Enigma recipe #{}: {} at offset {},{} - found {}, wanted {}",
                call, where, x, y, found, wanted);
        return false;
    }

    /** Finds and reports the first cell that should have been empty and is not. */
    private static boolean refuseExtras(int call, int x, int y, CraftingInput input) {
        for (int cx = 0; cx < input.width(); cx++) {
            for (int cy = 0; cy < input.height(); cy++) {
                boolean inBox = cx >= x && cx < x + WIDTH && cy >= y && cy < y + HEIGHT;
                boolean blankInPattern = inBox && cx == x && cy == y + 2;
                if ((!inBox || blankInPattern) && !at(input, cx, cy).isEmpty()) {
                    LOGGER.info("Enigma recipe #{}: unwanted {} at {},{} for offset {},{}",
                            call, describeCell(input, cx, cy), cx, cy, x, y);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Prints the grid twice, under both readings of {@code CraftingInput.getItem}.
     *
     * <p>This is the one measurement that settles the transposition question without trusting any
     * theory about it. Read as rows of three, the correct table is the one that looks like
     * <pre>
     *   M M .
     *   M C .
     *   . S .
     * </pre>
     * and the wrong one is its mirror.
     */
    private static void logGrid(int call, CraftingInput input) {
        LOGGER.info("Enigma recipe #{}: as getItem(column, row):", call);
        for (int y = 0; y < input.height(); y++) {
            StringBuilder row = new StringBuilder("    ");
            for (int cx = 0; cx < input.width(); cx++) {
                row.append(String.format("%-24s", describeCell(input, cx, y)));
            }
            LOGGER.info("Enigma recipe #{}: {}", call, row);
        }
        LOGGER.info("Enigma recipe #{}: as getItem(row, column):", call);
        for (int cy = 0; cy < input.width(); cy++) {
            StringBuilder row = new StringBuilder("    ");
            for (int cx = 0; cx < input.height(); cx++) {
                row.append(String.format("%-24s", describeCell(input, cy, cx)));
            }
            LOGGER.info("Enigma recipe #{}: {}", call, row);
        }
    }

    /**
     * Reads one cell, with x as the column and y as the row.
     *
     * <p>Exists because {@code CraftingInput.getItem(row, column)} indexes column-major while its
     * backing list is built row-major, so the first argument is the column despite its name. One
     * place to notice that is better than seven.
     */
    private static ItemStack at(CraftingInput input, int x, int y) {
        return input.getItem(x, y);
    }

    /**
     * Every cell outside the shape has to be empty, or a grid with extra items would match.
     *
     * <p>Inside the two-by-three box exactly one cell is blank - the bottom left - because the bottom
     * row of the pattern is {@code " S"}.
     */
    private static boolean restIsEmpty(CraftingInput input, int x, int y) {
        for (int cx = 0; cx < input.width(); cx++) {
            for (int cy = 0; cy < input.height(); cy++) {
                boolean inBox = cx >= x && cx < x + WIDTH && cy >= y && cy < y + HEIGHT;
                boolean blankInPattern = inBox && cx == x && cy == y + 2;
                if ((!inBox || blankInPattern) && !at(input, cx, cy).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The progress a cell would display, or -1 when it holds no incomplete precision mechanism.
     *
     * <p>Create stores the progress as a float and displays it as {@code round(progress * 13)}, which
     * runs 0 to 13, while the number a player reads off the item's bar is one higher: the first
     * assembly step produces a 1/15 intermediate and the fifteenth finishes it. {@link EnigmaCode}
     * speaks in those displayed numbers, so the one is added back here.
     */
    private static int progressAt(CraftingInput input, int x, int y) {
        ItemStack stack = at(input, x, y);
        if (!(stack.getItem() instanceof SequencedAssemblyItem item)) {
            return -1;
        }
        if (!stack.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())) {
            return -1;
        }
        return Math.round(item.getProgress(stack) * 13) + 1;
    }

    /** One cell, briefly, for the log. */
    private static String describeCell(CraftingInput input, int x, int y) {
        ItemStack stack = at(input, x, y);
        if (stack.isEmpty()) {
            return ".";
        }
        int progress = progressAt(input, x, y);
        if (progress >= 0) {
            return "mechanism " + progress + "/15";
        }
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(CEItems.ENIGMA_WRENCH.get());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(CEItems.ENIGMA_WRENCH.get());
    }

    /**
     * What the recipe claims to need, for JEI and the recipe book.
     *
     * <p>Deliberately looser than {@link #matches}: the mechanisms are named by item only, so no
     * progress is revealed. Row-major, in the order the pattern reads.
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient intermediate = Ingredient.of(AllItems.INCOMPLETE_PRECISION_MECHANISM.get());
        return NonNullList.of(Ingredient.EMPTY,
                intermediate, intermediate,
                intermediate, Ingredient.of(AllBlocks.COGWHEEL.get().asItem()),
                Ingredient.of(WOODEN_RODS));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= WIDTH && height >= HEIGHT;
    }

    @Override
    public boolean isSpecial() {
        // JEI hides special recipes, and this one has to be visible there.
        return false;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CERecipes.ENIGMA_WRENCH_SERIALIZER.get();
    }
}
