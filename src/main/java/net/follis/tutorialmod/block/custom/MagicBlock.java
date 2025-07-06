package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.particle.ModParticles;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class MagicBlock extends Block {
    public MagicBlock(Settings settings) {
        super(settings);
    }

    private static final Map<Block, EntityType<?>> GOLEM_MAP =
            Map.of(
                    ModBlocks.PINK_GARNET_BLOCK, ModEntities.MANTIS,
                    Blocks.GOLD_BLOCK, EntityType.IRON_GOLEM
            );

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        world.addParticle(ModParticles.PINK_GARNET_PARTICLE,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                0, 1, 0);

        world.playSound(player, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1f, 1f);
        return ActionResult.SUCCESS;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if(entity instanceof ItemEntity itemEntity) {
            if(isValidItem(itemEntity.getStack())){
                itemEntity.setStack(new ItemStack(Items.DIAMOND, itemEntity.getStack().getCount()));
            }
        }

        super.onSteppedOn(world, pos, state, entity);
    }

    private boolean isValidItem(ItemStack stack) {
        return stack.isIn(ModTags.Items.TRANSFORMABLE_ITEMS);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.tutorialmod.magic_block.tooltip"));

        super.appendTooltip(stack, context, tooltip, options);
    }

    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.trySpawnEntity(world, pos);
        }
    }

    private void trySpawnEntity(World world, BlockPos pos) {
        GOLEM_MAP.forEach((block, entityType) -> {
            BlockPattern.Result result = this.getCorrespondingPattern(block).searchAround(world, pos);
            if (result != null) {
                Entity entity = entityType.create(world);
                if (entity != null) {
                    entity.getType().create(world);
                    spawnEntity(world, result, entity, result.translate(1, 2, 0).getBlockPos());
                }
            }
        });
    }


    private static void spawnEntity(World world, BlockPattern.Result patternResult, Entity entity, BlockPos pos) {
        for(int i = 0; i < patternResult.getWidth(); ++i) {
            for(int j = 0; j < patternResult.getHeight(); ++j) {
                CachedBlockPosition cachedBlockPosition = patternResult.translate(i, j, 0);
                world.setBlockState(cachedBlockPosition.getBlockPos(), Blocks.AIR.getDefaultState(), 2);
                world.syncWorldEvent(2001, cachedBlockPosition.getBlockPos(), Block.getRawIdFromState(cachedBlockPosition.getBlockState()));
            }
        }
        entity.refreshPositionAndAngles((double)pos.getX() + (double)0.5F, (double)pos.getY() + 0.05, (double)pos.getZ() + (double)0.5F, 0.0F, 0.0F);
        world.spawnEntity(entity);

        for(ServerPlayerEntity serverPlayerEntity : world.getNonSpectatingEntities(ServerPlayerEntity.class, entity.getBoundingBox().expand(5.0F))) {
            Criteria.SUMMONED_ENTITY.trigger(serverPlayerEntity, entity);
        }

        for(int i = 0; i < patternResult.getWidth(); ++i) {
            for(int j = 0; j < patternResult.getHeight(); ++j) {
                CachedBlockPosition cachedBlockPosition = patternResult.translate(i, j, 0);
                world.updateNeighbors(cachedBlockPosition.getBlockPos(), Blocks.AIR);
            }
        }    }


    private BlockPattern getCorrespondingPattern(Block block) {

        return BlockPatternBuilder.start().aisle(new String[]{
                        "~^~",
                        "###",
                        "~#~"
                }).
                where('^', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(ModBlocks.MAGIC_BLOCK))).
                where('#', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(block))).
                where('~', (pos) -> pos.getBlockState().isAir()).build();
    }
}
