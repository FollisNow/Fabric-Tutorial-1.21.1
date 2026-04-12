package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.item.ModItems;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem;
import net.follis.tutorialmod.item.custom.VisionMonocleItem;
import net.follis.tutorialmod.util.IBugVariants;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.entity.passive.WolfEntity.FOLLOW_TAMED_PREDICATE;

public class SpiderlingEntity extends TameableEntity implements Angerable, IBugVariants, JumpingMount {
    protected int soundTicks;
    protected float jumpStrength;
    protected boolean inAir;

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ANGER;
    private static final TrackedData<Byte> SPIDER_FLAGS;
    private static final TrackedData<Float> GROWTH_SIZE = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_HEALTH = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MOVEMENT_SPEED = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> JUMP_STRENGTH = DataTracker.registerData(SpiderlingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    public static final float MAXIMUM_SIZE = 2F;
    public static final float MINIMUM_SIZE = 0.25F;
    public static final float MAXIMUM_HEALTH = 32F;
    public static final float MINIMUM_HEALTH = 1F;
    public static final float MAXIMUM_SPEED = 0.50F;
    public static final float MINIMUM_SPEED = 0.05F;
    public static final float MAXIMUM_JUMP = 1F;
    public static final float MINIMUM_JUMP = 0.4F;

    @Nullable
    private UUID angryAt;
    private static final UniformIntProvider ANGER_TIME_RANGE;
    private UUID ownerUuid;

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
        this.goalSelector.add(3, new TemptGoal(this, 1.25D, this::foodSelector, false));
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

    private boolean foodSelector(ItemStack stack) {
        return stack.isIn(ItemTags.MEAT);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
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

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // if server or baby+food (because client has particles)
        if (!this.getWorld().isClient || this.isBaby() && this.isBreedingItem(itemStack)) {

            // Check if the mob is tamed
            if (this.isTamed()) {

                // Check for breeding and health
                if (this.isBreedingItem(itemStack)) {
                    boolean bl = this.receiveFood(player, itemStack);
                    if (bl) {
                        itemStack.decrementUnlessCreative(1, player);
                    }

                    if (this.getWorld().isClient) {
                        return ActionResult.CONSUME;
                    } else {
                        return bl ? ActionResult.SUCCESS : ActionResult.PASS;
                    }
                }


                if (itemStack.getItem() instanceof AbstractEntityJarItem || itemStack.getItem() instanceof VisionMonocleItem) {
                    return ActionResult.PASS;
                }

                ActionResult actionResult = super.interactMob(player, hand);
                if (!actionResult.isAccepted() && this.isOwner(player) && player.isSneaking()) {
                    this.toggleSitting();
                    return ActionResult.SUCCESS_NO_ITEM_USED;
                }

                if (this.hasPassengers() || this.isBaby()) {
                    return super.interactMob(player, hand);
                }

                if (!actionResult.isAccepted() && this.getGrowthSize() >= 1.5F && !this.isBreedingItem(itemStack)) {
                    this.putPlayerOnBack(player);
                    return ActionResult.SUCCESS_NO_ITEM_USED;
                }

                return actionResult;
            } else if (this.isBreedingItem(itemStack) && !this.hasAngerTime()) {
                    itemStack.decrementUnlessCreative(1, player);
                    this.tryTame(player);
                    return ActionResult.SUCCESS;
            } else {
                return super.interactMob(player, hand);
            }
        } else {
            boolean shouldConsume = this.isOwner(player) || this.isTamed() || this.isBreedingItem(itemStack) && !this.isTamed() && !this.hasAngerTime();
            return shouldConsume ? ActionResult.CONSUME : ActionResult.PASS;
        }
    }

    private boolean receiveFood(PlayerEntity player, ItemStack itemStack) {
        boolean bl = false;
        float f;
        int i;
        if (itemStack.isOf(ModItems.LOCUST_GOLD)) {
            f = 12.0F;
            i = 240;
            if (!this.getWorld().isClient && this.isTamed() && this.getBreedingAge() == 0 && !this.isInLove()) {
                bl = true;
                this.lovePlayer(player);
            }
        } else if (itemStack.isIn(ModTags.Items.LOCUST_ITEMS)) {
            f = 4.0F;
            i = 60;
            if (!this.getWorld().isClient && this.isTamed() && this.getBreedingAge() == 0 && !this.isInLove()) {
                bl = true;
                this.lovePlayer(player);
            }
        } else {
            f = 2.0F;
            i = 20;
        }

        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(f);
            bl = true;
        }

        if (this.isBaby()) {
            this.getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), 0.0, 0.0, 0.0);
            if (!this.getWorld().isClient) {
                this.growUp(i);
                bl = true;
            }
        }

        if (bl) {
            this.playEatingAnimation();
            this.emitGameEvent(GameEvent.EAT);
        }

        return bl;
    }
    private void playEatingAnimation() {
        if (!this.isSilent()) {
            SoundEvent soundEvent = this.getAmbientSound();
            if (soundEvent != null) {
                this.getWorld()
                        .playSound(
                                null, this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                        );
            }
        }
    }

    private void toggleSitting() {
        this.setSitting(!this.isSitting());
        this.jumping = false;
        this.navigation.stop();
        this.setTarget(null);
    }

    public void tickMovement() {
        super.tickMovement();

        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }

        if (!this.getWorld().isClient && this.isAlive()) {
            if (this.age % 200 == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }
        }

    }

    @Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
        super.tickControlled(controllingPlayer, movementInput);
        Vec2f vec2f = this.getControlledRotation(controllingPlayer);
        this.setRotation(vec2f.y, vec2f.x);
        this.prevYaw = this.bodyYaw = this.headYaw = this.getYaw();
        if (this.isLogicalSideForUpdatingMovement() && !this.isInSittingPose()) {
            if (movementInput.z <= 0.0) {
                this.soundTicks = 0;
            }

            if (this.isOnGround()) {
                this.setInAir(false);
                if (this.jumpStrength > 0.0F && !this.isInAir()) {
                    this.jump(this.jumpStrength, movementInput);
                }

                this.jumpStrength = 0.0F;
            }

            this.setClimbingWall(this.horizontalCollision);

        }
    }
    @Override
    protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
        return (float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED
        );
    }
    protected Vec2f getControlledRotation(LivingEntity controllingPassenger) {
        return new Vec2f(controllingPassenger.getPitch() * 0.5F, controllingPassenger.getYaw());
    }
    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        if (!this.isInSittingPose()) {
            float f = controllingPlayer.sidewaysSpeed * 0.5F;
            float g = controllingPlayer.forwardSpeed;
            if (g <= 0.0F) {
                g *= 0.25F;
            }
            return new Vec3d(f, 0.0, g);
        }
        return Vec3d.ZERO;
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if (passenger instanceof LivingEntity) {
            ((LivingEntity)passenger).bodyYaw = this.bodyYaw;
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() { //validated
        Entity var2 = this.getFirstPassenger();
           if (var2 instanceof PlayerEntity) {
               return (PlayerEntity)var2;
           }
        return super.getControllingPassenger();
    }
    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor)
                .add(0.0, -0.25, 0.0);
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() && this.hasPassengers();
    }
    @Override
    public void slowMovement(BlockState state, Vec3d multiplier) {
        if (!state.isOf(Blocks.COBWEB)) {
            super.slowMovement(state, multiplier);
        }
    }

    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @Nullable
    private Vec3d locateSafeDismountingPos(Vec3d offset, LivingEntity passenger) {
        double d = this.getX() + offset.x;
        double e = this.getBoundingBox().minY;
        double f = this.getZ() + offset.z;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (EntityPose entityPose : passenger.getPoses()) {
            mutable.set(d, e, f);
            double g = this.getBoundingBox().maxY + 0.75;

            do {
                double h = this.getWorld().getDismountHeight(mutable);
                if ((double)mutable.getY() + h > g) {
                    break;
                }

                if (Dismounting.canDismountInBlock(h)) {
                    Box box = passenger.getBoundingBox(entityPose);
                    Vec3d vec3d = new Vec3d(d, (double)mutable.getY() + h, f);
                    if (Dismounting.canPlaceEntityAt(this.getWorld(), passenger, box.offset(vec3d))) {
                        passenger.setPose(entityPose);
                        return vec3d;
                    }
                }

                mutable.move(Direction.UP);
            } while (!((double)mutable.getY() < g));
        }

        return null;
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Vec3d vec3d = getPassengerDismountOffset(
                this.getWidth(), passenger.getWidth(), this.getYaw() + (passenger.getMainArm() == Arm.RIGHT ? 90.0F : -90.0F)
        );
        Vec3d vec3d2 = this.locateSafeDismountingPos(vec3d, passenger);
        if (vec3d2 != null) {
            return vec3d2;
        } else {
            Vec3d vec3d3 = getPassengerDismountOffset(
                    this.getWidth(), passenger.getWidth(), this.getYaw() + (passenger.getMainArm() == Arm.LEFT ? 90.0F : -90.0F)
            );
            Vec3d vec3d4 = this.locateSafeDismountingPos(vec3d3, passenger);
            return vec3d4 != null ? vec3d4 : this.getPos();
        }
    }

    @Override
    public void setJumpStrength(int strength) {
        if (strength < 0) {
            strength = 0;
        } else {
            this.jumping = true;
        }

        if (strength >= 90) {
            this.jumpStrength = 1.0F;
        } else {
            this.jumpStrength = 0.4F + 0.4F * (float)strength / 90.0F;
        }

    }

    @Override
    public boolean canJump() {
        return this.hasPassengers() && !this.isInSittingPose();
    }

    @Override
    public void startJumping(int height) {
        this.jumping = true;
        this.playJumpSound();
    }

    @Override
    public void stopJumping() {
    }
    protected void jump(float strength, Vec3d movementInput) {
        double d = this.getJumpVelocity(strength);
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, d, vec3d.z);
        this.setInAir(true);
        this.velocityDirty = true;
        if (movementInput.z > 0.0) {
            float f = MathHelper.sin(this.getYaw() * (float) (Math.PI / 180.0));
            float g = MathHelper.cos(this.getYaw() * (float) (Math.PI / 180.0));
            this.setVelocity(this.getVelocity().add(-1F * f * strength, 0.0, 1F * g * strength));
        }
    }

    public void setInAir(boolean inAir) {
        this.inAir = inAir;
    }
    public boolean isInAir() {
        return this.inAir;
    }


    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            this.setClimbingWall(this.horizontalCollision);
        }
        this.jumping = false;

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

    @Override
    public boolean tryAttack(Entity target) {
        boolean bl = super.tryAttack(target);
        if (bl && target instanceof LivingEntity livingEntity) {
            switch (this.getVariant()) {
                case POISON -> {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 5 * 20), this);
                }
                case WITHER -> {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 5 * 20), this);
                }
                case FIRE -> {
                    livingEntity.setOnFireForTicks(5 * 20);
                }
                case LUNGING -> {

                }
                case WEAVER -> {
                    World world = this.getWorld();
                    BlockPos pos = livingEntity.getBlockPos();
                    if (world.getBlockState(pos).isReplaceable()) {
                        world.setBlockState(pos, Blocks.COBWEB.getDefaultState());
                    }
                }
                default -> {}
            }
            this.onAttacking(target);
            return true;
        }


        return bl;
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

            //attributes are inherited from the parents average size + variance centered on 0, +/- 10%
            float babySize = (float) ((this.getGrowthSize() + spiderling.getGrowthSize()) / 2 * getVariationInPercent(10, 1));
            babySize = Math.clamp(babySize, MINIMUM_SIZE, MAXIMUM_SIZE);
            double babyHealth = (getBaseHealth(this) + getBaseHealth(spiderling)) / 2 * getVariationInPercent(10, 1);
            babyHealth = Math.clamp(babyHealth, MINIMUM_HEALTH, MAXIMUM_HEALTH);
            double babySpeed = (getBaseSpeed(this) + getBaseSpeed(spiderling)) / 2 * getVariationInPercent(10, 1);
            babySpeed = Math.clamp(babySpeed, MINIMUM_SPEED, MAXIMUM_SPEED);
            double babyJump = (getBaseJump(this) + getBaseJump(spiderling)) / 2 * getVariationInPercent(5, 1);
            babyJump = Math.clamp(babyJump, MINIMUM_JUMP, MAXIMUM_JUMP);
            baby.setGrowthSize(babySize);
            baby.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(babyHealth);
            baby.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(babySpeed);
            baby.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH).setBaseValue(babyJump);
        }
        return baby;
    }

    double getBaseHealth(SpiderlingEntity spiderling) {
        return spiderling.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH);
    }
    double getBaseSpeed(SpiderlingEntity spiderling) {
        return spiderling.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    }
    double getBaseJump(SpiderlingEntity spiderling) {
        return spiderling.getAttributeBaseValue(EntityAttributes.GENERIC_JUMP_STRENGTH);
    }
    double getVariationInPercent(float percentage, int iterations) {
        float variation = 0;
        for (int i = 0; i < iterations; i++) {
            variation += random.nextFloat();
        }
        variation /= iterations;
        variation = variation * percentage - percentage;
        return 1 + variation/100;
    }

    public void increaseAllStats() {
        float size = this.getGrowthSize() * 1.1F;
        size = Math.clamp(size, MINIMUM_SIZE, MAXIMUM_SIZE);
        double health = this.getBaseHealth(this) * 1.1F;
        health = Math.clamp(health, MINIMUM_HEALTH, MAXIMUM_HEALTH);
        double speed = this.getBaseSpeed(this) * 1.1F;
        speed = Math.clamp(speed, MINIMUM_SPEED, MAXIMUM_SPEED);
        double jump = this.getBaseJump(this) * 1.1F;
        jump = Math.clamp(jump, MINIMUM_JUMP, MAXIMUM_JUMP);
        this.setGrowthSize(size);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(health);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        this.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH).setBaseValue(jump);
    }

    @Override
    public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
        StatusEffectCategory category = effect.getEffectType().value().getCategory();
        boolean isBeneficial = category == StatusEffectCategory.NEUTRAL || category == StatusEffectCategory.BENEFICIAL;

        if (isBeneficial) {
            StatusEffectInstance infiniteEffect = new StatusEffectInstance(effect.getEffectType(), StatusEffectInstance.INFINITE, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles());
            this.playSound(SoundEvents.BLOCK_BREWING_STAND_BREW, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
            return super.addStatusEffect(infiniteEffect, source);
        }
        return super.addStatusEffect(effect, source);
    }

    public void washEntity () {
        this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        this.clearStatusEffects();
    }

    @Override
    public float getScale() {
        AttributeContainer attributeContainer = this.getAttributes();
        return attributeContainer == null ? 1.0F : this.clampScale((float)attributeContainer.getValue(EntityAttributes.GENERIC_SCALE) * this.getGrowthSize());
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
        return null;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {

    }

    @Override
    public void chooseRandomAngerTime() {

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

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(ANGER, 0);
        builder.add(SPIDER_FLAGS, (byte)0);
        builder.add(GROWTH_SIZE, 1F);
        builder.add(MAX_HEALTH, 8F);
        builder.add(MOVEMENT_SPEED, 0.35F);
        builder.add(JUMP_STRENGTH, 0.7F);
    }

    @Override
    public int getTypeVariant() {
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

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        nbt.putFloat("GrowthSize", this.getGrowthSize());
        nbt.putFloat("MaxHealth", (float) this.getAttributeBaseValue(EntityAttributes.GENERIC_MAX_HEALTH));
        nbt.putFloat("MovementSpeed", (float) this.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        nbt.putFloat("JumpStrength", (float) this.getAttributeBaseValue(EntityAttributes.GENERIC_JUMP_STRENGTH));

        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }

        this.writeAngerToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        this.dataTracker.set(GROWTH_SIZE, nbt.getFloat("GrowthSize"));
        this.dataTracker.set(MAX_HEALTH, nbt.getFloat("MaxHealth"));
        this.dataTracker.set(MOVEMENT_SPEED, nbt.getFloat("MovementSpeed"));
        this.dataTracker.set(JUMP_STRENGTH, nbt.getFloat("JumpStrength"));

        UUID uUID;
        if (nbt.containsUuid("Owner")) {
            uUID = nbt.getUuid("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = ServerConfigHandler.getPlayerUuidByName(this.getServer(), string);
        }

        if (uUID != null) {
            this.setOwnerUuid(uUID);
        }
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_JUMP_STRENGTH, 0.7)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 6.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0F);
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return true;
    }

    /* SOUNDS */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SPIDER_AMBIENT;
    }
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SPIDER_HURT;
    }
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SPIDER_DEATH;
    }
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.isLiquid()) {
            BlockState blockState = this.getWorld().getBlockState(pos.up());
            BlockSoundGroup blockSoundGroup = state.getSoundGroup();
            if (blockState.isOf(Blocks.SNOW)) {
                blockSoundGroup = blockState.getSoundGroup();
            }

            if (this.hasPassengers()) {
                this.soundTicks++;
                if (this.soundTicks > 5 && this.soundTicks % 3 == 0) {
                    this.playWalkSound(blockSoundGroup);
                } else if (this.soundTicks <= 5) {
                    this.playSound(SoundEvents.ENTITY_SPIDER_STEP, blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
                }
            } else if (this.isWooden(blockSoundGroup)) {
                this.playSound(SoundEvents.ENTITY_SPIDER_STEP, blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
            } else {
                this.playSound(SoundEvents.ENTITY_SPIDER_STEP, blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
            }
        }
    }

    protected void playWalkSound(BlockSoundGroup group) {
        this.playSound(SoundEvents.ENTITY_SPIDER_STEP, group.getVolume() * 0.15F, group.getPitch());
    }
    protected void playJumpSound() {
        this.playSound(SoundEvents.ENTITY_SPIDER_HURT, 0.4F, 1.0F);
    }

    private boolean isWooden(BlockSoundGroup soundGroup) {
        return soundGroup == BlockSoundGroup.WOOD
                || soundGroup == BlockSoundGroup.NETHER_WOOD
                || soundGroup == BlockSoundGroup.NETHER_STEM
                || soundGroup == BlockSoundGroup.CHERRY_WOOD
                || soundGroup == BlockSoundGroup.BAMBOO_WOOD;
    }


    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {

        Optional<RegistryKey<Biome>> currentBiomeKey = world.getBiome(this.getBlockPos()).getKey();
        SpiderlingVariant variant;
//        if (this.random.nextInt(100) <= 5) {
//            variant = SpiderlingVariant.DEFAULT;
//        }
//        else if (currentBiomeKey.isPresent() && biomeMap.containsKey(currentBiomeKey.get()) && this.random.nextFloat() < 0.5F){
//            variant = biomeMap.get(currentBiomeKey.get());
//        } else {
//            variant = SpiderlingVariant.byId(this.random.nextBetween(0, SpiderlingVariant.values().length - 1));
//        }
        variant = SpiderlingVariant.DEFAULT;
        this.setVariant(variant);
        this.setGrowthSize((this.random.nextFloat() * 2f - 1)*0.05f + 1);

        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    private static final Map<RegistryKey<Biome>, SpiderlingVariant> biomeMap = new HashMap<>() {{
        put(BiomeKeys.PLAINS, SpiderlingVariant.DEFAULT);
    }};


    protected void putPlayerOnBack(PlayerEntity player) {
//        this.setAngry(false);
        if (!this.getWorld().isClient) {
            player.setYaw(this.getYaw());
            player.setPitch(this.getPitch());
            player.startRiding(this);
        }
    }

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
