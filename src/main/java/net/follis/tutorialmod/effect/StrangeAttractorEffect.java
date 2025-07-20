package net.follis.tutorialmod.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class StrangeAttractorEffect extends StatusEffect {
    public StrangeAttractorEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        Box box = new Box(entity.getBlockPos()).expand(10);
        List<Entity> neighbours = entity.getWorld().getOtherEntities(entity, box);

        if (entity instanceof PlayerEntity) {
            for (Entity neighbour : neighbours) {
                if (neighbour instanceof ItemEntity itemEntity) {
                    Vec3d diff = entity.getPos().add(0, 1, 0).subtract(itemEntity.getPos()).normalize();
                    itemEntity.setVelocity(diff.multiply(0.5));
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
