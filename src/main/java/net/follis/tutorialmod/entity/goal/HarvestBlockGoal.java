package net.follis.tutorialmod.entity.goal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class HarvestBlockGoal extends Goal {
    private final AnimalEntity entity;
    private final Block blockToHarvest; // The block type to harvest
    private final int range;
    private final int height;
    private final boolean isRandomized;
    private int searchCooldown;
    private BlockPos targetPos;
    private List<BlockPos> harvestableBlocks;

    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.height = 2;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.isRandomized = false;
    }
    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range, boolean isRandomized) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.isRandomized = isRandomized;
        this.height = 2;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range, int height) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.height = height;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.isRandomized = false;
    }
    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range, int height,  boolean isRandomized) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.height = height;
        this.isRandomized = isRandomized;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 10% chance to try harvesting
        return this.entity.getRandom().nextInt(10) == 0;
    }

    @Override
    public void start() {
        this.searchCooldown = 0;
        this.harvestableBlocks = new ArrayList<>();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            //return;
        } else {
            this.targetPos = findHarvestableBlock(this.range, this.height);
            if (this.targetPos != null){
                this.searchCooldown = 80;
            } else {
                this.searchCooldown = 800;
            }
        }


        if (this.targetPos != null) {
            this.entity.getNavigation().startMovingTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), 1.1F);

            // Check if the entity is close enough to harvest
            if (this.targetPos.isWithinDistance(this.entity.getBlockPos(), 2)) {
                harvestBlock(this.targetPos);
            }
        }
    }


    private BlockPos findHarvestableBlock(int radius, int vertical) {
        BlockPos entityPos = this.entity.getBlockPos();
        if (this.harvestableBlocks != null ){
            this.harvestableBlocks.clear();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -vertical; y <= vertical; y++) { // Check the block directly below and above
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = entityPos.add(x, y, z);
                        BlockState blockState = this.entity.getWorld().getBlockState(pos);

                        if (blockState.isOf(this.blockToHarvest) && canHarvest(blockState)) {
                            this.harvestableBlocks.add(pos);
                        }
                    }
                }
            }
            if (!this.harvestableBlocks.isEmpty()) {
                if (this.isRandomized) {
                    return this.harvestableBlocks.get(this.entity.getRandom().nextInt(this.harvestableBlocks.size()));
                }
                return this.harvestableBlocks.getFirst();
            }
        }
        return null; // No harvestable block found
    }

    private boolean canHarvest(BlockState blockState) {
        if (this.blockToHarvest instanceof CropBlock cropBlock) {
            return cropBlock.isMature(blockState);
        }
        return true; // Allow harvesting for non-CropBlocks
    }

    private void harvestBlock(BlockPos pos) {
        this.entity.getWorld().breakBlock(pos, true, this.entity);
    }
}