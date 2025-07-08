package net.follis.tutorialmod.entity.goal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class HarvestBlockGoal extends Goal {
    private final AnimalEntity entity;
    private final Block blockToHarvest; // The block type to harvest
    private final int range;
    private final boolean isRandomized;
    private int harvestCooldown;
    private BlockPos targetPos;
    private List<BlockPos> harvestableBlocks;

    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range, boolean isRandomized) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.isRandomized = isRandomized;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.isRandomized = false;
    }

    @Override
    public boolean canStart() {
        // 10% chance to try harvesting
        return this.entity.getRandom().nextInt(10) == 0;
    }

    @Override
    public void start() {
        this.harvestCooldown = 0;
        this.harvestableBlocks = new ArrayList<>();
    }

    @Override
    public void tick() {
        if (this.harvestCooldown > 0) {
            this.harvestCooldown--;
            //return;
        } else {
            this.targetPos = findHarvestableBlock(this.range);
            if (this.targetPos != null){
                this.harvestCooldown = 80;
            } else {
                System.out.println("No harvestable block found.");
                this.harvestCooldown = 800;
            }
        }


        if (this.targetPos != null) {
            this.entity.getNavigation().startMovingTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), 1.1F);
            System.out.println("Moving towards: " + this.targetPos);

            // Check if the entity is close enough to harvest
            if (this.entity.squaredDistanceTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ()) < 2.0) {
                harvestBlock(this.targetPos);
            }
        }
    }


    private BlockPos findHarvestableBlock(int radius) {
        BlockPos entityPos = this.entity.getBlockPos();
        if (this.harvestableBlocks != null ){
            this.harvestableBlocks.clear();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -1; y <= 1; y++) { // Check the block directly below and above
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