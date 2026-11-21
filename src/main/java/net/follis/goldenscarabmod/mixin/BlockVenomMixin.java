package net.follis.goldenscarabmod.mixin;

import net.follis.goldenscarabmod.effect.VenomEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockVenomMixin {
    @Inject(method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void goldenscarabmod$ignoreFortune(BlockState state, World world, BlockPos pos, @Nullable BlockEntity be,
                                                      @Nullable Entity entity, ItemStack tool, CallbackInfo ci) {
        if (entity instanceof LivingEntity living && living.hasStatusEffect(VenomEffects.PRIZE)
                && tool.getEnchantments().getEnchantments().stream().anyMatch(e -> e.matchesKey(Enchantments.FORTUNE))) {
            ItemStack stripped = tool.copy();
            EnchantmentHelper.apply(stripped, builder -> builder.remove(e -> e.matchesKey(Enchantments.FORTUNE)));
            Block.dropStacks(state, world, pos, be, entity, stripped);
            ci.cancel();
        }
    }
}
