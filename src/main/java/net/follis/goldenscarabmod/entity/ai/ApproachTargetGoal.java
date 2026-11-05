package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.EnumSet;

/**
 * Walks the boss towards its target until it is within stopDistance. This replaces the melee goal:
 * the boss does not hit anything by itself, it just gets into range for its sheet attacks.
 * Attack goals have a higher priority, so they take over as soon as they start.
 */
public class ApproachTargetGoal extends Goal {

    private final PathAwareEntity self;
    private final double speed;
    private final double stopDistance;
    private int repathIn;

    public ApproachTargetGoal(PathAwareEntity self, double speed, double stopDistance) {
        this.self = self;
        this.speed = speed;
        this.stopDistance = stopDistance;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity t = self.getTarget();
        return t != null && t.isAlive() && self.squaredDistanceTo(t) > stopDistance * stopDistance;
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void start() {
        repathIn = 0;
    }

    @Override
    public void tick() {
        LivingEntity t = self.getTarget();
        if (t == null) return;
        self.getLookControl().lookAt(t, 30.0f, 30.0f);
        if (--repathIn <= 0) {
            repathIn = 10;
            self.getNavigation().startMovingTo(t, speed);
        }
    }

    @Override
    public void stop() {
        self.getNavigation().stop();
    }
}