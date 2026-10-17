package com.createenigma.content;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serialiser for {@link EnigmaWrenchRecipe}.
 *
 * <p>Both codecs are units, because the recipe carries no data: its pattern, its ingredients and its
 * answer are all in the class. The JSON that names this serialiser is therefore a single line, and
 * nothing in a datapack can change what the recipe asks for - which is the point, since the answer is
 * derived from the world seed.
 */
public class EnigmaWrenchRecipeSerializer implements RecipeSerializer<EnigmaWrenchRecipe> {

    private static final MapCodec<EnigmaWrenchRecipe> CODEC =
            MapCodec.unit(EnigmaWrenchRecipe::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> STREAM_CODEC =
            StreamCodec.unit(new EnigmaWrenchRecipe());

    @Override
    public MapCodec<EnigmaWrenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
