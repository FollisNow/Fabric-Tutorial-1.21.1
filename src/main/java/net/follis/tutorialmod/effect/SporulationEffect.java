package net.follis.tutorialmod.effect;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.*;

import java.util.ArrayList;
import java.util.List;

public class SporulationEffect extends StatusEffect {
    public SporulationEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }
    List<BlockPos> positions = new ArrayList<>();

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.isDead()){
            summonMushroomPatch(entity.getBlockPos(), entity.getWorld(), 10);
            entity.removeStatusEffect(ModEffects.SPORULATION);
        }



        return super.applyUpdateEffect(entity, amplifier);
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
            if (world.random.nextInt(5) == 0)
                world.setBlockState(mushroomPos, Blocks.BROWN_MUSHROOM.getDefaultState());
        }

    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }
}
