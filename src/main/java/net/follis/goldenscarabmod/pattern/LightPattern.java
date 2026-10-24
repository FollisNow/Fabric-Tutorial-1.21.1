package net.follis.goldenscarabmod.pattern;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.follis.goldenscarabmod.network.FlashPayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** "Let there be light": a wind-up that foretells the flash, then a flash that blinds whoever is looking at it. */
public final class LightPattern {
    // ---- tuning ----
    public static final double FLASH_RADIUS = 24.0;
    /** Entities whose look direction is within this cosine of the flash are hit: -0.2 is about 100 degrees. */
    public static final double FACING_DOT = -0.2;

    /** Ticks of fully white screen right after the flash (40 = 2 seconds). */
    public static final int WHITE_TICKS = 40;
    /** Ticks the white then takes to fade out. Set to 1 for a hard cut. */
    public static final int WHITE_FADE_TICKS = 10;
    /** Ticks of blindness once the white is gone (40 = 2 seconds). */
    public static final int BLIND_TICKS = 40;

    private static final double COLLAPSE_START_RADIUS = 2.2;
    private static final int BURST_PARTICLES = 40;

    private LightPattern() {}

    /** Call every tick of the wind-up. progress runs from 0 (start) to 1 (the flash). */
    public static void telegraph(ServerWorld world, Vec3d origin, float progress, int tick) {
        Random random = world.getRandom();
        double radius = 0.15 + COLLAPSE_START_RADIUS * (1.0 - progress);
        int count = 2 + (int) (progress * 3);
        for (int i = 0; i < count; i++) {
            Vec3d dir = randomDirection(random);
            Vec3d p = origin.add(dir.multiply(radius));
            // with a count of 0 the offset is used as a velocity: the particle drifts toward the centre
            world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, -dir.x, -dir.y, -dir.z, 0.12);
        }
        if (tick % 4 == 0) {
            world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.PLAYERS, 2.0F, 0.6F + 1.4F * progress);   // rising pitch: the noise indicator
        }
    }

    /**
     * The flash itself. caster is never affected (pass null to affect everyone, which is handy for testing);
     * affects can exclude more entities, such as the boss's own minions.
     */
    public static void flash(ServerWorld world, @Nullable LivingEntity caster, Vec3d origin, Predicate<LivingEntity> affects) {
        // the burst: one big white flash, and light flying outward
        world.spawnParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        Random random = world.getRandom();
        for (int i = 0; i < BURST_PARTICLES; i++) {
            Vec3d d = randomDirection(random);
            world.spawnParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z, 0, d.x, d.y, d.z, 0.5);
        }
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS, 3.0F, 1.6F);

        // who sees it
        Box area = new Box(origin, origin).expand(FLASH_RADIUS);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && !e.isSpectator() && affects.test(e))) {

            Vec3d eye = target.getEyePos();
            Vec3d toFlash = origin.subtract(eye);
            double distance = toFlash.length();
            if (distance > FLASH_RADIUS || distance < 1.0E-4) continue;

            // looking away protects: only entities facing the flash see it
            if (target.getRotationVec(1.0F).dotProduct(toFlash.multiply(1.0 / distance)) < FACING_DOT) continue;
            // so does any solid block in between
            if (!hasLineOfSight(world, target, eye, origin)) continue;

            // white screen first, then blindness: the effect starts now and outlasts the white by BLIND_TICKS
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,
                    WHITE_TICKS + BLIND_TICKS, 0, false, true, true));

            if (target instanceof ServerPlayerEntity player) {
                ServerPlayNetworking.send(player, new FlashPayload(WHITE_TICKS, WHITE_FADE_TICKS));
            }
        }
    }

    private static boolean hasLineOfSight(ServerWorld world, LivingEntity viewer, Vec3d from, Vec3d to) {
        BlockHitResult hit = world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, viewer));
        // the flash may sit right against a wall, so a hit very close to it still counts as seen
        return hit.getType() == HitResult.Type.MISS || hit.getPos().squaredDistanceTo(to) < 1.0;
    }

    private static Vec3d randomDirection(Random random) {
        Vec3d v = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return v.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : v.normalize();
    }
}