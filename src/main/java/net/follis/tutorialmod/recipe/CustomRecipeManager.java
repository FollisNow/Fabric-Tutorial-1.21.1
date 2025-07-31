package net.follis.tutorialmod.recipe;

import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class CustomRecipeManager {
    public static List<GoldenHotelRecipeBuilder> getRecipes() {
        List<GoldenHotelRecipeBuilder> recipes = new ArrayList<>();

        recipes.add(createRecipe(ModItems.GOLDEN_NEEDLE,
                List.of(ModItems.GOLD_CHAIN, ModItems.GOLD_CHAIN),
                Items.STICK));

        recipes.add(createRecipe(ModItems.BUG_JAR,
                List.of(Blocks.GOLD_BLOCK, ModBlocks.GOLD_FENCE_GATE),
                Items.DECORATED_POT));

        return recipes;
    }

    private static GoldenHotelRecipeBuilder createRecipe(ItemConvertible output,
                                                         List<ItemConvertible> inputs,
                                                         ItemConvertible mainInput) {
        GoldenHotelRecipeBuilder builder = new GoldenHotelRecipeBuilder(output, 1, new ArrayList<>(), Ingredient.EMPTY);

        for (ItemConvertible input : inputs) {
            builder.input(input);
        }

        builder.inputMain(mainInput);

        return builder;
    }


}
