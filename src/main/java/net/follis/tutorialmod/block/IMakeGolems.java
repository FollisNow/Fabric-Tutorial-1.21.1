package net.follis.tutorialmod.block;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public interface IMakeGolems {

    default void trySpawnEntity(World world, BlockPos pos, Map<Block, EntityType<?>> GOLEM_MAP) {
        GOLEM_MAP.forEach((block, entityType) -> {
            BlockPattern.Result result = this.getCorrespondingPattern(block).searchAround(world, pos);
            if (result != null) {
                Entity entity = entityType.create(world);
                if (entity != null) {
                    spawnEntity(world, result, entity, result.translate(1, 2, 0).getBlockPos());
                }
            }
        });
    }

    private static void spawnEntity(World world, BlockPattern.Result patternResult, Entity entity, BlockPos pos) {
        for(int i = 0; i < patternResult.getWidth(); ++i) {
            for(int j = 0; j < patternResult.getHeight(); ++j) {
                CachedBlockPosition cachedBlockPosition = patternResult.translate(i, j, 0);
                world.setBlockState(cachedBlockPosition.getBlockPos(), Blocks.AIR.getDefaultState(), 2);
                world.syncWorldEvent(2001, cachedBlockPosition.getBlockPos(), Block.getRawIdFromState(cachedBlockPosition.getBlockState()));
            }
        }
        entity.refreshPositionAndAngles((double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.05, (double)pos.getZ() + (double)0.5F, 0.0F, 0.0F);
        world.spawnEntity(entity);

        for(ServerPlayerEntity serverPlayerEntity : world.getNonSpectatingEntities(ServerPlayerEntity.class, entity.getBoundingBox().expand(5.0F))) {
            Criteria.SUMMONED_ENTITY.trigger(serverPlayerEntity, entity);
        }

        for(int i = 0; i < patternResult.getWidth(); ++i) {
            for(int j = 0; j < patternResult.getHeight(); ++j) {
                CachedBlockPosition cachedBlockPosition = patternResult.translate(i, j, 0);
                world.updateNeighbors(cachedBlockPosition.getBlockPos(), Blocks.AIR);
            }
        }    }

    default BlockPattern getCorrespondingPattern(Block block) {
        return BlockPatternBuilder.start().aisle(
                "~^~",
                        "###",
                        "~#~").
                where('^', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(ModBlocks.MAGIC_BLOCK))).
                where('#', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(block))).
                where('~', (pos) -> pos.getBlockState().isAir()).build();
    }
}
