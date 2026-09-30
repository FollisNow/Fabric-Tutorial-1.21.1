package net.follis.tutorialmod.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public record GoldenHotelRecipeInput(ItemStack catalyst, ItemStack input1, ItemStack input2) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return switch (slot) {
            case 0 -> catalyst;
            case 1 -> input1;
            case 2 -> input2;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int getSize() {
        return 3;
    }
}