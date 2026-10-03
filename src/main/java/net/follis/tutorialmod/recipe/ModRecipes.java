package net.follis.tutorialmod.recipe;

import net.follis.tutorialmod.TutorialMod;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {
    public static final RecipeSerializer<GrowthChamberRecipe> GROWTH_CHAMBER_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER, Identifier.of(TutorialMod.MOD_ID, "growth_chamber"),
                    new GrowthChamberRecipe.Serializer());
    public static final RecipeType<GrowthChamberRecipe> GROWTH_CHAMBER_TYPE = Registry.register(
            Registries.RECIPE_TYPE, Identifier.of(TutorialMod.MOD_ID, "growth_chamber"), new RecipeType<GrowthChamberRecipe>() {
                @Override
                public String toString() {
                    return "growth_chamber";
                }
            });

    public static final RecipeSerializer<GoldenHotelRecipe> GOLDEN_HOTEL_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER, Identifier.of(TutorialMod.MOD_ID, "golden_hotel"),
            new GoldenHotelRecipe.Serializer());
    public static final RecipeType<GoldenHotelRecipe> GOLDEN_HOTEL_TYPE = Registry.register(
            Registries.RECIPE_TYPE, Identifier.of(TutorialMod.MOD_ID, "golden_hotel"), new RecipeType<GoldenHotelRecipe>() {
                @Override
                public String toString() {
                    return "golden_hotel";
                }
            });

    public static final RecipeSerializer<CaddisflyFluteRecipe> CADDISFLY_FLUTE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER, Identifier.of(TutorialMod.MOD_ID, "crafting_special_caddisfly_flute"),
            new SpecialRecipeSerializer<>(CaddisflyFluteRecipe::new));

    public static final RecipeSerializer<GoldenHotelRevivalRecipe> GOLDEN_HOTEL_REVIVAL_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER, Identifier.of(TutorialMod.MOD_ID, "golden_hotel_revival"),
            new GoldenHotelRevivalRecipe.Serializer());
    public static final RecipeType<GoldenHotelRevivalRecipe> GOLDEN_HOTEL_REVIVAL_TYPE = Registry.register(
            Registries.RECIPE_TYPE, Identifier.of(TutorialMod.MOD_ID, "golden_hotel_revival"), new RecipeType<GoldenHotelRevivalRecipe>() {
                @Override
                public String toString() {
                    return "golden_hotel_revival";
                }
            });

    public static void registerRecipes() {
        TutorialMod.LOGGER.info("Registering Custom Recipes for " + TutorialMod.MOD_ID);
    }
}
