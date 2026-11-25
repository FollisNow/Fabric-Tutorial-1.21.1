package net.follis.goldenscarabmod.item.custom;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.*;
import net.follis.goldenscarabmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.follis.goldenscarabmod.util.IBugVariants.*;

/** All tooltips for captured bugs, shared by BugItem (figurines) and AbstractEntityJarItem (jars). */
public final class BugTooltips {
    private BugTooltips() {}

    /** Full tooltip for a specimen. */
    public static void appendFigurine(List<Text> tooltip, ItemStack stack, @Nullable RegistryWrapper.WrapperLookup registries) {
        BugData bugData = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        if (bugData != null) append(tooltip, bugData, stack, false, registries);
    }

    /** One compact entry for a bug stored in a jar. */
    public static void appendJarEntry(List<Text> tooltip, BugData bugData, @Nullable RegistryWrapper.WrapperLookup registries) {
        append(tooltip, bugData, null, true, registries);
    }

    private static void append(List<Text> tooltip, BugData bugData, @Nullable ItemStack stack, boolean compact,
                               @Nullable RegistryWrapper.WrapperLookup registries) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(bugData.getIdentifier());
        NbtCompound nbt = bugData.getNbt(); // copies, so read it once
        int variantId = nbt.getInt("Variant");

        if (type == ModEntities.BEETLE) {
            beetle(tooltip, bugData, nbt, variantId, compact);
        } else if (type == ModEntities.MOTH) {
            // ...unchanged
        } else if (type == ModEntities.LOCUST) {
            // ...unchanged
        } else if (type == ModEntities.SPIDERLING) {
            spiderling(tooltip, bugData, nbt);
        } else if (type == ModEntities.LARVAE && stack != null) {
            larvae(tooltip, stack);
        } else if (type == ModEntities.SCORPION) {
            scorpion(tooltip, bugData, nbt, variantId, compact);
        } else if (type == ModEntities.GOLDEN_SILVERFISH) {
            tooltip.add(Text.translatable(bugData.convertToKey()).withColor(darkGold.getRgb()));
        } else if (type == ModEntities.BOOKSKITO) {
            bookskito(tooltip, bugData, nbt, variantId, compact, registries);
        } else {
            tooltip.add(Text.translatable(bugData.convertToKey()).formatted(Formatting.GRAY));
        }
    }
    // =====================================================================
    // Per-bug tooltips
    // =====================================================================

    private static void beetle(List<Text> tooltip, BugData bugData, NbtCompound nbt, int variantId, boolean compact) {
        Identifier geneId = Identifier.tryParse(nbt.getString("PotionGene"));
        StatusEffect effect = geneId == null ? null : Registries.STATUS_EFFECT.get(geneId);
        if (effect != null) {
            tooltip.add(Text.translatable(bugData.convertToKey()).withColor(BeetleColors.getFirst().getRgb())
                    .append(" ")
                    .append(Text.literal(Text.translatable(effect.getTranslationKey()).getString().toLowerCase())
                            .setStyle(Style.EMPTY)
                            .withColor(effect.getColor())));
            return;
        }
        TextColor color = getColorOrDefault(BeetleColors, variantId);
        MutableText line = Text.translatable(bugData.convertToKey()).withColor(color.getRgb());
        if (!compact) line.append(" ").append(Text.literal(BeetleVariant.byId(variantId).getName()));
        tooltip.add(line);
    }

    private static void spiderling(List<Text> tooltip, BugData bugData, NbtCompound nbt) {
        tooltip.add(Text.translatable(bugData.convertToKey())
                .append(" ")
                .append(stat("⇵", nbt.getFloat("GrowthSize"), SpiderlingEntity.MINIMUM_SIZE, SpiderlingEntity.MAXIMUM_SIZE, "  "))
                .append(stat("♥", nbt.getFloat("MaxHealth"), SpiderlingEntity.MINIMUM_HEALTH, SpiderlingEntity.MAXIMUM_HEALTH, "  "))
                .append(stat("»", nbt.getFloat("MovementSpeed"), SpiderlingEntity.MINIMUM_SPEED, SpiderlingEntity.MAXIMUM_SPEED, "  "))
                .append(stat("⏶", nbt.getFloat("JumpStrength"), SpiderlingEntity.MINIMUM_JUMP, SpiderlingEntity.MAXIMUM_JUMP, "")));
    }

    private static Text stat(String symbol, float value, float min, float max, String suffix) {
        return Text.literal(symbol + String.format("%.2f", value) + symbol + suffix)
                .withColor(interpolateColor(value, min, max));
    }

    private static void larvae(List<Text> tooltip, ItemStack stack) {
        CaddisflyCocoonItem.CocoonData data = stack.get(ModDataComponentTypes.COCOON);
        if (data == null) return;
        for (CocoonMaterial material : data.segments()) {
            if (material != null) {
                tooltip.add(Text.literal(material.asString()).withColor(material.getColor()));
            }
        }
    }

    private static void scorpion(List<Text> tooltip, BugData bugData, NbtCompound nbt, int variantId, boolean compact) {
        TextColor color = getColorOrDefault(ScorpionColors, variantId);
        MutableText line = Text.translatable(bugData.convertToKey()).withColor(color.getRgb())
                .append(" ").append(Text.literal(ScorpionVariant.byId(variantId).getName()));
        ScorpionShard shard = variantId == ScorpionVariant.GOLD.getId()
                ? ScorpionShard.byName(nbt.getString("Shard"))
                : ScorpionShard.NONE;
        Text shardName = Text.translatable(shard.getTooltipKey()).withColor(shard.getColor());

        if (compact) {
            // jar: everything on one line
            if (shard != ScorpionShard.NONE) line.append(" ").append(shardName);
            tooltip.add(line);
            return;
        }

        tooltip.add(line);
        if (shard == ScorpionShard.NONE) return;
        tooltip.add(shardName);
        if (Screen.hasShiftDown()) {
            tooltip.add(Text.translatable(shard.getTooltipKey() + ".desc", shard.getMinutes())
                    .formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.goldenscarabmod.shift_info"));
        }
    }

    private static final int COMPACT_ENCHANTS_SHOWN = 3;
    private static final int FULL_ENCHANTS_SHOWN = 8;

    private static void bookskito(List<Text> tooltip, BugData bugData, NbtCompound nbt, int variantId, boolean compact,
                                  @Nullable RegistryWrapper.WrapperLookup registries) {
        TextColor color = getColorOrDefault(BookskitoColors, variantId);
        MutableText line = Text.translatable(bugData.convertToKey()).withColor(color.getRgb())
                .append(" ").append(Text.literal(BookskitoVariant.byId(variantId).getName()));

        // only the gold (enchanted) kind can hold enchantments
        List<EnchantEntry> enchants = variantId == BookskitoVariant.GOLD.getId()
                ? readEnchantments(nbt, registries) : List.of();

        if (compact) {
            // jar: one line, the first few enchantments and a count of the rest
            int shown = Math.min(enchants.size(), COMPACT_ENCHANTS_SHOWN);
            for (int i = 0; i < shown; i++) {
                line.append(i == 0 ? " " : ", ").append(enchants.get(i).name());
            }
            if (enchants.size() > shown) {
                line.append(" ").append(Text.literal("+" + (enchants.size() - shown)).formatted(Formatting.DARK_GRAY));
            }
            tooltip.add(line);
            return;
        }

        tooltip.add(line);
        int shown = Math.min(enchants.size(), FULL_ENCHANTS_SHOWN);
        for (int i = 0; i < shown; i++) {
            tooltip.add(enchants.get(i).name());
        }
        if (enchants.size() > shown) {
            tooltip.add(Text.translatable("tooltip.goldenscarabmod.more", enchants.size() - shown)
                    .formatted(Formatting.DARK_GRAY));
        }
    }

    private record EnchantEntry(RegistryEntry<Enchantment> enchantment, int level) {
        Text name() {
            return Enchantment.getName(this.enchantment, this.level); // vanilla "Sharpness V", curses in red
        }
    }

    /**
     * The bookskito keeps its enchantments in hidden equipment stacks, which are part of the specimen's saved entity
     * data. This reads them back, highest level first, then by name.
     */
    private static List<EnchantEntry> readEnchantments(NbtCompound nbt, @Nullable RegistryWrapper.WrapperLookup registries) {
        if (registries == null) {
            MinecraftClient client = MinecraftClient.getInstance(); // some tooltips (e.g. recipe viewers) give no lookup
            if (client != null && client.world != null) {
                registries = client.world.getRegistryManager();
            }
        }
        if (registries == null) return List.of();

        Map<RegistryEntry<Enchantment>, Integer> merged = new HashMap<>();
        for (String key : new String[]{"HandItems", "ArmorItems"}) {
            NbtList stacks = nbt.getList(key, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = ItemStack.fromNbtOrEmpty(registries, stacks.getCompound(i));
                ItemEnchantmentsComponent held = stack.getOrDefault(
                        DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : held.getEnchantmentEntries()) {
                    merged.merge(entry.getKey(), entry.getIntValue(), Math::max);
                }
            }
        }

        List<EnchantEntry> result = new ArrayList<>();
        merged.forEach((enchantment, level) -> result.add(new EnchantEntry(enchantment, level)));
        result.sort(Comparator.comparingInt((EnchantEntry e) -> -e.level())
                .thenComparing(e -> e.name().getString()));
        return result;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Red (min) to green (max). */
    private static int interpolateColor(float value, float minValue, float maxValue) {
        float t = MathHelper.clamp((value - minValue) / (maxValue - minValue), 0F, 1F);
        int r = (int) (255 * (1 - t));
        int g = (int) (255 * t);
        return (r << 16) | (g << 8);
    }
}