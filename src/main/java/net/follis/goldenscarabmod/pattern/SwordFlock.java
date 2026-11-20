package net.follis.goldenscarabmod.pattern;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** The semi-permanent flock of swords that circles the boss's arena, and the wandering swords added as the boss weakens. */
public final class SwordFlock {
    // ---- tuning ----
    public static final int MAX_FLOCK = 10;
    public static final int INITIAL_COUNT = 4;
    /** A new sword joins the flock this often (200 ticks = 10 seconds), up to MAX_FLOCK. */
    public static final int SPAWN_INTERVAL_TICKS = 200;
    /** Boss health, as a share of its maximum, at or below which each wandering sword appears. */
    public static final double BLACK_SWORD_AT = 0.75;
    public static final double RED_SWORD_AT = 0.50;

    private static final float FLOCK_DAMAGE = 2.5F;   // per stab; set to 0 for a flock that is only for show
    private static final float BLACK_DAMAGE = 2.0F;
    private static final float RED_DAMAGE = 1.5F;
    private static final int HIT_COOLDOWN = 20;
    private static final int EMERGE_STAGGER = 8;      // ticks between the first swords coming up
    private static final int PERMANENT = Integer.MAX_VALUE;

    private static final Map<UUID, Flock> FLOCKS = new HashMap<>();
    private static boolean registered;

    private SwordFlock() {}

