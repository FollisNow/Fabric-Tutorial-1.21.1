package net.follis.tutorialmod.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Optional;

public class HatredEffect extends StatusEffect {
    public HatredEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity)) {
            Box box = new Box(entity.getBlockPos()).expand(10);
            List<Entity> neighbours = entity.getWorld().getOtherEntities(entity, box);

            for (Entity neighbour : neighbours){
                if (neighbour instanceof MobEntity mob) {
                    mob.setTarget(entity);
                    if (neighbour instanceof Angerable angerable && entity.hasStatusEffect(ModEffects.HATRED)) {
                        angerable.setAngryAt(entity.getUuid());
                        angerable.setAngerTime(entity.getStatusEffect(ModEffects.HATRED).getDuration());
                    } else if (neighbour instanceof WardenEntity warden) {
                        Optional<LivingEntity> currentPrimeSuspect = warden.getPrimeSuspect();
                        if (currentPrimeSuspect.isPresent() && currentPrimeSuspect.get() != entity) {
                            warden.removeSuspect(currentPrimeSuspect.get());
                            warden.increaseAngerAt(entity);
                        }
                    }
                }
            }
        }

        return super.applyUpdateEffect(entity, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
