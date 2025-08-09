package net.follis.tutorialmod.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.InstantStatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.jetbrains.annotations.Nullable;

public class InstantBleedEffect extends InstantStatusEffect {
    protected InstantBleedEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        StatusEffectInstance existingEffect = entity.getStatusEffect(ModEffects.BLEEDING);
        if (existingEffect != null) {
            int newAmplifier = existingEffect.getAmplifier() + amplifier + 1; // +1 to account for the current level
            entity.removeStatusEffect(ModEffects.BLEEDING); // Remove the existing effect
            entity.addStatusEffect(new StatusEffectInstance(ModEffects.BLEEDING, 1200, newAmplifier, true, false, true));
        } else {
            // Just apply the effect normally
            entity.addStatusEffect(new StatusEffectInstance(ModEffects.BLEEDING, 1200, amplifier, true, false, true));
        }
        entity.damage(entity.getDamageSources().magic(), (float)(6 << amplifier));
        return true;
    }

    public void applyInstantEffect(@Nullable Entity source, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity) {
        StatusEffectInstance existingEffect = target.getStatusEffect(ModEffects.BLEEDING);
        if (existingEffect != null) {
            int newAmplifier = existingEffect.getAmplifier() + amplifier + 1; // +1 to account for the current level
            target.removeStatusEffect(ModEffects.BLEEDING); // Remove the existing effect
            target.addStatusEffect(new StatusEffectInstance(ModEffects.BLEEDING, 1200, newAmplifier, true, false, true));
        } else {
            // Just apply the effect normally
            target.addStatusEffect(new StatusEffectInstance(ModEffects.BLEEDING, 1200, amplifier, true, false, true));
        }
        if (source == null) {
            target.damage(target.getDamageSources().magic(), (float)((6 << amplifier) + (double)0.5F));
        } else {
            target.damage(target.getDamageSources().indirectMagic(source, attacker), (float)((6 << amplifier) + (double)0.5F));
        }
    }
}

