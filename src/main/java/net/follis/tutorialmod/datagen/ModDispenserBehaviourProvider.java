package net.follis.tutorialmod.datagen;

import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPointer;

public class ModDispenserBehaviourProvider implements DispenserBehavior {

    public static void registerDispenserBehaviour() {
        DispenserBlock.registerBehavior(Items.WOODEN_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.STONE_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.IRON_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.GOLDEN_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.DIAMOND_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(Items.NETHERITE_PICKAXE.asItem(), new PickaxeDispenserBehavior());
        DispenserBlock.registerBehavior(ModItems.PINK_GARNET_PICKAXE.asItem(), new PickaxeDispenserBehavior());
    }

    @Override
    public ItemStack dispense(BlockPointer pointer, ItemStack stack) {
        return null;
    }
}
