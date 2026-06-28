package com.archmichaeliii.dwarvenengineering.content.turbine.fuel;

import com.archmichaeliii.dwarvenengineering.register.DwarvenRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A data-driven definition of a fluid the Dwarven Turbine can burn.
 *
 * <p>The fuel fluid is referenced by its registry id (e.g. {@code petrochem:diesel}) so this mod
 * needs no compile-time dependency on Petrochem — the recipe simply does nothing if the fluid
 * is absent. Each fuel carries its own burn rate and electrical power, which is what lets premium
 * fuels (LPG, gasoline) out-produce crude petroleum.</p>
 *
 * <ul>
 *   <li>{@code fluid}     - registry id of the burnable fluid</li>
 *   <li>{@code burnRate}  - millibuckets consumed per tick while burning</li>
 *   <li>{@code power}     - electrical power produced while burning, in Watts (1 W = 1 Create su)</li>
 * </ul>
 *
 * This is a data-only recipe: it is never "crafted", it is looked up at runtime by the turbine
 * controller, so the vanilla crafting methods are inert.
 */
public record TurbineFuelRecipe(ResourceLocation fluid, int burnRate, int power) implements Recipe<RecipeInput> {

    /** @return true if the given fluid stack is this recipe's fuel. */
    public boolean matchesFluid(FluidStack stack) {
        if (stack.isEmpty())
            return false;
        return BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(fluid);
    }

    // --- inert vanilla crafting contract (this recipe is looked up, never crafted) ---
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getToastSymbol() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DwarvenRecipeTypes.TURBINE_FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return DwarvenRecipeTypes.TURBINE_FUEL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<TurbineFuelRecipe> {

        public static final MapCodec<TurbineFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(TurbineFuelRecipe::fluid),
                Codec.INT.fieldOf("burn_rate").forGetter(TurbineFuelRecipe::burnRate),
                Codec.INT.fieldOf("power").forGetter(TurbineFuelRecipe::power)
        ).apply(instance, TurbineFuelRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, TurbineFuelRecipe> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, TurbineFuelRecipe::fluid,
                ByteBufCodecs.VAR_INT, TurbineFuelRecipe::burnRate,
                ByteBufCodecs.VAR_INT, TurbineFuelRecipe::power,
                TurbineFuelRecipe::new
        );

        @Override
        public MapCodec<TurbineFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TurbineFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
