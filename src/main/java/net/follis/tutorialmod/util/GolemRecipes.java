package net.follis.tutorialmod.util;

import net.follis.tutorialmod.block.IMakeGolems;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.entity.ModEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;

import java.util.Map;

public final class GolemRecipes {
    private GolemRecipes() {}

    private static Map<IMakeGolems.GolemBlockPair, EntityType<?>> goldGolemMap;

    public static Map<IMakeGolems.GolemBlockPair, EntityType<?>> goldGolemMap() {
        if (goldGolemMap == null) {
            goldGolemMap = Map.of(
                    new IMakeGolems.GolemBlockPair(ModBlocks.GOLDEN_PEDESTAL, ModBlocks.ENGRAVED_GOLD), ModEntities.GOLD_PANNER,
                    new IMakeGolems.GolemBlockPair(ModBlocks.ENGRAVED_GOLD, Blocks.GOLD_BLOCK), ModEntities.GOLD_CARVER
            );
        }
        return goldGolemMap;
    }
}
