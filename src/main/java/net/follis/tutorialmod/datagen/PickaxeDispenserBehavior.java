package net.follis.tutorialmod.datagen;

import net.follis.tutorialmod.block.custom.AmethystBeeHiveBlock;
import net.follis.tutorialmod.block.entity.custom.AmethystBeeHiveBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

public class PickaxeDispenserBehavior extends FallibleItemDispenserBehavior {
    protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
        ServerWorld serverWorld = pointer.world();
        if (!serverWorld.isClient()) {
            BlockPos blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
            this.setSuccess(tryPickaxeBlock(serverWorld, blockPos));
            if (this.isSuccess()) {
                stack.damage(1, serverWorld, null, (item) -> {
                });
            }
        }

        return stack;
    }

    private static boolean tryPickaxeBlock(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isIn(BlockTags.BEEHIVES, (state) -> state.contains(AmethystBeeHiveBlock.HONEY_LEVEL) && state.getBlock() instanceof AmethystBeeHiveBlock)) {
            int i = blockState.get(AmethystBeeHiveBlock.HONEY_LEVEL);
            if (i >= 5) {
                world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                AmethystBeeHiveBlock.dropAmethystShard(world, pos);
                ((AmethystBeeHiveBlock)blockState.getBlock()).takeHoney(world, blockState, pos, null, AmethystBeeHiveBlockEntity.AmethystBeeState.BEE_RELEASED);
                world.emitGameEvent(null, GameEvent.SHEAR, pos);
                return true;
            }
        }

        return false;
    }
}
