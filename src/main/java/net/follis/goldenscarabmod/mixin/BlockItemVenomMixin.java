package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemVenomMixin {
    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void goldenscarabmod$noPlacing(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = context.getPlayer();
        if (player != null && player.hasStatusEffect(VenomEffects.SHELTER)) cir.setReturnValue(false);
    }
}
