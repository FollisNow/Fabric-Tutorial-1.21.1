package net.follis.tutorialmod.item.custom;

import dev.architectury.platform.Mod;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.DartEntity;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class DartShooterItem extends Item {
    public DartShooterItem(Settings settings) {
        super(settings);
    }
    @Override
    public Text getName() {
        return Text.translatable(this.getTranslationKey()).formatted(Formatting.GOLD);
    }
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (world instanceof ServerWorld serverWorld && itemStack.getMaxDamage() - itemStack.getDamage() > 0) {
            DartEntity dart = new DartEntity(ModEntities.DART, world);
            dart.refreshPositionAndAngles(user.getPos().add(0.0, 1.5, 0.0), user.getYaw(), user.getPitch());
            dart.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 3f, 0f);
            serverWorld.spawnEntity(dart);
            itemStack.setDamage(itemStack.getDamage()+1);
            user.getItemCooldownManager().set(this, 10);
            user.incrementStat(Stats.USED.getOrCreateStat(this));

        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world instanceof ServerWorld && entity instanceof PlayerEntity player && player.getInventory().contains(ModItems.DART.getDefaultStack())) {
            int dartSlot = player.getInventory().getSlotWithStack(ModItems.DART.getDefaultStack());
            for (int i = 0; i < dartSlot; i++) {
                if (stack.getDamage() > 0) {
                    stack.setDamage(stack.getDamage() - 1);
                    player.getInventory().removeStack(dartSlot, 1);
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }
}
