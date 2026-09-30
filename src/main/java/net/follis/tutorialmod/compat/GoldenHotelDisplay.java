package net.follis.tutorialmod.compat;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.follis.tutorialmod.recipe.GoldenHotelRecipe;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class GoldenHotelDisplay extends BasicDisplay {
    public GoldenHotelDisplay(RecipeEntry<GoldenHotelRecipe> recipe) {
        super(List.of(
                        EntryIngredients.ofIngredient(recipe.value().getIngredients().get(0)), // catalyst
                        EntryIngredients.ofIngredient(recipe.value().getIngredients().get(1)), // pedestal 1
                        EntryIngredients.ofIngredient(recipe.value().getIngredients().get(2))), // pedestal 2
                List.of(EntryIngredient.of(EntryStacks.of(recipe.value().getResult(null)))));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return GoldenHotelCategory.GOLDEN_HOTEL;
    }
}