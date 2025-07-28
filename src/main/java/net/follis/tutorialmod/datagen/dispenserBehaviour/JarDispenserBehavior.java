package net.follis.tutorialmod.datagen.dispenserBehaviour;

import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.custom.BugJarItem;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.List;

public class JarDispenserBehavior extends FallibleItemDispenserBehavior {
    protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
        ServerWorld serverWorld = pointer.world();
        if (!serverWorld.isClient()) {
            BlockPos blockPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
            List<BugJarItem.BugData> bugDataList = new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.BUGS, new ArrayList<>()));
            if (!bugDataList.isEmpty()){
                BugJarItem.BugData lastBug = bugDataList.removeLast();
                releaseBug(blockPos, lastBug, serverWorld);
                stack.set(ModDataComponentTypes.BUGS, bugDataList);
                this.setSuccess(true);
            }
            this.setSuccess(false);
        }

        return stack;
    }

    private void releaseBug(BlockPos pos, BugJarItem.BugData bugData, World world) {
        Entity entity = bugData.loadEntity(world);
        if (entity != null) {
            positionEntity(entity, pos);
            world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, world.getBlockState(pos)));
            world.spawnEntity(entity);
        }
    }

    private void positionEntity(Entity entity, BlockPos pos) {
        double x = pos.getX() + 0.5 + (double) entity.getRandom().nextInt(5) / 10;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5 + (double) entity.getRandom().nextInt(5) / 10;
        entity.refreshPositionAndAngles(x, y, z, entity.getYaw(), entity.getPitch());
    }
}
