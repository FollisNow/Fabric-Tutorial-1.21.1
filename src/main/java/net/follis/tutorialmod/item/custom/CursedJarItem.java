package net.follis.tutorialmod.item.custom;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;


public class CursedJarItem extends AbstractEntityJarItem {
    private static final int maxPoisonLevel = 10;

    public CursedJarItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(world instanceof ServerWorld serverWorld) {
            if(serverWorld.getTime() % 80 == 0) {
                if (entity instanceof PlayerEntity player) {
                    if (tryKillBug(player, slot)) {
                        entity.getWorld().playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 0.7F, 0.3F);
                        int poisonLevel = player.getInventory().getStack(slot).getOrDefault(ModDataComponentTypes.STACKS_TRACKING, 1);
                        makeBugsDeadlier(player, slot);
                        poisonLevel++;
                        if (poisonLevel >= maxPoisonLevel) {
                            player.getInventory().getStack(slot).set(ModDataComponentTypes.STACKS_TRACKING, 0);
                            player.dropItem(ModItems.LOCUST_RED);
                        } else {
                            player.getInventory().getStack(slot).set(ModDataComponentTypes.STACKS_TRACKING, poisonLevel);
                        }
                    }
                }
            }
        }
    }
}