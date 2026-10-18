package com.createenigma.content;

import com.createenigma.registry.CERecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * The Enigma Wrench, crafted in the Wrench's own pattern out of three precision mechanism
 * intermediates whose assembly steps are this world's numbers.
 *
 * <pre>
 *   M M
 *   M C
 *     S
 * </pre>
 *
 * <p>M is an incomplete precision mechanism at a specific step, C a cogwheel, S a wooden rod - the
 * vanilla wrench exactly, with gold sheets replaced by the thing that looks like it was made by
 * something that knew what it was doing.
 *
 * <h2>Why this extends {@link ShapedRecipe}</h2>
 *
 * <p>It always was a shaped recipe; it just did not say so, and that cost two visible bugs in JEI.
 *
 * <p>JEI decides how to draw a crafting recipe by asking whether it is a {@code ShapedRecipe} (JEI's
 * {@code CraftingCategoryExtension#getWidth} checks that instanceof and returns 0 otherwise). A
 * recipe that merely implements {@link net.minecraft.world.item.crafting.CraftingRecipe} reports no
 * width or height, so it was drawn as 无序: five cells in a row, not the shape above.
 *
 * <p>Worse, the two views disagreed about how many cells there are. A two-by-three shape has six, and
 * JEI maps ingredient <i>i</i> into grid slot {@code {0, 1, 3, 4, 6, 7}}. This class used to declare
 * five ingredients, one per non-blank cell, so the fifth - the rod - was drawn and transferred into
 * slot 6, the blank bottom-left corner, while the crafting table wanted it in slot 7. Pressing JEI's
 * transfer button therefore moved the player's mechanisms out of the inventory into the wrong cells,
 * and the result never appeared. Both symptoms, one cause: five ingredients declared for a six-cell
 * shape.
 *
 * <p>Deriving from {@code ShapedRecipe} fixes both at the source. The pattern knows its own width,
 * height and six ingredients - blank cell included - so {@code getIngredients()} is right without
 * being written by hand, and JEI sees a shape.
 *
 * <h2>Declared loosely, matched strictly</h2>
 *
 * <p>The pattern names the intermediates by item only. That is what JEI draws, so the viewer shows
 * the shape of the recipe without ever showing which step each one needs. {@link #matches} then
 * checks the real steps. The two are allowed to differ, and that gap is the whole feature.
 *
 * <p>Nothing is given for a near miss. A wrong step simply fails to match, so the result slot stays
 * empty. The three numbers are written plainly in the ponder scene, and reading them is the puzzle.
 *
 * <h2>Where the steps are checked</h2>
 *
 * <p>{@code ShapedRecipe} finds the pattern's position but does not report it, and the steps can only
 * be checked once the position is known. So the shape is confirmed by the superclass and the offset
 * is then located again here, cell by cell, against the same pattern.
 */
public class EnigmaWrenchRecipe extends ShapedRecipe {

    /**
     * The three mechanism cells of the pattern, as {column, row}, in the order {@link EnigmaCode}
     * reads them out.
     *
     * <p>Must stay in step with the {@code pattern} in the recipe's json: those three cells are what
     * {@link #matches} pins to the world's numbers. The gametest asserts the loaded pattern really
     * does put a mechanism in each of them, so editing the json without editing this fails a test
     * instead of silently asking for the wrong steps.
     */
    private static final int[][] MECHANISM_CELLS = {{0, 0}, {1, 0}, {0, 1}};

    /**
     * The recipe's own copy of its result, because {@code ShapedRecipe.result} is package-private and
     * therefore unreadable from here - the codec needs a getter to write the field back out.
     */
    private final ItemStack result;

    public EnigmaWrenchRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
            ItemStack result) {
        super(group, category, pattern, result);
        this.result = result;
    }

    /** What this recipe produces. The same stack {@code ShapedRecipe} hands to the result slot. */
    public ItemStack result() {
        return result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // The shape is the superclass's job, and it already handles cropping and offsets.
        if (!super.matches(input, level)) {
            return false;
        }

        int width = pattern.width();
        int height = pattern.height();
        for (int x = 0; x + width <= input.width(); x++) {
            for (int y = 0; y + height <= input.height(); y++) {
                if (patternAt(input, x, y) && stepsAt(input, x, y, EnigmaCodeState.progresses(level))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether the pattern's cells all sit at this offset. Blank pattern cells must be empty. */
    private boolean patternAt(CraftingInput input, int x, int y) {
        NonNullList<Ingredient> ingredients = pattern.ingredients();
        int width = pattern.width();
        for (int row = 0; row < pattern.height(); row++) {
            for (int column = 0; column < width; column++) {
                if (!ingredients.get(column + row * width).test(at(input, x + column, y + row))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether the three mechanism cells carry exactly this world's steps, in order. */
    private boolean stepsAt(CraftingInput input, int x, int y, int[] wanted) {
        for (int i = 0; i < MECHANISM_CELLS.length; i++) {
            if (MechanismStep.of(at(input, x + MECHANISM_CELLS[i][0], y + MECHANISM_CELLS[i][1])) != wanted[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads one cell, with x as the column and y as the row.
     *
     * <p>Exists because {@code CraftingInput.getItem(row, column)} indexes column-major while its
     * backing list is built row-major, so the first argument is the column despite its name. One
     * place to notice that is better than several. Reading a grid the other way round is silent: the
     * recipe simply never matches, and the grid it prints in a log is transposed too, so the mistake
     * looks like the player's.
     */
    private static ItemStack at(CraftingInput input, int x, int y) {
        return input.getItem(x, y);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CERecipes.ENIGMA_WRENCH_SERIALIZER.get();
    }
}
