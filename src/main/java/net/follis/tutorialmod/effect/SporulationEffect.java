package net.follis.tutorialmod.effect;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.ArrayList;
import java.util.List;

public class SporulationEffect extends StatusEffect {
    public SporulationEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }
    private final List<BlockPos> positions = new ArrayList<>();

    @Override
    public void onEntityRemoval(LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED) {
            this.summonMushroomPatch(entity.getBlockPos(), entity.getWorld(), 10);
            entity.removeStatusEffect(ModEffects.SPORULATION);
        }
    }

    private void summonMushroomPatch(BlockPos pos, World world, int range) {
        BlockPos testBlock;
        this.positions.clear();
        for(int x = -range; x <= range; x++) {
            for(int y = -range; y <= range; y++) {
                 testBlock = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + y);
                if (world.getBlockState(testBlock).isAir() || world.getBlockState(testBlock).isIn(BlockTags.REPLACEABLE)) {
                        if((world.getBlockState(testBlock.down()).isSolidBlock(world, testBlock.down()) && ((world.getLightLevel(LightType.BLOCK, testBlock) + world.getLightLevel(LightType.SKY, testBlock)) < 7) ||
                                world.getBlockState(testBlock.down()).isIn(BlockTags.MUSHROOM_GROW_BLOCK))) {
                            this.positions.add(testBlock);
                        }
                }
            }
        }
        for (BlockPos mushroomPos : this.positions){
            if (world.random.nextInt(5) == 0) {
                world.setBlockState(mushroomPos, Blocks.BROWN_MUSHROOM.getDefaultState(), Block.NOTIFY_ALL);
                world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, mushroomPos, 0);
            }

        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
