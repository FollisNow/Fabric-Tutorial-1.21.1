package net.follis.goldenscarabmod.pattern;

import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.follis.goldenscarabmod.entity.custom.GroundSwordEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** The boss's sword attacks. Each method returns how many ticks the attack lasts. */
public final class SwordPatterns {
    /** The usual targets: players who can be hurt. */
    public static final Predicate<LivingEntity> PLAYERS =
            e -> e instanceof PlayerEntity p && !p.isCreative() && !p.isSpectator();

    // ---- 1. orbiting, homing swords ----
    private static final int ORBIT_COUNT = 8;
    private static final double ORBIT_SPEED = 0.12;       // radians per tick
    private static final int ORBIT_APPEAR_STEP = 3;       // ticks between two swords appearing
    private static final int ORBIT_TICKS = 50;            // how long they circle before the first launch
    private static final int LAUNCH_STAGGER = 4;          // ticks between two launches
    private static final int ORBIT_DURATION = 220;        // the whole attack, in ticks
    private static final float ORBIT_DAMAGE = 3.0F;       // per stab
    private static final int ORBIT_HIT_COOLDOWN = 12;     // ticks before a sword can stab the same target again
    private static final int ORBIT_GATHER_TICKS = 20;   // how long a sword takes to glide from the body to its orbit
    private static final int WALL_GATHER_TICKS = 25;
    // ---- 2. stabs from below ----
    private static final int STAB_COLUMNS = 7;
    private static final double STAB_COLUMN_SPACING = 1.3;
    private static final int STAB_ROWS = 10;
    private static final double STAB_ROW_SPACING = 1.5;
    private static final double STAB_START_DISTANCE = 2.5;
    private static final int STAB_ROW_DELAY = 3;          // ticks between two rows: sets how fast the rectangle moves
    private static final int STAB_WARN_TICKS = 14;        // warning before a sword comes up
    private static final float STAB_DAMAGE = 5.0F;
    private static final double STAB_JITTER = 0.2;        // a little disorder, so it does not look like a grid

    // ---- 3. wall of swords ----
    private static final int WALL_COLUMNS = 9;
    private static final int WALL_ROWS = 3;
    private static final double WALL_COLUMN_SPACING = 1.1;
    private static final double WALL_ROW_SPACING = 1.2;
    private static final double WALL_BASE_HEIGHT = 0.4;   // height of the lowest row above the boss's feet
    private static final double WALL_BEHIND = 2.5;        // distance behind the boss's body
    private static final int WALL_FORM_TICKS = 45;        // from the cast to the launch
    private static final int WALL_FLIGHT_TICKS = 70;
    private static final float WALL_DAMAGE = 3.0F;

    // ---- 4. a wave of stabs spreading from a point ----
    private static final int WAVE_RINGS = 5;
    private static final double WAVE_FIRST_RADIUS = 2.5;
    private static final double WAVE_RING_SPACING = 2.2;
    private static final double WAVE_SWORD_SPACING = 2.0;   // along a ring: sets how many swords it has
    private static final int WAVE_RING_DELAY = 4;           // ticks between two rings: how fast the wave spreads (about 0.55 blocks per tick)
    private static final float WAVE_DAMAGE = 5.0F;

    // ---- 5. the swords of a body dive into the ground ----
    private static final float DIVE_DAMAGE = 4.0F;
    private static final double DIVE_SPREAD = 0.8;   // blocks farther out than where the sword hangs: the swords make a ring around the boss

    /**
     * Every sword on the pool's body leaves it and dives into the ground around the caster, all arriving together after
     * `ticks`. They stay stabbed a while, then go back to the body by themselves. Returns `ticks`.
     */
    public static int stabIntoGround(ServerWorld world, LivingEntity caster, SwordPool pool, Predicate<LivingEntity> targets, int ticks) {
        Random random = world.getRandom();
        int slot = 0;

        while (true) {
            Vec3d from = pool.takeSwordPosition();
            if (from == null) break;

            // straight down from where it hangs, a little farther out from the boss
            Vec3d out = new Vec3d(from.x - caster.getX(), 0.0, from.z - caster.getZ());
            double reach = out.length();
            if (reach < 0.05) {
                double a = random.nextDouble() * 2.0 * Math.PI;
                out = new Vec3d(Math.cos(a), 0.0, Math.sin(a));
                reach = 0.05;
            } else {
                out = out.multiply(1.0 / reach);
            }
            double distance = reach + DIVE_SPREAD;
            double x = caster.getX() + out.x * distance;
            double z = caster.getZ() + out.z * distance;
            Double top = groundTop(world, x, caster.getY(), z);
            double y = (top != null ? top : caster.getY()) - 0.4;   // aimed a little under the surface, so it ends up in the ground

            FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
            sword.configure(caster, targets, DIVE_DAMAGE, 1000, slot++, FlyingSwordEntity.ALREADY_VISIBLE, Integer.MAX_VALUE, 0);
            sword.startDive(from, new Vec3d(x, y, z), ticks);
            sword.markReturning(Integer.MAX_VALUE);   // stabbed in the ground a while, then back to the body
            world.spawnEntity(sword);
        }
        return ticks;
    }

