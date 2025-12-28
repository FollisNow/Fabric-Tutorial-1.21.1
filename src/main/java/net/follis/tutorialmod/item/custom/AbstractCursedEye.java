package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractCursedEye extends BowItem {

    public AbstractCursedEye(Settings settings) {
        super(settings);
    }



    protected static LivingEntity pseudoRaycast(ServerPlayerEntity serverPlayer) {
        HitResult hit = serverPlayer.raycast(128, 0, false);
        double max_distance = hit.getType() == HitResult.Type.BLOCK ?
                hit.getPos().subtract(serverPlayer.getPos()).length() : 128;

        Vec3d playerView = serverPlayer.getRotationVec(1.0F).normalize();

        if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {

            // Sample along the view direction
            Vec3d currentPos = new Vec3d(serverPlayer.getX(), serverPlayer.getEyeY(), serverPlayer.getZ());
            double stepSize = 0.5; // Distance to move in each sample
            for (double distance = 0; distance <= max_distance; distance += stepSize) {

                currentPos = currentPos.add(playerView.multiply(stepSize));

                LivingEntity hitEntity = getLivingEntity(serverPlayer, serverWorld, currentPos, stepSize);

                if (hitEntity != null)
                    return hitEntity;
            }
        }
        return null;
    }

    protected static LivingEntity getLivingEntity(ServerPlayerEntity serverPlayer, ServerWorld serverWorld, Vec3d currentPos, double stepSize) {
        Predicate<Entity> predicate = entity -> entity instanceof LivingEntity && entity.isAlive();
        List<Entity> entityList = serverWorld.getOtherEntities(
                serverPlayer,
                new Box(currentPos, currentPos.add(stepSize, stepSize, stepSize)),
                predicate
        );

        return entityList.stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .findFirst()
                .orElse(null);
    }

    protected boolean tryToCurse(ServerPlayerEntity serverPlayer) {
        LivingEntity entityHit = pseudoRaycast(serverPlayer);
        if (entityHit != null) {
            entityHit.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 10));
            return true;
        }
        return false;
    }

    protected abstract void applyEffects(LivingEntity entityHit);

}
