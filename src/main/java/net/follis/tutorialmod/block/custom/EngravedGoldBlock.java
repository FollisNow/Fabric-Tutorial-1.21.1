package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.block.IMakeGolems;
import net.follis.tutorialmod.util.GolemRecipes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EngravedGoldBlock extends Block implements IMakeGolems {
    public EngravedGoldBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.trySpawnEntity(world, pos, GolemRecipes.goldGolemMap());
        }
    }
}
