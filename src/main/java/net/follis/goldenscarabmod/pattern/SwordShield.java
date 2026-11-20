package net.follis.goldenscarabmod.pattern;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** The sword shield: a gyroscope of swords around the boss, and an invisible sphere that sends projectiles back. */
public final class SwordShield {
    // ---- tuning ----
    public static final int SWORDS_PER_RING = 8;
    /** The rings: tilt and yaw of their plane (radians), and how fast the swords turn (radians per tick, the sign sets the direction). */
    private static final double[][] RINGS = {
            {0.0, 0.0, 0.11},
            {1.05, 0.0, -0.09},
            {1.05, Math.PI / 2.0, 0.10}
    };
    private static final double MIN_RADIUS = 3.0;      // the rings are at least this far from the boss's centre
    private static final double MARGIN = 0.4;          // the deflecting sphere lies this far outside the swords
    private static final double DEFLECT_SPEED = 0.9;   // a deflected projectile keeps this share of its speed
    private static final int APPEAR_STEP = 1;          // ticks between two swords appearing
    private static final int ACTIVE_FROM = 8;          // the sphere works from this tick on, while the swords are still appearing
    private static final int GATHER_TICKS = 20;   // how long a sword takes to glide from the body to its ring
    private static boolean registered;
    private static final List<Shield> ACTIVE = new ArrayList<>();

    private SwordShield() {}

