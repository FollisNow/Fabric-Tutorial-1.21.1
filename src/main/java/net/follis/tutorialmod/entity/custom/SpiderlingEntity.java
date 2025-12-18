package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.entity.passive.WolfEntity.FOLLOW_TAMED_PREDICATE;

public class SpiderlingEntity extends TameableEntity implements Angerable {

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ANGER;
    private static final TrackedData<Byte> SPIDER_FLAGS;
    private static final TrackedData<Float> GROWTH_SIZE = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;

    public SpiderlingEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setTamed(false, false);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
    }
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(1, new TameableEntity.TameableEscapeDangerGoal(1.5F, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new AvoidLlamaGoal<>(this, LlamaEntity.class, 24.0F, 1.5F, 1.5F));
        this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(5, new MeleeAttackGoal(this, 1.0F, true));
        this.goalSelector.add(6, new FollowOwnerGoal(this, 1.0F, 10.0F, 2.0F));
        this.goalSelector.add(7, new AnimalMateGoal(this, 1.0F));
        this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0F));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new RevengeGoal(this)).setGroupRevenge());
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(5, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE));
        this.targetSelector.add(6, new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
        this.targetSelector.add(7, new ActiveTargetGoal<>(this, AbstractSkeletonEntity.class, false));
        this.targetSelector.add(8, new UniversalAngerGoal<>(this, true));
    }

    private void tryTame(PlayerEntity player) {
        if (this.random.nextInt(3) == 0) {
            this.setOwner(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setSitting(true);
            this.getWorld().sendEntityStatus(this, (byte)7);
        } else {
            this.getWorld().sendEntityStatus(this, (byte)6);
        }
    }
    public boolean canBreedWith(AnimalEntity other) {
        if (other == this) {
            return false;
        } else if (!this.isTamed()) {
            return false;
        } else if (!(other instanceof SpiderlingEntity spiderlingEntity)) {
            return false;
        } else {
            if (!spiderlingEntity.isTamed()) {
                return false;
            } else if (spiderlingEntity.isInSittingPose()) {
                return false;
            } else {
                return this.isInLove() && spiderlingEntity.isInLove();
            }
        }
    }

    protected void updateAttributesForTamed() {
        if (this.isTamed()) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40.0F);
            this.setHealth(40.0F);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(8.0F);
        }
    }
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (!this.getWorld().isClient || this.isBaby() && this.isBreedingItem(itemStack)) {
            if (this.isTamed()) {
                if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    itemStack.decrementUnlessCreative(1, player);
                    FoodComponent foodComponent = itemStack.get(DataComponentTypes.FOOD);
                    float f = foodComponent != null ? (float)foodComponent.nutrition() : 1.0F;
                    this.heal(2.0F * f);
                    return ActionResult.success(this.getWorld().isClient());
                } else if (!itemStack.isOf(Items.SHEARS) || !this.isOwner(player)) {
                    ActionResult actionResult = super.interactMob(player, hand);
                    if (!actionResult.isAccepted() && this.isOwner(player)) {
                        this.setSitting(!this.isSitting());
                        this.jumping = false;
                        this.navigation.stop();
                        this.setTarget(null);
                        return ActionResult.SUCCESS_NO_ITEM_USED;
                    } else {
                        return actionResult;
                    }
                } else {
                    return ActionResult.SUCCESS;
                }
            } else if (itemStack.isIn(ModTags.Items.LOCUST_ITEMS) && !this.hasAngerTime()) {
                itemStack.decrementUnlessCreative(1, player);
                this.tryTame(player);
                return ActionResult.SUCCESS;
            } else if (itemStack.getItem() instanceof AbstractEntityJarItem) {
                return ActionResult.PASS;
            }
            else {
                return super.interactMob(player, hand);
            }
        } else {
            boolean bl = this.isOwner(player) || this.isTamed() || itemStack.isIn(ModTags.Items.LOCUST_ITEMS) && !this.isTamed() && !this.hasAngerTime();
            return bl ? ActionResult.CONSUME : ActionResult.PASS;
        }
    }

    public void tickMovement() {
        super.tickMovement();

        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }

    }

    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            this.setClimbingWall(this.horizontalCollision);
        }

    }
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.getWorld().isClient) {
                this.setSitting(false);
            }

            return super.damage(source, amount);
        }
    }
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity) && !(target instanceof ArmorStandEntity)) {
            if (target instanceof SpiderlingEntity spiderlingEntity) {
                return !spiderlingEntity.isTamed() || spiderlingEntity.getOwner() != owner;
            } else {
                if (target instanceof PlayerEntity playerEntity) {
                    if (owner instanceof PlayerEntity playerEntity2) {
                        if (!playerEntity2.shouldDamagePlayer(playerEntity)) {
                            return false;
                        }
                    }
                }

                if (target instanceof AbstractHorseEntity abstractHorseEntity) {
                    if (abstractHorseEntity.isTame()) {
                        return false;
                    }
                }

                if (target instanceof TameableEntity tameableEntity) {
                    return !tameableEntity.isTamed();
                }

                return true;
            }
        } else {
            return false;
        }
    }


    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(ModTags.Items.LOCUST_ITEMS);
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        SpiderlingEntity baby = ModEntities.SPIDERLING.create(world);
        if (baby != null && entity instanceof SpiderlingEntity spiderling) {
            if (((this.getVariant() != SpiderlingVariant.DEFAULT || spiderling.getVariant() != SpiderlingVariant.DEFAULT) && random.nextInt(100) == 0) ||
                    (this.getVariant() == SpiderlingVariant.DEFAULT && spiderling.getVariant() == SpiderlingVariant.DEFAULT)) {
                baby.setVariant(SpiderlingVariant.DEFAULT);
            } else {
                baby.setVariant(this.getVariant());
            }

            if (baby.getVariant() == SpiderlingVariant.DEFAULT) {
                world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }

            //growth is inherited from the parents average size + variance centered on 0, +/- 10%
            float babySize = (this.getGrowthSize() + spiderling.getGrowthSize()) / 2 + (random.nextFloat() * 2f - 1f) * 0.1f;
            baby.setGrowthSize(babySize);

        }
        return baby;
    }

