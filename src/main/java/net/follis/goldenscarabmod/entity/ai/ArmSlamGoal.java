package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.ArmSlamFormation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.EnumSet;

/**
 * Server side of the arm slam. The boss stays rooted while the swarm turns into a giant arm (on the clients). The
 * fist follows the target until ArmSlamFormation.LOCK, when the impact point is fixed (so the slam can be dodged),
 * and lands at ArmSlamFormation.IMPACT, where everything near the impact point is hurt and thrown.
 * The screen shake and the sheets popping off are played by the clients themselves.
 */
public class ArmSlamGoal extends Goal {

    private static final int COOLDOWN = 90;
    private static final float RADIUS = ArmSlamFormation.BLAST_RADIUS;      // blast radius around the impact point
    private static final float DAMAGE = 28f;       // at the centre; half of it at the edge
    private static final int END = ArmSlamFormation.IMPACT + 12;

    private final GoldenSwarmEntity boss;
    private LivingEntity target;
    private Vec3d impact = Vec3d.ZERO;
    private int tick;

    public ArmSlamGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // range and line of sight are decided in one place: the entity (ARM_MIN_DIST / ARM_MAX_DIST)
        return boss.canStartAttack(GoldenSwarmEntity.PATTERN_ARM_SLAM)
                && boss.isPatternUsable(GoldenSwarmEntity.PATTERN_ARM_SLAM);
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
        target = boss.getTarget();
        boss.getNavigation().stop();
        impact = groundPoint(target);
        boss.beginPattern(GoldenSwarmEntity.PATTERN_ARM_SLAM, 0, impact, ArmSlamFormation.SLAM_TICKS, false);
    }

    @Override
    public void tick() {
        tick++;

        if (target != null && target.isAlive()) {
            boss.getLookControl().lookAt(target, 40.0f, 40.0f);
            // the fist follows the target while it is raised
            if (tick < ArmSlamFormation.LOCK && tick % 4 == 0) {
                impact = groundPoint(target);
                boss.setPatternTarget(impact);
            }
        }

        if (tick == ArmSlamFormation.LOCK) {
            if (target != null && target.isAlive()) impact = groundPoint(target);
            boss.setPatternPath(impact, ArmSlamFormation.SLAM_TICKS, false);   // final: from now on it can be dodged
        }

        if (tick == ArmSlamFormation.IMPACT) {
            slam();
        }
    }

    @Override
    public void stop() {
        boss.finishAttack(COOLDOWN);
        boss.endPattern();
        target = null;
    }

    // ------------------------------------------------------------------------------

    /** The ground under an entity's feet, within the arm's reach of the boss. */
    private Vec3d groundPoint(LivingEntity e) {
        Vec3d feet = e.getPos();
        double dx = feet.x - boss.getX(), dz = feet.z - boss.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        double x = feet.x, z = feet.z;
        if (len > ArmSlamFormation.REACH) {   // the arm only reaches so far: land as close as it can get
            x = boss.getX() + dx / len * ArmSlamFormation.REACH;
            z = boss.getZ() + dz / len * ArmSlamFormation.REACH;
        }
        BlockHitResult hit = boss.getWorld().raycast(new RaycastContext(
                new Vec3d(x, feet.y + 0.6, z), new Vec3d(x, feet.y - 8.0, z),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
        return hit.getType() == HitResult.Type.MISS ? new Vec3d(x, feet.y, z) : hit.getPos();
    }

    private void slam() {
        if (!(boss.getWorld() instanceof ServerWorld world)) return;

        Box area = new Box(impact, impact).expand(RADIUS);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e))) {
            double d2 = distanceSqToBox(victim.getBoundingBox(), impact);
            if (d2 > RADIUS * RADIUS) continue;

            double falloff = 1.0 - 0.5 * Math.sqrt(d2) / RADIUS;
            victim.damage(world.getDamageSources().mobAttack(boss), (float) (DAMAGE * falloff * boss.getDamageMultiplier()));
            victim.takeKnockback(1.4, impact.x - victim.getX(), impact.z - victim.getZ());
            victim.setVelocity(victim.getVelocity().add(0.0, 0.5, 0.0));   // thrown up a little by the shock
            victim.velocityModified = true;
        }

        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 2.5f, 0.5f);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 2.0f, 0.7f);
        world.spawnParticles(ParticleTypes.CRIT, impact.x, impact.y + 0.3, impact.z, 80, 2.0, 0.3, 2.0, 0.4);
        world.spawnParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.2, impact.z, 30, 2.5, 0.2, 2.5, 0.1);
    }

    /** Squared distance from a point to the closest point of a box (0 if the point is inside it). */
    private static double distanceSqToBox(Box box, Vec3d p) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}