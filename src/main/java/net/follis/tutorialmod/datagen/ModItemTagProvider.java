package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.PINK_GARNET)
                .add(ModItems.RAW_PINK_GARNET)
                .add(Items.COAL)
                .add(Items.STICK)
                .add(Items.APPLE);

        getOrCreateTagBuilder(ItemTags.SWORDS)
                .add(ModItems.PINK_GARNET_SWORD)
                .add(ModItems.GOLDEN_NEEDLE)
                .add(ModItems.WOODEN_MACUAHUITL)
                .add(ModItems.GOLDEN_MACUAHUITL)
                .add(ModItems.GOLDEN_KNIFE);
        getOrCreateTagBuilder(ItemTags.PICKAXES)
                .add(ModItems.PINK_GARNET_PICKAXE);
        getOrCreateTagBuilder(ItemTags.SHOVELS)
                .add(ModItems.PINK_GARNET_SHOVEL);
        getOrCreateTagBuilder(ItemTags.AXES)
                .add(ModItems.PINK_GARNET_AXE);
        getOrCreateTagBuilder(ItemTags.HOES)
                .add(ModItems.PINK_GARNET_HOE);

        getOrCreateTagBuilder(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.PINK_GARNET_HELMET)
                .add(ModItems.PINK_GARNET_CHESTPLATE)
                .add(ModItems.PINK_GARNET_LEGGINGS)
                .add(ModItems.PINK_GARNET_BOOTS);

        getOrCreateTagBuilder(ItemTags.TRIM_MATERIALS)
                .add(ModItems.PINK_GARNET);

        getOrCreateTagBuilder(ItemTags.TRIM_TEMPLATES)
                .add(ModItems.KAUPEN_SMITHING_TEMPLATE);

        getOrCreateTagBuilder(ItemTags.LOGS_THAT_BURN)
                .add(ModBlocks.GOLDEN_LOG.asItem())
                .add(ModBlocks.GOLDEN_WOOD.asItem())
                .add(ModBlocks.STRIPPED_GOLDEN_LOG.asItem())
                .add(ModBlocks.STRIPPED_GOLDEN_WOOD.asItem());

        getOrCreateTagBuilder(ItemTags.PLANKS)
                .add(ModBlocks.GOLDEN_PLANKS.asItem());

        getOrCreateTagBuilder(ModTags.Items.GOLD_BLOCKS)
                .add(Blocks.GOLD_BLOCK.asItem())
                .add(ModBlocks.GOLD_STAIRS.asItem())
                .add(ModBlocks.GOLD_SLAB.asItem())
                .add(ModBlocks.GOLD_BUTTON.asItem())
                .add(ModBlocks.GOLD_FENCE.asItem())
                .add(ModBlocks.GOLD_FENCE_GATE.asItem())
                .add(ModBlocks.GOLD_WALL.asItem())

                .add(ModBlocks.GOLDEN_BRICKS.asItem())
                .add(ModBlocks.GOLDEN_BRICK_STAIRS.asItem())
                .add(ModBlocks.GOLDEN_BRICK_SLAB.asItem())
                .add(ModBlocks.GOLDEN_BRICK_WALL.asItem())

                .add(ModBlocks.GOLD_BRICKS.asItem())
                .add(ModBlocks.GOLD_BRICK_STAIRS.asItem())
                .add(ModBlocks.GOLD_BRICK_SLAB.asItem())
                .add(ModBlocks.GOLD_BRICK_WALL.asItem())

                .add(ModBlocks.COBBLED_GOLD.asItem())
                .add(ModBlocks.COBBLED_GOLD_STAIRS.asItem())
                .add(ModBlocks.COBBLED_GOLD_SLAB.asItem())
                .add(ModBlocks.COBBLED_GOLD_WALL.asItem())

                .add(ModBlocks.CUT_GOLD.asItem())
                .add(ModBlocks.CUT_GOLD_STAIRS.asItem())
                .add(ModBlocks.CUT_GOLD_SLAB.asItem())
                .add(ModBlocks.CUT_GOLD_WALL.asItem())

                .add(ModBlocks.GOLD_LARGE_BRICKS.asItem())
                .add(ModBlocks.GOLD_LARGE_BRICK_STAIRS.asItem())
                .add(ModBlocks.GOLD_LARGE_BRICK_SLAB.asItem())
                .add(ModBlocks.GOLD_LARGE_BRICK_WALL.asItem())

                .add(ModBlocks.CRUMBLED_GOLD.asItem())
                .add(ModBlocks.CHISELED_GOLD.asItem())
                .add(ModBlocks.CHISELED_GOLD_BRICKS.asItem())
                .add(ModBlocks.SHAPED_GOLD.asItem())
                .add(ModBlocks.ENGRAVED_GOLD.asItem())
                .add(ModBlocks.SCULPTED_GOLD.asItem());

        getOrCreateTagBuilder(ModTags.Items.GOLDEN_ITEMS)
                .add(Items.GOLDEN_HELMET)
                .add(Items.GOLDEN_CHESTPLATE)
                .add(Items.GOLDEN_LEGGINGS)
                .add(Items.GOLDEN_BOOTS);

        getOrCreateTagBuilder(ModTags.Items.MACUAHUITL)
                .add(ModItems.WOODEN_MACUAHUITL)
                .add(ModItems.GOLDEN_MACUAHUITL);

        getOrCreateTagBuilder(ModTags.Items.LOCUST_ITEMS)
                .add(ModItems.LOCUST_GOLD)
                .add(ModItems.LOCUST_DREAM)
                .add(ModItems.LOCUST_GRASSHOPPER)
                .add(ModItems.LOCUST_RED);

        getOrCreateTagBuilder(ItemTags.MEAT)
                .addTag(ModTags.Items.LOCUST_ITEMS)
                .add(ModItems.GRILLED_LOCUST);

        getOrCreateTagBuilder(ModTags.Items.GOLDEN_VEGETAL_FOOD)
                .add(ModItems.GOLDEN_SWEET_BERRIES)
                .add(ModItems.GOLDEN_GLOW_BERRIES)
                .add(Items.GOLDEN_APPLE)
                .add(Items.GOLDEN_CARROT)
                .add(Items.GLISTERING_MELON_SLICE);

        getOrCreateTagBuilder(ModTags.Items.GOLDEN_FOOD)
                .addTag(ModTags.Items.GOLDEN_VEGETAL_FOOD)
                .add(ModItems.GOLDEN_SPIDER_EYE);


    }
}
