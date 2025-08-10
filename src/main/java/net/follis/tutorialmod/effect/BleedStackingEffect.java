package net.follis.tutorialmod.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class BleedStackingEffect extends StatusEffect {
    protected BleedStackingEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > amplifier + 1) {
            entity.damage(entity.getDamageSources().magic(), amplifier + 1);
        } else if (entity.getHealth() > 1.0F) {
            entity.damage(entity.getDamageSources().magic(), entity.getHealth() - 1);
        }

        return true;
    }

    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
