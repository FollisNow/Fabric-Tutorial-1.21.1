package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.block.custom.InfestedGoldenBlocks;
import net.follis.goldenscarabmod.util.IBugVariants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PowderSnowJumpGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Golden silverfish: hides inside GOLDEN blocks only (see {@link InfestedGoldenBlocks}) and wakes
 * infested golden blocks when hurt.
 *
 * <p>Still extends SilverfishEntity so your vanilla-style model/renderer, sounds, arthropod tags and
 * body-yaw behavior keep working, but everything that touches infested blocks is rewritten here:
 * the vanilla goals are never registered and the vanilla path-favor check is overridden.
 */
public class GoldenSilverfishEntity extends SilverfishEntity implements IBugVariants {
    /**
     * Assigned in initGoals(), which runs inside the MobEntity constructor, so this field must NOT have an
     * initializer (it would be reset to null right after super()).
     */
    @Nullable
    private CallForHelpGoal callForHelpGoal;

    public GoldenSilverfishEntity(EntityType<? extends SilverfishEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    @Override
    protected void initGoals() {
        this.callForHelpGoal = new CallForHelpGoal(this);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new PowderSnowJumpGoal(this, this.getWorld()));
        this.goalSelector.add(3, this.callForHelpGoal);
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAndInfestGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                player -> !this.isWearingGoldOrImmune(player)));
    }

    /**
     * Your old version never triggered its call-for-help goal: vanilla's damage() only notifies
     * vanilla's own (private, never assigned here) goal.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.getWorld().isClient
                && this.callForHelpGoal != null
                && (source.getAttacker() != null || source.isIn(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH))) {
            this.callForHelpGoal.onHurt();
        }
        return super.damage(source, amount);
    }

    /**
     * Single choke point for gold immunity: whatever goal tries to target a player wearing gold
     * (revenge, group revenge, active target), the target is dropped.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && this.isWearingGoldOrImmune(target)) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        // a player who puts gold on mid-chase stops being a target
        if (!this.getWorld().isClient && this.age % 10 == 0) {
            LivingEntity target = this.getTarget();
            if (target != null && this.isWearingGoldOrImmune(target)) {
                this.setTarget(null);
            }
        }
    }

    /** Required by IBugVariants (gold checks). The golden silverfish has no variants. */
    @Override
    public int getTypeVariant() {
        return 0;
    }

    /** Vanilla prefers walking next to VANILLA infestable blocks; prefer golden ones instead. */
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return InfestedGoldenBlocks.isInfestable(world.getBlockState(pos.down()))
                ? 10.0F
                : super.getPathfindingFavor(pos, world);
    }

    // =====================================================================
    // Goals
    // =====================================================================

    /** When hurt, wakes nearby infested GOLDEN blocks (closest first, 50% chance to stop after each). */
    static class CallForHelpGoal extends Goal {
        private static final int RANGE_XZ = 10;
        private static final int RANGE_Y = 5;

        private final GoldenSilverfishEntity silverfish;
        private int delay;

        CallForHelpGoal(GoldenSilverfishEntity silverfish) {
            this.silverfish = silverfish;
        }

        void onHurt() {
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
            if (--this.delay > 0) return;
            this.delay = 0;

            World world = this.silverfish.getWorld();
            Random random = this.silverfish.getRandom();
            BlockPos origin = this.silverfish.getBlockPos();

            List<BlockPos> infested = new ArrayList<>();
            for (BlockPos pos : BlockPos.iterate(
                    origin.add(-RANGE_XZ, -RANGE_Y, -RANGE_XZ),
                    origin.add(RANGE_XZ, RANGE_Y, RANGE_XZ))) {
                if (world.getBlockState(pos).getBlock() instanceof InfestedGoldenBlocks) {
                    infested.add(pos.toImmutable()); // iterate() reuses one mutable instance
                }
            }
            infested.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(origin)));

            for (BlockPos pos : infested) {
                BlockState state = world.getBlockState(pos);
                if (!(state.getBlock() instanceof InfestedGoldenBlocks infestedBlock)) continue;

                if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                    // breaking drops it, which spawns the next golden silverfish (see InfestedGoldenBlocks)
                    world.breakBlock(pos, true, this.silverfish);
                } else {
                    world.setBlockState(pos, infestedBlock.toRegularState(state), Block.NOTIFY_ALL);
                }

                if (random.nextBoolean()) return;
            }
        }
    }

    /** Wanders around; sometimes hides inside an adjacent golden block (and disappears into it). */
    static class WanderAndInfestGoal extends WanderAroundGoal {
        @Nullable
        private Direction direction;
        private boolean canInfest;

        WanderAndInfestGoal(GoldenSilverfishEntity silverfish) {
            super(silverfish, 1.0, 10);
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.mob.getTarget() != null || !this.mob.getNavigation().isIdle()) {
                return false;
            }

            Random random = this.mob.getRandom();
            if (this.mob.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)
                    && random.nextInt(toGoalTicks(10)) == 0) {
                this.direction = Direction.random(random);
                BlockState neighbour = this.mob.getWorld().getBlockState(this.neighbourPos());
                if (InfestedGoldenBlocks.isInfestable(neighbour)) {
                    this.canInfest = true;
                    return true;
                }
            }

            this.canInfest = false;
            return super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return !this.canInfest && super.shouldContinue();
        }

        @Override
        public void start() {
            if (!this.canInfest) {
                super.start();
                return;
            }

            World world = this.mob.getWorld();
            BlockPos pos = this.neighbourPos();
            BlockState state = world.getBlockState(pos);
            if (InfestedGoldenBlocks.isInfestable(state)) {
                world.setBlockState(pos, InfestedGoldenBlocks.fromRegularState(state), Block.NOTIFY_ALL);
                this.mob.playSpawnEffects();
                this.mob.discard();
            }
        }

        private BlockPos neighbourPos() {
            return BlockPos.ofFloored(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).offset(this.direction);
        }
    }
}