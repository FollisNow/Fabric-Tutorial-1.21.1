package net.follis.goldenscarabmod.pattern;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The sword star: swords leave the boss's body together and form a flat star in front of it. The star spins in its own
 * plane and glides at the target slowly, homing in on it a little: enough that standing still gets you hit, not enough
 * to follow someone who moves out of the way. It stays stabbed in the first block it meets, with its spin stopped, then
 * the swords are called back to the body. Several stars can follow one another.
 *
 * The server only moves the centre of the star and tells the clients which way it travels; each client works out where
 * the blades are (see FlyingSwordEntity.getStarPose), so the server computes the blades only to hit things with them.
 */
public final class SwordStarPattern {
    /** How many stars one attack sends, how many swords each has, and the ticks between two stars. */
    public static final int STARS = 3;
    public static final int SWORDS_PER_STAR = 5;
    public static final int INTERVAL = 36;

    private static final int MIN_SWORDS = 3;           // with fewer swords on the body the star is skipped
    private static final double DISTANCE = 4.0;        // where the star forms: this far in front of the boss, at the height of its body
    private static final double BLADE_HALF = 0.5;      // half the length of a blade: 0.5 times the sword renderer's SCALE
    private static final double REACH = FlyingSwordEntity.STAR_RADIUS + BLADE_HALF;   // from the centre to the tip of a blade
    private static final double START_SPEED = 0.15;    // blocks per tick: slow, then it speeds up a little
    private static final double MAX_SPEED = 0.3;
    private static final double ACCELERATION = 0.006;
    private static final double MAX_DISTANCE = 45.0;   // how far a star can fly if it meets nothing
    private static final double STAB_DEPTH = 0.35;     // how deep the forward blade goes into the block
    private static final int STUCK_TICKS = 40;         // how long it stays in the block before the swords are called back
    private static final float DAMAGE = 2.5F;          // per sword, each time it hits
    private static final int HIT_COOLDOWN = 10;

    // ---- homing ----
    private static final double HOMING_TURN = 0.02;           // radians per tick: how fast the star can turn toward the target
    private static final double HOMING_LOCK_DISTANCE = 6.0;   // closer than this it flies straight: the last moment to step aside
    private static final int HOMING_TICKS = 100;              // it only homes in during the first 5 seconds of the flight

    private static final List<Star> ACTIVE = new ArrayList<>();
    private static boolean registered;

    private SwordStarPattern() {}

