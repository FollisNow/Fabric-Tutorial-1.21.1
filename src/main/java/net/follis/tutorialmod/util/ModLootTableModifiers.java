package net.follis.tutorialmod.util;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.Identifier;

public class ModLootTableModifiers {
    private static final Identifier GRASS_BLOCK_ID
            = Identifier.of("minecraft", "blocks/short_grass");

    private static final Identifier BEE_ID
            = Identifier.of("minecraft", "entities/bee");
    private static final Identifier ENDERMITE_ID
            = Identifier.of("minecraft", "entities/endermite");
    private static final Identifier SILVERFISH_ID
            = Identifier.of("minecraft", "entities/silverfish");
    private static final Identifier SPIDER_ID
            = Identifier.of("minecraft", "entities/spider");
    private static final Identifier CAVE_SPIDER_ID
            = Identifier.of("minecraft", "entities/cave_spider");


    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) -> {
            if(GRASS_BLOCK_ID.equals(key.getValue())) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.25f)) // Drops 25% of the time
                        .with(ItemEntry.builder(ModItems.CAULIFLOWER_SEEDS))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());

                tableBuilder.pool(poolBuilder.build());
            }

            AddChitinLootTo(BEE_ID, key, tableBuilder);
            AddChitinLootTo(ENDERMITE_ID, key, tableBuilder);
            AddChitinLootTo(SILVERFISH_ID, key, tableBuilder);
            AddChitinLootTo(SPIDER_ID, key, tableBuilder);
            AddChitinLootTo(CAVE_SPIDER_ID, key, tableBuilder);


            if(LootTables.IGLOO_CHEST_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(1.0f)) // Drops 100% of the time
                        .with(ItemEntry.builder(ModItems.CHISEL))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());

                tableBuilder.pool(poolBuilder.build());
            }

            if(LootTables.SHIPWRECK_TREASURE_CHEST.equals(key) || LootTables.BURIED_TREASURE_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.5f)) // Drops 50% of the time
                        .with(ItemEntry.builder(ModBlocks.GOLDEN_SAPLING))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());

                tableBuilder.pool(poolBuilder.build());
            }
        });
    }

    private static void AddChitinLootTo(Identifier identifier, RegistryKey<LootTable> key, LootTable.Builder tableBuilder) {

        if (identifier.equals(key.getValue())){
            LootPool.Builder poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(1.0f))
                    .with(ItemEntry.builder(ModItems.CHITIN))
                    .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());
            tableBuilder.pool(poolBuilder.build());
        }
    }
}
