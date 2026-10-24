package net.follis.goldenscarabmod.block.entity;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.types.Type;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.follis.goldenscarabmod.block.entity.custom.*;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Set;

import static net.follis.goldenscarabmod.GoldenScarabMod.LOGGER;

public class ModBlockEntities {
    public static final BlockEntityType<GoldenPedestalBlockEntity> GOLDEN_PEDESTAL_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "golden_pedestal_be"),
                    BlockEntityType.Builder.create(GoldenPedestalBlockEntity::new, ModBlocks.GOLDEN_PEDESTAL).build(null));

    public static final BlockEntityType<GoldenHotelBlockEntity> GOLDEN_HOTEL_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "golden_hotel_be"),
                    BlockEntityType.Builder.create(GoldenHotelBlockEntity::new, ModBlocks.GOLDEN_HOTEL).build(null));

    public static final BlockEntityType<GoldRepulsorBlockEntity> GOLD_REPULSOR_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "gold_repulsor_be"),
                    BlockEntityType.Builder.create(GoldRepulsorBlockEntity::new, ModBlocks.GOLD_REPULSOR).build(null));

    public static final BlockEntityType<TrappedSkullBlockEntity> TRAPPED_SKULL_BE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(GoldenScarabMod.MOD_ID, "trapped_skull"),
            BlockEntityType.Builder.create(TrappedSkullBlockEntity::new,
                    ModBlocks.TRAPPED_SKELETON_SKULL, ModBlocks.TRAPPED_SKELETON_WALL_SKULL,
                    ModBlocks.TRAPPED_ZOMBIE_HEAD, ModBlocks.TRAPPED_ZOMBIE_WALL_HEAD
                    /* , ...every other trapped block */).build(null));

    public static final BlockEntityType<CrownBlockEntity> CROWN_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "crown_block_be"),   // use your own mod-id constant here
                    BlockEntityType.Builder.create(CrownBlockEntity::new, ModBlocks.CROWN_BLOCK).build(null));

    public static final BlockEntityType<GrowthChamberBlockEntity> GROWTH_CHAMBER_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "growth_chamber_be"),
                    BlockEntityType.Builder.create(GrowthChamberBlockEntity::new, ModBlocks.GROWTH_CHAMBER).build(null));


    public static final BlockEntityType<AmethystBeeHiveBlockEntity> AMETHYST_BEE_HIVE_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(GoldenScarabMod.MOD_ID, "amethyst_bee_hive_be"),
                    BlockEntityType.Builder.create(AmethystBeeHiveBlockEntity::new, ModBlocks.AMETHYST_BEE_HIVE).build(null));


    public static void registerBlockEntities() {
        LOGGER.info("Registering Block Entities for " + GoldenScarabMod.MOD_ID);
    }

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, ModBlockEntities.Builder<T> builder) {
        if (builder.blocks.isEmpty()) {
            LOGGER.warn("Block entity type {} requires at least one valid block to be defined!", id);
        }

        Type<?> type = Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, builder.build(type));
    }
    public static final class Builder<T extends BlockEntity> implements net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType.Builder<T> {
        private final BlockEntityType.BlockEntityFactory<? extends T> factory;
        final Set<Block> blocks;

        private Builder(BlockEntityType.BlockEntityFactory<? extends T> factory, Set<Block> blocks) {
            this.factory = factory;
            this.blocks = blocks;
        }

        /**
         * {@return a new builder of a block entity type that supports {@code blocks}}
         */
        public static <T extends BlockEntity> ModBlockEntities.Builder<T> create(BlockEntityType.BlockEntityFactory<? extends T> factory, Block... blocks) {
            return new ModBlockEntities.Builder<>(factory, ImmutableSet.copyOf(blocks));
        }

        /**
         * Builds the block entity type.
         *
         * @return the built block entity type
         *
         * @param type the datafixer type of the block entity, or {@code null} if there is none
         */
        public BlockEntityType<T> build(Type<?> type) {
            return new BlockEntityType<>(this.factory, this.blocks, type);
        }
    }
}
