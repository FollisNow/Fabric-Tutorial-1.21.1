package net.follis.tutorialmod.block.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.custom.GoldenSilverfishEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.InfestedBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

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

    @Override
    protected void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.onStacksDropped(state, world, pos, tool, dropExperience);
        if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) && !EnchantmentHelper.hasAnyEnchantmentsIn(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            this.spawnGoldenSilverfish(world, pos);
        }
    }

}
