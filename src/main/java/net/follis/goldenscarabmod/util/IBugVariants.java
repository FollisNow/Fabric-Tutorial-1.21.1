package net.follis.goldenscarabmod.util;

import net.follis.goldenscarabmod.enchantment.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Shared bug behaviour: variant id, variant colors and the gold / enchantment immunity checks.
 *
 * <p>This interface must NOT touch ModEntities / ModItems in its static fields: interface fields are
 * initialized when the first implementing class loads, which can happen before registration is done.
 * The entity-keyed lookups (colors, variant classes, items) live in {@link BugRegistry}, which is lazy.
 */
public interface IBugVariants {
    int getTypeVariant();

    TextColor darkGold = TextColor.fromRgb(0xBC7F04); // Custom gold-like color

    List<TextColor> BeetleColors = List.of(
            darkGold,
            TextColor.fromRgb(0x55FF55),
            TextColor.fromRgb(0xFF5555),
            TextColor.fromRgb(0x8A4A09),
            TextColor.fromRgb(0x301208)
    );

    List<TextColor> LocustColors = List.of(
            darkGold,
            TextColor.fromRgb(0x4DCF9F),
            TextColor.fromRgb(0x55FF55),
            TextColor.fromRgb(0xFF5555)
    );

    List<TextColor> MantisColors = List.of(
            TextColor.fromRgb(0x55FF55),
            TextColor.fromRgb(0xEFA7CD)
    );

    List<TextColor> MothColors = List.of(
            darkGold,                       // GOLD
            TextColor.fromRgb(0x917142),    // OAK
            TextColor.fromRgb(0xF5CC27),    // OAK_CRACKED
            TextColor.fromRgb(0xF0EEEB),    // BIRCH
            TextColor.fromRgb(0xBBEC3C),    // BIRCH_RADIANT
            TextColor.fromRgb(0x195157),    // BIRCH_NEGATIVE
            TextColor.fromRgb(0x2E1C0A),    // SPRUCE
            TextColor.fromRgb(0xFF1B1E),    // SPRUCE_RUBY
            TextColor.fromRgb(0x3F311D),    // DARK_OAK
            TextColor.fromRgb(0x004D18),    // DARK_OAK_EMERALD
            TextColor.fromRgb(0xFFC0FF),    // CHERRY
            TextColor.fromRgb(0xFF78FF),    // CHERRY_BLOOM
            TextColor.fromRgb(0x54B257),    // MANGROVE
            TextColor.fromRgb(0x5CD160),    // MANGROVE_TANGLED
            TextColor.fromRgb(0xA7AC1D),    // JUNGLE
            TextColor.fromRgb(0x006B00),    // JUNGLE_SPIDER
            TextColor.fromRgb(0x7B7368),    // ACACIA
            TextColor.fromRgb(0x0718FB),    // ACACIA_SAPPHIRE
            darkGold                        // EYE
    );

    List<TextColor> SpiderlingColors = List.of(
            TextColor.fromRgb(0x4a3321)
    );

    List<TextColor> ScorpionColors = List.of(
            TextColor.fromRgb(0xFFB921),  // GOLD
            TextColor.fromRgb(0xD94B3B),  // RED
            TextColor.fromRgb(0x6B6B7A),  // BLACK (lightened so it stays readable on the dark tooltip)
            TextColor.fromRgb(0x4B7BE5)   // BLUE
    );

    List<TextColor> BookskitoColors = List.of(
            darkGold,                        // GOLD
            TextColor.fromRgb(0x8B5A2B)      // REGULAR (brown)
    );

    /**
     * List equivalent of Map.getOrDefault: returns the color at `index`,
     * or `fallback` if the index is out of bounds (negative or unassigned variant id).
     */
    static TextColor getColorOrDefault(List<TextColor> colors, int index, TextColor fallback) {
        return (index >= 0 && index < colors.size()) ? colors.get(index) : fallback;
    }

    static TextColor getColorOrDefault(List<TextColor> colors, int index) {
        return getColorOrDefault(colors, index, TextColor.fromFormatting(Formatting.GRAY));
    }

    default boolean isWearingGoldOrImmune(LivingEntity entity) {
        return isWearingGold(entity) || hasImmunityEnchant(entity);
    }

    default boolean isWearingGold(LivingEntity entity) {
        for (ItemStack stack : entity.getAllArmorItems()) {
            if (stack.isIn(ModTags.Items.GOLDEN_ITEMS)) {
                return true;
            }
        }
        return false;
    }

    default boolean hasImmunityEnchant(LivingEntity entity) {
        for (RegistryEntry<Enchantment> enchant : entity.getEquippedStack(EquipmentSlot.HEAD).getEnchantments().getEnchantments()) {
            if (enchant.matchesKey(ModEnchantments.LORD_OF_THE_FLIES)) {
                return true;
            }
        }
        return false;
    }
}