    private SwordPatterns() {}

    // ================================================================ 1. orbiting, homing swords

    public static int orbitingSwords(ServerWorld world, LivingEntity caster, Predicate<LivingEntity> targets) {
        return orbitingSwords(world, caster, targets, null);
    }

    /** With a pool, the swords leave the caster's body; without one they appear in place. */
    public static int orbitingSwords(ServerWorld world, LivingEntity caster, Predicate<LivingEntity> targets, @Nullable SwordPool pool) {
        double radius = caster.getWidth() * 0.5 + 2.5;
        double height = pool != null ? pool.bodyCenterHeight() : caster.getHeight() * 0.55;
        int taken = pool == null ? 0 : Math.min(pool.swordsAvailable(), ORBIT_COUNT);

        for (int i = 0; i < ORBIT_COUNT; i++) {
            Vec3d from = pool != null && SwordPool.detaches(i, ORBIT_COUNT, taken) ? pool.takeSwordPosition() : null;

            FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
            sword.configure(caster, targets, ORBIT_DAMAGE, ORBIT_HIT_COOLDOWN, i,
                    from != null ? FlyingSwordEntity.ALREADY_VISIBLE : i * ORBIT_APPEAR_STEP,
                    ORBIT_DURATION, ORBIT_TICKS + i * LAUNCH_STAGGER);
            sword.startOrbit(radius, height, 2.0 * Math.PI * i / ORBIT_COUNT, ORBIT_SPEED);
            if (from != null) sword.gatherFrom(from, ORBIT_GATHER_TICKS);
            world.spawnEntity(sword);
        }
        return ORBIT_DURATION;
    }

    // ================================================================ 2. stabs from below, in a moving rectangle

    public static int groundStabs(ServerWorld world, LivingEntity caster, Vec3d direction, Predicate<LivingEntity> targets) {
        Vec3d dir = horizontal(direction, caster);
        Vec3d side = new Vec3d(-dir.z, 0, dir.x);
        Vec3d origin = caster.getPos();
        Random random = world.getRandom();

        for (int row = 0; row < STAB_ROWS; row++) {
            for (int col = 0; col < STAB_COLUMNS; col++) {
                double along = STAB_START_DISTANCE + row * STAB_ROW_SPACING;
                double across = (col - (STAB_COLUMNS - 1) / 2.0) * STAB_COLUMN_SPACING;
                Vec3d p = origin.add(dir.multiply(along)).add(side.multiply(across))
                        .add((random.nextDouble() - 0.5) * 2 * STAB_JITTER, 0, (random.nextDouble() - 0.5) * 2 * STAB_JITTER);

                Double top = groundTop(world, p.x, origin.y, p.z);
                if (top == null) continue;   // no floor there: a pit, or an unloaded chunk

                int warnAt = row * STAB_ROW_DELAY;
                GroundSwordEntity sword = new GroundSwordEntity(ModEntities.GROUND_SWORD, world);
                sword.configure(caster, targets, STAB_DAMAGE, warnAt, warnAt + STAB_WARN_TICKS);
                sword.setPosition(p.x, top, p.z);
                world.spawnEntity(sword);
            }
        }
        return (STAB_ROWS - 1) * STAB_ROW_DELAY + STAB_WARN_TICKS
                + GroundSwordEntity.RISE_TICKS + GroundSwordEntity.UP_TICKS + GroundSwordEntity.SINK_TICKS;
    }

    /** The height of the floor surface at this column, found near referenceY, or null if there is none. */
    @Nullable
    public static Double groundTop(ServerWorld world, double x, double referenceY, double z) {
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        if (!world.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) return null;

        int startY = MathHelper.floor(referenceY) + 2;
        for (int y = startY; y >= startY - 7; y--) {
            BlockPos pos = new BlockPos(bx, y, bz);
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
            BlockPos above = pos.up();
            if (!shape.isEmpty() && world.getBlockState(above).getCollisionShape(world, above).isEmpty()) {
                return y + shape.getMax(Direction.Axis.Y);
            }
        }
        return null;
    }

