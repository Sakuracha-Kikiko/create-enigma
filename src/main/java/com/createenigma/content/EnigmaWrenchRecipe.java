package com.createenigma.content;

import com.createenigma.registry.CEItems;
import com.createenigma.registry.CERecipes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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

    /** Create's tag for wooden rods, the same one the vanilla wrench recipe names. */
    private static final TagKey<Item> WOODEN_RODS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods/wooden"));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // A shaped recipe may sit anywhere in a grid, so every offset is tried. Note that the grid
        // handed in is already cropped to the cells that hold something, so a 3x3 table showing this
        // pattern arrives as 2x3.
        for (int x = 0; x + WIDTH <= input.width(); x++) {
            for (int y = 0; y + HEIGHT <= input.height(); y++) {
                if (matchesAt(input, level, x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAt(CraftingInput input, Level level, int x, int y) {
        int[] wanted = EnigmaCodeState.progresses(level);

        return isIntermediateAt(input, x, y, wanted[0])
                && isIntermediateAt(input, x + 1, y, wanted[1])
                && isIntermediateAt(input, x, y + 1, wanted[2])
                && at(input, x + 1, y + 1).is(AllBlocks.COGWHEEL.get().asItem())
                && at(input, x + 1, y + 2).is(WOODEN_RODS)
                && restIsEmpty(input, x, y);
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
     * Whether a cell holds an incomplete precision mechanism at exactly the wanted step.
     *
     * <p>Create stores the progress as a float and displays it as {@code round(progress * 13)}, which
     * runs 0 to 13, while the number a player reads off the item's bar is one higher: the first
     * assembly step produces a 1/15 intermediate and the fifteenth finishes it. {@link EnigmaCode}
     * speaks in those displayed numbers, so the one is added back here.
     */
    private static boolean isIntermediateAt(CraftingInput input, int x, int y, int wanted) {
        ItemStack stack = at(input, x, y);
        if (!(stack.getItem() instanceof SequencedAssemblyItem item)) {
            return false;
        }
        return stack.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())
                && Math.round(item.getProgress(stack) * 13) + 1 == wanted;
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
