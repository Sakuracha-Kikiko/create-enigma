package com.createenigma.registry;

import com.createenigma.CreateEnigma;
import com.createenigma.content.EnigmaWrenchRecipe;
import com.createenigma.content.EnigmaWrenchRecipeSerializer;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This mod's recipe type.
 *
 * <p>The recipe itself lives in a datapack JSON with no fields - the whole of its behaviour is the
 * code behind {@link EnigmaWrenchRecipe}. The type exists so the game knows what that JSON is and so
 * the recipe manager will hand it out.
 */
public final class CERecipes {

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CreateEnigma.MOD_ID);

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateEnigma.MOD_ID);

    public static final Supplier<RecipeType<EnigmaWrenchRecipe>> ENIGMA_WRENCH_TYPE =
            TYPES.register("enigma_wrench", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return CreateEnigma.MOD_ID + ":enigma_wrench";
                }
            });

    public static final Supplier<RecipeSerializer<EnigmaWrenchRecipe>> ENIGMA_WRENCH_SERIALIZER =
            SERIALIZERS.register("enigma_wrench", EnigmaWrenchRecipeSerializer::new);

    private CERecipes() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
