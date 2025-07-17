package net.follis.tutorialmod.entity.custom;

import com.google.common.collect.Lists;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.custom.AmethystBeeHiveBlockEntity;
import net.follis.tutorialmod.datagen.ModPointOfInterestTypeTagProvider;
import net.follis.tutorialmod.datagen.ModPointOfInterestTypes;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.util.ModTags;
import net.follis.tutorialmod.villager.ModVillagers;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.NoWaterTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmethystBeeEntity extends AnimalEntity implements Angerable, Flutterer {
    public static final float field_30271 = 120.32113F;
    public static final int field_28638 = MathHelper.ceil(1.4959966F);
    private static final TrackedData<Byte> BEE_FLAGS;
    private static final TrackedData<Integer> ANGER;
    private static final int NEAR_TARGET_FLAG = 2;
    private static final int HAS_STUNG_FLAG = 4;
    private static final int HAS_NECTAR_FLAG = 8;
    private static final int MAX_LIFETIME_AFTER_STINGING = 1200;
    private static final int FLOWER_NAVIGATION_START_TICKS = 2400;
    private static final int POLLINATION_FAIL_TICKS = 3600;
    private static final int field_30287 = 4;
    private static final int MAX_POLLINATED_CROPS = 10;
    private static final int NORMAL_DIFFICULTY_STING_POISON_DURATION = 10;
    private static final int HARD_DIFFICULTY_STING_POISON_DURATION = 18;
    private static final int TOO_FAR_DISTANCE = 32;
    private static final int field_30292 = 2;
    private static final int MIN_HIVE_RETURN_DISTANCE = 16;
    private static final int field_30294 = 20;
    public static final String CROPS_GROWN_SINCE_POLLINATION_KEY = "CropsGrownSincePollination";
    public static final String CANNOT_ENTER_HIVE_TICKS_KEY = "CannotEnterHiveTicks";
    public static final String TICKS_SINCE_POLLINATION_KEY = "TicksSincePollination";
    public static final String HAS_STUNG_KEY = "HasStung";
    public static final String HAS_NECTAR_KEY = "HasNectar";
    public static final String FLOWER_POS_KEY = "flower_pos";
    public static final String HIVE_POS_KEY = "hive_pos";
    private static final UniformIntProvider ANGER_TIME_RANGE;
    @Nullable
    private UUID angryAt;
    private float currentPitch;
    private float lastPitch;
    private int ticksSinceSting;
    int ticksSincePollination;
    private int cannotEnterHiveTicks;
    private int cropsGrownSincePollination;
    private static final int field_30274 = 200;
    int ticksLeftToFindHive;
    private static final int field_30275 = 200;
    int ticksUntilCanPollinate;
    @Nullable
    BlockPos flowerPos;
    @Nullable
    BlockPos hivePos;
    PollinateGoal pollinateGoal;
    MoveToHiveGoal moveToHiveGoal;
    private MoveToFlowerGoal moveToFlowerGoal;
    private int ticksInsideWater;


    public AmethystBeeEntity(EntityType<? extends AmethystBeeEntity> entityType, World world) {
        super(entityType, world);
        this.ticksUntilCanPollinate = MathHelper.nextInt(this.random, 20, 60);
        this.moveControl = new FlightMoveControl(this, 20, true);
        this.lookControl = new BeeLookControl(this);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
    }

    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(BEE_FLAGS, (byte)0);
        builder.add(ANGER, 0);
    }

    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    protected void initGoals() {
        this.goalSelector.add(0, new StingGoal(this, 1.4F, true));
        this.goalSelector.add(1, new EnterHiveGoal());
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0F));
        this.goalSelector.add(3, new TemptGoal(this, 1.25F, (stack) -> stack.isOf(Items.AMETHYST_SHARD), false));
        this.pollinateGoal = new PollinateGoal();
        this.goalSelector.add(4, this.pollinateGoal);
        this.goalSelector.add(5, new FollowParentGoal(this, 1.25F));
        this.goalSelector.add(5, new FindHiveGoal());
        this.moveToHiveGoal = new MoveToHiveGoal();
        this.goalSelector.add(5, this.moveToHiveGoal);
        this.moveToFlowerGoal = new MoveToFlowerGoal();
        this.goalSelector.add(6, this.moveToFlowerGoal);
        this.goalSelector.add(7, new GrowCropsGoal());
        this.goalSelector.add(8, new BeeWanderAroundGoal());
        this.goalSelector.add(9, new SwimGoal(this));
        this.targetSelector.add(1, (new BeeRevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(2, new StingTargetGoal(this));
        this.targetSelector.add(3, new UniversalAngerGoal<>(this, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0F)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F);
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.hasHive()) {
            assert this.getHivePos() != null;
            nbt.put("hive_pos", NbtHelper.fromBlockPos(this.getHivePos()));
        }

        if (this.hasFlower()) {
            assert this.getFlowerPos() != null;
            nbt.put("flower_pos", NbtHelper.fromBlockPos(this.getFlowerPos()));
        }

        nbt.putBoolean("HasNectar", this.hasNectar());
        nbt.putBoolean("HasStung", this.hasStung());
        nbt.putInt("TicksSincePollination", this.ticksSincePollination);
        nbt.putInt("CannotEnterHiveTicks", this.cannotEnterHiveTicks);
        nbt.putInt("CropsGrownSincePollination", this.cropsGrownSincePollination);
        this.writeAngerToNbt(nbt);
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.hivePos = NbtHelper.toBlockPos(nbt, "hive_pos").orElse(null);
        this.flowerPos = NbtHelper.toBlockPos(nbt, "flower_pos").orElse(null);
        super.readCustomDataFromNbt(nbt);
        this.setHasNectar(nbt.getBoolean("HasNectar"));
        this.setHasStung(nbt.getBoolean("HasStung"));
        this.ticksSincePollination = nbt.getInt("TicksSincePollination");
        this.cannotEnterHiveTicks = nbt.getInt("CannotEnterHiveTicks");
        this.cropsGrownSincePollination = nbt.getInt("CropsGrownSincePollination");
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    public boolean tryAttack(Entity target) {
        DamageSource damageSource = this.getDamageSources().sting(this);
        boolean bl = target.damage(damageSource, (float)((int)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
        if (bl) {
            World var5 = this.getWorld();
            if (var5 instanceof ServerWorld serverWorld) {
                EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
            }

            if (target instanceof LivingEntity livingEntity) {
                livingEntity.setStingerCount(livingEntity.getStingerCount() + 1);
                int i = 0;
                if (this.getWorld().getDifficulty() == Difficulty.NORMAL) {
                    i = 10;
                } else if (this.getWorld().getDifficulty() == Difficulty.HARD) {
                    i = 18;
                }

                if (i > 0) {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, i * 20, 0), this);
                }
            }

            this.setHasStung(true);
            this.stopAnger();
            this.playSound(SoundEvents.ENTITY_BEE_STING, 1.0F, 1.0F);
        }

        return bl;
    }

    public void tick() {
        super.tick();
        if (this.hasNectar() && this.getCropsGrownSincePollination() < 10 && this.random.nextFloat() < 0.05F) {
            for(int i = 0; i < this.random.nextInt(2) + 1; ++i) {
                this.addParticle(this.getWorld(), this.getX() - (double)0.3F, this.getX() + (double)0.3F, this.getZ() - (double)0.3F, this.getZ() + (double)0.3F, this.getBodyY(0.5F));
            }
        }

        this.updateBodyPitch();
    }

    private void addParticle(World world, double lastX, double x, double lastZ, double z, double y) {
        world.addParticle(ParticleTypes.FALLING_NECTAR, MathHelper.lerp(world.random.nextDouble(), lastX, x), y, MathHelper.lerp(world.random.nextDouble(), lastZ, z), 0.0F, 0.0F, 0.0F);
    }

    void startMovingTo(BlockPos pos) {
        Vec3d vec3d = Vec3d.ofBottomCenter(pos);
        int i = 0;
        BlockPos blockPos = this.getBlockPos();
        int j = (int)vec3d.y - blockPos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int m = blockPos.getManhattanDistance(pos);
        if (m < 15) {
            k = m / 2;
            l = m / 2;
        }

        Vec3d vec3d2 = NoWaterTargeting.find(this, k, l, i, vec3d, (float)Math.PI / 10F);
        if (vec3d2 != null) {
            this.navigation.setRangeMultiplier(0.5F);
            this.navigation.startMovingTo(vec3d2.x, vec3d2.y, vec3d2.z, 1.0F);
        }
    }

    @Nullable
    public BlockPos getFlowerPos() {
        return this.flowerPos;
    }

    public boolean hasFlower() {
        return this.flowerPos != null;
    }

    public void setFlowerPos(@Nullable BlockPos flowerPos) {
        this.flowerPos = flowerPos;
    }

    @Debug
    public int getMoveGoalTicks() {
        return Math.max(this.moveToHiveGoal.ticks, this.moveToFlowerGoal.ticks);
    }

    @Debug
    public List<BlockPos> getPossibleHives() {
        return this.moveToHiveGoal.possibleHives;
    }

    private boolean failedPollinatingTooLong() {
        return this.ticksSincePollination > 3600;
    }

    boolean canEnterHive() {
        if (this.cannotEnterHiveTicks <= 0 && this.pollinateGoal.isRunning() && !this.hasStung() && this.getTarget() == null) {
            boolean bl = this.failedPollinatingTooLong() || this.getWorld().isRaining() || this.getWorld().isNight() || this.hasNectar();
            return bl && !this.isHiveNearFire();
        } else {
            return false;
        }
    }

    public void setCannotEnterHiveTicks(int cannotEnterHiveTicks) {
        this.cannotEnterHiveTicks = cannotEnterHiveTicks;
    }

    public float getBodyPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.lastPitch, this.currentPitch);
    }

    private void updateBodyPitch() {
        this.lastPitch = this.currentPitch;
        if (this.isNearTarget()) {
            this.currentPitch = Math.min(1.0F, this.currentPitch + 0.2F);
        } else {
            this.currentPitch = Math.max(0.0F, this.currentPitch - 0.24F);
        }

    }

    protected void mobTick() {
        boolean bl = this.hasStung();
        if (this.isInsideWaterOrBubbleColumn()) {
            ++this.ticksInsideWater;
        } else {
            this.ticksInsideWater = 0;
        }

        if (this.ticksInsideWater > 20) {
            this.damage(this.getDamageSources().drown(), 1.0F);
        }

        if (bl) {
            ++this.ticksSinceSting;
            if (this.ticksSinceSting % 5 == 0 && this.random.nextInt(MathHelper.clamp(1200 - this.ticksSinceSting, 1, 1200)) == 0) {
                this.damage(this.getDamageSources().generic(), this.getHealth());
            }
        }

        if (!this.hasNectar()) {
            ++this.ticksSincePollination;
        }

        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), false);
        }

    }

    public void resetPollinationTicks() {
        this.ticksSincePollination = 0;
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) {
            return false;
        } else {
            BlockEntity blockEntity = this.getWorld().getBlockEntity(this.hivePos);
            return blockEntity instanceof AmethystBeeHiveBlockEntity && ((AmethystBeeHiveBlockEntity)blockEntity).isNearFire();
        }
    }

    public int getAngerTime() {
        return (Integer)this.dataTracker.get(ANGER);
    }

    public void setAngerTime(int angerTime) {
        this.dataTracker.set(ANGER, angerTime);
    }

    @Nullable
    public UUID getAngryAt() {
        return this.angryAt;
    }

    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    private boolean doesHiveHaveSpace(BlockPos pos) {
        BlockEntity blockEntity = this.getWorld().getBlockEntity(pos);
        if (blockEntity instanceof AmethystBeeHiveBlockEntity) {
            return !((AmethystBeeHiveBlockEntity)blockEntity).isFullOfBees();
        } else {
            return false;
        }
    }

    @Debug
    public boolean hasHive() {
        return this.hivePos != null;
    }

    @Nullable
    @Debug
    public BlockPos getHivePos() {
        return this.hivePos;
    }

    @Debug
    public GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    int getCropsGrownSincePollination() {
        return this.cropsGrownSincePollination;
    }

    private void resetCropCounter() {
        this.cropsGrownSincePollination = 0;
    }

    void addCropCounter() {
        ++this.cropsGrownSincePollination;
    }

    public void tickMovement() {
        super.tickMovement();
        if (!this.getWorld().isClient) {
            if (this.cannotEnterHiveTicks > 0) {
                --this.cannotEnterHiveTicks;
            }

            if (this.ticksLeftToFindHive > 0) {
                --this.ticksLeftToFindHive;
            }

            if (this.ticksUntilCanPollinate > 0) {
                --this.ticksUntilCanPollinate;
            }

            boolean bl = this.hasAngerTime() && !this.hasStung() && this.getTarget() != null && this.getTarget().squaredDistanceTo(this) < (double)4.0F;
            this.setNearTarget(bl);
            if (this.age % 20 == 0 && !this.isHiveValid()) {
                this.hivePos = null;
            }
        }

    }

    boolean isHiveValid() {
        if (!this.hasHive()) {
            return false;
        } else if (this.isTooFar(this.hivePos)) {
            return false;
        } else {
            BlockEntity blockEntity = this.getWorld().getBlockEntity(this.hivePos);
            return blockEntity != null && blockEntity.getType() == ModBlockEntities.AMETHYST_BEE_HIVE_BE;
        }
    }

    public boolean hasNectar() {
        return this.getBeeFlag(8);
    }

    void setHasNectar(boolean hasNectar) {
        if (hasNectar) {
            this.resetPollinationTicks();
        }

        this.setBeeFlag(8, hasNectar);
    }

    public boolean hasStung() {
        return this.getBeeFlag(4);
    }

    private void setHasStung(boolean hasStung) {
        this.setBeeFlag(4, hasStung);
    }

    private boolean isNearTarget() {
        return this.getBeeFlag(2);
    }

    private void setNearTarget(boolean nearTarget) {
        this.setBeeFlag(2, nearTarget);
    }

    boolean isTooFar(BlockPos pos) {
        return !this.isWithinDistance(pos, 32);
    }

    private void setBeeFlag(int bit, boolean value) {
        if (value) {
            this.dataTracker.set(BEE_FLAGS, (byte)(this.dataTracker.get(BEE_FLAGS) | bit));
        } else {
            this.dataTracker.set(BEE_FLAGS, (byte)(this.dataTracker.get(BEE_FLAGS) & ~bit));
        }

    }

    private boolean getBeeFlag(int location) {
        return (this.dataTracker.get(BEE_FLAGS) & location) != 0;
    }


    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world) {
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }

            public void tick() {
                if (AmethystBeeEntity.this.pollinateGoal.isRunning()) {
                    super.tick();
                }
            }
        };
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(false);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.AMETHYST_SHARD);
    }

    boolean isFlowers(BlockPos pos) {
        return this.getWorld().canSetBlock(pos) && (
                this.getWorld().getBlockState(pos).isOf(Blocks.SMALL_AMETHYST_BUD) ||
                this.getWorld().getBlockState(pos).isOf(Blocks.MEDIUM_AMETHYST_BUD) ||
                this.getWorld().getBlockState(pos).isOf(Blocks.LARGE_AMETHYST_BUD) ||
                this.getWorld().getBlockState(pos).isOf(Blocks.AMETHYST_CLUSTER));
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    protected SoundEvent getAmbientSound() {
        return null;
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BEE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BEE_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public AmethystBeeEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return ModEntities.AMETHYST_BEE.create(serverWorld);
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    public boolean isFlappingWings() {
        return this.isInAir() && this.age % field_28638 == 0;
    }

    public boolean isInAir() {
        return !this.isOnGround();
    }

    public void onHoneyDelivered() {
        this.setHasNectar(false);
        this.resetCropCounter();
    }

    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.getWorld().isClient) {
                this.pollinateGoal.cancel();
            }

            return super.damage(source, amount);
        }
    }

    protected void swimUpward(TagKey<Fluid> fluid) {
        this.setVelocity(this.getVelocity().add(0.0F, 0.01, 0.0F));
    }

    public Vec3d getLeashOffset() {
        return new Vec3d(0.0F, 0.5F * this.getStandingEyeHeight(), this.getWidth() * 0.2F);
    }

    boolean isWithinDistance(BlockPos pos, int distance) {
        return pos.isWithinDistance(this.getBlockPos(), distance);
    }

    public void setHivePos(@Nullable BlockPos pos) {
        this.hivePos = pos;
    }

    static {
        BEE_FLAGS = DataTracker.registerData(AmethystBeeEntity.class, TrackedDataHandlerRegistry.BYTE);
        ANGER = DataTracker.registerData(AmethystBeeEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }

    class BeeRevengeGoal extends RevengeGoal {
        BeeRevengeGoal(final AmethystBeeEntity bee) {
            super(bee, new Class[0]);
        }

        public boolean shouldContinue() {
            return AmethystBeeEntity.this.hasAngerTime() && super.shouldContinue();
        }

        protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
            if (mob instanceof AmethystBeeEntity && this.mob.canSee(target)) {
                mob.setTarget(target);
            }

        }
    }

    static class StingTargetGoal extends ActiveTargetGoal<PlayerEntity> {
        public StingTargetGoal(AmethystBeeEntity bee) {
            super(bee, PlayerEntity.class, 10, true, false, bee::shouldAngerAt);
        }

        public boolean canStart() {
            return this.canSting() && super.canStart();
        }

        public boolean shouldContinue() {
            boolean bl = this.canSting();
            if (bl && this.mob.getTarget() != null) {
                return super.shouldContinue();
            } else {
                this.target = null;
                return false;
            }
        }

        private boolean canSting() {
            AmethystBeeEntity amethystBeeEntity = (AmethystBeeEntity)this.mob;
            return amethystBeeEntity.hasAngerTime() && !amethystBeeEntity.hasStung();
        }
    }

    abstract class NotAngryGoal extends Goal {
        public abstract boolean canBeeStart();

        public abstract boolean canBeeContinue();

        public boolean canStart() {
            return this.canBeeStart() && !AmethystBeeEntity.this.hasAngerTime();
        }

        public boolean shouldContinue() {
            return this.canBeeContinue() && !AmethystBeeEntity.this.hasAngerTime();
        }
    }

    class BeeWanderAroundGoal extends Goal {
        private static final int MAX_DISTANCE = 22;

        BeeWanderAroundGoal() {
            this.setControls(EnumSet.of(Control.MOVE));
        }

        public boolean canStart() {
            return AmethystBeeEntity.this.navigation.isIdle() && AmethystBeeEntity.this.random.nextInt(10) == 0;
        }

        public boolean shouldContinue() {
            return AmethystBeeEntity.this.navigation.isFollowingPath();
        }

        public void start() {
            Vec3d vec3d = this.getRandomLocation();
            if (vec3d != null) {
                AmethystBeeEntity.this.navigation.startMovingAlong(AmethystBeeEntity.this.navigation.findPathTo(BlockPos.ofFloored(vec3d), 1), (double)1.0F);
            }

        }

        @Nullable
        private Vec3d getRandomLocation() {
            Vec3d vec3d2;
            if (AmethystBeeEntity.this.isHiveValid() && !AmethystBeeEntity.this.isWithinDistance(AmethystBeeEntity.this.hivePos != null ? AmethystBeeEntity.this.hivePos : null, 22)) {
                Vec3d vec3d = Vec3d.ofCenter(AmethystBeeEntity.this.hivePos);
                vec3d2 = vec3d.subtract(AmethystBeeEntity.this.getPos()).normalize();
            } else {
                vec3d2 = AmethystBeeEntity.this.getRotationVec(0.0F);
            }

            int i = 8;
            Vec3d vec3d3 = AboveGroundTargeting.find(AmethystBeeEntity.this, 8, 7, vec3d2.x, vec3d2.z, ((float)Math.PI / 2F), 3, 1);
            return vec3d3 != null ? vec3d3 : NoPenaltySolidTargeting.find(AmethystBeeEntity.this, 8, 4, -2, vec3d2.x, vec3d2.z, (float)Math.PI / 2F);
        }
    }

    @Debug
    public class MoveToHiveGoal extends NotAngryGoal {
        public static final int field_30295 = 600;
        int ticks;
        private static final int field_30296 = 3;
        final List<BlockPos> possibleHives;
        @Nullable
        private Path path;
        private static final int field_30297 = 60;
        private int ticksUntilLost;

        MoveToHiveGoal() {
            this.ticks = AmethystBeeEntity.this.getWorld().random.nextInt(10);
            this.possibleHives = Lists.newArrayList();
            this.setControls(EnumSet.of(Control.MOVE));
        }

        public boolean canBeeStart() {
            return AmethystBeeEntity.this.hivePos != null
                    && !AmethystBeeEntity.this.hasPositionTarget()
                    && AmethystBeeEntity.this.canEnterHive() && !this.isCloseEnough(AmethystBeeEntity.this.hivePos)
                    && AmethystBeeEntity.this.getWorld().getBlockState(AmethystBeeEntity.this.hivePos).isIn(BlockTags.BEEHIVES);
        }

        public boolean canBeeContinue() {
            return this.canBeeStart();
        }

        public void start() {
            this.ticks = 0;
            this.ticksUntilLost = 0;
            super.start();
        }

        public void stop() {
            this.ticks = 0;
            this.ticksUntilLost = 0;
            AmethystBeeEntity.this.navigation.stop();
            AmethystBeeEntity.this.navigation.resetRangeMultiplier();
        }

        public void tick() {
            if (AmethystBeeEntity.this.hivePos != null) {
                ++this.ticks;
                if (this.ticks > this.getTickCount(600)) {
                    this.makeChosenHivePossibleHive();
                } else if (!AmethystBeeEntity.this.navigation.isFollowingPath()) {
                    if (!AmethystBeeEntity.this.isWithinDistance(AmethystBeeEntity.this.hivePos, 16)) {
                        if (AmethystBeeEntity.this.isTooFar(AmethystBeeEntity.this.hivePos)) {
                            this.setLost();
                        } else {
                            AmethystBeeEntity.this.startMovingTo(AmethystBeeEntity.this.hivePos);
                        }
                    } else {
                        boolean bl = this.startMovingToFar(AmethystBeeEntity.this.hivePos);
                        if (!bl) {
                            this.makeChosenHivePossibleHive();
                        } else if (this.path != null && (AmethystBeeEntity.this.navigation.getCurrentPath() != null && AmethystBeeEntity.this.navigation.getCurrentPath().equalsPath(this.path))) {
                            ++this.ticksUntilLost;
                            if (this.ticksUntilLost > 60) {
                                this.setLost();
                                this.ticksUntilLost = 0;
                            }
                        } else {
                            this.path = AmethystBeeEntity.this.navigation.getCurrentPath();
                        }

                    }
                }
            }
        }

        private boolean startMovingToFar(BlockPos pos) {
            AmethystBeeEntity.this.navigation.setRangeMultiplier(10.0F);
            AmethystBeeEntity.this.navigation.startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 2, 1.0F);
            return AmethystBeeEntity.this.navigation.getCurrentPath() != null && AmethystBeeEntity.this.navigation.getCurrentPath().reachesTarget();
        }

        boolean isPossibleHive(BlockPos pos) {
            return this.possibleHives.contains(pos);
        }

        private void addPossibleHive(BlockPos pos) {
            this.possibleHives.add(pos);

            while(this.possibleHives.size() > 3) {
                this.possibleHives.removeFirst();
            }

        }

        void clearPossibleHives() {
            this.possibleHives.clear();
        }

        private void makeChosenHivePossibleHive() {
            if (AmethystBeeEntity.this.hivePos != null) {
                this.addPossibleHive(AmethystBeeEntity.this.hivePos);
            }

            this.setLost();
        }

        private void setLost() {
            AmethystBeeEntity.this.hivePos = null;
            AmethystBeeEntity.this.ticksLeftToFindHive = 200;
        }

        private boolean isCloseEnough(BlockPos pos) {
            if (AmethystBeeEntity.this.isWithinDistance(pos, 2)) {
                return true;
            } else {
                Path path = AmethystBeeEntity.this.navigation.getCurrentPath();
                return path != null && path.getTarget().equals(pos) && path.reachesTarget() && path.isFinished();
            }
        }
    }

    public class MoveToFlowerGoal extends NotAngryGoal {
        private static final int MAX_FLOWER_NAVIGATION_TICKS = 600;
        int ticks;

        MoveToFlowerGoal() {
            this.ticks = AmethystBeeEntity.this.getWorld().random.nextInt(10);
            this.setControls(EnumSet.of(Control.MOVE));
        }

        public boolean canBeeStart() {
            return AmethystBeeEntity.this.flowerPos != null && !AmethystBeeEntity.this.hasPositionTarget()
                    && this.shouldMoveToFlower() && AmethystBeeEntity.this.isFlowers(AmethystBeeEntity.this.flowerPos)
                    && !AmethystBeeEntity.this.isWithinDistance(AmethystBeeEntity.this.flowerPos, 2);
        }

        public boolean canBeeContinue() {
            return this.canBeeStart();
        }

        public void start() {
            this.ticks = 0;
            super.start();
        }

        public void stop() {
            this.ticks = 0;
            AmethystBeeEntity.this.navigation.stop();
            AmethystBeeEntity.this.navigation.resetRangeMultiplier();
        }

        public void tick() {
            if (AmethystBeeEntity.this.flowerPos != null) {
                ++this.ticks;
                if (this.ticks > this.getTickCount(600)) {
                    AmethystBeeEntity.this.flowerPos = null;
                } else if (!AmethystBeeEntity.this.navigation.isFollowingPath()) {
                    if (AmethystBeeEntity.this.isTooFar(AmethystBeeEntity.this.flowerPos)) {
                        AmethystBeeEntity.this.flowerPos = null;
                    } else {
                        AmethystBeeEntity.this.startMovingTo(AmethystBeeEntity.this.flowerPos);
                    }
                }
            }
        }

        private boolean shouldMoveToFlower() {
            return AmethystBeeEntity.this.ticksSincePollination > 2400;
        }
    }

    class BeeLookControl extends LookControl {
        BeeLookControl(final MobEntity entity) {
            super(entity);
        }

        public void tick() {
            if (!AmethystBeeEntity.this.hasAngerTime()) {
                super.tick();
            }
        }

        protected boolean shouldStayHorizontal() {
            return AmethystBeeEntity.this.pollinateGoal.isRunning();
        }
    }

    class PollinateGoal extends NotAngryGoal {
        private static final int field_30300 = 400;
        private static final int field_30301 = 20;
        private static final int field_30302 = 60;
        private final Predicate<BlockState> flowerPredicate = (state) -> {
            if (state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED)) {
                return false;
            } else return state.isOf(Blocks.SMALL_AMETHYST_BUD) ||
                    state.isOf(Blocks.MEDIUM_AMETHYST_BUD) ||
                    state.isOf(Blocks.LARGE_AMETHYST_BUD) ||
                    state.isOf(Blocks.AMETHYST_CLUSTER);
        };
        private static final double field_30303 = 0.1;
        private static final int field_30304 = 25;
        private static final float field_30305 = 0.35F;
        private static final float field_30306 = 0.6F;
        private static final float field_30307 = 0.33333334F;
        private int pollinationTicks;
        private int lastPollinationTick;
        private boolean running;
        @Nullable
        private Vec3d nextTarget;
        private int ticks;
        private static final int field_30308 = 600;

        PollinateGoal() {
            this.setControls(EnumSet.of(Control.MOVE));
        }

        public boolean canBeeStart() {
            if (AmethystBeeEntity.this.ticksUntilCanPollinate > 0) {
                return false;
            } else if (AmethystBeeEntity.this.hasNectar()) {
                return false;
            } else if (AmethystBeeEntity.this.getWorld().isRaining()) {
                return false;
            } else {
                Optional<BlockPos> optional = this.getFlower();
                if (optional.isPresent()) {
                    AmethystBeeEntity.this.flowerPos = (BlockPos)optional.get();
                    AmethystBeeEntity.this.navigation.startMovingTo(
                            (double)AmethystBeeEntity.this.flowerPos.getX() + (double)0.5F,
                            (double)AmethystBeeEntity.this.flowerPos.getY() + (double)0.5F,
                            (double)AmethystBeeEntity.this.flowerPos.getZ() + (double)0.5F, 1.2F);
                    return true;
                } else {
                    AmethystBeeEntity.this.ticksUntilCanPollinate = MathHelper.nextInt(AmethystBeeEntity.this.random, 20, 60);
                    return false;
                }
            }
        }

        public boolean canBeeContinue() {
            if (!this.running) {
                return false;
            } else if (!AmethystBeeEntity.this.hasFlower()) {
                return false;
            } else if (AmethystBeeEntity.this.getWorld().isRaining()) {
                return false;
            } else if (this.completedPollination()) {
                return AmethystBeeEntity.this.random.nextFloat() < 0.2F;
            } else if (AmethystBeeEntity.this.age % 20 == 0 && !AmethystBeeEntity.this.isFlowers(AmethystBeeEntity.this.flowerPos)) {
                AmethystBeeEntity.this.flowerPos = null;
                return false;
            } else {
                return true;
            }
        }

        private boolean completedPollination() {
            return this.pollinationTicks > 400;
        }

        boolean isRunning() {
            return !this.running;
        }

        void cancel() {
            this.running = false;
        }

        public void start() {
            this.pollinationTicks = 0;
            this.ticks = 0;
            this.lastPollinationTick = 0;
            this.running = true;
            AmethystBeeEntity.this.resetPollinationTicks();
        }

        public void stop() {
            if (this.completedPollination()) {
                AmethystBeeEntity.this.setHasNectar(true);
            }

            this.running = false;
            AmethystBeeEntity.this.navigation.stop();
            AmethystBeeEntity.this.ticksUntilCanPollinate = 200;
        }

        public boolean shouldRunEveryTick() {
            return true;
        }

        public void tick() {
            ++this.ticks;
            if (this.ticks > 600) {
                AmethystBeeEntity.this.flowerPos = null;
            } else {
                assert AmethystBeeEntity.this.flowerPos != null;
                Vec3d vec3d = Vec3d.ofBottomCenter(AmethystBeeEntity.this.flowerPos).add(0.0F, 0.6F, 0.0F);
                if (vec3d.distanceTo(AmethystBeeEntity.this.getPos()) > (double)1.0F) {
                    this.nextTarget = vec3d;
                    this.moveToNextTarget();
                } else {
                    if (this.nextTarget == null) {
                        this.nextTarget = vec3d;
                    }

                    boolean bl = AmethystBeeEntity.this.getPos().distanceTo(this.nextTarget) <= 0.1;
                    boolean bl2 = true;
                    if (!bl && this.ticks > 600) {
                        AmethystBeeEntity.this.flowerPos = null;
                    } else {
                        if (bl) {
                            boolean bl3 = AmethystBeeEntity.this.random.nextInt(25) == 0;
                            if (bl3) {
                                this.nextTarget = new Vec3d(vec3d.getX() + (double)this.getRandomOffset(), vec3d.getY(), vec3d.getZ() + (double)this.getRandomOffset());
                                AmethystBeeEntity.this.navigation.stop();
                            } else {
                                bl2 = false;
                            }

                            AmethystBeeEntity.this.getLookControl().lookAt(vec3d.getX(), vec3d.getY(), vec3d.getZ());
                        }

                        if (bl2) {
                            this.moveToNextTarget();
                        }

                        ++this.pollinationTicks;
                        if (AmethystBeeEntity.this.random.nextFloat() < 0.05F && this.pollinationTicks > this.lastPollinationTick + 60) {
                            this.lastPollinationTick = this.pollinationTicks;
                            AmethystBeeEntity.this.playSound(SoundEvents.ENTITY_BEE_POLLINATE, 1.0F, 1.0F);
                        }

                    }
                }
            }
        }

        private void moveToNextTarget() {
            assert this.nextTarget != null;
            AmethystBeeEntity.this.getMoveControl().moveTo(this.nextTarget.getX(), this.nextTarget.getY(), this.nextTarget.getZ(), 0.35F);
        }

        private float getRandomOffset() {
            return (AmethystBeeEntity.this.random.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
        }

        private Optional<BlockPos> getFlower() {
            return this.findFlower(this.flowerPredicate);
        }

        private Optional<BlockPos> findFlower(Predicate<BlockState> predicate) {
            BlockPos blockPos = AmethystBeeEntity.this.getBlockPos();
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            for(int i = 0; (double)i <= 5.0; i = i > 0 ? -i : 1 - i) {
                for(int j = 0; (double)j < 5.0; ++j) {
                    for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            mutable.set(blockPos, k, i - 1, l);
                            if (blockPos.isWithinDistance(mutable, 5.0) && predicate.test(AmethystBeeEntity.this.getWorld().getBlockState(mutable))) {
                                return Optional.of(mutable);
                            }
                        }
                    }
                }
            }

            return Optional.empty();
        }
    }

    class FindHiveGoal extends NotAngryGoal {
        public boolean canBeeStart() {
            return AmethystBeeEntity.this.ticksLeftToFindHive == 0 && !AmethystBeeEntity.this.hasHive() && AmethystBeeEntity.this.canEnterHive();
        }

        public boolean canBeeContinue() {
            return false;
        }

        public void start() {
            AmethystBeeEntity.this.ticksLeftToFindHive = 200;
            List<BlockPos> list = this.getNearbyFreeHives();
            if (!list.isEmpty()) {
                for(BlockPos blockPos : list) {
                    if (!AmethystBeeEntity.this.moveToHiveGoal.isPossibleHive(blockPos)) {
                        AmethystBeeEntity.this.hivePos = blockPos;
                        return;
                    }
                }

                AmethystBeeEntity.this.moveToHiveGoal.clearPossibleHives();
                AmethystBeeEntity.this.hivePos = list.getFirst();
            }
        }

        private List<BlockPos> getNearbyFreeHives() {
            BlockPos blockPos = AmethystBeeEntity.this.getBlockPos();
            PointOfInterestStorage pointOfInterestStorage = ((ServerWorld)AmethystBeeEntity.this.getWorld()).getPointOfInterestStorage();
            Stream<PointOfInterest> stream = pointOfInterestStorage.getInCircle((poiType) -> poiType.isIn(ModTags.PointOfInterestTypes.AMETHYST_BEE_HOME), blockPos, 20, PointOfInterestStorage.OccupationStatus.ANY);

            return stream.map(PointOfInterest::getPos).filter(AmethystBeeEntity.this::doesHiveHaveSpace).sorted(Comparator.comparingDouble((blockPos2) -> blockPos2.getSquaredDistance(blockPos))).collect(Collectors.toList());
        }
    }

    class GrowCropsGoal extends NotAngryGoal {
        static final int field_30299 = 30;

        public boolean canBeeStart() {
            if (AmethystBeeEntity.this.getCropsGrownSincePollination() >= 10) {
                return false;
            } else if (AmethystBeeEntity.this.random.nextFloat() < 0.3F) {
                return false;
            } else {
                return AmethystBeeEntity.this.hasNectar() && AmethystBeeEntity.this.isHiveValid();
            }
        }

        public boolean canBeeContinue() {
            return this.canBeeStart();
        }

        public void tick() {
            if (AmethystBeeEntity.this.random.nextInt(this.getTickCount(30)) == 0) {
                for(int i = 1; i <= 2; ++i) {
                    BlockPos blockPos = AmethystBeeEntity.this.getBlockPos().down(i);
                    BlockState blockState = AmethystBeeEntity.this.getWorld().getBlockState(blockPos);
                    Block block = blockState.getBlock();
                    BlockState blockState2 = null;
                    if (blockState.isIn(BlockTags.BEE_GROWABLES)) {
                        if (block instanceof CropBlock cropBlock) {
                            if (!cropBlock.isMature(blockState)) {
                                blockState2 = cropBlock.withAge(cropBlock.getAge(blockState) + 1);
                            }
                        } else if (block instanceof StemBlock) {
                            int j = blockState.get(StemBlock.AGE);
                            if (j < 7) {
                                blockState2 = blockState.with(StemBlock.AGE, j + 1);
                            }
                        } else if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
                            int j = blockState.get(SweetBerryBushBlock.AGE);
                            if (j < 3) {
                                blockState2 = blockState.with(SweetBerryBushBlock.AGE, j + 1);
                            }
                        } else if (blockState.isOf(Blocks.CAVE_VINES) || blockState.isOf(Blocks.CAVE_VINES_PLANT)) {
                            ((Fertilizable)blockState.getBlock()).grow((ServerWorld)AmethystBeeEntity.this.getWorld(), AmethystBeeEntity.this.random, blockPos, blockState);
                        }

                        if (blockState2 != null) {
                            AmethystBeeEntity.this.getWorld().syncWorldEvent(2011, blockPos, 15);
                            AmethystBeeEntity.this.getWorld().setBlockState(blockPos, blockState2);
                            AmethystBeeEntity.this.addCropCounter();
                        }
                    }
                }

            }
        }
    }

    class StingGoal extends MeleeAttackGoal {
        StingGoal(final PathAwareEntity mob, final double speed, final boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
        }

        public boolean canStart() {
            return super.canStart() && AmethystBeeEntity.this.hasAngerTime() && !AmethystBeeEntity.this.hasStung();
        }

        public boolean shouldContinue() {
            return super.shouldContinue() && AmethystBeeEntity.this.hasAngerTime() && !AmethystBeeEntity.this.hasStung();
        }
    }

    class EnterHiveGoal extends NotAngryGoal {
        public boolean canBeeStart() {
            if (AmethystBeeEntity.this.hasHive() && AmethystBeeEntity.this.canEnterHive()) {
                assert AmethystBeeEntity.this.hivePos != null;
                if (AmethystBeeEntity.this.hivePos.isWithinDistance(AmethystBeeEntity.this.getPos(), 2.0F)) {
                    BlockEntity blockEntity = AmethystBeeEntity.this.getWorld().getBlockEntity(AmethystBeeEntity.this.hivePos);
                    if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
                        if (!amethystBeeHiveBlockEntity.isFullOfBees()) {
                            return true;
                        }

                        AmethystBeeEntity.this.hivePos = null;
                    }
                }
            }

            return false;
        }

        public boolean canBeeContinue() {
            return false;
        }

        public void start() {
            BlockEntity blockEntity = AmethystBeeEntity.this.getWorld().getBlockEntity(AmethystBeeEntity.this.hivePos);
            if (blockEntity instanceof AmethystBeeHiveBlockEntity amethystBeeHiveBlockEntity) {
                amethystBeeHiveBlockEntity.tryEnterHive(AmethystBeeEntity.this);
            }

        }
    }
}

