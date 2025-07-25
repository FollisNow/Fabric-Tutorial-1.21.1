package net.follis.tutorialmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.follis.tutorialmod.particle.ModParticles;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

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
}
