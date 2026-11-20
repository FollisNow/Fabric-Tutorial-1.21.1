package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.KingInYellowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/** One attack of the boss: a wind-up that warns the players, the attack itself, then a moment of recovery. */
public abstract class BossPattern {
    /** Stored on the boss while the pattern is being cast (0 means "not casting"), so the client can pick an animation. */
    public final int id;
    public final int windUpTicks;
    public final int recoveryTicks;
    /** Ticks before this same pattern can be chosen again. */
    public final int cooldownTicks;
    /** How likely it is to be chosen, compared with the other patterns that can be used right now. */
    public final int weight;

    protected BossPattern(int id, int windUpTicks, int recoveryTicks, int cooldownTicks, int weight) {
        this.id = id;
        this.windUpTicks = windUpTicks;
        this.recoveryTicks = recoveryTicks;
        this.cooldownTicks = cooldownTicks;
        this.weight = weight;
    }

    public abstract boolean canUse(KingInYellowEntity boss, LivingEntity target);

    /** Called every tick of the wind-up. progress runs from 0 to 1. */
    public void windUpTick(ServerWorld world, KingInYellowEntity boss, LivingEntity target, int tick, float progress) {}

    /** The moment the pattern lands. */
    public abstract void execute(ServerWorld world, KingInYellowEntity boss, LivingEntity target);

    /**
     * True if the boss sinks into the ground at the start of this pattern, stays out of sight while it
     * lasts, and comes back out as it ends. The wind-up should be at least as long as going under.
     */
    public boolean hidesBoss() {
        return false;
    }

    /** A fixed number of ticks after execute during which the boss stays where it is, whatever else is going on. */
    public int holdTicks(KingInYellowEntity boss) {
        return 0;
    }

    /**
     * How many ticks the attack still has to run, for an effect whose length is not fixed: the wave
     * still travelling, say. 0 means it is over. A pattern that hides the boss keeps it underground until the
     * attack is nearly over, and starts bringing it back KingInYellowEntity.RISE_LEAD_TICKS before the end.
     */
    public int ticksUntilDone(KingInYellowEntity boss) {
        return 0;
    }

    /** A point the boss keeps looking at for the whole pattern, instead of its target. Null means the target. */
    @Nullable
    public Vec3d focus(KingInYellowEntity boss, LivingEntity target) {
        return null;
    }
}