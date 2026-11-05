package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.SwirlFormation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Server side of the swirl relocation. The boss disassembles for DISASSEMBLE ticks, then the real entity is
 * carried along a straight line to a spot beyond its target (so the swirl sweeps through the target's area),
 * invulnerable and without collisions, hurting every foe the swirl crosses. At the destination it
 * settles back to normal while the sheets reassemble on the clients.
 * The clients only receive the flight duration; they follow the entity's own movement.
 */
public class TranslocateAttackGoal extends Goal {

    private static final int COOLDOWN = 50;
    private static final double AIRBORNE_MAX_TARGET_DIST = 30.0;                  // when airborne it relocates even from further away
    private static final double MIN_TRAVEL = 5.0, MAX_TRAVEL = 22.0;             // how far the boss may relocate
    private static final double SWIRL_RADIUS = SwirlFormation.HIT_RADIUS;                               // the width of the damaging swirl
    private static final float DAMAGE = 10f;

    private final GoldenSwarmEntity boss;
    private final Set<LivingEntity> alreadyHit = new HashSet<>();

    private LivingEntity target;
    private Vec3d origin = Vec3d.ZERO;
    private Vec3d dest = Vec3d.ZERO;
    private int flight;
    private int total;
    private int tick;
    private boolean failed;
    private boolean travelling;
    private int retryAge;   // after a failed destination search, wait a moment before trying again

    public TranslocateAttackGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (boss.age < retryAge) return false;
        if (!boss.canStartAttack(GoldenSwarmEntity.PATTERN_TRANSLOCATE)) return false;
        LivingEntity t = boss.getTarget();
        if (t == null || !t.isAlive()) return false;
        double d2 = boss.squaredDistanceTo(t);
        // airborne: relocate whatever the distance to the target, just to get back on the ground
        if (boss.isAirborne()) return d2 <= AIRBORNE_MAX_TARGET_DIST * AIRBORNE_MAX_TARGET_DIST;
        // otherwise the range is decided in one place: the entity (TRANSLOCATE_MIN_DIST / TRANSLOCATE_MAX_DIST)
        return boss.isPatternUsable(GoldenSwarmEntity.PATTERN_TRANSLOCATE);
    }

    @Override
    public boolean shouldContinue() {
        return !failed && tick < total;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        tick = 0;
        failed = false;
        travelling = false;
        alreadyHit.clear();
        target = boss.getTarget();
        origin = boss.getPos();
        boss.getNavigation().stop();

        Vec3d spot = pickDestination();
        if (spot == null) {
            failed = true;   // nowhere sensible to go: skip this attack (stop() sets a short cooldown)
            return;
        }
        dest = spot;
        flight = SwirlFormation.travelTicks(origin.distanceTo(dest));
        total = SwirlFormation.totalTicks(flight);
        boss.beginPattern(GoldenSwarmEntity.PATTERN_TRANSLOCATE, 0, dest, flight, false);
    }

    @Override
    public void tick() {
        if (failed) return;
        tick++;

        if (tick <= SwirlFormation.DISASSEMBLE && target != null && target.isAlive()) {
            boss.getLookControl().lookAt(target, 40.0f, 40.0f);
        }

        if (tick == SwirlFormation.DISASSEMBLE) {
            beginTravel();
        }
        int k = tick - SwirlFormation.DISASSEMBLE;
        if (k >= 1 && k <= flight) {
            travelStep(k);
        }
        if (k == flight) {
            arrive();
        }
    }

    @Override
    public void stop() {
        endTravelState();   // always leave the boss in a normal state, whatever interrupted the attack
        if (failed) retryAge = boss.age + 20;   // airborne ignores the cooldown, so this stops it retrying every tick
        boss.resetAirTicks();                   // it was in the air during the trip: do not count that as "still airborne"
        boss.finishAttack(failed ? 30 : COOLDOWN);
        boss.endPattern();
        target = null;
    }

    // ------------------------------------------------------------------- the trip

    private void beginTravel() {
        travelling = true;
        boss.setInvulnerable(true);
        boss.noClip = true;
        boss.setNoGravity(true);
        boss.setVelocity(Vec3d.ZERO);
        boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.2f, 0.7f);
    }

    private void travelStep(int k) {
        Vec3d prev = origin.lerp(dest, (k - 1) / (double) flight);
        Vec3d cur = origin.lerp(dest, k / (double) flight);
        boss.refreshPositionAndAngles(cur.x, cur.y, cur.z, boss.getYaw(), boss.getPitch());
        boss.setVelocity(Vec3d.ZERO);

        sweep(prev.add(0.0, SwirlFormation.CHEST, 0.0), cur.add(0.0, SwirlFormation.CHEST, 0.0));
    }

    private void arrive() {
        endTravelState();
        boss.fallDistance = 0f;
        boss.getNavigation().stop();
        boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 2.0f, 0.8f);
    }

    private void endTravelState() {
        if (!travelling) return;
        travelling = false;
        boss.setInvulnerable(false);
        boss.noClip = false;
        boss.setNoGravity(false);
        boss.setVelocity(Vec3d.ZERO);
    }

    // ------------------------------------------------------------------- damage

    /** Hurts every foe whose hitbox is close to the stretch of path the swirl covered this tick (once each). */
    private void sweep(Vec3d a, Vec3d b) {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;

        Vec3d d = b.subtract(a);
        double len2 = d.lengthSquared();
        Box area = new Box(a, b).expand(SWIRL_RADIUS + 1.5);

        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e) && !alreadyHit.contains(e))) {
            Vec3d c = victim.getBoundingBox().getCenter();
            double u = len2 < 1e-6 ? 0.0
                    : MathHelper.clamp(((c.x - a.x) * d.x + (c.y - a.y) * d.y + (c.z - a.z) * d.z) / len2, 0.0, 1.0);
            Vec3d closest = a.add(d.multiply(u));
            if (distanceSqToBox(victim.getBoundingBox(), closest) > SWIRL_RADIUS * SWIRL_RADIUS) continue;

            alreadyHit.add(victim);
            victim.damage(world.getDamageSources().mobAttack(boss), DAMAGE);
            victim.takeKnockback(1.0, -d.x, -d.z);   // pushed along the swirl's direction of travel
        }

        world.spawnParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 3, 0.6, 0.6, 0.6, 0.1);
    }

    // ------------------------------------------------------------------ destination

    /**
     * A free standing spot beyond the target, so the swirl passes through the target's area. Tries a few random
     * spots and keeps the first one with floor under it, room for the boss, a sensible distance and a clear
     * line from the boss (the swirl does not go through walls). Returns null if none works.
     */
    private Vec3d pickDestination() {
        if (target == null) return null;
        World world = boss.getWorld();
        Vec3d from = boss.getPos();
        Vec3d tp = target.getPos();

        double dx = tp.x - from.x, dz = tp.z - from.z;
        double hl = Math.sqrt(dx * dx + dz * dz);
        if (hl < 1e-3) {
            dx = 1.0;
            dz = 0.0;
            hl = 1.0;
        }
        dx /= hl;
        dz /= hl;

        for (int attempt = 0; attempt < 14; attempt++) {
            double beyond = 3.0 + boss.getRandom().nextDouble() * 5.0;       // past the target
            double side = (boss.getRandom().nextDouble() - 0.5) * 6.0;       // and a bit to either side
            double x = tp.x + dx * beyond - dz * side;
            double z = tp.z + dz * beyond + dx * side;

            Vec3d spot = floorAt(world, x, tp.y, z);
            if (spot == null) continue;
            double dist = from.distanceTo(spot);
            if (dist < MIN_TRAVEL || dist > MAX_TRAVEL) continue;
            if (!world.isSpaceEmpty(boss, boss.getDimensions(boss.getPose()).getBoxAt(spot).offset(0.0, 0.01, 0.0))) continue;
            if (!clearLine(from.add(0.0, SwirlFormation.CHEST, 0.0), spot.add(0.0, SwirlFormation.CHEST, 0.0))) continue;
            return spot;
        }
        return null;
    }

    private Vec3d floorAt(World world, double x, double y, double z) {
        BlockHitResult hit = world.raycast(new RaycastContext(new Vec3d(x, y + 3.0, z), new Vec3d(x, y - 6.0, z),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getPos();
    }

    private boolean clearLine(Vec3d a, Vec3d b) {
        BlockHitResult hit = boss.getWorld().raycast(new RaycastContext(a, b,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** Squared distance from a point to the closest point of a box (0 if the point is inside it). */
    private static double distanceSqToBox(Box box, Vec3d p) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}