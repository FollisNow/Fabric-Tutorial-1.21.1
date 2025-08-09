package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.BambooTrapEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class BambooTrapItem extends Item {
    public BambooTrapItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        World world = context.getWorld();

        world.playSound(null, context.getBlockPos().getX(), context.getBlockPos().getY(), context.getBlockPos().getZ(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            Entity entity = ModEntities.BAMBOO_TRAP.spawnFromItemStack((ServerWorld)world, context.getStack(), user, context.getBlockPos().up(), SpawnReason.SPAWN_EGG, false, false);
        }


        ItemStack itemStack = context.getStack();

        if (user != null) {
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }
}
