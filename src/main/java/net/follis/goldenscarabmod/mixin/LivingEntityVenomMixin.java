package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityVenomMixin {
    /** ARCHER / DANGER: +50% damage taken, after armor. */
    @Inject(method = "modifyAppliedDamage", at = @At("RETURN"), cancellable = true)
    private void goldenscarabmod$venomDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        float result = cir.getReturnValueF();
        if (self.hasStatusEffect(VenomEffects.ARCHER) && source.isIn(DamageTypeTags.IS_PROJECTILE)) result *= 1.5F;
        if (self.hasStatusEffect(VenomEffects.DANGER) && source.isIn(DamageTypeTags.IS_EXPLOSION)) result *= 1.5F;
        cir.setReturnValue(result);
    }

    /** EXPLORER: cannot sprint. */
    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean goldenscarabmod$noSprint(boolean sprinting) {
        return sprinting && !((LivingEntity) (Object) this).hasStatusEffect(VenomEffects.EXPLORER);
    }

    /** FLOW: holding jump in water no longer swims you up. */
    @Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
    private void goldenscarabmod$noSwimUp(TagKey<Fluid> fluid, CallbackInfo ci) {
        if (fluid == FluidTags.WATER && ((LivingEntity) (Object) this).hasStatusEffect(VenomEffects.FLOW)) ci.cancel();
    }

    /** SNORT: sneaking / invisibility / mob heads no longer reduce detection range. */
    @Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
    private void goldenscarabmod$noHiding(@Nullable Entity entity, CallbackInfoReturnable<Double> cir) {
        if (((LivingEntity) (Object) this).hasStatusEffect(VenomEffects.SNORT)) cir.setReturnValue(1.0D);
    }
}
