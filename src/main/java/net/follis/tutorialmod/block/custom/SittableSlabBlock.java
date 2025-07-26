package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.ChairEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
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

public class SittableSlabBlock extends SlabBlock {
    public SittableSlabBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(!world.isClient()) {
            Entity entity;
            List<ChairEntity> entities = world.getEntitiesByType(ModEntities.CHAIR, new Box(pos), chair -> true);
            if(entities.isEmpty()) {
                entity = ModEntities.CHAIR.spawn((ServerWorld) world, pos, SpawnReason.TRIGGERED);
                boolean bl;

                SlabType slabType = state.get(TYPE);
                switch (slabType) {
                    case DOUBLE, TOP -> bl = true;
                    default -> bl = false;
                }
                if (entity != null)
                    positionEntity(entity, pos, bl);
            } else {
                entity = entities.getFirst();
            }
            player.startRiding(entity);
        }
        return ActionResult.SUCCESS;
    }

    private void positionEntity(Entity entity, BlockPos pos, Boolean isTopSlab) {
        double x = pos.getX() + 0.5;
        double y = isTopSlab? pos.getY() + 0.5: pos.getY();
        double z = pos.getZ() + 0.5;
        entity.refreshPositionAndAngles(x, y, z, entity.getYaw(), entity.getPitch());
    }
}
