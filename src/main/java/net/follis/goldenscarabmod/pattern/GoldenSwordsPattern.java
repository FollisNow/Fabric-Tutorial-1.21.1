package net.follis.goldenscarabmod.pattern;

import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.entity.custom.GoldenSwordProjectileEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** "Light swords from above": swords appear one after another over the target, then all fall together. */
public final class GoldenSwordsPattern {
    public static final int SWORD_COUNT = 16;
    /** X: ticks between two swords appearing. They all fall one interval after the last one appears. */
    public static final int SPAWN_INTERVAL = 5;
    /** How high the swords hover above the top of the target's head. */
    public static final double HOVER_HEIGHT = 4.0;
    /** Radius of the ring the swords hover in. */
    public static final double RING_RADIUS = 1.25;

    private GoldenSwordsPattern() {}

    /** Ticks from the cast until the swords fall. */
    public static int fallTick() {
        return SWORD_COUNT * SPAWN_INTERVAL;
    }

    public static void summon(ServerWorld world, LivingEntity caster, LivingEntity target) {
        int fallAt = fallTick();

        for (int i = 0; i < SWORD_COUNT; i++) {
            double angle = 2.0 * Math.PI * i / SWORD_COUNT;
            Vec3d offset = new Vec3d(Math.cos(angle) * RING_RADIUS, HOVER_HEIGHT, Math.sin(angle) * RING_RADIUS);

            GoldenSwordProjectileEntity sword = new GoldenSwordProjectileEntity(ModEntities.GOLDEN_SWORD, world);
            sword.setup(caster, target, offset, i * SPAWN_INTERVAL, fallAt);
            world.spawnEntity(sword);
        }
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.NEUTRAL, 1.0F, 1.5F);
    }
}