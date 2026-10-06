package net.follis.tutorialmod.entity.misc;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class ActiveFlySwarm {
    public Vec3d position;
    public final Vec3d direction;
    public final UUID ownerId;
    public int age = 0;

    public static final int MAX_AGE = 80;
    public static final double SPEED = 0.175;
    public static final double HIT_RADIUS = 1.6;
    public static final float DAMAGE = 1.0F;
    public static final int HIT_COOLDOWN = 5; // ticks between repeat hits on the same target

    private final Map<UUID, Integer> hitCooldowns = new HashMap<>();

    public ActiveFlySwarm(Vec3d origin, Vec3d direction, UUID ownerId) {
        this.position = origin;
        this.direction = direction.normalize();
        this.ownerId = ownerId;
    }

    public void advance() {
        this.position = this.position.add(this.direction.multiply(SPEED));
        this.age++;
        hitCooldowns.replaceAll((uuid, ticks) -> ticks - 1);
    }

    public boolean isExpired() {
        return this.age >= MAX_AGE;
    }

    public boolean canHit(LivingEntity entity) {
        return hitCooldowns.getOrDefault(entity.getUuid(), 0) <= 0;
    }

    public void markHit(LivingEntity entity) {
        hitCooldowns.put(entity.getUuid(), HIT_COOLDOWN);
    }
}
