package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.component.ModDataComponentTypes;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.*;
import net.follis.goldenscarabmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.follis.goldenscarabmod.util.IBugVariants.*;

/** All tooltips for captured bugs, shared by BugItem (figurines) and AbstractEntityJarItem (jars). */
public final class BugTooltips {
    private BugTooltips() {}

    /** Full tooltip for a figurine. */
    public static void appendFigurine(List<Text> tooltip, ItemStack stack) {
        BugData bugData = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        if (bugData != null) append(tooltip, bugData, stack, false);
    }

    /** One compact entry for a bug stored in a jar. */
    public static void appendJarEntry(List<Text> tooltip, BugData bugData) {
        append(tooltip, bugData, null, true);
    }

    private static void append(List<Text> tooltip, BugData bugData, @Nullable ItemStack stack, boolean compact) {
        EntityType<?> type = Registries.ENTITY_TYPE.get(bugData.getIdentifier());
        NbtCompound nbt = bugData.getNbt(); // copies, so read it once
        int variantId = nbt.getInt("Variant");

        if (type == ModEntities.BEETLE) {
            beetle(tooltip, bugData, nbt, variantId, compact);
        } else if (type == ModEntities.MOTH) {
            TextColor color = getColorOrDefault(MothColors, variantId);
            tooltip.add(Text.translatable(bugData.convertToKey()).withColor(color.getRgb())
                    .append(" ").append(Text.literal(MothVariant.byId(variantId).getName())));
        } else if (type == ModEntities.LOCUST) {
            TextColor color = getColorOrDefault(LocustColors, variantId);
            MutableText line = Text.translatable(bugData.convertToKey()).withColor(color.getRgb());
            if (!compact) line.append(" ").append(Text.literal(LocustVariant.byId(variantId).getName()));
            tooltip.add(line);
        } else if (type == ModEntities.SPIDERLING) {
            spiderling(tooltip, bugData, nbt);
        } else if (type == ModEntities.LARVAE && stack != null) {
            larvae(tooltip, stack);
        } else if (type == ModEntities.SCORPION) {
            scorpion(tooltip, bugData, nbt, variantId, compact);
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