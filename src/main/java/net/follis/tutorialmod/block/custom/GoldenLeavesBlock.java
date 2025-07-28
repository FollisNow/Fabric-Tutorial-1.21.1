package net.follis.tutorialmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoldenLeavesBlock extends LeavesBlock {
    public static final MapCodec<GoldenLeavesBlock> CODEC = createCodec(GoldenLeavesBlock::new);
    public MapCodec<GoldenLeavesBlock> getCodec() {
        return CODEC;
    }

    public GoldenLeavesBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        if (random.nextInt(10) == 0) {
            BlockPos blockPos = pos.down();
            BlockState blockState = world.getBlockState(blockPos);
            if (!isFaceFullSquare(blockState.getCollisionShape(world, blockPos), Direction.UP)) {
                ParticleUtil.spawnParticle(world, pos, random, ModParticles.GOLDEN_LEAVES_PARTICLE);
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        if (random.nextInt(60) == 0) {
            BlockPos blockPos = pos.down();
            if (world.getBlockState(blockPos).isAir()) {
                world.spawnEntity(new ItemEntity(world, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, Items.GOLD_NUGGET.getDefaultStack()));
            }
        }
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return super.hasRandomTicks(state) || !state.get(PERSISTENT);
    }

}
