package net.follis.tutorialmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.follis.tutorialmod.item.custom.VisionMonocleItem;
import net.follis.tutorialmod.util.IBugVariants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public ClientPlayerEntity player;
    private static final double BUG_VISION_RADIUS = 100.0D;

    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    private boolean tutorialmod$showBugOutlines(boolean original, Entity entity) {
        if (original || this.player == null || !(entity.getType().isIn(EntityTypeTags.ARTHROPOD))) {
            return original;
        }

        ItemStack offhand = this.player.getOffHandStack();
        if (!(offhand.getItem() instanceof VisionMonocleItem)) {
            return original;
        }

        return entity.squaredDistanceTo(this.player) <= BUG_VISION_RADIUS * BUG_VISION_RADIUS;
    }
}