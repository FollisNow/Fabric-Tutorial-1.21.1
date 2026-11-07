package net.follis.goldenscarabmod.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModLootTableModifiers {
    private static final Identifier GRASS_BLOCK_ID
            = Identifier.of("minecraft", "blocks/short_grass");
    private static final Identifier BLAZE_ID
            = Identifier.of("minecraft", "entities/blaze");


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

            // chitin on every arthropod table (vanilla, this mod, other mods); the tag is checked at drop time
            if (key.getValue().getPath().startsWith("entities/")) {
                LootPool.Builder chitin = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(EntityPropertiesLootCondition.builder(LootContext.EntityTarget.THIS,
                                EntityPredicate.Builder.create().type(EntityTypeTags.ARTHROPOD)))
                        .with(ItemEntry.builder(ModItems.CHITIN))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)).build());
                tableBuilder.pool(chitin.build());
            }
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
