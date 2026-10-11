package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.custom.TrappedSkullBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class TrappedWallSkullBlock extends WallSkullBlock {
    public TrappedWallSkullBlock(SkullBlock.SkullType type, Settings settings) {
        super(type, settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrappedSkullBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.TRAPPED_SKULL_BE, TrappedSkullBlockEntity::tick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof TrappedSkullBlockEntity be ? be.getEventListener() : null;
    }
}
