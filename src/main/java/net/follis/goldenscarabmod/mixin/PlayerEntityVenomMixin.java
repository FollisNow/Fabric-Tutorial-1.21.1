package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityVenomMixin {
    /** ARMS_UP: no critical hits. */
    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean goldenscarabmod$noCrit(PlayerEntity self, RegistryEntry<StatusEffect> effect) {
        if (effect == StatusEffects.BLINDNESS && self.hasStatusEffect(VenomEffects.ARMS_UP)) return true;
        return self.hasStatusEffect(effect);
    }

    /** BLADE: sweeping hits nobody else. */
    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private <T extends Entity> List<T> goldenscarabmod$noSweep(World world, Class<T> type, Box box) {
        if (((PlayerEntity) (Object) this).hasStatusEffect(VenomEffects.BLADE)) return List.of();
        return world.getNonSpectatingEntities(type, box);
    }
}