    /** Call once from the mod initializer. */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (ACTIVE.isEmpty()) return;
            Iterator<Shield> it = ACTIVE.iterator();
            while (it.hasNext()) {
                Shield shield = it.next();
                if (shield.world == world && !shield.tick()) it.remove();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ACTIVE.clear());
    }

    /**
     * Raises a shield around the boss for durationTicks. deflects chooses which projectiles are turned away;
     * the boss's own projectiles and the swords themselves are never touched. Returns the duration.
     */
    public static int start(ServerWorld world, LivingEntity boss, int durationTicks, Predicate<ProjectileEntity> deflects) {
        return start(world, boss, durationTicks, deflects, null);
    }

    /** With a pool, part of the swords leave the boss's body; the rest appear in place. */
    public static int start(ServerWorld world, LivingEntity boss, int durationTicks, Predicate<ProjectileEntity> deflects,
                            @Nullable SwordPool pool) {
        stop(boss);

        double radius = Math.max(MIN_RADIUS, boss.getWidth() * 0.5 + 1.8);
        double maxTilt = 0.0;
        for (double[] ring : RINGS) maxTilt = Math.max(maxTilt, ring[0]);
        // high enough that the tilted rings do not dip into the floor
        double centerHeight = pool != null ? pool.bodyCenterHeight()
                : Math.max(boss.getHeight() * 0.5, radius * Math.sin(maxTilt) + 0.3);

        int total = RINGS.length * SWORDS_PER_RING;
        int taken = pool == null ? 0 : Math.min(pool.swordsAvailable(), total);
        Shield shield = new Shield(world, boss.getUuid(), radius, centerHeight, durationTicks, deflects,
                taken > 0 ? GATHER_TICKS : ACTIVE_FROM);   // the sphere works once the swords have arrived

        int slot = 0;
        for (int r = 0; r < RINGS.length; r++) {
            double[] ring = RINGS[r];
            for (int i = 0; i < SWORDS_PER_RING; i++) {
                Vec3d from = pool != null && SwordPool.detaches(slot, total, taken) ? pool.takeSwordPosition() : null;

                FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
                // these swords never launch and never hurt: launchAt is never reached and nobody is a valid target
                sword.configure(boss, e -> false, 0.0F, 20, slot,
                        from != null ? FlyingSwordEntity.ALREADY_VISIBLE : slot * APPEAR_STEP,
                        durationTicks, Integer.MAX_VALUE);
                sword.startOrbit(radius, centerHeight, 2.0 * Math.PI * i / SWORDS_PER_RING + r * 0.5, ring[2], ring[0], ring[1]);
                if (from != null) sword.gatherFrom(from, GATHER_TICKS);
                world.spawnEntity(sword);
                shield.swords.add(sword.getUuid());
                slot++;
            }
        }
        ACTIVE.add(shield);
        world.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.HOSTILE, 1.5F, 1.6F);
        return durationTicks;
    }
    /** Takes the shield down early. */
    public static void stop(LivingEntity boss) {
        Iterator<Shield> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Shield shield = it.next();
            if (shield.bossId.equals(boss.getUuid())) {
                shield.end();
                it.remove();
            }
        }
    }

    private static final class Shield {
        final ServerWorld world;
        final UUID bossId;
        final double radius;
        final double centerHeight;
        final int duration;
        final Predicate<ProjectileEntity> deflects;
        final List<UUID> swords = new ArrayList<>();
        final int activeFrom;
        int age;

        Shield(ServerWorld world, UUID bossId, double radius, double centerHeight, int duration,
               Predicate<ProjectileEntity> deflects, int activeFrom) {
            this.world = world;
            this.bossId = bossId;
            this.radius = radius;
            this.centerHeight = centerHeight;
            this.duration = duration;
            this.deflects = deflects;
            this.activeFrom = activeFrom;
        }

        /** Returns false once the shield is over. */
        boolean tick() {
            if (!(world.getEntity(bossId) instanceof LivingEntity boss) || !boss.isAlive()) {
                end();
                return false;
            }
            age++;
            if (age >= duration) {
                end();
                return false;
            }
            if (age < activeFrom) return true;   // was: if (age < ACTIVE_FROM) return true;
            
            Vec3d center = boss.getPos().add(0, centerHeight, 0);
            double surface = radius + MARGIN;
            Box area = new Box(center, center).expand(surface + 10.0);
            for (ProjectileEntity p : world.getEntitiesByClass(ProjectileEntity.class, area, p -> canDeflect(p, boss))) {
                deflect(boss, p, center, surface);
            }
            return true;
        }

        private boolean canDeflect(ProjectileEntity p, LivingEntity boss) {
            return !(p instanceof FlyingSwordEntity)
                    && !(p instanceof FishingBobberEntity)
                    && p.getOwner() != boss && deflects.test(p);
        }

        /**
         * Runs at the start of the tick, so p is where it was last tick and is about to move by its velocity.
         * If that move would take it through the surface from outside, it bounces off the surface instead.
         */
        private void deflect(LivingEntity boss, ProjectileEntity p, Vec3d center, double surface) {
            Vec3d velocity = p.getVelocity();
            double a = velocity.lengthSquared();
            if (a < 1.0E-4) return;

            Vec3d from = p.getPos();
            Vec3d d = from.subtract(center);
            double c = d.lengthSquared() - surface * surface;
            if (c <= 0.0) return;   // already inside: it was not caught on its way in

            double b = 2.0 * d.dotProduct(velocity);
            double discriminant = b * b - 4.0 * a * c;
            if (discriminant < 0.0) return;   // its path misses the sphere

            double t = (-b - Math.sqrt(discriminant)) / (2.0 * a);   // where along this tick's move it reaches the surface
            if (t < 0.0 || t > 1.0) return;

            Vec3d contact = from.add(velocity.multiply(t));
            Vec3d normal = contact.subtract(center).normalize();
            p.setPosition(contact.add(normal.multiply(0.05)));
            p.setVelocity(velocity.subtract(normal.multiply(2.0 * velocity.dotProduct(normal))).multiply(DEFLECT_SPEED));
            p.velocityModified = true;   // tells the clients about the new direction
            p.setOwner(boss);            // so a deflected arrow can hurt whoever shot it

            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, contact.x, contact.y, contact.z, 10, 0.2, 0.2, 0.2, 0.3);
            world.spawnParticles(ParticleTypes.END_ROD, contact.x, contact.y, contact.z, 4, 0.1, 0.1, 0.1, 0.05);
            world.playSound(null, contact.x, contact.y, contact.z, SoundEvents.ITEM_SHIELD_BLOCK,
                    SoundCategory.HOSTILE, 1.0F, 1.5F);
        }

        void end() {
            for (UUID id : swords) {
                if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.retire();
            }
        }
    }
}