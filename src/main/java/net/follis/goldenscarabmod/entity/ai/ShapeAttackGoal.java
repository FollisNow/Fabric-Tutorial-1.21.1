package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.ShapeAttackFormation;
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
 * Server side of the "geometric shape" attack. It handles timing, the aim lock, path planning and damage;
 * the sheets are animated on the clients from the pattern data announced through the entity.
 * From the boss' second phase on, one attack fires three shapes in a row (see startLeg and ShapeAttackFormation's
 * VARIANT_* bits). At tick LOCK of each shape it decides where the shape ends up and tells the clients (setPatternPath):
 *   SPEAR  flies at the target's lower body, passes through it and is planted where that line meets the ground
 *   CUBE   the target itself (the cube shatters on it)
 *   RING   perfectly horizontal line from the boss towards the target, stopped only by a block or
 *          RING_MAX_DISTANCE (entities do not stop it, they are hurt as it passes through)
 */
public class ShapeAttackGoal extends Goal {

    private static final int COOLDOWN = 60;                 // ticks between attacks, after the goal ends

    private static final float SPEAR_RADIUS = 1.7f, SPEAR_DAMAGE = 16f;
    private static final float SPEAR_AIM_HEIGHT = 0.25f;   // fraction of the target's height the spear flies at, above its feet
    private static final float CUBE_RADIUS = ShapeAttackFormation.CUBE_BLAST_RADIUS, CUBE_DAMAGE = 12f;
    private static final float RING_PATH_RADIUS = 1.6f, RING_PATH_DAMAGE = 12f;   // hits everything it passes through, once
    private static final double RING_THICKNESS = 0.6;                              // half-thickness of the hit disc along the flight
    private static final float RING_SHATTER_RADIUS = 2.8f, RING_SHATTER_DAMAGE = 4f;

    private final GoldenSwarmEntity boss;
    private final Set<LivingEntity> alreadyHit = new HashSet<>();

    private LivingEntity target;
    private Vec3d aim = Vec3d.ZERO;       // where the boss is aiming while it tracks
    private Vec3d impact = Vec3d.ZERO;    // where the shape ends up (final, from LOCK on)
    private Vec3d ringStart = Vec3d.ZERO;
    private Vec3d spearHit = Vec3d.ZERO;  // where the spear passes through the target (centre of its damage)
    private int shape;
    private int flight;
    private boolean breaks;
    private int tick;            // ticks since the start of the current shape

    // the chain: from the boss' second phase on, one attack fires three shapes in quick succession
    private int legs;            // how many shapes this attack fires
    private int leg;             // which one we are on (0-based)
    private int variant;         // ShapeAttackFormation.VARIANT_* of the current shape
    private int windup;          // wind-up and aim-lock length of the current shape (shorter after the first)
    private int lock;
    private boolean chainBroken; // the target is gone: no more shapes

