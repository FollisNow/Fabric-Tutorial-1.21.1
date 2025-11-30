package net.follis.tutorialmod.mixin;


import net.follis.tutorialmod.effect.ModEffects;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    @Shadow
    public abstract float getAttackCooldownProgress(float baseTime);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "attack")
    public void attackMixin(Entity target, CallbackInfo info) {
        if (target instanceof LivingEntity living && (this.getMainHandStack().isIn(ModTags.Items.MACUAHUITL)) && this.getAttackCooldownProgress(0f) > 0.8f) {
            living.addStatusEffect(new StatusEffectInstance(ModEffects.APPLY_BLEEDING, 60, 0), this);
        }
    }
}
