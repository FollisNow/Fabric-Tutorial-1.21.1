package net.follis.tutorialmod.mixin;

import net.follis.tutorialmod.effect.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.world.World;
import net.minecraft.world.event.Vibrations;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WardenEntity.class)
public abstract class WardenEntityMixin extends HostileEntity implements Vibrations {
    protected WardenEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "isValidTarget", cancellable = true)
    @Contract("null->false")
    private void isValidTarget(@Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity livingEntity) {
            if (this.getWorld() == entity.getWorld() && livingEntity.hasStatusEffect(ModEffects.HATRED) && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity) && livingEntity.getType() != EntityType.ARMOR_STAND && !livingEntity.isInvulnerable() && !livingEntity.isDead() && this.getWorld().getWorldBorder().contains(livingEntity.getBoundingBox())) {
                cir.setReturnValue(true); // Allow targeting other wardens
            }
        }

    }
}