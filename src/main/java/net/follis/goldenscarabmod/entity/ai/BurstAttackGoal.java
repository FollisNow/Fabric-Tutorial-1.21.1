package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.BurstFormation;
import net.follis.goldenscarabmod.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Server side of the fragmentation burst. The boss charges for BurstFormation.WINDUP ticks, then an
 * explosion front expands from its chest (BurstFormation.shellRadius, the same curve the shards follow
 * on the clients). Every foe the front reaches is hurt once, harder when close, and only if the boss
 * can see it: the shards do not pass through walls.
 */
public class BurstAttackGoal extends Goal {

    private static final int COOLDOWN = 70;
    private static final float DAMAGE = 14f;              // at point blank; falls to half at the edge
    private static final int END = BurstFormation.WINDUP + BurstFormation.EXPAND + 6;

    // The countdown sound. Swap it for any other SoundEvent you like (a bell, a chain, a custom sound).
    private static final SoundEvent TICK_SOUND = ModSounds.GOLDEN_SWARM_WINDUP_BURST;
    private static final float TICK_VOLUME = 2.5f;   // above 1.0 it carries further, handy so players hear it from cover
    private static final float TICK_PITCH = 0.7f;
    private static final SoundEvent BURST_SOUND = SoundEvents.BLOCK_ANVIL_LAND;
    private static final float BURST_VOLUME = 1.2f;   // above 1.0 it carries further, handy so players hear it from cover
    private static final float BURST_PITCH = 0.6f;

    private final GoldenSwarmEntity boss;
    private final Set<LivingEntity> alreadyHit = new HashSet<>();
    private LivingEntity target;
    private int tick;

    public BurstAttackGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // range and line of sight are decided in one place: the entity (BURST_MAX_DIST)
        return boss.canStartAttack(GoldenSwarmEntity.PATTERN_BURST)
                && boss.isPatternUsable(GoldenSwarmEntity.PATTERN_BURST);
    }

    @Override
    public boolean shouldContinue() {
        return tick < END;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        tick = 0;
        alreadyHit.clear();
        target = boss.getTarget();
        boss.getNavigation().stop();
        // the burst has no aim point or flight: the extra values are unused
        boss.beginPattern(GoldenSwarmEntity.PATTERN_BURST, 0, boss.getPos(), 1, false);
    }

    @Override
    public void tick() {
        tick++;

        if (target != null && target.isAlive() && tick < BurstFormation.WINDUP) {
            boss.getLookControl().lookAt(target, 40.0f, 40.0f);
        }

        // ticking bomb: the same sound at 1s, 2s and 3s, then the burst itself at 4s
        if (tick < BurstFormation.WINDUP && tick % BurstFormation.TICK_INTERVAL == 0) {
            playTick();
        }
        if (tick == BurstFormation.WINDUP) {
            burstEffects();
        }
        if (tick >= BurstFormation.WINDUP && tick <= BurstFormation.WINDUP + BurstFormation.EXPAND) {
            sweepShell();
        }
    }

    @Override
    public void stop() {
        boss.finishAttack(COOLDOWN);
        boss.endPattern();
        target = null;
    }

    private Vec3d centre() {
        return boss.getPos().add(0.0, BurstFormation.CENTER_HEIGHT, 0.0);
    }

    private void playTick() {
        boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                TICK_SOUND, SoundCategory.HOSTILE, TICK_VOLUME, TICK_PITCH);
    }

    private void burstEffects() {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;
        Vec3d c = centre();
        world.playSound(null, c.x, c.y, c.z, BURST_SOUND, SoundCategory.HOSTILE, BURST_VOLUME, BURST_PITCH);
        world.spawnParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 80, 1.5, 1.0, 1.5, 0.5);
    }

    /** Hurts every foe the expanding front has reached since the last tick. */
    private void sweepShell() {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;

        float radius = BurstFormation.shellRadius(tick);
        Vec3d c = centre();
        Box area = new Box(c, c).expand(radius + 1.0);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e) && !alreadyHit.contains(e))) {
            double d2 = distanceSqToBox(victim.getBoundingBox(), c);
            if (d2 > radius * radius) continue;
            if (!boss.canSee(victim)) continue;   // shards stop at walls

            alreadyHit.add(victim);
            double falloff = 1.0 - 0.5 * Math.min(1.0, Math.sqrt(d2) / BurstFormation.REACH);
            victim.damage(world.getDamageSources().mobAttack(boss), (float) (DAMAGE * falloff));
            victim.takeKnockback(1.0, c.x - victim.getX(), c.z - victim.getZ());
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