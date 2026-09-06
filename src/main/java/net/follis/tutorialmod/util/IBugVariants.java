package net.follis.tutorialmod.util;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.*;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

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
            TextColor.fromRgb(0x917142),
            TextColor.fromRgb(0xF5CC27),
            TextColor.fromRgb(0xF0EEEB),
            TextColor.fromRgb(0xBBEC3C),
            TextColor.fromRgb(0x195157),
            TextColor.fromRgb(0x2E1C0A),
            TextColor.fromRgb(0xFF1B1E),
            TextColor.fromRgb(0x3F311D),
            TextColor.fromRgb(0x004D18),
            TextColor.fromRgb(0xFFC0FF),
            TextColor.fromRgb(0xFF78FF),
            TextColor.fromRgb(0x54B257),
            TextColor.fromRgb(0x5CD160),
            TextColor.fromRgb(0xA7AC1D),
            TextColor.fromRgb(0x006B00),
            TextColor.fromRgb(0x7B7368),
            TextColor.fromRgb(0x0718FB),
            darkGold
    );

    List<TextColor> SpiderlingColors = List.of(
            TextColor.fromRgb(0x4a3321)
    );


    Map<String, List<TextColor>> BugColors = Map.of(
            ModEntities.BEETLE.getName().getString(), BeetleColors,
            ModEntities.LOCUST.getName().getString(), LocustColors,
            ModEntities.MANTIS.getName().getString(), MantisColors,
            ModEntities.MOTH.getName().getString(), MothColors,
            ModEntities.SPIDERLING.getName().getString(), SpiderlingColors
    );

    Map<String, Class<?>> bugVariants = Map.of(
            ModEntities.BEETLE.getName().getString(), BeetleVariant.class,
            ModEntities.LOCUST.getName().getString(), LocustVariant.class,
            ModEntities.MANTIS.getName().getString(), MantisVariant.class,
            ModEntities.MOTH.getName().getString(), MothVariant.class,
            ModEntities.SPIDERLING.getName().getString(), SpiderlingVariant.class,
            ModEntities.SCORPION.getName().getString(), ScorpionVariant.class
    );

    /**
     * List equivalent of Map.getOrDefault — returns the color at `index`,
     * or `fallback` if the index is out of bounds (negative or unassigned variant id).
     */
    static TextColor getColorOrDefault(List<TextColor> colors, int index, TextColor fallback) {
        return (index >= 0 && index < colors.size()) ? colors.get(index) : fallback;
    }

    static TextColor getColorOrDefault(List<TextColor> colors, int index) {
        return getColorOrDefault(colors, index, TextColor.fromFormatting(Formatting.GRAY));
    }
}