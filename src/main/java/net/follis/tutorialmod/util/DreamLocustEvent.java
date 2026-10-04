package net.follis.tutorialmod.util;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.follis.tutorialmod.component.ModAttachments;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.follis.tutorialmod.entity.custom.LocustVariant;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DreamLocustEvent implements EntitySleepEvents.StopSleeping {
    @Override
    public void onStopSleeping(LivingEntity entity, BlockPos sleepingPos) {
        if (!(entity instanceof PlayerEntity player)) return;
        if (player.getWorld().isClient) return;

        Boolean marked = player.getAttachedOrElse(ModAttachments.KILLED_LOCUST, false);
        if (!marked) return;

        player.removeAttached(ModAttachments.KILLED_LOCUST);

        World world = player.getWorld();
        for (int i = 0; i < 3; i++) {
            LocustEntity locust = ModEntities.LOCUST.create(world);
            if (locust != null) {
                locust.setVariant(LocustVariant.DREAM);
                locust.refreshPositionAndAngles(sleepingPos.getX() + 0.5, sleepingPos.getY() + 0.5, sleepingPos.getZ() + 0.5, entity.getYaw(), entity.getPitch());
                world.spawnEntity(locust);
            }
        }
    }
}
