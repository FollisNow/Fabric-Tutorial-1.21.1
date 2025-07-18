package net.follis.tutorialmod.datagen;

import dev.architectury.registry.registries.DeferredRegister;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.item.ModItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPointer;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

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
