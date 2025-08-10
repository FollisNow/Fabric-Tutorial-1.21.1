package net.follis.tutorialmod.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import org.jetbrains.annotations.Nullable;

public class BleedStackingEffect extends StatusEffect {
    protected BleedStackingEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > amplifier) {
            entity.damage(entity.getDamageSources().magic(), amplifier);
        } else if (entity.getHealth() > 1) {
            entity.damage(entity.getDamageSources().magic(), entity.getHealth() - 1);
        }
        return true;
    }

    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 80 == 0;
    }
}