    public ShapeAttackGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // range, line of sight and the rest are decided in one place: the entity (SHAPE_MIN_DIST / SHAPE_MAX_DIST)
        return boss.canStartAttack(GoldenSwarmEntity.PATTERN_SHAPE_THROW)
                && boss.isPatternUsable(GoldenSwarmEntity.PATTERN_SHAPE_THROW);
    }

    @Override
    public boolean shouldContinue() {
        return !chainBroken && (leg < legs - 1 || tick < legEnd());
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        target = boss.getTarget();
        legs = boss.getShapeLegs();   // 1, or 3 from the boss' second phase on
        leg = 0;
        chainBroken = false;
        boss.getNavigation().stop();
        startLeg();
    }

    /** Starts one shape of the chain: picks it, tells the clients, resets the per-shape state. */
    private void startLeg() {
        tick = 0;
        alreadyHit.clear();
        shape = boss.getRandom().nextInt(ShapeAttackFormation.SHAPE_COUNT);
        flight = ShapeAttackFormation.baseFlight(shape);
        breaks = shape == ShapeAttackFormation.CUBE;
        // every shape after the first assembles quickly; every shape followed by another one skips the recall
        variant = (leg > 0 ? ShapeAttackFormation.VARIANT_SHORT_WINDUP : 0)
                | (leg < legs - 1 ? ShapeAttackFormation.VARIANT_CHAINED : 0);
        windup = ShapeAttackFormation.windupFor(variant);
        lock = ShapeAttackFormation.lockFor(variant);
        aim = aimPoint(target);
        impact = aim;
        boss.beginPattern(GoldenSwarmEntity.PATTERN_SHAPE_THROW, shape, aim, flight, breaks, variant);
    }

    @Override
    public void tick() {
        tick++;

        if (target != null && target.isAlive()) {
            boss.getLookControl().lookAt(target, 40.0f, 40.0f);
            // track the target during the windup so the shape visibly follows it
            if (tick < lock && tick % 4 == 0) {
                aim = aimPoint(target);
                boss.setPatternTarget(aim);
            }
        }

        if (tick == lock) {
            lockPath();
        }
        if (tick == windup) {
            boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                    SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.2f,
                    shape == ShapeAttackFormation.SPEAR ? 1.1f : 0.6f);
        }
        if (shape == ShapeAttackFormation.RING && tick > windup && tick <= impactTick()) {
            sweepRing();
        }
        if (tick == impactTick()) {
            impact();
        }

        // the next shape of the chain starts right after this one has hit
        if (tick >= legEnd() && leg < legs - 1) {
            LivingEntity next = boss.getTarget();
            if (next == null || !next.isAlive()) {
                chainBroken = true;   // nothing left to shoot at
                return;
            }
            target = next;
            leg++;
            startLeg();
        }
    }

    @Override
    public void stop() {
        boss.finishAttack(Math.max(10, Math.round(COOLDOWN * boss.getCooldownMultiplier())));   // the clone's is shorter
        boss.endPattern();
        target = null;
    }

    private int impactTick() {
        return windup + flight;
    }

    /** When the current shape is over: a shape followed by another one only waits a moment, the last one lingers. */
    private int legEnd() {
        boolean chained = (variant & ShapeAttackFormation.VARIANT_CHAINED) != 0;
        return impactTick() + (chained ? ShapeAttackFormation.CHAIN_HOLD : 12);
    }

    // ------------------------------------------------------------------------ planning

    /** Decide the final path, then announce it. After this tick the aim can no longer be dodged by tracking. */
    private void lockPath() {
        if (target != null && target.isAlive()) aim = aimPoint(target);

        switch (shape) {
            case ShapeAttackFormation.SPEAR -> {
                // The spear flies at the target's lower body (not its feet), passes through it and is
                // planted where that same line meets the ground, a little behind the target.
                boolean alive = target != null && target.isAlive();
                Vec3d feet = alive ? target.getPos() : aim;
                float height = alive ? target.getHeight() : 1.8f;
                spearHit = feet.add(0.0, height * SPEAR_AIM_HEIGHT, 0.0);

                Vec3d launch = ShapeAttackFormation.anchor(ShapeAttackFormation.SPEAR, boss.getAttackScale(),
                        boss.getX(), boss.getY(), boss.getZ(), spearHit.x, spearHit.z,
                        boss.bodyYaw * MathHelper.RADIANS_PER_DEGREE);
                Vec3d dir = spearHit.subtract(launch);
                double dl = dir.length();
                dir = dl > 1e-3 ? dir.multiply(1.0 / dl) : new Vec3d(0.0, -1.0, 0.0);

                BlockHitResult hit = boss.getWorld().raycast(new RaycastContext(spearHit,
                        spearHit.add(dir.multiply(8.0)),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
                impact = hit.getType() == HitResult.Type.MISS ? spearHit : hit.getPos();
                breaks = false;
            }
            case ShapeAttackFormation.CUBE -> {
                impact = aim;
                breaks = true;
            }
            default -> planRing();
        }
        boss.setPatternPath(impact, flight, breaks);
    }

    /** Ring: straight line through the aim point, stopped by the first block or the maximum distance. */
    private void planRing() {
        World world = boss.getWorld();
        float yaw = boss.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d start = ShapeAttackFormation.anchor(ShapeAttackFormation.RING, boss.getAttackScale(),
                boss.getX(), boss.getY(), boss.getZ(), aim.x, aim.z, yaw);
        ringStart = start;

        // Purely horizontal: only the direction towards the aim point on the XZ plane is used, so the ring
        // keeps the same height for the whole flight and never dives into the floor.
        Vec3d d = aim.subtract(start);
        double hxz = Math.sqrt(d.x * d.x + d.z * d.z);
        Vec3d dir = hxz < 1e-3
                ? new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw))
                : new Vec3d(d.x / hxz, 0.0, d.z / hxz);

        // the ring is wide, so test the centre plus four points on its rim and stop at the closest block
        Vec3d right = new Vec3d(0.0, 1.0, 0.0).crossProduct(dir).normalize();
        Vec3d up = dir.crossProduct(right);
        double[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        double clear = ShapeAttackFormation.RING_MAX_DISTANCE;
        boolean blocked = false;
        for (double[] o : offsets) {
            Vec3d from = start.add(right.multiply(o[0] * 1.2 * boss.getAttackScale())).add(up.multiply(o[1] * 1.2 * boss.getAttackScale()));
            Vec3d to = from.add(dir.multiply(ShapeAttackFormation.RING_MAX_DISTANCE));
            BlockHitResult hit = world.raycast(new RaycastContext(from, to,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
            if (hit.getType() != HitResult.Type.MISS) {
                double dist = from.distanceTo(hit.getPos());
                if (dist < clear) {
                    clear = dist;
                    blocked = true;
                }
            }
        }

        double endDist = Math.max(0.0, clear - 0.3);
        impact = start.add(dir.multiply(endDist));
        flight = Math.max(8, (int) Math.ceil(endDist / ShapeAttackFormation.RING_SPEED));
        breaks = blocked;   // only shatters on an obstacle; at max distance it just hovers, then is called back
    }

    private static Vec3d aimPoint(LivingEntity e) {
        return e.getBoundingBox().getCenter();
    }

    // -------------------------------------------------------------------------- damage

    /** The ring hurts everything it passes through (each victim once), pushing it along its direction of travel. */
    private void sweepRing() {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;

        Vec3d dir = impact.subtract(ringStart);
        double dl = dir.length();
        if (dl < 1e-3) return;          // blocked right at the start: nothing to sweep
        dir = dir.multiply(1.0 / dl);   // horizontal, the ring never moves vertically

        // where the ring was last tick and where it is now: everything in between has been passed through
        float u = Math.min(1f, (tick - windup) / (float) flight);
        float uPrev = Math.max(0f, Math.min(1f, (tick - 1 - windup) / (float) flight));
        Vec3d centre = ringStart.lerp(impact, u);
        Vec3d prev = ringStart.lerp(impact, uPrev);
        double step = centre.distanceTo(prev);

        double ringR = RING_PATH_RADIUS * boss.getAttackScale();   // a clone's ring is smaller
        Box area = new Box(prev, centre).expand(ringR + 1.5);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e) && !alreadyHit.contains(e))) {
            // Judge by the victim's hitbox centre and size, never by its feet: the ring flies about
            // two blocks above the ground, so a feet-based distance would miss almost everything.
            Vec3d c = victim.getBoundingBox().getCenter();
            double halfW = victim.getWidth() * 0.5;
            double halfH = victim.getHeight() * 0.5;

            // along the flight direction: within the ring's thickness, including the distance covered this tick
            double along = (c.x - prev.x) * dir.x + (c.z - prev.z) * dir.z;
            double margin = RING_THICKNESS * boss.getAttackScale() + halfW;
            if (along < -margin || along > step + margin) continue;

            // across: the whole disc counts, including the hole in the middle
            double lateral = Math.abs((c.x - centre.x) * -dir.z + (c.z - centre.z) * dir.x);
            double vertical = Math.abs(c.y - centre.y);
            double dLat = Math.max(0.0, lateral - halfW);
            double dVert = Math.max(0.0, vertical - halfH);
            if (dLat * dLat + dVert * dVert > ringR * ringR) continue;

            alreadyHit.add(victim);
            victim.damage(world.getDamageSources().mobAttack(boss), RING_PATH_DAMAGE * boss.getDamageMultiplier());
            victim.takeKnockback(0.8, -dir.x, -dir.z);
        }
    }

    private void impact() {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;

        switch (shape) {
            case ShapeAttackFormation.SPEAR -> damageArea(world, spearHit, SPEAR_RADIUS, SPEAR_DAMAGE);
            case ShapeAttackFormation.CUBE -> damageArea(world, impact, CUBE_RADIUS, CUBE_DAMAGE);
            default -> {
                if (!breaks) return;   // reached max distance without hitting anything: no crash
                damageArea(world, impact, RING_SHATTER_RADIUS, RING_SHATTER_DAMAGE);
            }
        }

        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.8f, shape == ShapeAttackFormation.SPEAR ? 1.8f : 1.3f);
        world.spawnParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z,
                40, 0.8, 0.5, 0.8, 0.3);
    }

    private void damageArea(ServerWorld world, Vec3d center, double baseRadius, float baseDamage) {
        double radius = baseRadius * boss.getAttackScale();     // a clone's blasts are smaller...
        float damage = baseDamage * boss.getDamageMultiplier(); // ...and weaker
        Box area = new Box(center, center).expand(radius);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e))) {
            if (distanceSqToBox(victim.getBoundingBox(), center) > radius * radius) continue;
            victim.damage(world.getDamageSources().mobAttack(boss), damage);
            victim.takeKnockback(0.9, center.x - victim.getX(), center.z - victim.getZ());
        }
    }

    /** Squared distance from a point to the closest point of a box (0 if the point is inside it). */
    private static double distanceSqToBox(Box box, Vec3d p) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}