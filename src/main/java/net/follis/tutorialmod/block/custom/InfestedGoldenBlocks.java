package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.GoldenSilverfishEntity;
import net.minecraft.block.Block;
import net.minecraft.block.InfestedBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class InfestedGoldenBlocks extends InfestedBlock {
    /**
     * Creates an infested block
     *
     * @param regularBlock the block this infested block should mimic
     * @param settings     block settings
     */
    public InfestedGoldenBlocks(Block regularBlock, Settings settings) {
        super(regularBlock, settings);
    }

    private void spawnGoldenSilverfish(ServerWorld world, BlockPos pos) {
        GoldenSilverfishEntity goldenSilverfish = ModEntities.GOLDEN_SILVERFISH.create(world);
        if (goldenSilverfish != null) {
            goldenSilverfish.refreshPositionAndAngles((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, 0.0F, 0.0F);
            world.spawnEntity(goldenSilverfish);
            goldenSilverfish.playSpawnEffects();
        }
    }

}
