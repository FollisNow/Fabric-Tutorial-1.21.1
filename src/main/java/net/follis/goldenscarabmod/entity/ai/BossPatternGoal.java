package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.KingInYellowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Makes the boss pick a pattern, warn with its wind-up, cast it, and recover. It stands still and faces its target throughout. */
public class BossPatternGoal extends Goal {
    /** Pause between two patterns. */
    private static final int GLOBAL_COOLDOWN_TICKS = 50;
    /** Cooldown of a pattern that was cancelled before it landed. */
    private static final int CANCELLED_COOLDOWN_TICKS = 40;

    private enum Stage { WIND_UP, HOLD, RECOVERY }

    private final KingInYellowEntity boss;
    private final Map<BossPattern, Long> readyAt = new HashMap<>();   // game time when each pattern may be used again
    private long nextPatternAt;
    @Nullable private BossPattern lastPattern;
    @Nullable private BossPattern forced;
    @Nullable private BossPattern chosen;    // picked in canStart, used in start
    @Nullable private BossPattern current;
    private Stage stage = Stage.WIND_UP;
    private int ticks;

    public BossPatternGoal(KingInYellowEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /** For testing: the next pattern is this one, whatever else is possible. */
    public void force(BossPattern pattern) {
        this.forced = pattern;
    }

    @Override
    public boolean canStart() {
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (forced == null && boss.getWorld().getTime() < nextPatternAt) return false;
        chosen = forced != null ? forced : choose(target);
        return chosen != null;
    }

    @Override
    public boolean shouldContinue() {
        return current != null && boss.isAlive();
    }

    @Override
    public void start() {
        current = chosen;
        forced = null;
        stage = Stage.WIND_UP;
        ticks = 0;
        boss.getNavigation().stop();
        boss.setCasting(current.id);
        if (current.hidesBoss()) boss.sink();
    }

    @Override
    public void tick() {
        if (current == null || !(boss.getWorld() instanceof ServerWorld world)) return;
        LivingEntity target = boss.getTarget();

        // the boss watches the pattern's focus point, or else its target
        if (target != null) {
            Vec3d focus = current.focus(boss, target);
            if (focus != null) boss.getLookControl().lookAt(focus.x, focus.y, focus.z, 30.0F, 30.0F);
            else boss.getLookControl().lookAt(target, 30.0F, 30.0F);
        }
        ticks++;

        switch (stage) {
            case WIND_UP -> {
                if (target == null || !target.isAlive()) {   // the target is gone: cancel
                    finish(false);
                    return;
                }
                current.windUpTick(world, boss, target, ticks, Math.min(1.0F, ticks / (float) current.windUpTicks));
                // a pattern that hides the boss waits until the boss is completely out of sight
                boolean ready = ticks >= current.windUpTicks && (!current.hidesBoss() || boss.isUnderground());
                if (ready) {
                    current.execute(world, boss, target);
                    stage = Stage.HOLD;
                    ticks = 0;
                }
            }
            case HOLD -> {
                // wait out the pattern's fixed hold time, and until the attack itself is nearly over
                int lead = current.hidesBoss() ? KingInYellowEntity.RISE_LEAD_TICKS : 0;
                if (ticks >= current.holdTicks(boss) && current.ticksUntilDone(boss) <= lead) {
                    if (current.hidesBoss()) boss.rise();
                    stage = Stage.RECOVERY;
                    ticks = 0;
                }
            }
            case RECOVERY -> {
                // recovery only starts counting once the boss is standing again
                if (ticks >= current.recoveryTicks && (!current.hidesBoss() || boss.isOnSurface())) {
                    finish(true);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (current != null) finish(false);   // interrupted
        boss.setCasting(0);
    }

    private void finish(boolean completed) {
        long now = boss.getWorld().getTime();
        if (current != null) {
            if (current.hidesBoss()) boss.rise();   // whatever happened, the boss does not stay underground
            readyAt.put(current, now + (completed ? current.cooldownTicks : CANCELLED_COOLDOWN_TICKS));
            lastPattern = current;
        }
        nextPatternAt = now + GLOBAL_COOLDOWN_TICKS;
        current = null;
    }

    /** A random pattern among those that are ready and usable, weighted, avoiding a repeat when there is a choice. */
    @Nullable
    private BossPattern choose(LivingEntity target) {
        long now = boss.getWorld().getTime();
        List<BossPattern> options = new ArrayList<>();
        for (BossPattern p : BossPatterns.ALL) {
            if (now >= readyAt.getOrDefault(p, 0L) && p.canUse(boss, target)) options.add(p);
        }
        if (options.size() > 1) options.remove(lastPattern);
        if (options.isEmpty()) return null;

        int total = 0;
        for (BossPattern p : options) total += p.weight;
        int roll = boss.getRandom().nextInt(total);
        for (BossPattern p : options) {
            roll -= p.weight;
            if (roll < 0) return p;
        }
        return options.get(0);
    }
}