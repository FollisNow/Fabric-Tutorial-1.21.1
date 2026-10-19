package net.follis.goldenscarabmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.item.ModItems;
import net.follis.goldenscarabmod.item.custom.AbstractEntityJarItem;
import net.follis.goldenscarabmod.loot.ModLootTables;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetComponentsLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ModChestLootTableProvider extends SimpleFabricLootTableProvider {
    public ModChestLootTableProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup, LootContextTypes.CHEST);
    }

    @Override
    public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> consumer) {

        // Everyday chests: a few small things, sometimes very little
        consumer.accept(ModLootTables.TEMPLE_COMMON, LootTable.builder()
                .pool(LootPool.builder()                                   // rubbish: always one
                        .rolls(UniformLootNumberProvider.create(3, 5))
                        .with(item(Items.STRING, 5, 1, 4))
                        .with(item(Items.POISONOUS_POTATO, 3, 1, 3))
                        .with(item(Items.WHEAT_SEEDS, 3, 1, 3))
                        .with(item(Items.MELON_SEEDS, 2, 1, 3))
                        .with(item(Items.PUMPKIN_SEEDS, 2, 1, 3))
                        .with(item(Items.TORCHFLOWER_SEEDS, 2, 1, 2))
                        .with(item(Items.GLISTERING_MELON_SLICE, 2, 1, 2))
                        .with(item(ModItems.CHITIN, 2, 1, 2))
                        .with(item(ModItems.GOLDEN_SWEET_BERRIES, 2, 1, 3)))
                .pool(LootPool.builder()                                   // good: always one
                        .rolls(UniformLootNumberProvider.create(1, 2))
                        .with(item(ModItems.GRILLED_LOCUST, 4, 2, 5))
                        .with(item(Items.GOLD_NUGGET, 4, 2, 8))
                        .with(item(ModItems.GOLDEN_CARAT, 2, 1, 2))
                        .with(item(ModItems.CADDISFLY_COCOON, 2, 1, 1)))
                .pool(LootPool.builder()                                   // excellent: only sometimes
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.15f))
                        .with(item(ModBlocks.GOLDEN_SAPLING.asItem(), 1, 1, 1))));

        // Treasure rooms: fewer rolls, better items
        consumer.accept(ModLootTables.TEMPLE_RARE, LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(1, 3))
                        .with(item(Items.GOLD_NUGGET, 8, 3, 12))
                        .with(item(Items.GOLD_INGOT, 4, 3, 8))
                        .with(item(Items.AMETHYST_SHARD, 3, 2, 4))
                        .with(item(Items.DIAMOND, 2, 1, 2))
                        .with(item(Items.RAW_GOLD_BLOCK, 2, 1, 1))
                        .with(item(ModItems.LOCUST_GOLD, 1, 1, 2))));


        // Super-rare reward: one guaranteed good item, plus a chance of a bonus
        consumer.accept(ModLootTables.TEMPLE_SUPER_RARE, LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(item(ModItems.PINK_GARNET_HAMMER, 3, 1, 1))
                        .with(item(ModItems.PINK_GARNET_HELMET, 3, 1, 1))
                        .with(item(ModItems.KAUPEN_SMITHING_TEMPLATE, 2, 1, 1))
                        .with(item(Items.DIAMOND, 4, 2, 4)))
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.35f))
                        .with(item(ModItems.PINK_GARNET, 1, 4, 8))));


        // Ultra-rare reward: one guaranteed good item, plus a chance of a bonus
        consumer.accept(ModLootTables.TEMPLE_ULTRA_RARE, LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(item(ModItems.PINK_GARNET_HAMMER, 3, 1, 1))
                        .with(item(ModItems.PINK_GARNET_HELMET, 3, 1, 1))
                        .with(item(ModItems.KAUPEN_SMITHING_TEMPLATE, 2, 1, 1))
                        .with(item(Items.DIAMOND, 4, 2, 4)))
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.35f))
                        .with(item(ModItems.PINK_GARNET, 1, 4, 8))));


        // Miniboss reward: one guaranteed good item, plus a chance of a bonus
        consumer.accept(ModLootTables.TEMPLE_MINIBOSS, LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(item(ModItems.GOLDEN_MACUAHUITL, 3, 1, 1))
                        .with(item(ModItems.DART_SHOOTER, 3, 1, 1))
                        .with(item(ModItems.GOLDEN_NEEDLE, 2, 1, 1)))
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.35f))
                        .with(item(ModItems.GOLDEN_KNIFE, 10, 1, 1))
                        .with(item(ModItems.CURSED_JAR, 5, 1, 1))
                        .with(filledJar(ModItems.BUG_JAR, 10, bug("beetle", 0)))
                        .with(filledJar(ModItems.BUG_JAR, 10, bug("moth", 14)))
                        .with(filledJar(ModItems.BUG_JAR, 10, bug("scorpion", 0)))
                        .with(filledJar(ModItems.BUG_JAR, 10, bug("locust", 0)))));
    }

    /** One weighted item entry with a count range. */
    private static LootPoolEntry.Builder<?> item(ItemConvertible item, int weight, int min, int max) {
        return ItemEntry.builder(item).weight(weight)
                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)));
    }
    /** A jar that already contains the given bugs. */
    private static LootPoolEntry.Builder<?> filledJar(ItemConvertible jar, int weight, AbstractEntityJarItem.BugData... bugs) {
        return ItemEntry.builder(jar).weight(weight)
                .apply(SetComponentsLootFunction.builder(ModDataComponentTypes.BUGS, List.of(bugs)));
    }

    /** One bug of the given type and variant. */
    private static AbstractEntityJarItem.BugData bug(String entity, int variant) {
        return AbstractEntityJarItem.BugData.ofDefault(Identifier.of(GoldenScarabMod.MOD_ID, entity), variant);
    }
}