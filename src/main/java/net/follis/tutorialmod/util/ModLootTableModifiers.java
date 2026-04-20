package net.follis.tutorialmod.util;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModLootTableModifiers {
    private static final Identifier GRASS_BLOCK_ID
            = Identifier.of("minecraft", "blocks/short_grass");
    private static final Identifier BEE_ID
            = Identifier.of("minecraft", "entities/bee");
    private static final Identifier BLAZE_ID
            = Identifier.of("minecraft", "entities/blaze");
    private static final Identifier ENDERMITE_ID
            = Identifier.of("minecraft", "entities/endermite");
    private static final Identifier SILVERFISH_ID
            = Identifier.of("minecraft", "entities/silverfish");
    private static final Identifier SPIDER_ID
            = Identifier.of("minecraft", "entities/spider");
    private static final Identifier CAVE_SPIDER_ID
            = Identifier.of("minecraft", "entities/cave_spider");
    public static final Identifier AMETHYST_BEE_ID =
            Identifier.of(TutorialMod.MOD_ID, "amethyst_bee");
    public static final Identifier BEETLE_ID =
            Identifier.of(TutorialMod.MOD_ID, "beetle");
    public static final Identifier SPIDERLING_ID =
            Identifier.of(TutorialMod.MOD_ID, "spiderling");
    public static final Identifier LOCUST_ID =
            Identifier.of(TutorialMod.MOD_ID, "locust");
    public static final Identifier MANTIS_ID =
            Identifier.of(TutorialMod.MOD_ID, "mantis");
    public static final Identifier MOTH_ID =
            Identifier.of(TutorialMod.MOD_ID, "moth");
    public static final Identifier SCORPION_ID =
            Identifier.of(TutorialMod.MOD_ID, "scorpion");
    public static final Identifier GOLDEN_SILVERFISH_ID =
            Identifier.of(TutorialMod.MOD_ID, "golden_silverfish");

    private static final List<Identifier> ARTHROPODS = List.of(
            BEE_ID,
            BLAZE_ID,
            ENDERMITE_ID,
            SILVERFISH_ID,
            SPIDER_ID,
            CAVE_SPIDER_ID,

            AMETHYST_BEE_ID,
            BEETLE_ID,
            SPIDERLING_ID,
            LOCUST_ID,
            MANTIS_ID,
            MOTH_ID,
            SCORPION_ID,
            GOLDEN_SILVERFISH_ID
    );

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

            AddItemLootTo(ARTHROPODS, key, tableBuilder, ModItems.CHITIN);
            AddItemLootTo(GOLDEN_SILVERFISH_ID, key, tableBuilder, Items.GOLD_NUGGET);
        });
    }

    private static void AddItemLootTo(List<Identifier> identifiers, RegistryKey<LootTable> key, LootTable.Builder tableBuilder, Item item) {
        for (Identifier identifier: identifiers) {
            if (identifier.equals(key.getValue())){
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(1.0f))
                        .with(ItemEntry.builder(item))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());
                tableBuilder.pool(poolBuilder.build());
            }
        }
    }
    private static void AddItemLootTo(Identifier identifier, RegistryKey<LootTable> key, LootTable.Builder tableBuilder, Item item) {
        if (identifier.equals(key.getValue())){
            LootPool.Builder poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(1.0f))
                    .with(ItemEntry.builder(item))
                    .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());
            tableBuilder.pool(poolBuilder.build());
        }
    }
}
