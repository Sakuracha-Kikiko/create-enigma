package com.createenigma.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Serialiser for {@link EnigmaWrenchRecipe}.
 *
 * <h2>The recipe carries data now, and has to</h2>
 *
 * <p>It used to carry none: the pattern lived in the class and the JSON was one line naming this
 * serialiser. That is why JEI drew it as shapeless - nothing outside the class could say how wide it
 * was. The pattern is now loaded from the datapack like any other shaped recipe, so
 * {@code getWidth()}, {@code getHeight()} and {@code getIngredients()} all come from the file that
 * declares the shape, and the recipe viewer draws it.
 *
 * <p>The one thing a datapack cannot change is the <b>matching</b>: {@link EnigmaWrenchRecipe#matches}
 * still decides which steps the three mechanisms must be at, because those come from the world seed.
 *
 * <h2>Why the fields are written out rather than delegated</h2>
 *
 * <p>{@code ShapedRecipe}'s own codec cannot be reused: its {@code result} field is package-private,
 * so a subclass in this package cannot offer a getter for it, and the codec needs one to write the
 * field back out. Hence the copy of the result held by {@link EnigmaWrenchRecipe}.
 *
 * <p>Both codecs follow vanilla's shaped recipe in shape and order, so the JSON reads exactly like a
 * normal shaped recipe with a different {@code type}.
 */
public class EnigmaWrenchRecipeSerializer implements RecipeSerializer<EnigmaWrenchRecipe> {

    public static final MapCodec<EnigmaWrenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(EnigmaWrenchRecipe::getGroup),
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.EQUIPMENT)
                    .forGetter(EnigmaWrenchRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(EnigmaWrenchRecipe::result))
            .apply(instance, EnigmaWrenchRecipe::new));

    /**
     * The pattern has to cross the wire.
     *
     * <p>The client needs it for two reasons: JEI reads the client's recipe manager, and so does the
     * recipe book. Sending nothing would leave the client with a recipe it cannot draw - which is
     * what the old no-op codec did, and why the shape could not have been shown even if the class had
     * reported a width.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EnigmaWrenchRecipe::getGroup,
                    CraftingBookCategory.STREAM_CODEC, EnigmaWrenchRecipe::category,
                    ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
                    ItemStack.STREAM_CODEC, EnigmaWrenchRecipe::result,
                    EnigmaWrenchRecipe::new);

    @Override
    public MapCodec<EnigmaWrenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
