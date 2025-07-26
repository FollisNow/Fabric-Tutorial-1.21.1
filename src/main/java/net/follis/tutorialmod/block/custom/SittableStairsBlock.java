package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.ChairEntity;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class SittableStairsBlock extends StairsBlock {
    public SittableStairsBlock(BlockState baseBlockState, AbstractBlock.Settings settings) {
        super(baseBlockState, settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(!world.isClient()) {
            Entity entity;
            List<ChairEntity> entities = world.getEntitiesByType(ModEntities.CHAIR, new Box(pos), chair -> true);
            if(entities.isEmpty()) {
                entity = ModEntities.CHAIR.spawn((ServerWorld) world, pos, SpawnReason.TRIGGERED);
            } else {
                entity = entities.getFirst();
            }
            player.startRiding(entity);
        }
        return ActionResult.SUCCESS;
    }
}
