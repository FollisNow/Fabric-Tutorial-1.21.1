package net.follis.tutorialmod.datagen;

import net.follis.tutorialmod.datagen.dispenserBehaviour.JarDispenserBehavior;
import net.follis.tutorialmod.datagen.dispenserBehaviour.PickaxeDispenserBehavior;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;

public class ModDispenserBehaviourProvider implements DispenserBehavior {

    public static void registerDispenserBehaviour() {
        DispenserBlock.registerBehavior(Items.WOODEN_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.STONE_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.IRON_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.GOLDEN_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.DIAMOND_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.NETHERITE_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(ModItems.PINK_GARNET_PICKAXE.asItem(), new PickaxeDispenserBehavior());

        DispenserBlock.registerBehavior(ModItems.BUG_JAR.asItem(), new JarDispenserBehavior());
        ItemDispenserBehavior itemDispenserBehavior = new ItemDispenserBehavior() {
            public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                Direction direction = pointer.state().get(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem)stack.getItem()).getEntityType(stack);

                try {
                    entityType.spawnFromItemStack(pointer.world(), stack, null, pointer.pos().offset(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
                } catch (Exception exception) {
                    LOGGER.error("Error while dispensing spawn egg from dispenser at {}", pointer.pos(), exception);
                    return ItemStack.EMPTY;
                }

                stack.decrement(1);
                pointer.world().emitGameEvent(null, GameEvent.ENTITY_PLACE, pointer.pos());
                return stack;
            }
        };

        for(SpawnEggItem spawnEggItem : SpawnEggItem.getAll()) {
            DispenserBlock.registerBehavior(spawnEggItem, itemDispenserBehavior);
        }
    }

    @Override
    public ItemStack dispense(BlockPointer pointer, ItemStack stack) {
        return null;
    }
}
