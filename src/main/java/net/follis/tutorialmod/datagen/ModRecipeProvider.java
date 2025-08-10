package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.CampfireCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmokingRecipe;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        List<ItemConvertible> PINK_GARNET_SMELTABLES = List.of(ModItems.RAW_PINK_GARNET, ModBlocks.PINK_GARNET_ORE,
                ModBlocks.PINK_GARNET_DEEPSLATE_ORE);

        offerSmelting(exporter, PINK_GARNET_SMELTABLES, RecipeCategory.MISC, ModItems.PINK_GARNET, 0.25f, 200, "pink_garnet");
        offerBlasting(exporter, PINK_GARNET_SMELTABLES, RecipeCategory.MISC, ModItems.PINK_GARNET, 0.25f, 100, "pink_garnet");
        offerReversibleCompactingRecipes(exporter, RecipeCategory.BUILDING_BLOCKS, ModItems.PINK_GARNET, RecipeCategory.DECORATIONS, ModBlocks.PINK_GARNET_BLOCK);
        offerReversibleCompactingRecipes(exporter, RecipeCategory.BUILDING_BLOCKS, ModItems.RAW_PINK_GARNET, RecipeCategory.DECORATIONS, ModBlocks.RAW_PINK_GARNET_BLOCK);
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.RAW_PINK_GARNET, 32)
                .input(ModBlocks.MAGIC_BLOCK)
                .criterion(hasItem(ModBlocks.MAGIC_BLOCK), conditionsFromItem(ModBlocks.MAGIC_BLOCK))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "raw_pink_garnet_from_magic_block"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Blocks.COBWEB)
                .pattern("# #")
                .pattern(" # ")
                .pattern("# #")
                .input('#', Items.STRING)
                .criterion(hasItem(Items.STRING), conditionsFromItem(Items.STRING))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.STRING, 5)
                .input(Blocks.COBWEB)
                .criterion(hasItem(Blocks.COBWEB), conditionsFromItem(Blocks.COBWEB))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.BAMBOO_TRAP)
                .pattern("###")
                .pattern("#^#")
                .pattern("###")
                .input('#', Items.BAMBOO)
                .input('^', Items.IRON_NUGGET)
                .criterion(hasItem(Items.BAMBOO), conditionsFromItem(Items.BAMBOO))
                .offerTo(exporter);



        // LOCUSTS
        Ingredient locustIngredients = Ingredient.fromTag(ModTags.Items.LOCUST_ITEMS);
        List<ItemConvertible> locusts = List.of(ModItems.LOCUST_GOLD, ModItems.LOCUST_DREAM, ModItems.LOCUST_GRASSHOPPER, ModItems.LOCUST_RED);
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.SLIME_BALL, 1)
                .input(ModItems.LOCUST_DREAM)
                .criterion(hasItem(ModItems.LOCUST_DREAM), conditionsFromItem(ModItems.LOCUST_DREAM))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "slime_ball_from_locust_dream"));
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.SLIME_BALL, 1)
                .input(ModItems.LOCUST_GRASSHOPPER)
                .criterion(hasItem(ModItems.LOCUST_GRASSHOPPER), conditionsFromItem(ModItems.LOCUST_GRASSHOPPER))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "slime_ball_from_locust_grasshopper"));
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.RED_DYE, 1)
                .input(ModItems.LOCUST_RED)
                .criterion(hasItem(ModItems.LOCUST_RED), conditionsFromItem(ModItems.LOCUST_RED))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "red_dye_from_locust_red"));

        offerSmelting(exporter, locusts, RecipeCategory.FOOD, ModItems.GRILLED_LOCUST, 0.25f, 200, "food");
        CookingRecipeJsonBuilder locustSmoker = CookingRecipeJsonBuilder.create(
                        locustIngredients, RecipeCategory.FOOD, ModItems.GRILLED_LOCUST, 0.2f, 100, RecipeSerializer.SMOKING, SmokingRecipe::new)
                .criterion(hasItem(ModItems.LOCUST_GRASSHOPPER), conditionsFromItem(ModItems.LOCUST_GRASSHOPPER));
        locustSmoker.offerTo(exporter, getItemPath(ModItems.GRILLED_LOCUST) + "_from_" + "smoking");
        CookingRecipeJsonBuilder locustCampfire = CookingRecipeJsonBuilder.create(
                        locustIngredients, RecipeCategory.FOOD, ModItems.GRILLED_LOCUST, 0.2f, 100, RecipeSerializer.CAMPFIRE_COOKING, CampfireCookingRecipe::new)
                .criterion(hasItem(ModItems.LOCUST_GRASSHOPPER), conditionsFromItem(ModItems.LOCUST_GRASSHOPPER));
        locustCampfire.offerTo(exporter, getItemPath(ModItems.GRILLED_LOCUST) + "_from_" + "campfire_cooking");



        // GOLDEN WOOD
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_PLANKS, 4)
                .input(ModBlocks.GOLDEN_LOG)
                .criterion(hasItem(ModBlocks.GOLDEN_LOG), conditionsFromItem(ModBlocks.GOLDEN_LOG))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "golden_planks_from_golden_log"));
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_PLANKS, 4)
                .input(ModBlocks.GOLDEN_WOOD)
                .criterion(hasItem(ModBlocks.GOLDEN_WOOD), conditionsFromItem(ModBlocks.GOLDEN_WOOD))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "golden_planks_from_golden_wood"));
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_PLANKS, 4)
                .input(ModBlocks.STRIPPED_GOLDEN_LOG)
                .criterion(hasItem(ModBlocks.STRIPPED_GOLDEN_LOG), conditionsFromItem(ModBlocks.STRIPPED_GOLDEN_LOG))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "golden_planks_from_stripped_golden_log"));
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_PLANKS, 4)
                .input(ModBlocks.STRIPPED_GOLDEN_WOOD)
                .criterion(hasItem(ModBlocks.STRIPPED_GOLDEN_WOOD), conditionsFromItem(ModBlocks.STRIPPED_GOLDEN_WOOD))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "golden_planks_from_stripped_golden_wood"));

        offerBarkBlockRecipe(exporter, ModBlocks.STRIPPED_GOLDEN_WOOD, ModBlocks.STRIPPED_GOLDEN_LOG);
        offerBarkBlockRecipe(exporter, ModBlocks.GOLDEN_WOOD, ModBlocks.GOLDEN_LOG);

        offerStairsRecipe(exporter, ModBlocks.GOLDEN_STAIRS, ModBlocks.GOLDEN_PLANKS);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_SLAB, ModBlocks.GOLDEN_PLANKS);
        offerFenceRecipe(exporter, ModBlocks.GOLDEN_FENCE, ModBlocks.GOLDEN_PLANKS);
        offerFenceGateRecipe(exporter, ModBlocks.GOLDEN_FENCE_GATE, ModBlocks.GOLDEN_PLANKS);

        // GOLDEN
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICKS)
                .pattern(" # ")
                .pattern("#^#")
                .pattern(" # ")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.STONE_BRICKS)
                .criterion(hasItem(Items.GOLD_NUGGET), conditionsFromItem(Items.GOLD_NUGGET))
                .offerTo(exporter);
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_DIRT)
                .pattern(" # ")
                .pattern("#^#")
                .pattern(" # ")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.DIRT)
                .criterion(hasItem(Items.GOLD_NUGGET), conditionsFromItem(Items.GOLD_NUGGET))
                .offerTo(exporter);
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_SAND)
                .pattern(" # ")
                .pattern("#^#")
                .pattern(" # ")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.SAND)
                .criterion(hasItem(Items.GOLD_NUGGET), conditionsFromItem(Items.GOLD_NUGGET))
                .offerTo(exporter);
        offerStairsRecipe(exporter, ModBlocks.GOLDEN_BRICK_STAIRS, ModBlocks.GOLDEN_BRICKS);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICK_SLAB, ModBlocks.GOLDEN_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICK_STAIRS, ModBlocks.GOLDEN_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICK_SLAB, ModBlocks.GOLDEN_BRICKS, 2);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICK_WALL, ModBlocks.GOLDEN_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLDEN_BRICK_WALL, ModBlocks.GOLDEN_BRICKS);

        // GOLD BRICKS
        offer2x2CompactingRecipe(exporter,RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICKS, Items.GOLD_INGOT);
        offerStairsRecipe(exporter, ModBlocks.GOLD_BRICK_STAIRS, ModBlocks.GOLD_BRICKS);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICK_SLAB, ModBlocks.GOLD_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICK_STAIRS, ModBlocks.GOLD_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICK_SLAB, ModBlocks.GOLD_BRICKS, 2);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICK_WALL, ModBlocks.GOLD_BRICKS);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BRICK_WALL, ModBlocks.GOLD_BRICKS);

        // GOLD
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.COBBLED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CRUMBLED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_GOLD_BRICKS, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_GOLD, Items.GOLD_BLOCK, 4);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SHAPED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.ENGRAVED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCULPTED_GOLD, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_LARGE_BRICKS, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_STAIRS, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_SLAB, Items.GOLD_BLOCK, 2);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_BUTTON, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_FENCE, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_FENCE_GATE, Items.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_WALL, Items.GOLD_BLOCK);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_WALL, Items.GOLD_BLOCK);
        offerCutCopperRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_GOLD, Blocks.GOLD_BLOCK);
        offerStonecuttingRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_CHAIN, Items.GOLD_BLOCK, 8);
        offerStairsRecipe(exporter, ModBlocks.GOLD_STAIRS, Blocks.GOLD_BLOCK);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_SLAB, Blocks.GOLD_BLOCK);

        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_CHAIN)
                .pattern("#")
                .pattern("^")
                .pattern("#")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.GOLD_INGOT)
                .criterion(hasItem(Items.GOLD_INGOT), conditionsFromItem(Items.GOLD_INGOT))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GOLD_LANTERN)
                .pattern("###")
                .pattern("#^#")
                .pattern("###")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.TORCH)
                .criterion(hasItem(Items.GOLD_NUGGET), conditionsFromItem(Items.GOLD_NUGGET))
                .offerTo(exporter);

        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModItems.VISION_MONOCLE)
                .pattern(" # ")
                .pattern("#^#")
                .pattern(" # ")
                .input('#', Items.GOLD_NUGGET)
                .input('^', Items.AMETHYST_SHARD)
                .criterion(hasItem(Items.AMETHYST_SHARD), conditionsFromItem(Items.AMETHYST_SHARD))
                .offerTo(exporter);


        offerSmithingTrimRecipe(exporter, ModItems.KAUPEN_SMITHING_TEMPLATE, Identifier.of(TutorialMod.MOD_ID, "kaupen"));
    }

    public static void offerStairsRecipe(RecipeExporter exporter, ItemConvertible output, ItemConvertible input) {
        createStairsRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
    }
    public static void offerFenceRecipe(RecipeExporter exporter, ItemConvertible output, ItemConvertible input) {
        createFenceRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
    }
    public static void offerFenceGateRecipe(RecipeExporter exporter, ItemConvertible output, ItemConvertible input) {
        createFenceGateRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
    }
}
