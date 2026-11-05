package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.CloneSummonFormation;
import net.follis.goldenscarabmod.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Server side of the clone summon. The boss charges (the sheets gather into a ball on the clients) and at
 * CloneSummonFormation.GATHER the clone is spawned at a free spot nearby. The clone itself, its health, its
 * lifetime and its recombination are handled by the entity.
 */
public class CloneSummonGoal extends Goal {

    private static final int COOLDOWN = 100;
    private static final int END = CloneSummonFormation.GATHER + 6;
    private static final SoundEvent CLONE_SOUND = ModSounds.GOLDEN_SWARM_CLONE;

    private final GoldenSwarmEntity boss;
    private LivingEntity target;
    private Vec3d spot = Vec3d.ZERO;
    private int tick;
    private boolean failed;

    public CloneSummonGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // no clone of a clone, no second clone, not in the air, a target in range: all decided by the entity
        return boss.canStartAttack(GoldenSwarmEntity.PATTERN_CLONE)
                && boss.isPatternUsable(GoldenSwarmEntity.PATTERN_CLONE);
    }

    @Override
    public boolean shouldContinue() {
        return !failed && tick < END;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        tick = 0;
        failed = false;
        target = boss.getTarget();
        boss.getNavigation().stop();

        Vec3d found = pickSpot();
        if (found == null) {
            failed = true;   // no room for a clone: skip the attack (stop() sets a short cooldown)
            return;
        }
        spot = found;
        // the aim point is unused by this pattern, the clone's position is sent when it spawns
        boss.beginPattern(GoldenSwarmEntity.PATTERN_CLONE, 0, spot, 1, false);
    }

    @Override
    public void tick() {
        if (failed) return;
        tick++;

        if (target != null && target.isAlive()) {
            boss.getLookControl().lookAt(target, 40.0f, 40.0f);
        }

        if (tick == CloneSummonFormation.GATHER) {
            if (boss.spawnClone(spot) && boss.getWorld() instanceof ServerWorld world) {
                world.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                        CLONE_SOUND, SoundCategory.HOSTILE, 1.0f, 1.2f);
                world.spawnParticles(ParticleTypes.CRIT, spot.x, spot.y + 1.0, spot.z, 30, 0.5, 0.8, 0.5, 0.2);
            }
        }
    }

    @Override
    public void stop() {
        boss.finishAttack(failed ? 30 : COOLDOWN);
        boss.endPattern();
        target = null;
    }

    /** A free standing spot 3-5 blocks from the boss, with a clear line to it. Null if none was found. */
    private Vec3d pickSpot() {
        World world = boss.getWorld();
        Vec3d from = boss.getPos();

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = 3.0 + boss.getRandom().nextDouble() * 2.0;
            double x = from.x + Math.cos(angle) * dist;
            double z = from.z + Math.sin(angle) * dist;

            BlockHitResult floor = world.raycast(new RaycastContext(new Vec3d(x, from.y + 2.0, z), new Vec3d(x, from.y - 4.0, z),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
            if (floor.getType() == HitResult.Type.MISS) continue;
            Vec3d candidate = floor.getPos();

            // room for a body of the clone's (smaller) size
            if (!world.isSpaceEmpty(boss, boss.getDimensions(boss.getPose())
                    .scaled(GoldenSwarmEntity.CLONE_SCALE).getBoxAt(candidate).offset(0.0, 0.01, 0.0))) continue;

            // nothing solid between the boss' chest and the clone's chest
            BlockHitResult line = world.raycast(new RaycastContext(from.add(0.0, 1.2, 0.0), candidate.add(0.0, 1.2, 0.0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
            if (line.getType() != HitResult.Type.MISS) continue;

            return candidate;
        }
        return null;
    }
}