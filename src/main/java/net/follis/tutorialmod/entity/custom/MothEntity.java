package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.particle.ModParticles;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class MothEntity extends AnimalEntity implements Flutterer, Angerable {
    public final AnimationState flyingAnimationState = new AnimationState();
    public final AnimationState roostingAnimationState = new AnimationState();

    public static final int field_28638 = MathHelper.ceil(1.4959966F);
    public float flapProgress;
    public float maxWingDeviation;
    public float prevMaxWingDeviation;
    public float prevFlapProgress;
    private float flapSpeed = 1.0F;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT;
    private static final TrackedData<Integer> ANGER;
    private static final TrackedData<Boolean> IS_ROOSTING;

    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;


    public MothEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, true);
    }

    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world);
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(true);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }


    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MothBiteGoal(this, 1.4F, true));

        this.goalSelector.add(1, new MothMateGoal(this, 1.15D));
        this.goalSelector.add(2, new MothTemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(4, new LatchGoal(this));
        this.goalSelector.add(5, new FlyToTreeGoal(this, 1.4F, 12, 6));
        this.goalSelector.add(6, new SwimGoal(this));

        this.targetSelector.add(1, (new MothRevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(2, new BiteTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 10, false, false, (entity) -> entity instanceof Monster && !(entity instanceof CreeperEntity) && !isWearingGold(entity)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, false, false, this::shouldAngerAt));
    }

    private boolean foodSelector(ItemStack stack) {
        if(this.getVariant() == MothVariant.OMEN) {
            return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(ModItems.GRILLED_LOCUST);
        } else {
            return stack.isIn(ItemTags.BEE_FOOD) || stack.isIn(ModTags.Items.LOCUST_ITEMS) || stack.isOf(ModItems.GRILLED_LOCUST) || stack.isIn(ItemTags.BEE_FOOD);
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0F).add(EntityAttributes.GENERIC_FLYING_SPEED, 1F).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0F);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        MothEntity baby = ModEntities.MOTH.create(world);
        if (baby != null && entity instanceof MothEntity moth) {
            if (((this.getVariant() != MothVariant.OMEN || moth.getVariant() != MothVariant.OMEN) && random.nextInt(100) == 0) ||
                    (this.getVariant() == MothVariant.OMEN && moth.getVariant() == MothVariant.OMEN)) {
                baby.setVariant(MothVariant.OMEN);
            } else {
                baby.setVariant(this.getVariant());
            }

        }
        return baby;
    }
    @Override
    public void breed(ServerWorld world, AnimalEntity other) {
        super.breed(world, other);
        this.setBreedingAge(1200);
        other.setBreedingAge(1200);
    }
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(ItemTags.BEE_FOOD);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRoosting()) {
            this.setVelocity(Vec3d.ZERO);
            this.getNavigation().stop();
        }

        if (this.getNavigation().getTargetPos() != null && this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d target = this.getNavigation().getTargetPos().toBottomCenterPos();

            serverWorld.spawnParticles(ModParticles.PINK_GARNET_PARTICLE,
                    target.x, target.y,
                    target.z, 1, 0, 0, 0, 0);
        }


        this.updateAnimations();
    }

    @Override
    protected void mobTick() {

    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        this.flapWings();
    }

    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.getWorld().isClient && this.isRoosting()) {
                this.setRoosting(false);
            }

            return super.damage(source, amount);
        }
    }

    // TARGETING
    @Override
    public boolean canTarget(EntityType<?> type) {
        return type != EntityType.CREEPER;
    }
    private boolean isWearingGold(LivingEntity entity) {
        for(ItemStack stack : entity.getAllArmorItems()){
            if (stack.isIn(ModTags.Items.GOLDEN_ITEMS))
                return true;
        }
        return false;
    }
    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canTarget(entity) || isWearingGold(entity)) {
            return false;
        } else {
            return entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld()) || entity.getUuid().equals(this.getAngryAt());
        }
    }


    /* VARIANT*/
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(IS_ROOSTING, false);
        builder.add(ANGER, 0);
    }

    public MothVariant getVariant() {
        return MothVariant.byId(this.getTypeVariant() & 255);
    }

    private int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(MothVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);

    }

    public boolean isRoosting() {
        return this.dataTracker.get(IS_ROOSTING);
    }

    protected void setRoosting(boolean bl) {
        this.dataTracker.set(IS_ROOSTING, bl);

    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        nbt.putBoolean("Roosting", this.isRoosting());

        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        this.dataTracker.set(IS_ROOSTING, nbt.getBoolean("Roosting"));
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {

        Optional<RegistryKey<Biome>> currentBiomeKey = world.getBiome(this.getBlockPos()).getKey();
        MothVariant variant;
        if (this.random.nextInt(100) == 0) {
            variant = MothVariant.OMEN;
        }
        else if (currentBiomeKey.isPresent() && biomeMap.containsKey(currentBiomeKey.get()) && this.random.nextFloat() < 0.5F){
            variant = biomeMap.get(currentBiomeKey.get());
        } else {
            variant = MothVariant.byId(this.random.nextBetween(1, MothVariant.values().length - 1));
        }
        this.setVariant(variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    private static final Map<RegistryKey<Biome>, MothVariant> biomeMap = new HashMap<>() {{
        put(BiomeKeys.PLAINS, MothVariant.LADYBUG);
        put(BiomeKeys.SUNFLOWER_PLAINS, MothVariant.LADYBUG);
        put(BiomeKeys.FLOWER_FOREST, MothVariant.LADYBUG);
        put(BiomeKeys.CHERRY_GROVE, MothVariant.LADYBUG);
        put(BiomeKeys.FOREST, MothVariant.BARK);
        put(BiomeKeys.GROVE, MothVariant.BARK);
        put(BiomeKeys.MEADOW, MothVariant.SCARAB);
    }};

    /* SOUNDS */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ALLAY_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PANDA_DEATH;
    }

    private void updateAnimations() {
        if (this.isRoosting()) {
            this.flyingAnimationState.stop();
            this.roostingAnimationState.startIfNotRunning(this.age);
        } else {
            this.roostingAnimationState.stop();
            this.flyingAnimationState.startIfNotRunning(this.age);
        }
    }

    public boolean isFlappingWings() {
        return this.isInAir() && this.age % field_28638 == 0;
    }
    @Override
    public boolean isInAir() {
        return !this.isOnGround();
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {}

    private void flapWings() {
        this.prevFlapProgress = this.flapProgress;
        this.prevMaxWingDeviation = this.maxWingDeviation;
        this.maxWingDeviation += (float)(!this.isOnGround() && !this.hasVehicle() ? 4 : -1) * 0.3F;
        this.maxWingDeviation = MathHelper.clamp(this.maxWingDeviation, 0.0F, 1.0F);
        if (!this.isOnGround() && this.flapSpeed < 1.0F) {
            this.flapSpeed = 1.0F;
        }

        this.flapSpeed *= 0.9F;
        Vec3d vec3d = this.getVelocity();
        if (!this.isOnGround() && vec3d.y < (double)0.0F) {
            this.setVelocity(vec3d.multiply(1.0F, 0.6, 1.0F));
        }

        this.flapProgress += this.flapSpeed * 2.0F;
    }

    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER);
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.dataTracker.set(ANGER, angerTime);

    }

    @Override
    public @Nullable UUID getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;

    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    class MothBiteGoal extends MeleeAttackGoal {
        MothBiteGoal(final MothEntity mob, final double speed, final boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
        }

        public boolean canStart() {
            return super.canStart() && MothEntity.this.hasAngerTime() && MothEntity.this.getVariant() == MothVariant.OMEN;
        }

        public boolean shouldContinue() {
            return super.shouldContinue() && MothEntity.this.hasAngerTime() && MothEntity.this.getVariant() == MothVariant.OMEN;
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting())
                MothEntity.this.setRoosting(false);
        }
    }

    class MothRevengeGoal extends RevengeGoal {
        MothRevengeGoal(final MothEntity Moth) {
            super(Moth);
        }

        public boolean shouldContinue() {
            return MothEntity.this.hasAngerTime() && super.shouldContinue() && MothEntity.this.getVariant() == MothVariant.OMEN;
        }

        protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
            if (mob instanceof MothEntity && this.mob.canSee(target) && !((MothEntity) mob).isWearingGold(target)) {
                mob.setTarget(target);
            } else {
                mob.setTarget(null);
            }

        }
    }

    static class BiteTargetGoal extends ActiveTargetGoal<PlayerEntity> {
        public BiteTargetGoal(MothEntity Moth) {
            super(Moth, PlayerEntity.class, 10, true, false, Moth::shouldAngerAt);
        }

        public boolean canStart() {
            return this.canBite() && super.canStart();
        }

        public boolean shouldContinue() {
            boolean bl = this.canBite();
            if (this.mob.getTarget() != null && mob instanceof MothEntity Moth && !Moth.isWearingGold(Moth.getTarget())) {
                this.target = null;
                return false;
            } else if (bl && this.mob.getTarget() != null ) {
                return super.shouldContinue();
            } else {
                this.target = null;
                return false;
            }
        }

        private boolean canBite() {
            MothEntity MothEntity = (MothEntity)this.mob;
            return MothEntity.hasAngerTime();
        }
    }

    static class FlyToTreeGoal extends Goal {
        protected final MothEntity mob;
        protected double targetX;
        protected double targetY;
        protected double targetZ;
        protected final double speed;
        protected final int horizontalRange;
        protected final int verticalRange;
        protected int chance;

        public FlyToTreeGoal(MothEntity mob, double speed, int horizontalRange, int verticalRange) {
            this(mob, speed, 120, verticalRange, horizontalRange);
        }
        public FlyToTreeGoal(MothEntity entity, double speed, int horizontalRange, int verticalRange, int chance) {
            this.mob = entity;
            this.speed = speed;
            this.horizontalRange = horizontalRange;
            this.verticalRange = verticalRange;
            this.chance = chance;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        public boolean canStart() {
            if (!this.mob.hasControllingPassenger() && (this.mob.random.nextInt(1200) == 0) || !this.mob.isRoosting()) {
                Vec3d vec3d = this.getWanderTarget();
                if (vec3d == null) {
                    return false;
                } else {
                    this.targetX = vec3d.x;
                    this.targetY = vec3d.y;
                    this.targetZ = vec3d.z;
                    return true;
                }
            }
            return false;
        }
        public boolean shouldContinue() {
            return !this.mob.getNavigation().isIdle() && !this.mob.hasControllingPassenger();
        }
        public void start() {
            this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
            if (this.mob.isRoosting())
                this.mob.setRoosting(false);
        }
        public void stop() {
            this.mob.getNavigation().stop();
            super.stop();
        }

        @Nullable
        protected Vec3d getWanderTarget() {
            Vec3d vec3d = null;
            if (this.mob.isTouchingWater()) {
                vec3d = FuzzyTargeting.find(this.mob, this.horizontalRange, this.verticalRange);
            } else {
                if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
                    vec3d = getRandomValidTreePos(serverWorld, this.mob.getBlockPos());
                }
            }
            return vec3d == null ? this.mob.getPos() : vec3d;
        }
        @Nullable
        private Vec3d getRandomValidTreePos(ServerWorld world, BlockPos originalPos) {
            HashSet<BlockPos> validPositions = new HashSet<>();
            BlockPos.Mutable offsetPos = originalPos.mutableCopy();

            for (BlockPos targetPos : BlockPos.iterateOutwards(originalPos, this.horizontalRange, this.verticalRange, this.horizontalRange)) {
                offsetPos.set(targetPos);
                if (!isValidTreeStructure(world, targetPos)) continue;
                validPositions.add(targetPos.toImmutable());
            }

            return !validPositions.isEmpty() ? new ArrayList<>(validPositions).get(world.random.nextInt(validPositions.size()-1)).toImmutable().toBottomCenterPos() : null;
        }
        private static boolean isValidTreeStructure(ServerWorld world, BlockPos pos) {
            if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return false;
            Predicate<BlockState> isTreeBlock = blockState -> blockState.isIn(BlockTags.LEAVES) || blockState.isIn(BlockTags.LOGS);

            int validTreeSides = 0;
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos entry = pos.offset(direction);
                if (!isTreeBlock.test(world.getBlockState(entry))) {
                    continue;
                }
                if (!isTreeBlock.test(world.getBlockState(entry.up()))) {
                    continue;
                }
                if (!isTreeBlock.test(world.getBlockState(entry.down()))) {
                    continue;
                }
                validTreeSides++;
            }
            return validTreeSides > 0 && validTreeSides < 4;
        }
    }

    static class LatchGoal extends Goal {
        protected final MothEntity mob;

        LatchGoal(MothEntity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canStart() {
            if (this.mob.getWorld() instanceof ServerWorld serverWorld){
                return this.mob.random.nextInt(10) == 0 && !this.mob.isRoosting() && isValidTreeStructure(serverWorld, this.mob.getBlockPos());
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            boolean bl = false;
            if (this.mob.getWorld() instanceof ServerWorld serverWorld){
                 bl = this.mob.getRandom().nextInt(6000) != 0 && isValidTreeStructure(serverWorld, this.mob.getBlockPos())
                         && !this.mob.hasControllingPassenger();
            }
            if (!bl) {
                this.mob.setRoosting(false);
            }
            return bl;
        }

        @Override
        public void start() {
            this.mob.setRoosting(true);
            this.mob.setPosition(this.mob.getBlockPos().toBottomCenterPos());
            this.mob.getNavigation().stop();

            if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
                Predicate<BlockState> isTreeBlock = blockState -> blockState.isIn(BlockTags.LEAVES) || blockState.isIn(BlockTags.LOGS);
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos entry = this.mob.getBlockPos().offset(direction);
                    if (isTreeBlock.test(serverWorld.getBlockState(entry))) {
                        alignForwardToDirection(direction, this.mob);
                        break;
                    }
                }
            }

        }

        private void alignForwardToDirection(Direction direction, MothEntity moth) {
            switch (direction) {
                case NORTH:
                    moth.setHeadYaw(180.0F);
                    moth.setYaw(180.0F);
                    break;
                case SOUTH:
                    moth.setHeadYaw(0.0F);
                    moth.setYaw(0.0F);
                    break;
                case WEST:
                    moth.setHeadYaw(90.0F);
                    moth.setYaw(90.0F);
                    break;
                case EAST:
                    moth.setHeadYaw(-90.0F);
                    moth.setYaw(-90.0F);
                    break;
                default:
                    // Handle invalid direction if needed
                    break;
            }
            mob.setPitch(0.0F); // Set pitch to level (0 degrees)
        }

        private static boolean isValidTreeStructure(ServerWorld world, BlockPos pos) {
            if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return false;
            Predicate<BlockState> isTreeBlock = blockState -> blockState.isIn(BlockTags.LEAVES) || blockState.isIn(BlockTags.LOGS);

            int validTreeSides = 0;
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos entry = pos.offset(direction);
                if (!isTreeBlock.test(world.getBlockState(entry))) {
                    continue;
                }
                if (!isTreeBlock.test(world.getBlockState(entry.up()))) {
                    continue;
                }
                if (!isTreeBlock.test(world.getBlockState(entry.down()))) {
                    continue;
                }
                validTreeSides++;
            }
            return validTreeSides > 0 && validTreeSides < 4;
        }
    }

    class MothMateGoal extends AnimalMateGoal {
        public MothMateGoal(MothEntity animal, double speed) {
            super(animal, speed);
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting())
                MothEntity.this.setRoosting(false);
        }
    }

    class MothTemptGoal extends TemptGoal {
        public MothTemptGoal(PathAwareEntity entity, double speed, Predicate<ItemStack> foodPredicate, boolean canBeScared) {
            super(entity, speed, foodPredicate, canBeScared);
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting())
                MothEntity.this.setRoosting(false);
        }
    }
    static {
        ANGER = DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
        DATA_ID_TYPE_VARIANT = DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.INTEGER);
        IS_ROOSTING = DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }
}
