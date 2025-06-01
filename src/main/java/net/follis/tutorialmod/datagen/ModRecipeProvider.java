package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.*;
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
        List<ItemConvertible> PINK_GARNET_SMELTABLES = List.of(
                ModItems.RAW_PINK_GARNET,
                ModBlocks.PINK_GARNET_ORE,
                ModBlocks.PINK_GARNET_DEEPSLATE_ORE);

        offerSmelting(exporter, PINK_GARNET_SMELTABLES, RecipeCategory.MISC, ModItems.PINK_GARNET, 0.25f, 200, "pink_garnet");
        offerBlasting(exporter, PINK_GARNET_SMELTABLES, RecipeCategory.MISC, ModItems.PINK_GARNET, 0.25f, 100, "pink_garnet");

        offerReversibleCompactingRecipes(exporter, RecipeCategory.BUILDING_BLOCKS, ModItems.PINK_GARNET, RecipeCategory.DECORATIONS, ModBlocks.PINK_GARNET_BLOCK);

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.RAW_PINK_GARNET_BLOCK)
                .pattern("RRR")
                .pattern("RRR")
                .pattern("RRR")
                .input('R', ModItems.RAW_PINK_GARNET)
                .criterion(hasItem(ModItems.RAW_PINK_GARNET), conditionsFromItem(ModItems.RAW_PINK_GARNET))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.RAW_PINK_GARNET, 9)
                .input(ModBlocks.RAW_PINK_GARNET_BLOCK)
                .criterion(hasItem(ModBlocks.RAW_PINK_GARNET_BLOCK), conditionsFromItem(ModBlocks.RAW_PINK_GARNET_BLOCK))
                .offerTo(exporter);

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.RAW_PINK_GARNET, 32)
                .input(ModBlocks.MAGIC_BLOCK)
                .criterion(hasItem(ModBlocks.RAW_PINK_GARNET_BLOCK), conditionsFromItem(ModBlocks.RAW_PINK_GARNET_BLOCK))
                .offerTo(exporter, Identifier.of(TutorialMod.MOD_ID, "raw_pink_garnet_from_magic_block"));

        createBasicToolRecipe(ModItems.PINK_GARNET_SWORD, ModItems.PINK_GARNET, exporter);
        createBasicToolRecipe(ModItems.PINK_GARNET_PICKAXE, ModItems.PINK_GARNET, exporter);
        createBasicToolRecipe(ModItems.PINK_GARNET_SHOVEL, ModItems.PINK_GARNET, exporter);
        createBasicToolRecipe(ModItems.PINK_GARNET_AXE, ModItems.PINK_GARNET, exporter);
        createBasicToolRecipe(ModItems.PINK_GARNET_HOE, ModItems.PINK_GARNET, exporter);

    }

    // Sub method for tool patterns
    void createBasicToolRecipe(Item tool, Item material, RecipeExporter exporter) {
        ShapedRecipeJsonBuilder recipeBuilder = null;

        if (tool instanceof SwordItem){
            recipeBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, tool)
                    .pattern(" R ")
                    .pattern(" R ")
                    .pattern(" S ")
                    .input('R', material)
                    .input('S', Items.STICK)
                    .criterion(hasItem(material), conditionsFromItem(material));
        }
        if (tool instanceof PickaxeItem){
            recipeBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, tool)
                    .pattern("RRR")
                    .pattern(" S ")
                    .pattern(" S ")
                    .input('R', material)
                    .input('S', Items.STICK)
                    .criterion(hasItem(material), conditionsFromItem(material));
        }
        if (tool instanceof ShovelItem){
            recipeBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, tool)
                    .pattern(" R ")
                    .pattern(" S ")
                    .pattern(" S ")
                    .input('R', material)
                    .input('S', Items.STICK)
                    .criterion(hasItem(material), conditionsFromItem(material));
        }
        if (tool instanceof AxeItem){
            recipeBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, tool)
                    .pattern(" RR")
                    .pattern(" SR")
                    .pattern(" S ")
                    .input('R', material)
                    .input('S', Items.STICK)
                    .criterion(hasItem(material), conditionsFromItem(material));
        }
        if (tool instanceof HoeItem){
            recipeBuilder = ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, tool)
                    .pattern(" RR")
                    .pattern(" S ")
                    .pattern(" S ")
                    .input('R', material)
                    .input('S', Items.STICK)
                    .criterion(hasItem(material), conditionsFromItem(material));

        }
        if (recipeBuilder != null) {
            recipeBuilder.offerTo(exporter);
        }
    }
}
