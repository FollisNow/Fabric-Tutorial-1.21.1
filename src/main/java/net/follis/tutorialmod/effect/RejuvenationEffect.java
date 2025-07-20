
package net.follis.tutorialmod.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class RejuvenationEffect extends StatusEffect {
    public RejuvenationEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof MobEntity mobEntity && !mobEntity.isBaby()) {
            mobEntity.setBaby(true);
            mobEntity.getWorld().playSound(mobEntity.getX(), mobEntity.getEyeY(), mobEntity.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, mobEntity.getSoundCategory(), 1.0F + mobEntity.getRandom().nextFloat(), mobEntity.getRandom().nextFloat() * 0.7F + 0.3F, false);
            switch (mobEntity) {
                case AbstractSkeletonEntity skelly -> {
                    skelly.convertTo(EntityType.ZOMBIE, true);
                }
                case WitchEntity witchy -> {
                    witchy.dropAllEquipment();
                    witchy.convertTo(EntityType.VILLAGER, false);
                }
                case ZombifiedPiglinEntity piggy -> {
                    piggy.convertTo(EntityType.PIGLIN, true);
                }
                default -> {
                }
            }
            mobEntity.removeStatusEffect(ModEffects.REJUNEVATION);
        }
        return super.applyUpdateEffect(entity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
