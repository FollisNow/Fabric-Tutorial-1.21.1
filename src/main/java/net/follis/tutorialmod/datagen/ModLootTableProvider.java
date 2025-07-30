package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.custom.CauliflowerCropBlock;
import net.follis.tutorialmod.block.custom.HoneyBerryBushBlock;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        RegistryWrapper.Impl<Enchantment> impl = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

        addDrop(ModBlocks.PINK_GARNET_BLOCK);
        addDrop(ModBlocks.RAW_PINK_GARNET_BLOCK);
        addDrop(ModBlocks.MAGIC_BLOCK);

        addDrop(ModBlocks.GOLDEN_PEDESTAL);
        addDrop(ModBlocks.GOLDEN_HOTEL);
        addDrop(ModBlocks.GROWTH_CHAMBER);
        addDrop(ModBlocks.AMETHYST_BEE_HIVE);

        addDrop(ModBlocks.PINK_GARNET_ORE, oreDrops(ModBlocks.PINK_GARNET_ORE, ModItems.RAW_PINK_GARNET));
        addDrop(ModBlocks.PINK_GARNET_DEEPSLATE_ORE, multipleOreDrops(ModBlocks.PINK_GARNET_DEEPSLATE_ORE, ModItems.RAW_PINK_GARNET, 3, 7));

        addDrop(ModBlocks.PINK_GARNET_STAIRS);
        addDrop(ModBlocks.PINK_GARNET_SLAB, slabDrops(ModBlocks.PINK_GARNET_SLAB));

        addDrop(ModBlocks.PINK_GARNET_BUTTON);
        addDrop(ModBlocks.PINK_GARNET_PRESSURE_PLATE);

        addDrop(ModBlocks.PINK_GARNET_WALL);
        addDrop(ModBlocks.PINK_GARNET_FENCE);
        addDrop(ModBlocks.PINK_GARNET_FENCE_GATE);

        addDrop(ModBlocks.PINK_GARNET_DOOR, doorDrops(ModBlocks.PINK_GARNET_DOOR));
        addDrop(ModBlocks.PINK_GARNET_TRAPDOOR);

        BlockStatePropertyLootCondition.Builder builder2 = BlockStatePropertyLootCondition.builder(ModBlocks.CAULIFLOWER_CROP)
                .properties(StatePredicate.Builder.create().exactMatch(CauliflowerCropBlock.AGE, CauliflowerCropBlock.MAX_AGE));
        this.addDrop(ModBlocks.CAULIFLOWER_CROP, this.cropDrops(ModBlocks.CAULIFLOWER_CROP, ModItems.CAULIFLOWER, ModItems.CAULIFLOWER_SEEDS, builder2));

        this.addDrop(ModBlocks.HONEY_BERRY_BUSH,
                block -> this.applyExplosionDecay(
                        block,LootTable.builder().pool(LootPool.builder().conditionally(
                                                        BlockStatePropertyLootCondition.builder(ModBlocks.HONEY_BERRY_BUSH).properties(StatePredicate.Builder.create().exactMatch(HoneyBerryBushBlock.AGE, 3))
                                                )
                                                .with(ItemEntry.builder(ModItems.HONEY_BERRIES))
                                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0F, 3.0F)))
                                                .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))
                                ).pool(LootPool.builder().conditionally(
                                                        BlockStatePropertyLootCondition.builder(ModBlocks.HONEY_BERRY_BUSH).properties(StatePredicate.Builder.create().exactMatch(HoneyBerryBushBlock.AGE, 2))
                                                ).with(ItemEntry.builder(ModItems.HONEY_BERRIES))
                                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0F, 2.0F)))
                                                .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE))))));

        addDrop(ModBlocks.CRYSTAL_MUSHROOM);



        addDrop(ModBlocks.PINK_GARNET_END_ORE, multipleOreDrops(ModBlocks.PINK_GARNET_END_ORE, ModItems.RAW_PINK_GARNET, 4, 9));
        addDrop(ModBlocks.PINK_GARNET_NETHER_ORE, multipleOreDrops(ModBlocks.PINK_GARNET_NETHER_ORE, ModItems.RAW_PINK_GARNET, 3, 8));

        addDrop(ModBlocks.GOLDEN_DIRT);
        addDrop(ModBlocks.GOLDEN_SAND);
        addDrop(ModBlocks.GOLDEN_LOG);
        addDrop(ModBlocks.GOLDEN_WOOD);
        addDrop(ModBlocks.STRIPPED_GOLDEN_LOG);
        addDrop(ModBlocks.STRIPPED_GOLDEN_WOOD);
        addDrop(ModBlocks.GOLDEN_PLANKS);
        addDrop(ModBlocks.GOLDEN_SAPLING);
        addDrop(ModBlocks.GOLDEN_LEAVES, leavesDrops(ModBlocks.GOLDEN_LEAVES, ModBlocks.GOLDEN_SAPLING, 0.0625f));
        addDrop(ModBlocks.GOLD_BRICKS);
        addDrop(ModBlocks.COBBLED_GOLD);
        addDrop(ModBlocks.CRUMBLED_GOLD);
        addDrop(ModBlocks.CHISELED_GOLD);
        addDrop(ModBlocks.CHISELED_GOLD_BRICKS);
        addDrop(ModBlocks.CUT_GOLD);
        addDrop(ModBlocks.GOLDEN_BRICKS, Blocks.STONE_BRICKS);
        addDrop(ModBlocks.SHAPED_GOLD);
        addDrop(ModBlocks.ENGRAVED_GOLD);
        addDrop(ModBlocks.SCULPTED_GOLD);
        addDrop(ModBlocks.GOLD_LARGE_BRICKS);

        addDrop(ModBlocks.GOLD_STAIRS);
        addDrop(ModBlocks.GOLD_SLAB, slabDrops(ModBlocks.GOLD_SLAB));
        addDrop(ModBlocks.GOLD_BUTTON);
        addDrop(ModBlocks.GOLD_WALL);
        addDrop(ModBlocks.GOLD_FENCE);
        addDrop(ModBlocks.GOLD_FENCE_GATE);
        addDrop(ModBlocks.GOLD_CHAIN, ModItems.GOLD_CHAIN);

        addDrop(ModBlocks.GOLDEN_BRICK_STAIRS);
        addDrop(ModBlocks.GOLDEN_BRICK_SLAB, slabDrops(ModBlocks.GOLDEN_BRICK_SLAB));
        addDrop(ModBlocks.GOLDEN_BRICK_WALL);

        addDrop(ModBlocks.GOLD_BRICK_STAIRS);
        addDrop(ModBlocks.GOLD_BRICK_SLAB, slabDrops(ModBlocks.GOLD_BRICK_SLAB));
        addDrop(ModBlocks.GOLD_BRICK_WALL);

        addDrop(ModBlocks.GOLDEN_STAIRS);
        addDrop(ModBlocks.GOLDEN_SLAB, slabDrops(ModBlocks.GOLDEN_SLAB));
        addDrop(ModBlocks.GOLDEN_FENCE);
        addDrop(ModBlocks.GOLDEN_FENCE_GATE);


    }

    public LootTable.Builder multipleOreDrops(Block drop, Item item, float minDrops, float maxDrops) {
        RegistryWrapper.Impl<Enchantment> impl = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        return this.dropsWithSilkTouch(drop, this.applyExplosionDecay(drop, ((LeafEntry.Builder<?>)
                ItemEntry.builder(item).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrops, maxDrops))))
                .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE)))));
    }
}
