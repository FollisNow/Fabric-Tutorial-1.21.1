package net.follis.tutorialmod.entity.misc;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlySwarmManager {
    private static final Map<ServerWorld, List<ActiveFlySwarm>> SWARMS = new HashMap<>();

    public static void spawnSwarm(ServerWorld world, Vec3d origin, Vec3d direction, UUID ownerId) {
        ActiveFlySwarm swarm = new ActiveFlySwarm(origin, direction, ownerId);
        SWARMS.computeIfAbsent(world, w -> new ArrayList<>()).add(swarm);

        fireFlyBurst(world, origin, direction);
    }

    private static final int FLY_COUNT = 128;
    private static final double FLY_SPEED = ActiveFlySwarm.SPEED;
    private static final double POSITION_SPREAD = ActiveFlySwarm.HIT_RADIUS; // blocks — width of the swarm cluster, constant along the whole flight

    private static void fireFlyBurst(ServerWorld world, Vec3d origin, Vec3d direction) {
        Vec3d up = Math.abs(direction.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d side = direction.crossProduct(up).normalize();
        Vec3d vertical = direction.crossProduct(side).normalize();
        Vec3d velocity = direction.multiply(FLY_SPEED);

        for (int i = 0; i < FLY_COUNT; i++) {
            double offsetSide = (world.random.nextDouble() - 0.5) * 2 * POSITION_SPREAD;
            double offsetVertical = (world.random.nextDouble() - 0.5) * 2 * POSITION_SPREAD;
            double offsetDepth = (world.random.nextDouble() - 0.5) * 2 * POSITION_SPREAD;
            Vec3d spawnPos = origin.add(side.multiply(offsetSide)).add(vertical.multiply(offsetVertical)).add(direction.normalize().multiply(offsetDepth));

            world.spawnParticles(ModParticles.FLY_PARTICLE,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    0, velocity.x, velocity.y, velocity.z, 1);
        }
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(FlySwarmManager::tickWorld);
    }

    private static void tickWorld(ServerWorld world) {
        List<ActiveFlySwarm> swarms = SWARMS.get(world);
        if (swarms == null || swarms.isEmpty()) return;

        swarms.removeIf(swarm -> {
            swarm.advance();
            tickSwarm(world, swarm);
            return swarm.isExpired();
        });
    }

    private static void tickSwarm(ServerWorld world, ActiveFlySwarm swarm) {
        Entity owner = world.getEntity(swarm.ownerId);
        DamageSource source = owner instanceof LivingEntity living
                ? world.getDamageSources().mobAttack(living)
                : world.getDamageSources().generic();

        Box box = Box.of(swarm.position, ActiveFlySwarm.HIT_RADIUS * 2, ActiveFlySwarm.HIT_RADIUS * 2, ActiveFlySwarm.HIT_RADIUS * 2);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                e -> !e.getUuid().equals(swarm.ownerId) && swarm.canHit(e))) {
            swarm.markHit(target);
            target.damage(source, ActiveFlySwarm.DAMAGE);
        }
    }
}
