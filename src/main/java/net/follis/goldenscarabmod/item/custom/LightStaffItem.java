package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.pattern.LightPattern;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LightStaffItem extends Item {
    /** How long the staff must be held before the flash (40 ticks = 2 seconds). */
    public static final int CHARGE_TICKS = 40;

    public LightStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    /** The staff is raised overhead, like a trident being charged. */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return CHARGE_TICKS;
    }

    /** Every tick while the staff is held: the noise and the collapsing light. */
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world instanceof ServerWorld serverWorld) {
            int elapsed = CHARGE_TICKS - remainingUseTicks;
            LightPattern.telegraph(serverWorld, origin(user), elapsed / (float) CHARGE_TICKS, elapsed);
        }
    }

    /** The charge ran its full length: the flash. */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world instanceof ServerWorld serverWorld) {
            LightPattern.flash(serverWorld, user, origin(user), e -> true);
        }
        return stack;
    }

    /** Released before the end: nothing happens, apart from a small fizzle. */
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.5F, 1.8F);
        }
    }

    /** The point the light gathers at: just in front of the user's face, at the top of the staff. */
    private static Vec3d origin(LivingEntity user) {
        return user.getEyePos().add(user.getRotationVec(1.0F).multiply(1.2)).add(0, 0.1, 0);
    }
}