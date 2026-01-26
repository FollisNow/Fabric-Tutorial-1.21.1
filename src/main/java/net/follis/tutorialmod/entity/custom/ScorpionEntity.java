package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.ai.ScorpionAttackGoal;
import net.follis.tutorialmod.util.IBugVariants;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ScorpionEntity extends AnimalEntity implements Angerable, IBugVariants {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    public int attackAnimationTimeout = 0;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT = DataTracker.registerData(ScorpionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ANGER;
    private static final TrackedData<Byte> SCORPION_FLAGS;
    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;


    public ScorpionEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new ScorpionAttackGoal(this, 1D, true));
        this.goalSelector.add(1, new AnimalMateGoal(this, 1.15D));
        this.goalSelector.add(2, new TemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new SwimGoal(this));

        this.targetSelector.add(1, (new ScorpionRevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(2, new BiteTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 10, false, false, (entity) -> entity instanceof Monster && !(entity instanceof CreeperEntity) && !isWearingGold(entity)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, false, false, this::shouldAngerAt));
    }

    private boolean foodSelector(ItemStack stack) {
        return stack.isIn(ItemTags.BEE_FOOD);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 7.0F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 6.0);
    }
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return true;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        ScorpionEntity baby = ModEntities.SCORPION.create(world);
        if (baby != null && entity instanceof ScorpionEntity) {
            baby.setVariant(ScorpionVariant.DESERT);
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
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            setupAnimationStates();
        }

//        if (!this.getWorld().isClient) {
//            boolean bl = this.getTarget() != null && this.getTarget().squaredDistanceTo(this) < (double)2.0F;
//            this.setScorpionFlag(2, bl);
//        }
    }
    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.age);
        } else {
            --this.idleAnimationTimeout;
        }

        if (this.isAttacking() && attackAnimationTimeout <= 0) {
            this.attackAnimationTimeout = 40;
            this.attackAnimationState.start(this.age);
        } else {
            --this.attackAnimationTimeout;
        }

        if (!this.isAttacking()) {
            this.attackAnimationState.stop();
        }
    }

    @Override
    protected void mobTick() {
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), false);
        }
    }

    // ATTACK PERKS
    @Override
    public boolean tryAttack(Entity target) {
        boolean bl = super.tryAttack(target);
        if (bl && target instanceof LivingEntity livingEntity) {
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 5 * 20), this);
            this.onAttacking(target);
            return true;
        }
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getAttacker();
            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 2.0F;
            }
            return super.damage(source, amount);
        }
    }
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (damageSource.isOf(DamageTypes.CACTUS) || damageSource.isOf(DamageTypes.SWEET_BERRY_BUSH) || damageSource.getAttacker() instanceof EnderDragonEntity || damageSource.isOf(DamageTypes.CRAMMING)) {
            return true;
        } else {
            return super.isInvulnerableTo(damageSource);
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

    /* VARIANT & POTION EFFECT*/
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(ANGER, 0);
        builder.add(SCORPION_FLAGS, (byte)0);
    }
    @Override
    public boolean isAttacking() {
        return this.getScorpionFlag(2);
    }

    @Override
    public void setAttacking(boolean attacking) {
        this.setScorpionFlag(2, attacking);
    }
    public ScorpionVariant getVariant() {
        return ScorpionVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(ScorpionVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }
    private void setScorpionFlag(int bit, boolean value) {
        if (value) {
            this.dataTracker.set(SCORPION_FLAGS, (byte)(this.dataTracker.get(SCORPION_FLAGS) | bit));
        } else {
            this.dataTracker.set(SCORPION_FLAGS, (byte)(this.dataTracker.get(SCORPION_FLAGS) & ~bit));
        }
    }
    public boolean getScorpionFlag(int location) {
        return (this.dataTracker.get(SCORPION_FLAGS) & location) != 0;
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
        ScorpionVariant variant = ScorpionVariant.DESERT;
        this.setVariant(variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    private static final Map<RegistryKey<Biome>, ScorpionVariant> biomeMap = new HashMap<>() {{
        put(BiomeKeys.DESERT, ScorpionVariant.DESERT);
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

    class ScorpionRevengeGoal extends RevengeGoal {
        ScorpionRevengeGoal(final ScorpionEntity scorpion) {
            super(scorpion);
        }

        public boolean shouldContinue() {
            return ScorpionEntity.this.hasAngerTime() && super.shouldContinue() && ScorpionEntity.this.getVariant() == ScorpionVariant.DESERT;
        }

        protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
            if (mob instanceof ScorpionEntity && this.mob.canSee(target) && !((ScorpionEntity) mob).isWearingGold(target)) {
                mob.setTarget(target);
            } else {
                mob.setTarget(null);
            }

        }
    }

    static class BiteTargetGoal extends ActiveTargetGoal<PlayerEntity> {
        public BiteTargetGoal(ScorpionEntity scorpion) {
            super(scorpion, PlayerEntity.class, 10, true, false, scorpion::shouldAngerAt);
        }

        public boolean canStart() {
            return this.canBite() && super.canStart();
        }

        public boolean shouldContinue() {
            boolean bl = this.canBite();
            if (this.mob.getTarget() != null && mob instanceof ScorpionEntity scorpion && !scorpion.isWearingGold(scorpion.getTarget())) {
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
            ScorpionEntity scorpion = (ScorpionEntity)this.mob;
            return scorpion.hasAngerTime();
        }
    }

    abstract class NotAngryGoal extends Goal {
        public abstract boolean canScorpionStart();

        public abstract boolean canScorpionContinue();

        public boolean canStart() {
            return this.canScorpionStart() && !ScorpionEntity.this.hasAngerTime();
        }

        public boolean shouldContinue() {
            return this.canScorpionContinue() && !ScorpionEntity.this.hasAngerTime();
        }
    }

    static {
        ANGER = DataTracker.registerData(ScorpionEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
        SCORPION_FLAGS = DataTracker.registerData(ScorpionEntity.class, TrackedDataHandlerRegistry.BYTE);
    }
}
