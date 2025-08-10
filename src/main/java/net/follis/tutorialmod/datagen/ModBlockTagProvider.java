package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.PINK_GARNET_BLOCK)
                .add(ModBlocks.RAW_PINK_GARNET_BLOCK)
                .add(ModBlocks.PINK_GARNET_ORE)
                .add(ModBlocks.PINK_GARNET_DEEPSLATE_ORE)
                .add(ModBlocks.MAGIC_BLOCK)
                .add(ModBlocks.GROWTH_CHAMBER)
                .add(ModBlocks.GOLDEN_PEDESTAL)
                .add(ModBlocks.GOLDEN_HOTEL)
                .add(ModBlocks.AMETHYST_BEE_HIVE)

                .add(ModBlocks.COBBLED_GOLD)
                .add(ModBlocks.CRUMBLED_GOLD)
                .add(ModBlocks.CHISELED_GOLD)
                .add(ModBlocks.CHISELED_GOLD_BRICKS)
                .add(ModBlocks.CUT_GOLD)
                .add(ModBlocks.SHAPED_GOLD)
                .add(ModBlocks.ENGRAVED_GOLD)
                .add(ModBlocks.SCULPTED_GOLD)
                .add(ModBlocks.GOLD_LARGE_BRICKS)
                .add(ModBlocks.GOLD_CHAIN)
                .add(ModBlocks.GOLD_LANTERN)

                .add(ModBlocks.GOLD_BRICKS)
                .add(ModBlocks.GOLD_BRICK_STAIRS)
                .add(ModBlocks.GOLD_BRICK_SLAB)
                .add(ModBlocks.GOLD_BRICK_WALL)

                .add(ModBlocks.GOLDEN_BRICKS)
                .add(ModBlocks.GOLDEN_BRICK_STAIRS)
                .add(ModBlocks.GOLDEN_BRICK_SLAB)
                .add(ModBlocks.GOLDEN_BRICK_WALL)

        ;

        getOrCreateTagBuilder(BlockTags.FENCES).add(ModBlocks.GOLD_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(ModBlocks.GOLD_FENCE_GATE);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.GOLD_WALL);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.GOLDEN_BRICK_WALL);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.GOLD_BRICK_WALL);


        getOrCreateTagBuilder(ModTags.Blocks.NEEDS_GOLDEN_TOOL)
                .addTag(BlockTags.NEEDS_DIAMOND_TOOL);


        getOrCreateTagBuilder(BlockTags.BEEHIVES)
                .add(ModBlocks.AMETHYST_BEE_HIVE);

        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.PINK_GARNET_DEEPSLATE_ORE);

        getOrCreateTagBuilder(BlockTags.FENCES).add(ModBlocks.PINK_GARNET_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(ModBlocks.PINK_GARNET_FENCE_GATE);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.PINK_GARNET_WALL);

        getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.MAGIC_BLOCK);

        getOrCreateTagBuilder(ModTags.Blocks.NEEDS_PINK_GARNET_TOOL)
                .add(ModBlocks.MAGIC_BLOCK)
                .addTag(BlockTags.NEEDS_IRON_TOOL);

        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(ModBlocks.GOLDEN_LOG)
                .add(ModBlocks.GOLDEN_WOOD)
                .add(ModBlocks.STRIPPED_GOLDEN_LOG)
                .add(ModBlocks.STRIPPED_GOLDEN_WOOD);

        getOrCreateTagBuilder(BlockTags.LEAVES)
                .add(ModBlocks.GOLDEN_LEAVES);
    }
}
