package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.entity.custom.MiniSunProjectileEntity;
import net.follis.tutorialmod.sound.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MiniSunItem extends Item {
    public MiniSunItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            MiniSunProjectileEntity miniSun = new MiniSunProjectileEntity(world, user);
            miniSun.setPosition(user.getX(), user.getY() + 2.0d, user.getZ());
            miniSun.setDuration(160);
            world.spawnEntity(miniSun);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.MINI_SUN_SPAWN, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }
}
