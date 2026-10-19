package net.follis.goldenscarabmod.item.custom;

import net.follis.goldenscarabmod.entity.misc.FlySwarmManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class FlyStaffItem extends Item {
    /** Charges the staff holds when full. */
    public static final int MAX_CHARGES = 5;
    /** Ticks to regenerate one charge (200 ticks = 10 seconds). */
    public static final int REGEN_TICKS = 100;
    /** Ticks between two uses. */
    private static final int USE_COOLDOWN = 10;

    private static final String NEXT_CHARGE_KEY = "NextCharge";

    public FlyStaffItem(Settings settings) {
        super(settings);
    }

    public static int charges(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamage();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (charges(stack) <= 0) {
            if (!world.isClient) {
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1.0F, 1.2F);
            }
            return TypedActionResult.fail(stack);
        }

        if (world instanceof ServerWorld serverWorld) {
            Vec3d origin = user.getEyePos();
            Vec3d direction = user.getRotationVec(1.0F);
            FlySwarmManager.spawnSwarm(serverWorld, origin, direction, user.getUuid());
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, SoundCategory.PLAYERS, 1.0F, 0.8F);

            boolean wasFull = stack.getDamage() == 0;
            stack.setDamage(stack.getDamage() + 1);
            // the regeneration timer starts when the first charge is spent, and keeps running while more are used
            if (wasFull) setNextCharge(stack, world.getTime() + REGEN_TICKS);

            user.getItemCooldownManager().set(this, USE_COOLDOWN);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && stack.getDamage() > 0) {
            long now = world.getTime();
            long next = getNextCharge(stack);

            if (next < 0 || next - now > REGEN_TICKS) {
                // no timer yet (e.g. a staff given already half empty), or one that comes from another world
                setNextCharge(stack, now + REGEN_TICKS);
            } else if (now >= next) {
                stack.setDamage(stack.getDamage() - 1);
                if (stack.getDamage() > 0) setNextCharge(stack, now + REGEN_TICKS);
                else clearNextCharge(stack);
                if (entity instanceof PlayerEntity) {
                    world.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6F, 1.4F);
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!type.isAdvanced()) {
            tooltip.add(Text.translatable("tooltip.goldenscarabmod.fly_staff.charges", charges(stack), stack.getMaxDamage())
                    .formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    /** Spending or regaining a charge changes the stack; that should not replay the "equip" animation. */
    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    // ---- the regeneration timer, stored in the stack's custom data ----

    private static long getNextCharge(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null || !data.contains(NEXT_CHARGE_KEY)) return -1;
        return data.copyNbt().getLong(NEXT_CHARGE_KEY);
    }

    private static void setNextCharge(ItemStack stack, long tick) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putLong(NEXT_CHARGE_KEY, tick));
    }

    private static void clearNextCharge(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(NEXT_CHARGE_KEY));
    }
}