    // ================================================================ 3. a wall of swords

    public static int swordWall(ServerWorld world, LivingEntity caster, Vec3d direction, Predicate<LivingEntity> targets) {
        return swordWall(world, caster, direction, targets, null);
    }

    public static int swordWall(ServerWorld world, LivingEntity caster, Vec3d direction, Predicate<LivingEntity> targets,
                                @Nullable SwordPool pool) {
        Vec3d dir = horizontal(direction, caster);   // locked now: the wall flies this way whatever the boss does meanwhile
        Vec3d side = new Vec3d(-dir.z, 0, dir.x);
        double behind = caster.getWidth() * 0.5 + WALL_BEHIND;
        int total = WALL_COLUMNS * WALL_ROWS;
        int taken = pool == null ? 0 : Math.min(pool.swordsAvailable(), total);

        int slot = 0;
        for (int col = 0; col < WALL_COLUMNS; col++) {
            for (int row = 0; row < WALL_ROWS; row++) {
                double across = (col - (WALL_COLUMNS - 1) / 2.0) * WALL_COLUMN_SPACING;
                double up = WALL_BASE_HEIGHT + row * WALL_ROW_SPACING;
                Vec3d offset = side.multiply(across).add(dir.multiply(-behind)).add(0, up, 0);
                Vec3d from = pool != null && SwordPool.detaches(slot, total, taken) ? pool.takeSwordPosition() : null;

                FlyingSwordEntity sword = new FlyingSwordEntity(ModEntities.FLYING_SWORD, world);
                sword.configure(caster, targets, WALL_DAMAGE, 1000, slot,
                        from != null ? FlyingSwordEntity.ALREADY_VISIBLE : col * 2 + row,
                        WALL_FORM_TICKS + WALL_FLIGHT_TICKS, WALL_FORM_TICKS);   // a cooldown of 1000: each target is hit once
                sword.startFormation(offset, dir);
                if (from != null) sword.gatherFrom(from, WALL_GATHER_TICKS);
                world.spawnEntity(sword);
                slot++;
            }
        }
        return WALL_FORM_TICKS + WALL_FLIGHT_TICKS;
    }/** The wave starting a default time from now, with the usual warning before its first ring. */
    public static int groundWave(ServerWorld world, LivingEntity caster, Vec3d center, Predicate<LivingEntity> targets) {
        return groundWave(world, caster, center, targets, STAB_WARN_TICKS);
    }

    /**
     * Rings of swords coming up out of the ground one ring after another, spreading out from a point. The first ring comes up
     * startDelay ticks from now. A ring's warning shows STAB_WARN_TICKS before it comes up, or from now if that is sooner.
     * Returns how long the whole wave lasts.
     */
    public static int groundWave(ServerWorld world, LivingEntity caster, Vec3d center, Predicate<LivingEntity> targets, int startDelay) {
        Random random = world.getRandom();

        for (int ring = 0; ring < WAVE_RINGS; ring++) {
            double radius = WAVE_FIRST_RADIUS + ring * WAVE_RING_SPACING;
            int count = Math.max(6, (int) Math.round(2.0 * Math.PI * radius / WAVE_SWORD_SPACING));
            double offset = random.nextDouble() * 2.0 * Math.PI;   // a different start for each ring, so they do not line up into spokes
            int riseAt = startDelay + ring * WAVE_RING_DELAY;
            int warnAt = Math.max(0, riseAt - STAB_WARN_TICKS);

            for (int i = 0; i < count; i++) {
                double angle = offset + 2.0 * Math.PI * i / count;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;

                Double top = groundTop(world, x, center.y, z);
                if (top == null) continue;   // no floor there: a pit, or an unloaded chunk

                GroundSwordEntity sword = new GroundSwordEntity(ModEntities.GROUND_SWORD, world);
                sword.configure(caster, targets, WAVE_DAMAGE, warnAt, riseAt);
                sword.setPosition(x, top, z);
                world.spawnEntity(sword);
            }
        }
        return startDelay + (WAVE_RINGS - 1) * WAVE_RING_DELAY
                + GroundSwordEntity.RISE_TICKS + GroundSwordEntity.UP_TICKS + GroundSwordEntity.SINK_TICKS;
    }
    /** The horizontal part of a direction, as a unit vector; falls back to where the caster is looking. */
    private static Vec3d horizontal(Vec3d v, LivingEntity caster) {
        Vec3d h = new Vec3d(v.x, 0, v.z);
        if (h.lengthSquared() < 1.0E-4) h = Vec3d.fromPolar(0.0F, caster.getYaw());
        return h.normalize();
    }
}