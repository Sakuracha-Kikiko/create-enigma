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
 * the viewer shows the shape of the recipe - three of them, plus a cogwheel and a rod - without ever
 * showing which progress each one needs. {@link #matches(CraftingInput, Level)} then checks the real
 * progresses. The two are allowed to differ, and that gap is the whole feature.
 *
 * <p>Nothing is given for a near miss. A wrong progress simply fails to match, so the result slot
 * stays empty. Handing back junk would suggest the attempt was close, and it is not: the three
 * numbers are written plainly in the ponder scene, and reading them is the puzzle.
 *
 * <h2>Why {@code isSpecial} is false</h2>
 *
 * <p>Despite the name, this recipe is not special as far as the game is concerned. JEI skips special
 * recipes - {@code CraftingCategoryExtension.isHandled} returns {@code !recipe.isSpecial()} - so
 * returning true here would hide the skeleton the whole design depends on. The recipe book stays
 * empty for a different reason: recipes are unlocked only by an advancement's {@code rewards.recipes},
 * and this one has none, so no player ever has it unlocked.
 */
public class EnigmaWrenchRecipe implements CraftingRecipe {

    /** Width and height of the shape. The bottom row has a leading space, so it is two wide. */
    private static final int WIDTH = 2;
    private static final int HEIGHT = 3;

    /** Create's tag for wooden rods, the same one the vanilla wrench recipe names. */
    private static final TagKey<Item> WOODEN_RODS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods/wooden"));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // A shaped recipe may sit anywhere in a larger grid, so every offset is tried. In a 3x3
        // grid a two-by-three pattern has exactly two possible positions.
        for (int x = 0; x + WIDTH <= input.width(); x++) {
            for (int y = 0; y + HEIGHT <= input.height(); y++) {
                if (matchesAt(input, level, x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * One position of the pattern.
     *
     * <p>Note the argument order of {@code CraftingInput.getItem}: <b>row first, then column</b>. The
     * loop variables here are named x and y for readability, so every read flips them.
     */
    private boolean matchesAt(CraftingInput input, Level level, int x, int y) {
        int[] wanted = EnigmaCodeState.progresses(level);

        if (!isIntermediateAt(input, x, y, wanted[0])) {
            return false;
        }
        if (!isIntermediateAt(input, x + 1, y, wanted[1])) {
            return false;
        }
        if (!isIntermediateAt(input, x, y + 1, wanted[2])) {
            return false;
        }
        if (!input.getItem(y + 1, x + 1).is(AllBlocks.COGWHEEL.get().asItem())) {
            return false;
        }
        if (!input.getItem(y + 2, x + 1).is(WOODEN_RODS)) {
            return false;
        }

        // Every cell the pattern does not cover has to be empty, or a grid with extra items would
        // count as a match.
        for (int cx = 0; cx < input.width(); cx++) {
            for (int cy = 0; cy < input.height(); cy++) {
                boolean onPattern = cx >= x && cx < x + WIDTH && cy >= y && cy < y + HEIGHT
                        && !(cx == x + 1 && cy == y + 2);
                if (!onPattern && !input.getItem(cy, cx).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Whether a slot holds an incomplete precision mechanism at exactly the wanted step.
     *
     * <p>Create stores the progress as a float and displays it as {@code round(progress * 13)}, which
     * runs 0 to 13, while the number a player reads is one higher: the first assembly step produces a
     * 1/15 intermediate and the fifteenth finishes it, so the reachable display values are 1 to 14.
     * {@link EnigmaCode} speaks in those displayed numbers, so the one is added back here.
     *
     * <p>An off-by-one in this expression is the worst kind of bug this mod can have: the scene says
     * 3, the player builds 3, and the recipe silently wants 2.
     */
    private static boolean isIntermediateAt(CraftingInput input, int x, int y, int wanted) {
        ItemStack stack = input.getItem(y, x);
        if (!(stack.getItem() instanceof SequencedAssemblyItem item)) {
            return false;
        }
        if (!stack.is(AllItems.INCOMPLETE_PRECISION_MECHANISM.get())) {
            return false;
        }
        return Math.round(item.getProgress(stack) * 13) + 1 == wanted;
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
     * <p>Deliberately looser than {@link #matches}: the three mechanisms are named by item only, so no
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
        // See the class comment: JEI hides special recipes, and this one has to be visible there.
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
