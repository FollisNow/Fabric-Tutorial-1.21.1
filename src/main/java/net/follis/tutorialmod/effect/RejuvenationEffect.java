
package net.follis.tutorialmod.effect;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class RejuvenationEffect extends StatusEffect {
    public RejuvenationEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if(entity.getRandom().nextInt(20) == 0) {
            if (entity instanceof MobEntity mobEntity && !mobEntity.isBaby()) {
                mobEntity.setBaby(true);

                if (!mobEntity.getWorld().isClient()) {
                    mobEntity.getWorld().playSound(mobEntity, mobEntity.getBlockPos(),SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.NEUTRAL, 1f, 0.7f);
                }

                switch (mobEntity) {
                    case AbstractSkeletonEntity skelly -> skelly.convertTo(EntityType.ZOMBIE, true);
                    case WitchEntity witchy -> {
                        witchy.dropAllEquipment();
                        witchy.convertTo(EntityType.VILLAGER, false);
                    }
                    case ZombifiedPiglinEntity piggy -> piggy.convertTo(EntityType.PIGLIN, true);
                    default -> {
                    }
                }
                mobEntity.removeStatusEffect(ModEffects.REJUNEVATION);
            }
        }

        return super.applyUpdateEffect(entity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
