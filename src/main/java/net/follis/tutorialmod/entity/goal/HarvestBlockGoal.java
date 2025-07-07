package net.follis.tutorialmod.entity.goal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.BlockPos;
import java.util.EnumSet;

public class HarvestBlockGoal extends Goal {
    private final AnimalEntity entity;
    private final Block blockToHarvest; // The block type to harvest
    private final int range;
    private int harvestCooldown;
    private BlockPos targetPos;

    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 10% chance to try harvesting
        return this.entity.getRandom().nextInt(10) == 0;
    }

    @Override
    public void start() {
        this.harvestCooldown = 0;
        //this.targetPos = findHarvestableBlock(this.range);
    }

    @Override
    public void tick() {
        if (this.harvestCooldown > 0) {
            this.harvestCooldown--;
            this.targetPos = null;
            return;
        } else {
            this.targetPos = findHarvestableBlock(this.range);
        }


        if (this.targetPos != null) {
            this.entity.getNavigation().startMovingTo((double)this.targetPos.getX(), (double)this.targetPos.getY(), (double)this.targetPos.getZ(), (double)1.1F);
            System.out.println("Moving towards: " + this.targetPos);

            // Check if the entity is close enough to harvest
            if (this.entity.squaredDistanceTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ()) < 2.0) {
                harvestBlock(this.targetPos);
            }
        }
    }

    private BlockPos findHarvestableBlock(int radius) {
        BlockPos entityPos = this.entity.getBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) { // Check the block directly below and above
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = entityPos.add(x, y, z);
                    BlockState blockState = this.entity.getWorld().getBlockState(pos);

                    if (blockState.isOf(this.blockToHarvest) && canHarvest(blockState)) {
                        System.out.println("harvestable block found.");
                        return pos; // Return the position of the block to harvest
                    }
                }
            }
        }
        this.harvestCooldown = 400; // Cooldown before the next harvest
        System.out.println("No harvestable block found.");
        return null; // No harvestable block found
    }

    private boolean canHarvest(BlockState blockState) {
        if (this.blockToHarvest instanceof CropBlock cropBlock) {
            return cropBlock.isMature(blockState);
        }
        return true; // Allow harvesting for non-CropBlocks
    }

    private void harvestBlock(BlockPos pos) {
        this.harvestCooldown = 40; // Cooldown before the next harvest
        this.entity.getWorld().breakBlock(pos, true, this.entity);
    }
}