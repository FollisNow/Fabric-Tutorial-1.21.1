package net.follis.tutorialmod.util;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.*;
import net.minecraft.text.TextColor;

import java.util.Map;

public interface IBugVariants {
    int getTypeVariant();

    TextColor darkGold = TextColor.fromRgb(0xBC7F04); // Custom gold-like color

    Map<Integer, TextColor> BeetleColors = Map.of(
            0, darkGold,
            1, TextColor.fromRgb(0x55FF55),
            2, TextColor.fromRgb(0xFF5555),
            3, TextColor.fromRgb(0x8A4A09),
            4, TextColor.fromRgb(0x301208)
    );
    Map<Integer, TextColor> LocustColors = Map.of(
            0, darkGold,
            1, TextColor.fromRgb(0x4DCF9F),
            2, TextColor.fromRgb(0x55FF55),
            3, TextColor.fromRgb(0xFF5555)
    );

    Map<Integer, TextColor> MantisColors = Map.of(
            0, TextColor.fromRgb(0x55FF55),
            1, TextColor.fromRgb(0xEFA7CD)
    );

    Map<Integer, TextColor> MothColors = Map.of(
            0, darkGold,
            1, TextColor.fromRgb(0x4DCF9F),
            2, TextColor.fromRgb(0x55FF55),
            3, TextColor.fromRgb(0xFF5555),
            4, TextColor.fromRgb(0x5F4A2B),
            5, TextColor.fromRgb(0xFF5555),
            6, TextColor.fromRgb(0x55FF55),
            7, TextColor.fromRgb(0xFF5555),
            8, TextColor.fromRgb(0x55FF55),
            9, TextColor.fromRgb(0xFF5555)
    );

    Map<Integer, TextColor> SpiderlingColors = Map.of(
            0, TextColor.fromRgb(0x4a3321)
    );


    Map<String, Map<Integer, TextColor>> BugColors = Map.of(
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
            ModEntities.SPIDERLING.getName().getString(), SpiderlingVariant.class
    );
}
