package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MothEntity extends AnimalEntity implements Flutterer, Angerable {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    public static final int field_28638 = MathHelper.ceil(1.4959966F);

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT;
    private static final TrackedData<Integer> ANGER;

    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;


    public MothEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 20, true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BiteGoal(this, 1.4F, true));

        this.goalSelector.add(1, new AnimalMateGoal(this, 1.15D));
        this.goalSelector.add(2, new TemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new SwimGoal(this));
        this.goalSelector.add(7, new GrowCropsGoal());

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
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 7.0F)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.65F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F);
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

            if (baby.getVariant() == MothVariant.OMEN) {

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

        this.setNoGravity(isNavigating());

        if (this.getWorld().isClient()) {
            this.setupAnimationStates();
        }
    }

    @Override
    protected void mobTick() {
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), false);
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

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());

        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
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

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 80;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
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
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world) {
            public boolean isValidPosition(BlockPos pos) {
                BlockState state = this.world.getBlockState(pos);
                return !state.isAir() || state.isSolid(); // Allow landing on solid blocks
            }
        };
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(false);
        birdNavigation.setCanEnterOpenDoors(true);

        MobNavigation mobNavigation = new MobNavigation(this, world) {
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };
        mobNavigation.setCanEnterOpenDoors(true);
        mobNavigation.setCanSwim(false);
        mobNavigation.setCanPathThroughDoors(false);

        if (birdNavigation.getTargetPos() != null && birdNavigation.getTargetPos().isWithinDistance(this.getPos(), 2))
            return mobNavigation;
        return birdNavigation;
    }

    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
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

    public boolean isGrounded() {
        return isOnGround();
    }

    class BiteGoal extends MeleeAttackGoal {
        BiteGoal(final PathAwareEntity mob, final double speed, final boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
        }

        public boolean canStart() {
            return super.canStart() && MothEntity.this.hasAngerTime() && MothEntity.this.getVariant() == MothVariant.OMEN;
        }

        public boolean shouldContinue() {
            return super.shouldContinue() && MothEntity.this.hasAngerTime() && MothEntity.this.getVariant() == MothVariant.OMEN;
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

    class GrowCropsGoal extends NotAngryGoal {
        public boolean canMothStart() {
            return MothEntity.this.random.nextFloat() > 0.975F;
        }

        public boolean canMothContinue() {
            return this.canMothStart();
        }

        public void tick() {
            if (MothEntity.this.random.nextInt(this.getTickCount(30)) == 0) {
                for(int i = 0; i <= 2; ++i) {
                    BlockPos blockPos = MothEntity.this.getBlockPos().up(2-i);
                    BlockState blockState = MothEntity.this.getWorld().getBlockState(blockPos);
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
                            ((Fertilizable)blockState.getBlock()).grow((ServerWorld) MothEntity.this.getWorld(), MothEntity.this.random, blockPos, blockState);
                        }

                        if (blockState2 != null) {
                            MothEntity.this.getWorld().syncWorldEvent(2011, blockPos, 15);
                            MothEntity.this.getWorld().setBlockState(blockPos, blockState2);
                        }
                    }
                }

            }
        }
    }

    abstract class NotAngryGoal extends Goal {
        public abstract boolean canMothStart();

        public abstract boolean canMothContinue();

        public boolean canStart() {
            return this.canMothStart() && !MothEntity.this.hasAngerTime();
        }

        public boolean shouldContinue() {
            return this.canMothContinue() && !MothEntity.this.hasAngerTime();
        }
    }

    static {
        ANGER = DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
        DATA_ID_TYPE_VARIANT = DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    }
}
