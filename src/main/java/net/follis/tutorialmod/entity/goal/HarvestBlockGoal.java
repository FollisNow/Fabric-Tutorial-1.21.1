package net.follis.tutorialmod.entity.goal;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

public class HarvestBlockGoal extends Goal {
    private final AnimalEntity entity;
    private final Block blockToHarvest; // The block type to harvest
    private final int range;
    private final boolean isRandomized;
    private int searchCooldown;
    private BlockPos targetPos;

    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.isRandomized = false;
    }
    public HarvestBlockGoal(AnimalEntity entity, Block blockToHarvest, int range, boolean isRandomized) {
        this.entity = entity;
        this.blockToHarvest = blockToHarvest;
        this.range = range;
        this.isRandomized = isRandomized;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 10% chance to try harvesting
        return this.entity.getRandom().nextInt(10) == 0;
    }

    @Override
    public void start() {
        this.searchCooldown = 0;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        searchRoutine(40, 800);

        if (this.targetPos != null) {
            this.entity.getNavigation().startMovingTo(this.targetPos.up().getX(), this.targetPos.up().getY(), this.targetPos.up().getZ(), 1.1F);

            // Check if the entity is close enough to harvest
            if (this.targetPos.isWithinDistance(this.entity.getBlockPos(), 2)) {
                harvestBlock(this.targetPos);
            }
        }
    }

    private void searchRoutine(int minCooldown, int maxCooldown) {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
        } else {

            Predicate<BlockPos> condition = blockPos -> {
                BlockState state = this.entity.getWorld().getBlockState(blockPos);
                Block block = state.getBlock();

                if (!(this.blockToHarvest instanceof CropBlock)){
                    return block == this.blockToHarvest;
                } else {
                    if (block == this.blockToHarvest) {
                        return ((CropBlock) block).isMature(state);
                    }
                }
                return false;
            };

            Optional<BlockPos> closestCropOrBlock = BlockPos.findClosest(this.entity.getBlockPos(), this.range, this.range, condition);
            this.targetPos = closestCropOrBlock.orElse(null);
            if (this.targetPos != null){
                this.searchCooldown = minCooldown;
            } else {
                this.searchCooldown = maxCooldown;
            }
        }
    }

    private void harvestBlock(BlockPos pos) {
        this.entity.getWorld().breakBlock(pos, true, this.entity);
    }
}