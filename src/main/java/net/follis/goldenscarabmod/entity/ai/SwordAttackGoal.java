package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.DominionEntity;
import net.follis.goldenscarabmod.pattern.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Picks one of the sword patterns every so often, as long as the boss has a target and enough swords left. */
public class SwordAttackGoal extends Goal {
    private enum Attack {
        // cost = swords needed on the body; fromBody = the pattern takes them from the body itself, otherwise they are just a cost
        //     cost  min  max  fromBody
        ORBIT(8, 0, 30, true),
        STABS(4, 3, 16, false),
        WALL(12, 5, 32, true),
        RAIN(5, 0, 22, true),
        SHIELD(10, 0, 20, true),
        BEAM(10, 6, 30, true),
        STAR(SwordStarPattern.STARS * SwordStarPattern.SWORDS_PER_STAR, 6, 30, true),
        CHANDELIER(10, 0, 14, true),;

        final int cost;
        final double minRange, maxRange;
        final boolean fromBody;

        Attack(int cost, double minRange, double maxRange, boolean fromBody) {
            this.cost = cost;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.fromBody = fromBody;
        }
    }

    private static final int FIRST_DELAY = 30;     // ticks between getting a target and the first attack
    private static final int PAUSE = 30;           // ticks between the end of an attack and the next one
    private static final int MAX_BUSY = 100;       // an attack never blocks the next one for longer than this
    private static final int SHIELD_TICKS = 200;

    private final DominionEntity boss;
    private int nextAttackAge;
    private int shieldUntil;
    private Attack last;

    public SwordAttackGoal(DominionEntity boss) {
        this.boss = boss;   // no controls: this goal runs alongside moving and looking
    }

    @Override
    public boolean canStart() {
        LivingEntity target = boss.getTarget();
        return target != null && target.isAlive() && DominionEntity.VALID_TARGETS.test(target);
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void start() {
        boss.ensureFlock();
        nextAttackAge = Math.max(nextAttackAge, boss.age + FIRST_DELAY);
    }

    @Override
    public void tick() {
        if (boss.age < nextAttackAge) return;
        LivingEntity target = boss.getTarget();
        if (target == null || !(boss.getWorld() instanceof ServerWorld world)) return;

        double distance = boss.distanceTo(target);
        List<Attack> options = new ArrayList<>();
        for (Attack attack : Attack.values()) {
//            if (attack != Attack.STAR) continue;   // TEMPORARY
            if (boss.getSwordCount() < attack.cost) continue;
            if (distance < attack.minRange || distance > attack.maxRange) continue;
            if (attack == Attack.SHIELD && boss.age < shieldUntil) continue;
            if (attack == Attack.BEAM && !boss.canSee(target)) continue;   // the beam does not go through walls
            if ((attack == Attack.BEAM || attack == Attack.STAR) && !boss.canSee(target)) continue;   // these do not go through walls
            if (attack == Attack.CHANDELIER && !boss.isOnGround()) continue;   // it can only start from the ground
            options.add(attack);
        }
        if (options.size() > 1) options.remove(last);   // do not repeat the same attack twice in a row
        if (options.isEmpty()) return;

        Attack attack = options.get(boss.getRandom().nextInt(options.size()));
        if (!attack.fromBody && !boss.takeSwords(attack.cost)) return;
        SwordPool pool = SwordPool.limit(boss, attack.cost);   // a pattern may take at most this many swords off the body
        last = attack;

        Vec3d toTarget = target.getPos().subtract(boss.getPos());
        int duration = switch (attack) {
            case ORBIT -> SwordPatterns.orbitingSwords(world, boss, DominionEntity.VALID_TARGETS, pool);
            case STABS -> SwordPatterns.groundStabs(world, boss, toTarget, DominionEntity.VALID_TARGETS);
            case WALL -> SwordPatterns.swordWall(world, boss, toTarget, DominionEntity.VALID_TARGETS, pool);
            case RAIN -> GoldenSwordsPattern.summon(world, boss, target, DominionEntity.VALID_TARGETS, pool) + 20;
            case SHIELD -> {
                shieldUntil = boss.age + SwordShield.start(world, boss, SHIELD_TICKS, p -> true, pool);
                yield 20;   // the shield stays up while the boss goes on attacking
            }
            case BEAM -> boss.startCannon(target);
            case STAR -> SwordStarPattern.start(world, boss, target, DominionEntity.VALID_TARGETS, pool);
            case CHANDELIER -> boss.startChandelier();
        };
        nextAttackAge = boss.age + Math.min(duration, MAX_BUSY) + PAUSE;
    }
}