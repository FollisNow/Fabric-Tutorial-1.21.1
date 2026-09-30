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

public record GoldenHotelRecipe(Ingredient catalyst, Ingredient input1, Ingredient input2, ItemStack output) implements Recipe<GoldenHotelRecipeInput> {
    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> list = DefaultedList.of();
        list.add(catalyst);
        list.add(input1);
        list.add(input2);
        return list;
    }

    @Override
    public boolean matches(GoldenHotelRecipeInput input, World world) {
        if (world.isClient()) {
            return false;
        }

        if (!catalyst.test(input.getStackInSlot(0))) {
            return false;
        }

        ItemStack a = input.getStackInSlot(1);
        ItemStack b = input.getStackInSlot(2);

        return (input1.test(a) && input2.test(b)) || (input1.test(b) && input2.test(a));
    }

    @Override
    public ItemStack craft(GoldenHotelRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GOLDEN_HOTEL_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GOLDEN_HOTEL_TYPE;
    }

    public static class Serializer implements RecipeSerializer<GoldenHotelRecipe> {
        public static final MapCodec<GoldenHotelRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("catalyst").forGetter(GoldenHotelRecipe::catalyst),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient1").forGetter(GoldenHotelRecipe::input1),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient2").forGetter(GoldenHotelRecipe::input2),
                ItemStack.CODEC.fieldOf("result").forGetter(GoldenHotelRecipe::output)
        ).apply(inst, GoldenHotelRecipe::new));

        public static final PacketCodec<RegistryByteBuf, GoldenHotelRecipe> STREAM_CODEC =
                PacketCodec.tuple(
                        Ingredient.PACKET_CODEC, GoldenHotelRecipe::catalyst,
                        Ingredient.PACKET_CODEC, GoldenHotelRecipe::input1,
                        Ingredient.PACKET_CODEC, GoldenHotelRecipe::input2,
                        ItemStack.PACKET_CODEC, GoldenHotelRecipe::output,
                        GoldenHotelRecipe::new);

        @Override
        public MapCodec<GoldenHotelRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, GoldenHotelRecipe> packetCodec() {
            return STREAM_CODEC;
        }
    }
}