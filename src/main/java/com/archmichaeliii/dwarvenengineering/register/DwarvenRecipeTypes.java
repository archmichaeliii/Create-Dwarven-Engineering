package com.archmichaeliii.dwarvenengineering.register;

import com.archmichaeliii.dwarvenengineering.DwarvenEngineering;
import com.archmichaeliii.dwarvenengineering.content.turbine.fuel.TurbineFuelRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the {@code dwarvenengineering:turbine_fuel} recipe type and serializer. The turbine
 * controller looks fuels up at runtime via {@link RecipeType}, so any datapack (including this
 * mod's bundled recipes referencing Petrochem fluids) can declare what burns and how much power
 * it makes.
 */
public class DwarvenRecipeTypes {

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, DwarvenEngineering.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, DwarvenEngineering.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<TurbineFuelRecipe>> TURBINE_FUEL_TYPE =
            TYPES.register("turbine_fuel", () -> RecipeType.<TurbineFuelRecipe>simple(DwarvenEngineering.asResource("turbine_fuel")));

    public static final DeferredHolder<RecipeSerializer<?>, TurbineFuelRecipe.Serializer> TURBINE_FUEL_SERIALIZER =
            SERIALIZERS.register("turbine_fuel", TurbineFuelRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }
}
