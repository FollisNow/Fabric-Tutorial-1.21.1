package net.follis.tutorialmod.util;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class DreamLocustEvent implements EntitySleepEvents.StopSleeping{
    @Override
    public void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
        if (entity instanceof PlayerEntity player && player.getInventory().contains(ModItems.LOCUST.getDefaultStack())) {
            LocustEntity locust = ModEntities.LOCUST.create(player.getWorld());
            if (locust != null)
                locust.refreshPositionAndAngles(sleepingPos.getX(), sleepingPos.getY(), sleepingPos.getZ(), entity.getYaw(), entity.getPitch());
            player.getWorld().spawnEntity(locust);
        }
    }
}
