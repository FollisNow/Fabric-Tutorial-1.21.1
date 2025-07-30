package net.follis.tutorialmod.block.entity;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.entity.custom.AmethystBeeHiveBlockEntity;
import net.follis.tutorialmod.block.entity.custom.GrowthChamberBlockEntity;
import net.follis.tutorialmod.block.entity.custom.GoldenPedestalBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<GoldenPedestalBlockEntity> GOLDEN_PEDESTAL_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(TutorialMod.MOD_ID, "golden_pedestal_be"),
                    BlockEntityType.Builder.create(GoldenPedestalBlockEntity::new, ModBlocks.GOLDEN_PEDESTAL).build(null));

    public static final BlockEntityType<GrowthChamberBlockEntity> GROWTH_CHAMBER_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(TutorialMod.MOD_ID, "growth_chamber_be"),
                    BlockEntityType.Builder.create(GrowthChamberBlockEntity::new, ModBlocks.GROWTH_CHAMBER).build(null));


    public static final BlockEntityType<AmethystBeeHiveBlockEntity> AMETHYST_BEE_HIVE_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(TutorialMod.MOD_ID, "amethyst_bee_hive_be"),
                    BlockEntityType.Builder.create(AmethystBeeHiveBlockEntity::new, ModBlocks.AMETHYST_BEE_HIVE).build(null));


    public static void registerBlockEntities() {
        TutorialMod.LOGGER.info("Registering Block Entities for " + TutorialMod.MOD_ID);
    }
}