//    @Override
//    public float getScale() {
//        AttributeContainer attributeContainer = this.getAttributes();
//        return attributeContainer == null ? 1.0F : this.clampScale((float)attributeContainer.getValue(EntityAttributes.GENERIC_SCALE) * this.getGrowthSize());
//    }


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
        return null;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {

    }

    @Override
    public void chooseRandomAngerTime() {

    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(ANGER, 0);
        builder.add(SPIDER_FLAGS, (byte)0);
        builder.add(GROWTH_SIZE, 1f);

    }
    public boolean isClimbing() {
        return this.isClimbingWall();
    }
    public boolean isClimbingWall() {
        return (this.dataTracker.get(SPIDER_FLAGS) & 1) != 0;
    }

    public void setClimbingWall(boolean climbing) {
        byte b = this.dataTracker.get(SPIDER_FLAGS);
        if (climbing) {
            b = (byte)(b | 1);
        } else {
            b = (byte)(b & -2);
        }

        this.dataTracker.set(SPIDER_FLAGS, b);
    }
    public SpiderlingVariant getVariant() {
        return SpiderlingVariant.byId(this.getTypeVariant() & 255);
    }

    private int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }
    private void setVariant(SpiderlingVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    private float getGrowthSize() {
        return this.dataTracker.get(GROWTH_SIZE);
    }
    private void setGrowthSize(float growthSize) {
        this.dataTracker.set(GROWTH_SIZE, growthSize);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 7.0F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        nbt.putFloat("GrowthSize", this.getGrowthSize());
        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        this.dataTracker.set(GROWTH_SIZE, nbt.getFloat("GrowthSize"));
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {

        Optional<RegistryKey<Biome>> currentBiomeKey = world.getBiome(this.getBlockPos()).getKey();
        SpiderlingVariant variant;
        if (this.random.nextInt(100) <= 5) {
            variant = SpiderlingVariant.DEFAULT;
        }
        else if (currentBiomeKey.isPresent() && biomeMap.containsKey(currentBiomeKey.get()) && this.random.nextFloat() < 0.5F){
            variant = biomeMap.get(currentBiomeKey.get());
        } else {
            variant = SpiderlingVariant.byId(this.random.nextBetween(0, SpiderlingVariant.values().length - 1));
        }
        this.setVariant(variant);
        this.setGrowthSize((this.random.nextFloat() * 2f - 1)*0.05f + 1);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    private static final Map<RegistryKey<Biome>, SpiderlingVariant> biomeMap = new HashMap<>() {{
        put(BiomeKeys.PLAINS, SpiderlingVariant.DEFAULT);
    }};


    class AvoidLlamaGoal<T extends LivingEntity> extends FleeEntityGoal<T> {
        private final SpiderlingEntity spiderling;

        public AvoidLlamaGoal(final SpiderlingEntity spiderling, final Class<T> fleeFromType, final float distance, final double slowSpeed, final double fastSpeed) {
            super(spiderling, fleeFromType, distance, slowSpeed, fastSpeed);
            this.spiderling = spiderling;
        }

        public boolean canStart() {
            if (super.canStart() && this.targetEntity instanceof LlamaEntity) {
                return !this.spiderling.isTamed() && this.isScaredOf((LlamaEntity)this.targetEntity);
            } else {
                return false;
            }
        }

        private boolean isScaredOf(LlamaEntity llama) {
            return llama.getStrength() >= SpiderlingEntity.this.random.nextInt(5);
        }

        public void start() {
            SpiderlingEntity.this.setTarget(null);
            super.start();
        }

        public void tick() {
            SpiderlingEntity.this.setTarget(null);
            super.tick();
        }
    }


    static {
        ANGER = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
        SPIDER_FLAGS = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.BYTE);
    }
}
