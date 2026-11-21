package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HungerManager.class)
public abstract class HungerManagerVenomMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;heal(F)V"))
    private void goldenscarabmod$noNaturalRegen(PlayerEntity player, float amount) {
        if (!player.hasStatusEffect(VenomEffects.HEART)) player.heal(amount);
    }
}
