package net.follis.tutorialmod.entity.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.InfestedBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class GoldenSilverfishEntity extends SilverfishEntity {

    public GoldenSilverfishEntity(EntityType<? extends SilverfishEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        CallForHelpGoal callForHelpGoal = new CallForHelpGoal(this);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new PowderSnowJumpGoal(this, this.getWorld()));
        this.goalSelector.add(3, callForHelpGoal);
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new GoldenSilverfishEntity.WanderAndInfestGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }


    static class CallForHelpGoal extends Goal {
        private final GoldenSilverfishEntity goldenSilverfish;
        private int delay;

        public CallForHelpGoal(GoldenSilverfishEntity goldenSilverfish) {
            this.goldenSilverfish = goldenSilverfish;
        }

        public void onHurt() {
            if (this.delay == 0) {
                this.delay = this.getTickCount(20);
            }
        }

        @Override
        public boolean canStart() {
            return this.delay > 0;
        }

        @Override
        public void tick() {
            this.delay--;
            if (this.delay <= 0) {
                World world = this.goldenSilverfish.getWorld();
                Random random = this.goldenSilverfish.getRandom();
                BlockPos blockPos = this.goldenSilverfish.getBlockPos();

                for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                    for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                        for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                            BlockPos blockPos2 = blockPos.add(j, i, k);
                            BlockState blockState = world.getBlockState(blockPos2);
                            Block block = blockState.getBlock();
                            if (block instanceof InfestedBlock) {
                                if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                                    world.breakBlock(blockPos2, true, this.goldenSilverfish);
                                } else {
                                    world.setBlockState(blockPos2, ((InfestedBlock)block).toRegularState(world.getBlockState(blockPos2)), Block.NOTIFY_ALL);
                                }

                                if (random.nextBoolean()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    static class WanderAndInfestGoal extends WanderAroundGoal {
        @Nullable
        private Direction direction;
        private boolean canInfest;

        public WanderAndInfestGoal(GoldenSilverfishEntity goldenSilverfish) {
            super(goldenSilverfish, 1.0, 10);
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.mob.getTarget() != null) {
                return false;
            } else if (!this.mob.getNavigation().isIdle()) {
                return false;
            } else {
                Random random = this.mob.getRandom();
                if (this.mob.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) && random.nextInt(toGoalTicks(10)) == 0) {
                    this.direction = Direction.random(random);
                    BlockPos blockPos = BlockPos.ofFloored(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).offset(this.direction);
                    BlockState blockState = this.mob.getWorld().getBlockState(blockPos);
                    if (InfestedBlock.isInfestable(blockState)) {
                        this.canInfest = true;
                        return true;
                    }
                }

                this.canInfest = false;
                return super.canStart();
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.canInfest ? false : super.shouldContinue();
        }

        @Override
        public void start() {
            if (!this.canInfest) {
                super.start();
            } else {
                WorldAccess worldAccess = this.mob.getWorld();
                BlockPos blockPos = BlockPos.ofFloored(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).offset(this.direction);
                BlockState blockState = worldAccess.getBlockState(blockPos);
                if (InfestedBlock.isInfestable(blockState)) {
                    worldAccess.setBlockState(blockPos, InfestedBlock.fromRegularState(blockState), Block.NOTIFY_ALL);
                    this.mob.playSpawnEffects();
                    this.mob.discard();
                }
            }
        }
    }

}
