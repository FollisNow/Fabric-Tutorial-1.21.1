package net.follis.tutorialmod.block.custom;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.custom.GoldenHotelBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldenHotelBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING;
    private static final ImmutableList<Vec3d> CANDLES_TO_PARTICLE_OFFSETS;

    private static final VoxelShape SHAPE =
            Block.createCuboidShape(1, 0, 1, 15, 1, 15);
    public static final MapCodec<GoldenHotelBlock> CODEC = GoldenHotelBlock.createCodec(GoldenHotelBlock::new);

    public GoldenHotelBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenHotelBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if(state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if(blockEntity instanceof GoldenHotelBlockEntity) {
                ItemScatterer.spawn(world, pos, ((GoldenHotelBlockEntity) blockEntity));
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.getBlockEntity(pos) instanceof GoldenHotelBlockEntity goldenHotelBlockEntity) {
            if(goldenHotelBlockEntity.isEmpty() && !stack.isEmpty()) {
                goldenHotelBlockEntity.setStack(0, stack.copyWithCount(1));
                world.playSound(player, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 2f);
                stack.decrement(1);

                goldenHotelBlockEntity.markDirty();
                world.updateListeners(pos, state, state, 0);
            } else if(stack.isEmpty() && !player.isSneaking()) {
                ItemStack stackOnHotel = goldenHotelBlockEntity.getStack(0);
                player.setStackInHand(Hand.MAIN_HAND, stackOnHotel);
                world.playSound(player, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                goldenHotelBlockEntity.clear();

                goldenHotelBlockEntity.markDirty();
                world.updateListeners(pos, state, state, 0);
            } else if(player.isSneaking() && !world.isClient()) {
                player.openHandledScreen(goldenHotelBlockEntity);
            }
        }

        return ItemActionResult.SUCCESS;
    }

    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
            this.getParticleOffsets().forEach((offset) -> spawnCandleParticles(world, offset.add(pos.getX(), pos.getY(), pos.getZ()), random));
    }

    private static void spawnCandleParticles(World world, Vec3d vec3d, Random random) {
        float f = random.nextFloat();
        if (f < 0.3F) {
            world.addParticle(ParticleTypes.SMOKE, vec3d.x, vec3d.y, vec3d.z, 0.0F, 0.0F, 0.0F);
            if (f < 0.17F) {
                world.playSound(vec3d.x + (double)0.5F, vec3d.y + (double)0.5F, vec3d.z + (double)0.5F,
                        SoundEvents.BLOCK_CANDLE_AMBIENT, SoundCategory.BLOCKS,
                        1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
            }
        }

        world.addParticle(ParticleTypes.SMALL_FLAME, vec3d.x, vec3d.y, vec3d.z, 0.0F, 0.0F, 0.0F);
    }



    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    protected Iterable<Vec3d> getParticleOffsets() {
        return CANDLES_TO_PARTICLE_OFFSETS;
    }
    static {
        FACING = Properties.HORIZONTAL_FACING;
        CANDLES_TO_PARTICLE_OFFSETS = ImmutableList.of(
                new Vec3d(0.0625F, 0.313F, 0.0625F),
                new Vec3d(0.9375F, 0.313F, 0.9375F),
                new Vec3d(0.0625F, 0.313F, 0.9375F),
                new Vec3d(0.9375F, 0.313F, 0.0625F));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if(world.isClient()) {
            return null;
        }

        return validateTicker(type, ModBlockEntities.GOLDEN_HOTEL_BE,
                (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }
}
