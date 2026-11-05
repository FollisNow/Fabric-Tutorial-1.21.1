package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.TentacleFormation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Server side of the tentacles. The area around the boss is cut into four quarters, one per tentacle (the first one
 * points at the boss' target when the attack starts, the others follow at 90 degree steps). A tentacle only ever
 * deals with foes standing in its own quarter, so the four never go for the same target: a foe belongs to exactly one
 * quarter at any moment, and a foe that is already being lashed by one tentacle is never picked by another.
 *
 * Each tentacle runs its own little cycle: idle (look for a foe in its quarter) -> windup (rears back, follows its
 * victim; the clients animate this from the command sent here) -> lash (the aim is locked a few ticks earlier, so it
 * can be dodged) -> hit -> recover. The cycle lengths come from TentacleFormation so the damage lands when the tip does.
 */
public class TentacleGoal extends Goal {

    private static final int COOLDOWN = 100;
    private static final float DAMAGE = 14f;
    private static final double KNOCKBACK = 1.3;

    private static final int IDLE = 0, WINDUP = 1, LASH = 2, RECOVER = 3;

    private final GoldenSwarmEntity boss;
    private final int[] state = new int[TentacleFormation.TENTACLES];
    private final int[] until = new int[TentacleFormation.TENTACLES];
    private final LivingEntity[] victim = new LivingEntity[TentacleFormation.TENTACLES];
    private final Vec3d[] aim = new Vec3d[TentacleFormation.TENTACLES];
    private double theta0;   // direction of the first tentacle, radians
    private int tick;

    public TentacleGoal(GoldenSwarmEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        // "is there a foe within reach?" is decided in one place: the entity (TENTACLE_REACH)
        return boss.canStartAttack(GoldenSwarmEntity.PATTERN_TENTACLES)
                && boss.isPatternUsable(GoldenSwarmEntity.PATTERN_TENTACLES);
    }

    @Override
    public boolean shouldContinue() {
        return tick < TentacleFormation.END + 8;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void start() {
        tick = 0;
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {
            state[k] = IDLE;
            victim[k] = null;
        }

        // the first tentacle points at the boss' target (or wherever the boss is facing)
        LivingEntity target = boss.getTarget();
        double angle = target != null
                ? Math.atan2(target.getZ() - boss.getZ(), target.getX() - boss.getX())
                : Math.toRadians(boss.bodyYaw + 90.0);
        int degrees = Math.floorMod((int) Math.round(Math.toDegrees(angle)), 360);
        theta0 = Math.toRadians(degrees);   // use the rounded value: the clients get the same number

        boss.getNavigation().stop();
        boss.beginPattern(GoldenSwarmEntity.PATTERN_TENTACLES, 0, boss.getPos(), 1, false, degrees);
    }

    @Override
    public void tick() {
        tick++;
        if (tick < TentacleFormation.GROW) return;

        // no new lash may start if it could not finish before the tentacles retract
        int lastStart = TentacleFormation.END - (TentacleFormation.WINDUP + TentacleFormation.LASH + TentacleFormation.RECOVER);

        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {
            switch (state[k]) {
                case IDLE -> {
                    // each tentacle looks around every 4th tick, at its own offset, so they do not all react together
                    if (tick <= lastStart && (tick + k) % 4 == 0) {
                        LivingEntity found = pickVictim(k);
                        if (found != null) {
                            victim[k] = found;
                            aim[k] = aimPoint(found);
                            boss.commandTentacle(k, aim[k]);   // the clients start this tentacle's windup now
                            state[k] = WINDUP;
                            until[k] = tick + TentacleFormation.WINDUP;
                        }
                    }
                }
                case WINDUP -> {
                    // follow the victim, then lock the aim 4 ticks before the lash (the clients stop following then too)
                    if (tick == until[k] - 4 - 1 && victim[k] != null && victim[k].isAlive()) {
                        aim[k] = aimPoint(victim[k]);
                        boss.updateTentacleAim(k, aim[k]);
                    }
                    if (tick >= until[k]) {
                        state[k] = LASH;
                        until[k] = tick + TentacleFormation.LASH;
                        boss.getWorld().playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                                SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.HOSTILE, 1.5f, 1.4f);
                    }
                }
                case LASH -> {
                    if (tick >= until[k]) {
                        hit(k);
                        state[k] = RECOVER;
                        until[k] = tick + TentacleFormation.RECOVER;
                    }
                }
                default -> {
                    if (tick >= until[k]) {
                        state[k] = IDLE;
                        victim[k] = null;
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        boss.finishAttack(COOLDOWN);
        boss.endPattern();
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) victim[k] = null;
    }

    // ------------------------------------------------------------------------------------------

    /** Which tentacle's quarter a position is in: 0..3, a quarter centred on the tentacle's own direction. */
    private int sectorOf(double x, double z) {
        double relative = Math.atan2(z - boss.getZ(), x - boss.getX()) - theta0;
        int sector = (int) Math.floor((relative + Math.PI / 4.0) / (Math.PI / 2.0));
        return Math.floorMod(sector, TentacleFormation.TENTACLES);
    }

    private List<LivingEntity> foesInReach() {
        double reach = TentacleFormation.REACH;
        return boss.getWorld().getEntitiesByClass(LivingEntity.class, boss.getBoundingBox().expand(reach),
                e -> e != boss && e.isAlive() && boss.isFoe(e) && boss.distanceTo(e) <= reach);
    }

    /** The nearest foe in tentacle k's own quarter that no other tentacle is busy with. */
    private LivingEntity pickVictim(int k) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : foesInReach()) {
            if (sectorOf(candidate.getX(), candidate.getZ()) != k) continue;
            if (isBeingLashed(candidate, k)) continue;
            double d = boss.squaredDistanceTo(candidate);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    /** True if a tentacle other than k is currently going for this entity. */
    private boolean isBeingLashed(LivingEntity e, int k) {
        for (int other = 0; other < TentacleFormation.TENTACLES; other++) {
            if (other != k && state[other] != IDLE && victim[other] == e) return true;
        }
        return false;
    }

    private static Vec3d aimPoint(LivingEntity e) {
        return e.getBoundingBox().getCenter();
    }

    /** The lash lands: hurt and throw back every foe near the tip, but only those in this tentacle's own quarter. */
    private void hit(int k) {
        if (!(boss.getWorld() instanceof ServerWorld world) || aim[k] == null) return;

        // the tip lands on the aim point, but never further than the tentacle reaches
        Vec3d tip = aim[k];
        double dx = tip.x - boss.getX(), dz = tip.z - boss.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > TentacleFormation.REACH) {
            tip = new Vec3d(boss.getX() + dx / len * TentacleFormation.REACH, tip.y, boss.getZ() + dz / len * TentacleFormation.REACH);
        }

        double radius = TentacleFormation.HIT_RADIUS;
        Box area = new Box(tip, tip).expand(radius);
        for (LivingEntity foe : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && boss.isFoe(e))) {
            if (sectorOf(foe.getX(), foe.getZ()) != k) continue;   // not this tentacle's business
            if (distanceSqToBox(foe.getBoundingBox(), tip) > radius * radius) continue;

            foe.damage(world.getDamageSources().mobAttack(boss), DAMAGE * boss.getDamageMultiplier());
            foe.takeKnockback(KNOCKBACK, boss.getX() - foe.getX(), boss.getZ() - foe.getZ());   // thrown away from the boss
            foe.setVelocity(foe.getVelocity().add(0.0, 0.3, 0.0));
            foe.velocityModified = true;
        }

        world.playSound(null, tip.x, tip.y, tip.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.7f, 1.6f);
        world.spawnParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 12, 0.4, 0.3, 0.4, 0.2);
    }

    /** Squared distance from a point to the closest point of a box (0 if the point is inside it). */
    private static double distanceSqToBox(Box box, Vec3d p) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}