package net.follis.tutorialmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.custom.AmethystBeeHiveBlockEntity;
import net.follis.tutorialmod.entity.custom.AmethystBeeEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.minecraft.block.CandleBlock.LIT;

public class AmethystBeeHiveBlock extends BlockWithEntity {
    public static final DirectionProperty FACING;
    public static final IntProperty HONEY_LEVEL;
    public static final int FULL_HONEY_LEVEL = 5;
    private static final int DROPPED_HONEYCOMB_COUNT = 3;

    public static final MapCodec<AmethystBeeHiveBlock> CODEC = AmethystBeeHiveBlock.createCodec(AmethystBeeHiveBlock::new);

    public AmethystBeeHiveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HONEY_LEVEL, 0).with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AmethystBeeHiveBlockEntity(pos, state);
    }


    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(HONEY_LEVEL);
    }

    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        if (!world.isClient && blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
            if (!EnchantmentHelper.hasAnyEnchantmentsIn(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                amethystBeeHiveBlockEntity.angerBees(player, state, AmethystBeeHiveBlockEntity.AmethystBeeState.EMERGENCY);
                world.updateComparators(pos, this);
                this.angerNearbyBees(world, pos);
            }

            Criteria.BEE_NEST_DESTROYED.trigger((ServerPlayerEntity)player, state, tool, amethystBeeHiveBlockEntity.getBeeCount());
        }

    }

    private void angerNearbyBees(World world, BlockPos pos) {
        Box box = (new Box(pos)).expand(8.0F, 6.0F, 8.0F);
        List<AmethystBeeEntity> list = world.getNonSpectatingEntities(AmethystBeeEntity.class, box);
        if (!list.isEmpty()) {
            List<PlayerEntity> list2 = world.getNonSpectatingEntities(PlayerEntity.class, box);
            if (list2.isEmpty()) {
                return;
            }

            for(AmethystBeeEntity amethystBeeEntity : list) {
                if (amethystBeeEntity.getTarget() == null) {
                    PlayerEntity playerEntity = Util.getRandom(list2, world.random);
                    amethystBeeEntity.setTarget(playerEntity);
                }
            }
        }

    }

    private boolean hasBees(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
            return !amethystBeeHiveBlockEntity.hasNoBees();
        } else {
            return false;
        }
    }

    public static void dropAmethystShard(World world, BlockPos pos) {
        dropStack(world, pos, new ItemStack(Items.AMETHYST_SHARD, 3));
    }

    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        int i = state.get(HONEY_LEVEL);
        boolean bl = false;
        if (i >= 5) {
            Item item = stack.getItem();
            if (stack.isIn(ItemTags.PICKAXES)) {
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                dropAmethystShard(world, pos);
                stack.damage(1, player, LivingEntity.getSlotForHand(hand));
                bl = true;
                world.emitGameEvent(player, GameEvent.SHEAR, pos);
            } else if (stack.isOf(Items.GLASS_BOTTLE)) {
                stack.decrement(1);
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, new ItemStack(Items.HONEY_BOTTLE));
                } else if (!player.getInventory().insertStack(new ItemStack(Items.HONEY_BOTTLE))) {
                    player.dropItem(new ItemStack(Items.HONEY_BOTTLE), false);
                }

                bl = true;
                world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }

            if (!world.isClient() && bl) {
                player.incrementStat(Stats.USED.getOrCreateStat(item));
            }
        }

        if (bl) {
            if (!hasCandles(world, pos)) {
                if (this.hasBees(world, pos)) {
                    this.angerNearbyBees(world, pos);
                }

                this.takeHoney(world, state, pos, player, AmethystBeeHiveBlockEntity.AmethystBeeState.EMERGENCY);
            } else {
                this.takeHoney(world, state, pos);
            }

            return ItemActionResult.success(world.isClient);
        } else {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }
    }

    protected boolean hasCandles(World world, BlockPos pos) {
        BlockPos onTop = pos.up();
        BlockState blockOnTop = world.getBlockState(onTop);
        return blockOnTop.contains(LIT) && blockOnTop.isIn(BlockTags.CANDLES) && blockOnTop.get(LIT);
    }

    public void takeHoney(World world, BlockState state, BlockPos pos, @Nullable PlayerEntity player, AmethystBeeHiveBlockEntity.AmethystBeeState amethystBeeState) {
        this.takeHoney(world, state, pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
            amethystBeeHiveBlockEntity.angerBees(player, state, amethystBeeState);
        }

    }

    public void takeHoney(World world, BlockState state, BlockPos pos) {
        world.setBlockState(pos, state.with(HONEY_LEVEL, 0), 3);
    }

    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(HONEY_LEVEL) >= 5) {
            for(int i = 0; i < random.nextInt(1) + 1; ++i) {
                this.spawnHoneyParticles(world, pos, state);
            }
        }

    }

    private void spawnHoneyParticles(World world, BlockPos pos, BlockState state) {
        if (state.getFluidState().isEmpty() && !(world.random.nextFloat() < 0.3F)) {
            VoxelShape voxelShape = state.getCollisionShape(world, pos);
            double d = voxelShape.getMax(Direction.Axis.Y);
            if (d >= (double)1.0F && !state.isIn(BlockTags.IMPERMEABLE)) {
                double e = voxelShape.getMin(Direction.Axis.Y);
                if (e > (double)0.0F) {
                    this.addHoneyParticle(world, pos, voxelShape, (double)pos.getY() + e - 0.05);
                } else {
                    BlockPos blockPos = pos.down();
                    BlockState blockState = world.getBlockState(blockPos);
                    VoxelShape voxelShape2 = blockState.getCollisionShape(world, blockPos);
                    double f = voxelShape2.getMax(Direction.Axis.Y);
                    if ((f < (double)1.0F || !blockState.isFullCube(world, blockPos)) && blockState.getFluidState().isEmpty()) {
                        this.addHoneyParticle(world, pos, voxelShape, (double)pos.getY() - 0.05);
                    }
                }
            }

        }
    }

    private void addHoneyParticle(World world, BlockPos pos, VoxelShape shape, double height) {
        this.addHoneyParticle(world, (double)pos.getX() + shape.getMin(Direction.Axis.X), (double)pos.getX() + shape.getMax(Direction.Axis.X), (double)pos.getZ() + shape.getMin(Direction.Axis.Z), (double)pos.getZ() + shape.getMax(Direction.Axis.Z), height);
    }

    private void addHoneyParticle(World world, double minX, double maxX, double minZ, double maxZ, double height) {
        world.addParticle(ParticleTypes.DRIPPING_HONEY, MathHelper.lerp(world.random.nextDouble(), minX, maxX), height, MathHelper.lerp(world.random.nextDouble(), minZ, maxZ), 0.0F, 0.0F, 0.0F);
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HONEY_LEVEL, FACING);
    }

    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, ModBlockEntities.AMETHYST_BEE_HIVE_BE, AmethystBeeHiveBlockEntity::serverTick);
    }

    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player.isCreative() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
                int i = state.get(HONEY_LEVEL);
                boolean bl = !amethystBeeHiveBlockEntity.hasNoBees();
                if (bl || i > 0) {
                    ItemStack itemStack = new ItemStack(this);
                    itemStack.applyComponentsFrom(amethystBeeHiveBlockEntity.createComponentMap());
                    itemStack.set(DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT.with(HONEY_LEVEL, i));
                    ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
            }
        }

        return super.onBreak(world, pos, state, player);
    }

    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        Entity entity = builder.getOptional(LootContextParameters.THIS_ENTITY);
        if (entity instanceof TntEntity || entity instanceof CreeperEntity || entity instanceof WitherSkullEntity || entity instanceof WitherEntity || entity instanceof TntMinecartEntity) {
            BlockEntity blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
            if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
                amethystBeeHiveBlockEntity.angerBees(null, state, AmethystBeeHiveBlockEntity.AmethystBeeState.EMERGENCY);
            }
        }

        return super.getDroppedStacks(state, builder);
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (world.getBlockState(neighborPos).getBlock() instanceof FireBlock) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
                amethystBeeHiveBlockEntity.angerBees(null, state, AmethystBeeHiveBlockEntity.AmethystBeeState.EMERGENCY);
            }
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    static {
        FACING = HorizontalFacingBlock.FACING;
        HONEY_LEVEL = Properties.HONEY_LEVEL;
    }
}