    /** Call once from the mod initializer, next to the other patterns. */
    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (ACTIVE.isEmpty()) return;
            Iterator<Star> it = ACTIVE.iterator();
            while (it.hasNext()) {
                Star star = it.next();
                if (star.world == world && !star.tick()) it.remove();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ACTIVE.clear());
    }

    /** Sends STARS stars one after another. Returns about how long the attack lasts, until the last star is launched. */
    public static int start(ServerWorld world, LivingEntity boss, LivingEntity target, Predicate<LivingEntity> targets, SwordPool pool) {
        return start(world, boss, target, targets, pool, STARS, SWORDS_PER_STAR, INTERVAL);
    }

    public static int start(ServerWorld world, LivingEntity boss, LivingEntity target, Predicate<LivingEntity> targets,
                            SwordPool pool, int stars, int swordsPerStar, int interval) {
        for (int i = 0; i < stars; i++) {
            ACTIVE.add(new Star(world, boss.getUuid(), target.getUuid(), targets, pool, swordsPerStar, i * interval));
        }
        return (stars - 1) * interval + FlyingSwordEntity.STAR_GATHER_TICKS + 20;
    }

    private enum State { WAITING, GATHER, FLIGHT, STUCK, DONE }

    /** One star, from the moment its swords leave the body until they are called back. */
    private static final class Star {
        final ServerWorld world;
        final UUID bossId, targetId;
        final Predicate<LivingEntity> targets;
        final SwordPool pool;
        final int wanted, delay;
        final List<UUID> swords = new ArrayList<>();
        final List<Vec3d> starts = new ArrayList<>();   // where each sword hung on the body
        Vec3d[] prevPlace = new Vec3d[0];                // where each blade was last tick, to hit what it sweeps through

        State state = State.WAITING;
        int age, stateAge;
        Vec3d center = Vec3d.ZERO;
        Vec3d dir = new Vec3d(0, 0, 1);   // the direction the star travels in: the plane of the star contains it
        Vec3d lastTarget;
        double spin, speed, travelled;
        boolean homing;

        Star(ServerWorld world, UUID bossId, UUID targetId, Predicate<LivingEntity> targets, SwordPool pool, int wanted, int delay) {
            this.world = world;
            this.bossId = bossId;
            this.targetId = targetId;
            this.targets = targets;
            this.pool = pool;
            this.wanted = wanted;
            this.delay = delay;
        }

        /** Returns false once this star is over. */
        boolean tick() {
            // if the boss is gone the swords see their owner is dead and vanish by themselves
            if (!(world.getEntity(bossId) instanceof LivingEntity boss) || !boss.isAlive()) return false;
            age++;
            switch (state) {
                case WAITING -> {
                    if (age > delay) begin(boss);
                }
                case GATHER -> tickGather(boss);
                case FLIGHT -> tickFlight(boss);
                case STUCK -> tickStuck();
                default -> {}
            }
            return state != State.DONE;
        }

        private Vec3d bodyCenter(LivingEntity boss) {
            return boss.getPos().add(0.0, pool.bodyCenterHeight(), 0.0);
        }

        private boolean targetAlive() {
            return world.getEntity(targetId) instanceof LivingEntity t && t.isAlive();
        }

        /** Where the target is, or was the last time it was seen. */
        private Vec3d targetPoint(LivingEntity boss) {
            if (world.getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
                lastTarget = t.getPos().add(0.0, t.getHeight() * 0.5, 0.0);
            }
            if (lastTarget == null) lastTarget = bodyCenter(boss).add(boss.getRotationVec(1.0F).multiply(10.0));
            return lastTarget;
        }

        /** In front of the boss, in the direction of the target, at the height of its body. */
        private Vec3d formationCenter(LivingEntity boss) {
            Vec3d flat = new Vec3d(dir.x, 0.0, dir.z);
            flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : flat.normalize();
            return bodyCenter(boss).add(flat.multiply(DISTANCE));
        }

        /** Turns a unit vector toward another by no more than maxAngle radians. */
        private static Vec3d turnToward(Vec3d current, Vec3d desired, double maxAngle) {
            double angle = Math.acos(MathHelper.clamp(current.dotProduct(desired), -1.0, 1.0));
            if (angle < 1.0E-4) return current;
            double k = Math.min(1.0, maxAngle / angle);
            Vec3d blended = current.multiply(1.0 - k).add(desired.multiply(k));
            return blended.lengthSquared() < 1.0E-6 ? current : blended.normalize();
        }

        /** Takes the swords off the body: all at once. */
        private void begin(LivingEntity boss) {
            int available = Math.min(pool.swordsAvailable(), wanted);
            if (available < MIN_SWORDS) {   // not enough swords on the body: this star is skipped
                state = State.DONE;
                return;
            }
            for (int i = 0; i < available; i++) {
                Vec3d from = pool.takeSwordPosition();
                if (from == null) break;
                starts.add(from);
            }
            int n = starts.size();
            if (n == 0) {
                state = State.DONE;
                return;
            }

            Vec3d to = targetPoint(boss).subtract(bodyCenter(boss));
            dir = to.lengthSquared() < 1.0E-4 ? boss.getRotationVec(1.0F) : to.normalize();
            center = formationCenter(boss);

            for (int i = 0; i < n; i++) {
                FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
                sword.configure(boss, targets, DAMAGE, HIT_COOLDOWN, i, FlyingSwordEntity.ALREADY_VISIBLE, Integer.MAX_VALUE, 0);
                sword.joinStar(i, n, starts.get(i), center);
                sword.markReturning(Integer.MAX_VALUE);   // they come back by themselves once the star is stuck
                world.spawnEntity(sword);
                swords.add(sword.getUuid());
            }
            prevPlace = new Vec3d[n];
            driveSwords(false);

            state = State.GATHER;
            stateAge = 0;
            world.playSound(null, boss.getX(), boss.getY() + pool.bodyCenterHeight(), boss.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.5F, 1.2F);
        }

        private void tickGather(LivingEntity boss) {
            stateAge++;
            Vec3d wantedDir = targetPoint(boss).subtract(bodyCenter(boss));
            if (wantedDir.lengthSquared() > 1.0E-4) dir = dir.lerp(wantedDir.normalize(), 0.25).normalize();   // the star keeps facing the target
            center = formationCenter(boss);

            spin += FlyingSwordEntity.STAR_SPIN;
            driveSwords(false);
            if (stateAge >= FlyingSwordEntity.STAR_GATHER_TICKS) launch(boss);
        }

        private void launch(LivingEntity boss) {
            Vec3d to = targetPoint(boss).subtract(center);
            if (to.lengthSquared() > 1.0E-4) dir = to.normalize();   // it sets off straight at the target
            state = State.FLIGHT;
            stateAge = 0;
            speed = START_SPEED;
            travelled = 0.0;
            homing = true;
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(),
                    SoundCategory.HOSTILE, 2.0F, 1.2F);
        }

        private void tickFlight(LivingEntity boss) {
            stateAge++;
            speed = Math.min(MAX_SPEED, speed + ACCELERATION);

            if (homing) {
                Vec3d toTarget = targetPoint(boss).subtract(center);
                double distance = toTarget.length();
                // it gives up when the time is over, when it is too close to correct its course, when the target is behind it, or when it is dead
                if (stateAge > HOMING_TICKS || distance < HOMING_LOCK_DISTANCE || toTarget.dotProduct(dir) < 0.0 || !targetAlive()) {
                    homing = false;
                } else {
                    dir = turnToward(dir, toTarget.multiply(1.0 / distance), HOMING_TURN);
                }
            }
            Vec3d step = dir.multiply(speed);

            // the forward blade is what meets the block, not the centre of the star
            Vec3d tip = center.add(dir.multiply(REACH));
            BlockHitResult hit = world.raycast(new RaycastContext(tip, tip.add(step),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));

            if (hit.getType() != HitResult.Type.MISS) {
                // the star stops with that tip STAB_DEPTH inside the block, and its spin stops on a blade pointing into it
                center = hit.getPos().add(dir.multiply(STAB_DEPTH - REACH));
                double stepAngle = 2.0 * Math.PI / Math.max(1, swords.size());
                spin = spin + (stepAngle - spin % stepAngle) % stepAngle;
                for (UUID id : swords) {
                    if (world.getEntity(id) instanceof FlyingSwordEntity sword) sword.stuckInStar(spin);
                }
                state = State.STUCK;
                stateAge = 0;
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(hit.getBlockPos())),
                        hit.getPos().x, hit.getPos().y, hit.getPos().z, 16, 0.4, 0.4, 0.4, 0.1);
                world.playSound(null, center.x, center.y, center.z, SoundEvents.ITEM_TRIDENT_HIT_GROUND,
                        SoundCategory.HOSTILE, 2.0F, 1.0F);
                driveSwords(false);
                return;
            }

            center = center.add(step);
            travelled += speed;
            spin += FlyingSwordEntity.STAR_SPIN;
            if (stateAge % 2 == 0) {
                world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 1, 0.3, 0.3, 0.3, 0.0);
            }
            driveSwords(true);

            if (travelled > MAX_DISTANCE) {   // it met nothing: call the swords back
                recallAll();
                state = State.DONE;
            }
        }

        private void tickStuck() {
            stateAge++;
            driveSwords(false);
            if (stateAge % 8 == 0) world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2, 0.6, 0.6, 0.6, 0.01);
            if (stateAge >= STUCK_TICKS) {
                recallAll();
                state = State.DONE;
            }
        }

        /** Where blade number i is. The clients work out the same place for themselves, from the centre, the direction and the spin. */
        private Vec3d bladeSpoke(int i, int n) {
            return FlyingSwordEntity.starSpoke(dir, 2.0 * Math.PI * i / n + spin);
        }

        /** Moves the star, and lets the blades hit what they swept through since the last tick. */
        private void driveSwords(boolean hits) {
            int n = swords.size();
            for (int i = 0; i < n; i++) {
                if (!(world.getEntity(swords.get(i)) instanceof FlyingSwordEntity sword)) continue;
                sword.driveStar(center, dir);

                Vec3d place = center.add(bladeSpoke(i, n).multiply(FlyingSwordEntity.STAR_RADIUS));
                if (hits && prevPlace[i] != null) sword.sweepHits(prevPlace[i], place);
                prevPlace[i] = place;
            }
        }

        /** Each sword leaves the star from where its blade is and flies home. */
        private void recallAll() {
            int n = swords.size();
            for (int i = 0; i < n; i++) {
                if (!(world.getEntity(swords.get(i)) instanceof FlyingSwordEntity sword)) continue;
                Vec3d spoke = bladeSpoke(i, n);
                sword.leaveStar(center.add(spoke.multiply(FlyingSwordEntity.STAR_RADIUS)), spoke);
                sword.recall();
            }
        }
    }
}