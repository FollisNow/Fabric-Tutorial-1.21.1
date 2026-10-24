package net.follis.goldenscarabmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.follis.goldenscarabmod.block.entity.ModBlockEntities;
import net.follis.goldenscarabmod.block.entity.custom.CrownBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CrownBlock extends BlockWithEntity {
    public static final MapCodec<CrownBlock> CODEC = createCodec(CrownBlock::new);

    public CrownBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CrownBlockEntity(pos, state);
    }

    /** BlockWithEntity makes blocks invisible by default; this one has a normal model. */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.CROWN_BE, CrownBlockEntity::tick);
    }
}