    /** Call once from the mod initializer. */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (FLOCKS.isEmpty()) return;
            Iterator<Flock> it = FLOCKS.values().iterator();
            while (it.hasNext()) {
                Flock flock = it.next();
                if (flock.world == world && !flock.tick()) it.remove();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> FLOCKS.clear());
    }

    /** Starts the flock for this boss, circling an arena of the given centre and radius. */
    public static void start(ServerWorld world, LivingEntity boss, Vec3d arenaCenter, double arenaRadius,
                             Predicate<LivingEntity> targets) {
        stop(boss);
        Flock flock = new Flock(world, boss.getUuid(), arenaCenter, arenaRadius, targets);
        FLOCKS.put(boss.getUuid(), flock);
        for (int i = 0; i < INITIAL_COUNT; i++) flock.spawnFlockSword(i * EMERGE_STAGGER);
    }

    /** Fades the whole flock out and forgets it. */
    public static void stop(LivingEntity boss) {
        Flock flock = FLOCKS.remove(boss.getUuid());
        if (flock != null) flock.retireAll();
    }

    /** Adds a black or red wandering sword by hand. They are also added automatically at the health thresholds. */
    public static void addWanderer(LivingEntity boss, FlyingSwordEntity.Kind kind) {
        Flock flock = FLOCKS.get(boss.getUuid());
        if (flock != null) flock.spawnWanderer(kind);
    }

    // ---- for a boss that flies with its flock ----

    /** Where the flock circles: the centre of its arena, or null if there is no flock. */
    @Nullable
    public static Vec3d circleCenter(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        return flock == null ? null : flock.center;
    }

    /** The radius of the circle the flock flies on. */
    public static double circleRadius(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        return flock == null ? 0.0 : flock.radius * FlyingSwordEntity.RING_FRACTION;
    }

    /** Where the flock is on its circle, as an angle around the centre (atan2 of z and x), or NaN if it has no swords out. */
    public static double flockAngle(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        return flock == null ? Double.NaN : flock.angle();
    }

    /** The average height of the swords of the flock, or NaN if it has none out. */
    public static double flockHeight(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        return flock == null ? Double.NaN : flock.height();
    }

    /** Brings the flock up to its full number of swords at once. */
    public static void fillUp(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        if (flock != null) flock.fillUp();
    }

    /** The whole flock, wanderers included, follows the boss for at most this many ticks instead of circling. */
    public static void follow(LivingEntity boss, int ticks) {
        Flock flock = FLOCKS.get(boss.getUuid());
        if (flock != null) flock.follow(boss, ticks);
    }

    /** The flock stops following the boss and goes back to circling. */
    public static void release(LivingEntity boss) {
        Flock flock = FLOCKS.get(boss.getUuid());
        if (flock != null) flock.release();
    }

    private static final class Flock {
        final ServerWorld world;
        final UUID bossId;
        final Vec3d center;
        final double radius;
        final Predicate<LivingEntity> targets;
        final List<UUID> members = new ArrayList<>();
        final List<UUID> wanderers = new ArrayList<>();
        boolean blackDone, redDone;
        int age;

        Flock(ServerWorld world, UUID bossId, Vec3d center, double radius, Predicate<LivingEntity> targets) {
            this.world = world;
            this.bossId = bossId;
            this.center = center;
            this.radius = radius;
            this.targets = targets;
        }

        /** Returns false once the flock is over. */
        boolean tick() {
            if (!(world.getEntity(bossId) instanceof LivingEntity boss) || !boss.isAlive()) {
                retireAll();
                return false;
            }
            age++;
            members.removeIf(id -> world.getEntity(id) == null);

            if (age % SPAWN_INTERVAL_TICKS == 0 && members.size() < MAX_FLOCK) spawnFlockSword(0);

            // the sword dance will go here: for now the wandering swords simply appear
            double health = boss.getHealth() / boss.getMaxHealth();
            if (!blackDone && health <= BLACK_SWORD_AT) blackDone = spawnWanderer(FlyingSwordEntity.Kind.BLACK);
            if (!redDone && health <= RED_SWORD_AT) redDone = spawnWanderer(FlyingSwordEntity.Kind.RED);
            return true;
        }

        void spawnFlockSword(int emergeDelay) {
            UUID id = spawn(FlyingSwordEntity.Kind.GOLD, false, emergeDelay);
            if (id != null) members.add(id);
        }

        boolean spawnWanderer(FlyingSwordEntity.Kind kind) {
            if (!(world.getEntity(bossId) instanceof LivingEntity boss)) return false;

            if (boss instanceof SwordPool pool) {
                Vec3d from = pool.takeSwordPosition();
                if (from == null) return false;   // no sword left on the body: it is tried again next tick

                float damage = switch (kind) {
                    case BLACK -> BLACK_DAMAGE;
                    case RED -> RED_DAMAGE;
                    default -> FLOCK_DAMAGE;
                };
                FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
                sword.configure(boss, targets, damage, HIT_COOLDOWN, members.size() + wanderers.size(),
                        FlyingSwordEntity.ALREADY_VISIBLE, PERMANENT, 0);
                sword.startDetachedWanderer(kind, center, radius, 0.8 + world.getRandom().nextDouble() * 2.4, from);
                world.spawnEntity(sword);
                wanderers.add(sword.getUuid());
                return true;
            }

            UUID id = spawn(kind, true, 0);   // a caster without a body (the test command): it rises from the floor as before
            if (id == null) return false;
            wanderers.add(id);
            return true;
        }

        /** Creates a sword under the floor at a random spot of the arena, ready to come up. */
        @Nullable
        private UUID spawn(FlyingSwordEntity.Kind kind, boolean wanderer, int emergeDelay) {
            if (!(world.getEntity(bossId) instanceof LivingEntity boss)) return null;

            Random random = world.getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * radius * 0.8;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;

            Double top = SwordPatterns.groundTop(world, x, center.y, z);
            if (top == null) return null;   // no floor there: it will be tried again later

            float damage = switch (kind) {
                case BLACK -> BLACK_DAMAGE;
                case RED -> RED_DAMAGE;
                default -> FLOCK_DAMAGE;
            };
            FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
            sword.configure(boss, targets, damage, HIT_COOLDOWN, members.size() + wanderers.size(), emergeDelay, PERMANENT, 0);
            sword.startBoid(kind, wanderer, center, radius, 0.8 + random.nextDouble() * 2.4, emergeDelay);
            sword.setPosition(x, top - 1.0, z);   // under the floor, so it rises out of it
            world.spawnEntity(sword);
            return sword.getUuid();
        }

        void retireAll() {
            for (UUID id : members) retire(id);
            for (UUID id : wanderers) retire(id);
        }

        private void retire(UUID id) {
            if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.retire();
        }

        /** Where the swords of the flock are on the circle: the average of their directions from the centre. */
        double angle() {
            double x = 0.0, z = 0.0;
            int n = 0;
            for (UUID id : members) {
                // only swords that have been out a while: a new one is still rising out of the floor somewhere else
                if (!(world.getEntity(id) instanceof FlyingSwordEntity sword) || sword.getTicks() < 40) continue;
                double dx = sword.getX() - center.x, dz = sword.getZ() - center.z;
                double length = Math.hypot(dx, dz);
                if (length < 1.0E-3) continue;
                x += dx / length;
                z += dz / length;
                n++;
            }
            return n == 0 ? Double.NaN : Math.atan2(z, x);
        }

        double height() {
            double sum = 0.0;
            int n = 0;
            for (UUID id : members) {
                if (!(world.getEntity(id) instanceof FlyingSwordEntity sword) || sword.getTicks() < 40) continue;
                sum += sword.getY();
                n++;
            }
            return n == 0 ? Double.NaN : sum / n;
        }

        void fillUp() {
            int missing = MAX_FLOCK - members.size();
            for (int i = 0; i < missing; i++) spawnFlockSword(i * 3);   // rising out of the floor one after another, quickly
        }

        void follow(LivingEntity leader, int ticks) {
            for (UUID id : members) {
                if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.startFollow(leader, ticks);
            }
            for (UUID id : wanderers) {
                if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.startFollow(leader, ticks);
            }
        }

        void release() {
            for (UUID id : members) {
                if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.stopFollow();
            }
            for (UUID id : wanderers) {
                if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.stopFollow();
            }
        }
    }
}