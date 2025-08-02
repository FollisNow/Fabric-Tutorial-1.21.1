package net.follis.tutorialmod.world.gen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.follis.tutorialmod.entity.ModEntities;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public class ModEntitySpawns {
    public static void addSpawns() {
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.CHERRY_GROVE),
                SpawnGroup.CREATURE, ModEntities.MANTIS, 30, 1, 2);

        SpawnRestriction.register(ModEntities.MANTIS, SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);


        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.FLOWER_FOREST),
                SpawnGroup.CREATURE, ModEntities.BEETLE, 30, 2, 4);

        SpawnRestriction.register(ModEntities.BEETLE, SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
    }
}
