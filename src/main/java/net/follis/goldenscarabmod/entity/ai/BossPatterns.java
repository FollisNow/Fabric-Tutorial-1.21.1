package net.follis.goldenscarabmod.entity.ai;

import net.follis.goldenscarabmod.entity.custom.KingInYellowEntity;
import net.follis.goldenscarabmod.pattern.FireRingPattern;
import net.follis.goldenscarabmod.pattern.GoldGuardsPattern;
import net.follis.goldenscarabmod.pattern.LightPattern;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** The boss's attacks. */
public final class BossPatterns {
    // ---- let there be light ----
    private static final double LIGHT_RANGE = 22.0;           // it flashes when its target is closer than this

    // ---- fire ring ----
    private static final double RING_TRIGGER_DISTANCE = 12.0; // it rings when a foe is closer than this
    private static final double RING_BLOCKS_PER_SECOND = 2.5;
    private static final double RING_RADIUS = 14.0;

    // ---- light swords ----
    private static final double SWORD_RANGE = 30.0;
    private static final int SWORD_TARGETS = 3;               // how many foes are attacked at once
    private static final int SWORD_COUNT = 6;                 // swords per foe
    private static final int SWORD_INTERVAL = 8;              // ticks between two swords appearing
    private static final int SWORD_LANDING_TICKS = 15;        // time for the swords to fall and land after they are released

    // ---- gold guards ----
    private static final int GUARD_COUNT = 3;                 // guards per summoning
    private static final int MAX_GUARDS = 6;                  // no more once this many are alive

    private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0F, 0.78F, 0.2F), 1.5F);

    private BossPatterns() {}

    // ---- let there be light: the whole room is whited out, unless you look away ----
    public static final BossPattern LIGHT = new BossPattern(1, 40, 30, 200, 3) {
        @Override
        public boolean canUse(KingInYellowEntity boss, LivingEntity target) {
            return boss.squaredDistanceTo(target) <= LIGHT_RANGE * LIGHT_RANGE && boss.getVisibilityCache().canSee(target);
        }

        @Override
        public void windUpTick(ServerWorld world, KingInYellowEntity boss, LivingEntity target, int tick, float progress) {
            LightPattern.telegraph(world, face(boss), progress, tick);
        }

        @Override
        public void execute(ServerWorld world, KingInYellowEntity boss, LivingEntity target) {
            LightPattern.flash(world, boss, face(boss), boss::isFoe);
        }
    };

    // ---- fire ring: the boss sinks into the ground and the wave rises from the pool; get up high or behind a wall ----
    public static final BossPattern FIRE_RING = new BossPattern(2, KingInYellowEntity.GO_UNDERGROUND_TICKS + 4, 40, 160, 3) {
        @Override
        public boolean canUse(KingInYellowEntity boss, LivingEntity target) {
            return !boss.getFoesNear(RING_TRIGGER_DISTANCE).isEmpty();
        }

        @Override
        public void execute(ServerWorld world, KingInYellowEntity boss, LivingEntity target) {
            FireRingPattern.cast(world, boss, boss::isFoe, RING_BLOCKS_PER_SECOND, RING_RADIUS);
        }

        @Override
        public boolean hidesBoss() {
            return true;
        }

        /** The boss stays under for as long as the wave has left to run. */
        @Override
        public int ticksUntilDone(KingInYellowEntity boss) {
            return FireRingPattern.remainingTicks(boss);
        }
    };

    // ---- gold guards: reinforcements that hunt its target ----
    public static final BossPattern GUARDS = new BossPattern(4, 30, 20, 400, 2) {
        @Override
        public boolean canUse(KingInYellowEntity boss, LivingEntity target) {
            return boss.liveGuardCount() <= MAX_GUARDS - GUARD_COUNT;
        }

        @Override
        public void windUpTick(ServerWorld world, KingInYellowEntity boss, LivingEntity target, int tick, float progress) {
            world.spawnParticles(GOLD, boss.getX(), boss.getY() + 0.1, boss.getZ(), 3, 1.5, 0.1, 1.5, 0.0);
            if (tick % 8 == 0) {
                world.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                        SoundCategory.HOSTILE, 2.0F, 0.6F + progress);
            }
        }

        @Override
        public void execute(ServerWorld world, KingInYellowEntity boss, LivingEntity target) {
            GoldGuardsPattern.summon(world, boss, target, KingInYellowEntity.GUARD_TYPE, GUARD_COUNT);
        }
    };

    public static final List<BossPattern> ALL = List.of(LIGHT, FIRE_RING, GUARDS);

    // ---- helpers ----

    /** The boss's face, where the flash gathers. */
    private static Vec3d face(KingInYellowEntity boss) {
        return boss.getEyePos().add(boss.getRotationVec(1.0F).multiply(1.0));
    }

    /** Its target first, then the nearest other foes, up to SWORD_TARGETS in all. */
    private static List<LivingEntity> victims(KingInYellowEntity boss, LivingEntity target) {
        List<LivingEntity> victims = new ArrayList<>();
        victims.add(target);
        for (LivingEntity e : boss.getFoesNear(SWORD_RANGE)) {
            if (victims.size() >= SWORD_TARGETS) break;
            if (e != target) victims.add(e);
        }
        return victims;
    }
}