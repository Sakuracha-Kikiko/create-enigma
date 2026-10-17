package com.createenigma.content;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Serialiser for {@link EnigmaWrenchRecipe}.
 *
 * <p>Both codecs carry no data, because the recipe has none: its pattern, ingredients and answer are
 * all in the class. The JSON that names this serialiser is therefore a single line, and nothing in a
 * datapack can change what the recipe asks for - which is the point, since the answer is derived from
 * the world seed.
 *
 * <h2>Why the stream codec is not {@code StreamCodec.unit}</h2>
 *
 * <p>{@code unit(value)} means "the only legal value is <b>this exact object</b>", and its encoder
 * checks for identity. The recipe sent to a client was parsed from JSON, so it is a different
 * instance from the one the unit codec holds - and the encoder threw
 * <em>Can't encode ... expected ...</em> while building the recipe sync packet. That failure happens
 * on join, in the packet that carries every recipe, so the client is disconnected before the world
 * is even visible.
 *
 * <p>What is wanted is "write nothing, read a fresh instance", which is {@code StreamCodec.of} with a
 * no-op encoder.
 */
public class EnigmaWrenchRecipeSerializer implements RecipeSerializer<EnigmaWrenchRecipe> {

    private static final MapCodec<EnigmaWrenchRecipe> CODEC =
            MapCodec.unit(EnigmaWrenchRecipe::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (RegistryFriendlyByteBuf buffer, EnigmaWrenchRecipe recipe) -> { },
                    (RegistryFriendlyByteBuf buffer) -> new EnigmaWrenchRecipe());

    @Override
    public MapCodec<EnigmaWrenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnigmaWrenchRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
