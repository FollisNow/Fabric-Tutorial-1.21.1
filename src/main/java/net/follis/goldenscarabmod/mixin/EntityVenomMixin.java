package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityVenomMixin {
    @ModifyVariable(method = "setSwimming", at = @At("HEAD"), argsOnly = true)
    private boolean goldenscarabmod$noSwimPose(boolean swimming) {
        return swimming && !((Object) this instanceof LivingEntity living && living.hasStatusEffect(VenomEffects.FLOW));
    }
}
