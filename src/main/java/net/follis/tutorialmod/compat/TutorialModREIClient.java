package net.follis.tutorialmod.compat;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.recipe.GoldenHotelRecipe;
import net.follis.tutorialmod.recipe.GrowthChamberRecipe;
import net.follis.tutorialmod.recipe.ModRecipes;
import net.follis.tutorialmod.screen.custom.GrowthChamberScreen;

public class TutorialModREIClient implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new GrowthChamberCategory());
        registry.add(new GoldenHotelCategory());

        registry.addWorkstations(GrowthChamberCategory.GROWTH_CHAMBER, EntryStacks.of(ModBlocks.GROWTH_CHAMBER));
        registry.addWorkstations(GoldenHotelCategory.GOLDEN_HOTEL, EntryStacks.of(ModBlocks.GOLDEN_HOTEL));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(GrowthChamberRecipe.class, ModRecipes.GROWTH_CHAMBER_TYPE,
                GrowthChamberDisplay::new);
        registry.registerRecipeFiller(GoldenHotelRecipe.class, ModRecipes.GOLDEN_HOTEL_TYPE,
                GoldenHotelDisplay::new);
    }
    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerClickArea(screen -> new Rectangle(((screen.width - 176) / 2) + 78,
                ((screen.height - 166) / 2) + 30, 20, 25), GrowthChamberScreen.class,
                GrowthChamberCategory.GROWTH_CHAMBER);
    }
}
