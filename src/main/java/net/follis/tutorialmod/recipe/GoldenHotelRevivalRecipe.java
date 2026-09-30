package net.follis.tutorialmod.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public record GoldenHotelRevivalRecipe(Ingredient input1, Ingredient input2) implements Recipe<GoldenHotelRecipeInput> {
    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> list = DefaultedList.of();
        list.add(input1);
        list.add(input2);
        return list;
    }

    @Override
    public boolean matches(GoldenHotelRecipeInput input, World world) {
        if (world.isClient()) {
            return false;
        }
        ItemStack a = input.getStackInSlot(1);
        ItemStack b = input.getStackInSlot(2);
        return (input1.test(a) && input2.test(b)) || (input1.test(b) && input2.test(a));
    }

    // Revival produces no ItemStack — the block entity spawns the entity directly.
    @Override
    public ItemStack craft(GoldenHotelRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GOLDEN_HOTEL_REVIVAL_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GOLDEN_HOTEL_REVIVAL_TYPE;
    }

    public static class Serializer implements RecipeSerializer<GoldenHotelRevivalRecipe> {
        public static final MapCodec<GoldenHotelRevivalRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient1").forGetter(GoldenHotelRevivalRecipe::input1),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient2").forGetter(GoldenHotelRevivalRecipe::input2)
        ).apply(inst, GoldenHotelRevivalRecipe::new));

        public static final PacketCodec<RegistryByteBuf, GoldenHotelRevivalRecipe> STREAM_CODEC =
                PacketCodec.tuple(
                        Ingredient.PACKET_CODEC, GoldenHotelRevivalRecipe::input1,
                        Ingredient.PACKET_CODEC, GoldenHotelRevivalRecipe::input2,
                        GoldenHotelRevivalRecipe::new);

        @Override
        public MapCodec<GoldenHotelRevivalRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, GoldenHotelRevivalRecipe> packetCodec() {
            return STREAM_CODEC;
        }
    }
}