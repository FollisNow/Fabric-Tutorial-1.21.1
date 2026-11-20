package net.follis.goldenscarabmod.pattern;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * "Light swords from above": the swords leave the caster's body one after another, gather over the target, hang still
 * for a moment so the target can dodge, then all fall together.
 */
public final class GoldenSwordsPattern {
    public static final int SWORD_COUNT = 5;
    /** Ticks between two swords leaving the body. */
    public static final int COLLECT_INTERVAL = 6;
    /** Ticks from the cast until the swords fall: 60 ticks are 3 seconds. */
    public static final int TOTAL_TICKS = 60;
    /** How high the swords hover above the top of the target's head. */
    public static final double HOVER_HEIGHT = 4.0;
    /** Radius of the ring the swords hover in. */
    public static final double RING_RADIUS = 1.0;

    private static final int AFTER_FALL_TICKS = 80;  // life left once they fall: long enough to hit the ground and fade
    private static final float DAMAGE = 3.0F;        // per sword; five swords landing together make 15

    private static final List<Rain> ACTIVE = new ArrayList<>();
    private static boolean registered;

    private GoldenSwordsPattern() {}

    /** Call once from the mod initializer, next to SwordShield.register() and SwordFlock.register(). */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (ACTIVE.isEmpty()) return;
            Iterator<Rain> it = ACTIVE.iterator();
            while (it.hasNext()) {
                Rain rain = it.next();
                if (rain.world == world && !rain.tick()) it.remove();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ACTIVE.clear());
    }

    /** Ticks from the cast until the swords fall. */
    public static int fallTick() {
        return TOTAL_TICKS;
    }

    /** Swords that appear over the target by themselves (no body to leave). */
    public static void summon(ServerWorld world, LivingEntity caster, LivingEntity target) {
        start(world, caster, target, e -> true, null, SWORD_COUNT, COLLECT_INTERVAL);
    }

    public static void summon(ServerWorld world, LivingEntity caster, LivingEntity target, int swordCount, int collectInterval) {
        start(world, caster, target, e -> true, null, swordCount, collectInterval);
    }

    /**
     * With a pool, the swords leave the caster's body one by one; without one they appear over the target one by one.
     * Returns the tick, counted from the cast, at which the swords fall.
     */
    public static int summon(ServerWorld world, LivingEntity caster, LivingEntity target,
                             Predicate<LivingEntity> targets, @Nullable SwordPool pool) {
        return start(world, caster, target, targets, pool, SWORD_COUNT, COLLECT_INTERVAL);
    }

    private static int start(ServerWorld world, LivingEntity caster, LivingEntity target, Predicate<LivingEntity> targets,
                             @Nullable SwordPool pool, int count, int interval) {
        int total = Math.max(TOTAL_TICKS, (count - 1) * interval + 30);
        ACTIVE.add(new Rain(world, caster.getUuid(), target.getUuid(), targets, pool, count, interval, total));
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.0F, 1.5F);
        return total;
    }

    /** One cast in progress: releases a sword every few ticks until all have left. */
    private static final class Rain {
        final ServerWorld world;
        final UUID casterId, targetId;
        final Predicate<LivingEntity> targets;
        @Nullable final SwordPool pool;
        final int count, interval, total;
        int age;
        int released;

        Rain(ServerWorld world, UUID casterId, UUID targetId, Predicate<LivingEntity> targets, @Nullable SwordPool pool,
             int count, int interval, int total) {
            this.world = world;
            this.casterId = casterId;
            this.targetId = targetId;
            this.targets = targets;
            this.pool = pool;
            this.count = count;
            this.interval = interval;
            this.total = total;
        }

        /** Returns false once every sword has left, or the caster or the target is gone. */
        boolean tick() {
            if (!(world.getEntity(casterId) instanceof LivingEntity caster) || !caster.isAlive()) return false;
            if (!(world.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) return false;

            while (released < count && age >= released * interval) {
                release(caster, target, released);
                released++;
            }
            age++;
            return released < count;
        }

        private void release(LivingEntity caster, LivingEntity target, int i) {
            double angle = 2.0 * Math.PI * i / count;
            Vec3d offset = new Vec3d(Math.cos(angle) * RING_RADIUS, HOVER_HEIGHT, Math.sin(angle) * RING_RADIUS);

            Vec3d from = null;
            if (pool != null) {
                from = pool.takeSwordPosition();   // the sword that is last on the body right now
                if (from == null) return;          // none left: this one does not come
            }

            // every sword falls at the same moment, whenever it left
            int fallAt = Math.max(1, total - age);
            FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
            // a cooldown of 1000: each target is hit once
            sword.configure(caster, targets, DAMAGE, 1000, i,
                    from != null ? FlyingSwordEntity.ALREADY_VISIBLE : 0,
                    fallAt + AFTER_FALL_TICKS, 0);
            sword.startRain(target, offset, fallAt, from);
            world.spawnEntity(sword);

            // a rising chime, one note per sword
            world.playSound(null, sword.getX(), sword.getY(), sword.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.HOSTILE, 1.2F, 0.8F + 0.12F * i);
        }
    }
}