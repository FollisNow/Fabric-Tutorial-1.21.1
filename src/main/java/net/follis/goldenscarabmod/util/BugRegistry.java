package net.follis.goldenscarabmod.util;

import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.BeetleVariant;
import net.follis.goldenscarabmod.entity.custom.LarvaeVariant;
import net.follis.goldenscarabmod.entity.custom.LocustVariant;
import net.follis.goldenscarabmod.entity.custom.MantisVariant;
import net.follis.goldenscarabmod.entity.custom.MothVariant;
import net.follis.goldenscarabmod.entity.custom.ScorpionVariant;
import net.follis.goldenscarabmod.entity.custom.SpiderlingVariant;
import net.follis.goldenscarabmod.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Per-bug lookups keyed by {@link EntityType} (never by localized names).
 *
 * <p>Everything lives in {@link Holder}, which the JVM only initializes on first use, i.e. after
 * ModEntities / ModItems are registered. Replaces the old IBugVariants.BugColors / bugVariants / bugItems.
 */
public final class BugRegistry {
    private BugRegistry() {}

    /** Variant colors of a bug, or an empty list when it has none (scorpion, larvae, ...). */
    public static List<TextColor> colors(EntityType<?> type) {
        return Holder.COLORS.getOrDefault(type, List.of());
    }

    /** Variant enum class of a bug, or null if the type is not a bug. */
    @Nullable
    public static Class<?> variantClass(EntityType<?> type) {
        return Holder.VARIANTS.get(type);
    }

    /** The jar/spawn item of a bug, or null if the type is not a bug. */
    @Nullable
    public static Item item(EntityType<?> type) {
        return Holder.ITEMS.get(type);
    }

    /** All bug items (unmodifiable view). Safe to call from client init: registration is done by then. */
    public static Collection<Item> items() {
        return Holder.ITEMS.values();
    }

    public static boolean isBug(EntityType<?> type) {
        return Holder.ITEMS.containsKey(type);
    }

    private static final class Holder {
        static final Map<EntityType<?>, List<TextColor>> COLORS = Map.ofEntries(
                Map.entry(ModEntities.BEETLE, IBugVariants.BeetleColors),
                Map.entry(ModEntities.LOCUST, IBugVariants.LocustColors),
                Map.entry(ModEntities.MANTIS, IBugVariants.MantisColors),
                Map.entry(ModEntities.MOTH, IBugVariants.MothColors),
                Map.entry(ModEntities.SPIDERLING, IBugVariants.SpiderlingColors)
        );

        static final Map<EntityType<?>, Class<?>> VARIANTS = Map.ofEntries(
                Map.entry(ModEntities.BEETLE, BeetleVariant.class),
                Map.entry(ModEntities.LOCUST, LocustVariant.class),
                Map.entry(ModEntities.MANTIS, MantisVariant.class),
                Map.entry(ModEntities.MOTH, MothVariant.class),
                Map.entry(ModEntities.SPIDERLING, SpiderlingVariant.class),
                Map.entry(ModEntities.SCORPION, ScorpionVariant.class),
                Map.entry(ModEntities.LARVAE, LarvaeVariant.class)
        );

        static final Map<EntityType<?>, Item> ITEMS = Map.ofEntries(
                Map.entry(ModEntities.MOTH, ModItems.MOTH_ITEM),
                Map.entry(ModEntities.BEETLE, ModItems.BEETLE_ITEM),
                Map.entry(ModEntities.LOCUST, ModItems.LOCUST_ITEM),
                Map.entry(ModEntities.MANTIS, ModItems.MANTIS_ITEM),
                Map.entry(ModEntities.SPIDERLING, ModItems.SPIDERLING_ITEM),
                Map.entry(ModEntities.SCORPION, ModItems.SCORPION_ITEM),
                Map.entry(ModEntities.LARVAE, ModItems.LARVAE_ITEM)
        );
    }
}