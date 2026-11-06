package net.follis.goldenscarabmod.entity.custom;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.follis.goldenscarabmod.network.MesmerizeManager;
import net.follis.goldenscarabmod.network.MesmerizePayload;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class MothEntity extends AngerableBugEntity implements Flutterer {
    private static final int WING_FLAP_INTERVAL = MathHelper.ceil(1.4959966F);

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT =
            DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_ROOSTING =
            DataTracker.registerData(MothEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final Map<RegistryKey<Biome>, MothVariant> BIOME_VARIANTS = Map.ofEntries(
            Map.entry(BiomeKeys.FOREST, MothVariant.OAK),
            Map.entry(BiomeKeys.FLOWER_FOREST, MothVariant.OAK),
            Map.entry(BiomeKeys.WINDSWEPT_FOREST, MothVariant.OAK),
            Map.entry(BiomeKeys.BIRCH_FOREST, MothVariant.BIRCH),
            Map.entry(BiomeKeys.OLD_GROWTH_BIRCH_FOREST, MothVariant.BIRCH),
            Map.entry(BiomeKeys.TAIGA, MothVariant.SPRUCE),
            Map.entry(BiomeKeys.SNOWY_TAIGA, MothVariant.SPRUCE),
            Map.entry(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA, MothVariant.SPRUCE),
            Map.entry(BiomeKeys.OLD_GROWTH_PINE_TAIGA, MothVariant.SPRUCE),
            Map.entry(BiomeKeys.DARK_FOREST, MothVariant.DARK_OAK),
            Map.entry(BiomeKeys.CHERRY_GROVE, MothVariant.CHERRY),
            Map.entry(BiomeKeys.MANGROVE_SWAMP, MothVariant.MANGROVE),
            Map.entry(BiomeKeys.JUNGLE, MothVariant.JUNGLE),
            Map.entry(BiomeKeys.BAMBOO_JUNGLE, MothVariant.JUNGLE),
            Map.entry(BiomeKeys.SPARSE_JUNGLE, MothVariant.JUNGLE),
            Map.entry(BiomeKeys.SAVANNA, MothVariant.ACACIA),
            Map.entry(BiomeKeys.SAVANNA_PLATEAU, MothVariant.ACACIA),
            Map.entry(BiomeKeys.WINDSWEPT_SAVANNA, MothVariant.ACACIA)
    );

    // ---- mesmerize tuning ----
    private static final double MESMERIZE_RANGE = 24.0D;
    private static final double MESMERIZE_RANGE_SQ = MESMERIZE_RANGE * MESMERIZE_RANGE;
    /** dot(look, toMoth) above this = inside the cone. 0.6 is about 53 degrees from the view direction. */
    private static final float MESMERIZE_CONE_THRESHOLD = 0.6F;
    /** dot above this = "directly looking at it" (about 5.7 degrees): the player breaks free. */
    private static final float MESMERIZE_INCONSEQUENTIAL_THRESHOLD = 0.995F;
    private static final int TRIGGER_SCAN_INTERVAL = 5;           // only look for NEW stares 4x/sec
    private static final float MESMERIZE_TURN_SPEED_DEGREES = 3F; // degrees turned per tick

    private final Set<UUID> mesmerizedPlayers = new HashSet<>();

    public final AnimationState flyingAnimationState = new AnimationState();
    public final AnimationState roostingAnimationState = new AnimationState();

    public float flapProgress;
    public float maxWingDeviation;
    public float prevMaxWingDeviation;
    public float prevFlapProgress;
    private float flapSpeed = 1.0F;

    public MothEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, true);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(true);
        navigation.setCanEnterOpenDoors(true);
        return navigation;
    }

    // =====================================================================
    // Goals / attributes
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MothBiteGoal(this, 1.4F, true));

        this.goalSelector.add(1, new MothMateGoal(this, 1.15D));
        this.goalSelector.add(2, new MothTemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(4, new LatchGoal(this));
        this.goalSelector.add(5, new FlyToTreeGoal(this, 1.4F, 8, 6));
        this.goalSelector.add(6, new SwimGoal(this));

        this.addCombatTargetGoals();
    }

    private boolean foodSelector(ItemStack stack) {
        return stack.isIn(ItemTags.BEE_FOOD);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return foodSelector(stack);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0F)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 1F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0F);
    }

    // =====================================================================
    // Variants / breeding
    // =====================================================================

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {
        RegistryKey<Biome> biome = world.getBiome(this.getBlockPos()).getKey().orElse(null);
        MothVariant variant = biome == null ? null : BIOME_VARIANTS.get(biome);
        if (variant == null) {
            variant = MothVariant.OAK;
        }
        this.setVariant(this.random.nextInt(100) == 0 ? rareForm(variant) : variant);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    /** The 1% mutation of each base variant (base variants without one map to themselves). */
    private static MothVariant rareForm(MothVariant variant) {
        return switch (variant) {
            case OAK -> MothVariant.OAK_CRACKED;
            case BIRCH -> MothVariant.BIRCH_RADIANT;
            case SPRUCE -> MothVariant.SPRUCE_RUBY;
            case DARK_OAK -> MothVariant.DARK_OAK_EMERALD;
            case CHERRY -> MothVariant.CHERRY_BLOOM;
            case MANGROVE -> MothVariant.MANGROVE_TANGLED;
            case JUNGLE -> MothVariant.JUNGLE_SPIDER;
            case ACACIA -> MothVariant.ACACIA_SAPPHIRE;
            default -> variant;
        };
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        MothEntity baby = ModEntities.MOTH.create(world);
        if (baby == null || !(entity instanceof MothEntity other)) {
            return baby;
        }

        MothVariant mine = this.getVariant();
        MothVariant theirs = other.getVariant();
        if (mine == theirs) {
            baby.setVariant(this.random.nextInt(100) == 0 ? rareForm(mine) : mine);
        } else {
            baby.setVariant(this.random.nextBoolean() ? mine : theirs);
        }
        return baby;
    }

    // =====================================================================
    // Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        this.updateAnimations();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            this.updateMesmerizedPlayers();
            if (this.age % TRIGGER_SCAN_INTERVAL == 0) {
                this.scanForNewStares();
                if (this.isRoosting() && !isValidTreeStructure(serverWorld, this.getBlockPos())) {
                    this.setRoosting(false);
                }
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        this.flapWings();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.getWorld().isClient && this.isRoosting()) {
            this.setRoosting(false);
        }
        return super.damage(source, amount);
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        super.pushAwayFrom(entity);
        if (!this.getWorld().isClient && !(entity instanceof MothEntity)) {
            this.setRoosting(false);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        for (UUID uuid : this.mesmerizedPlayers) {
            ServerPlayerEntity player = this.findPlayer(uuid);
            if (player != null) {
                MesmerizeManager.release(player, this);
                this.sendMesmerizeState(player, false);
            }
        }
        this.mesmerizedPlayers.clear();
        super.remove(reason);
    }

    // =====================================================================
    // Mesmerize
    // =====================================================================

    /** Finds an online player in ANY dimension (getWorld().getPlayerByUuid only sees this world). Server only. */
    @Nullable
    private ServerPlayerEntity findPlayer(UUID uuid) {
        MinecraftServer server = this.getServer();
        return server == null ? null : server.getPlayerManager().getPlayer(uuid);
    }

    private void updateMesmerizedPlayers() {
        if (this.getVariant().isNotMesmerizing() || this.mesmerizedPlayers.isEmpty()) return;

        Iterator<UUID> it = this.mesmerizedPlayers.iterator();
        while (it.hasNext()) {
            ServerPlayerEntity player = this.findPlayer(it.next());
            if (player == null) {
                // offline: MesmerizeManager has to drop the claim on disconnect
                it.remove();
                continue;
            }

            boolean release = player.getWorld() != this.getWorld()
                    || player.isRemoved()
                    || player.squaredDistanceTo(this) > MESMERIZE_RANGE_SQ
                    || !player.canSee(this)
                    || !this.isTowardsMoth(player)
                    || this.isDirectlyLookingAtMoth(player);

            if (release) {
                MesmerizeManager.release(player, this);
                this.sendMesmerizeState(player, false);
                it.remove();
            } else {
                // the client does the actual rotating, we just keep it informed
                this.sendMesmerizeState(player, true);
            }
        }
    }

    private void sendMesmerizeState(ServerPlayerEntity player, boolean active) {
        ServerPlayNetworking.send(player,
                new MesmerizePayload(this.getId(), active, MESMERIZE_TURN_SPEED_DEGREES * 20)); // deg/tick -> deg/sec
    }

    private void scanForNewStares() {
        if (this.getVariant().isNotMesmerizing()) return;

        List<PlayerEntity> nearby = this.getWorld().getEntitiesByClass(PlayerEntity.class,
                this.getBoundingBox().expand(MESMERIZE_RANGE),
                player -> !player.isSpectator() && !player.isCreative()
                        && !this.mesmerizedPlayers.contains(player.getUuid()));

        for (PlayerEntity player : nearby) {
            if (player instanceof ServerPlayerEntity serverPlayer
                    && this.isTowardsMoth(serverPlayer)
                    && !this.isDirectlyLookingAtMoth(serverPlayer) // otherwise: claim/release flicker while staring
                    && this.canSee(serverPlayer)
                    && MesmerizeManager.tryClaim(serverPlayer, this)) {
                this.mesmerizedPlayers.add(serverPlayer.getUuid());
            }
        }
    }

    private double lookDot(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toMoth = this.getPos().subtract(player.getEyePos()).normalize();
        return look.dotProduct(toMoth);
    }

    private boolean isTowardsMoth(PlayerEntity player) {
        return this.lookDot(player) > MESMERIZE_CONE_THRESHOLD;
    }

    private boolean isDirectlyLookingAtMoth(PlayerEntity player) {
        return this.lookDot(player) > MESMERIZE_INCONSEQUENTIAL_THRESHOLD;
    }

    // =====================================================================
    // Variant / roosting / NBT
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(IS_ROOSTING, false);
    }

    public MothVariant getVariant() {
        return MothVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(MothVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    public boolean isRoosting() {
        return this.dataTracker.get(IS_ROOSTING);
    }

    public void setRoosting(boolean roosting) {
        this.dataTracker.set(IS_ROOSTING, roosting);
    }

    /**
     * Roosting is deliberately NOT saved: it only means something while LatchGoal is running.
     * Restoring it from NBT left reloaded moths "roosting" with no goal holding them in place
     * (LatchGoal.canStart requires !isRoosting). They simply re-latch within a few ticks instead.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt); // also writes anger
        nbt.putInt("Variant", this.getTypeVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt); // also reads anger
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
    }

    // =====================================================================
    // Sounds / animation / flight
    // =====================================================================

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ALLAY_DEATH;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @Override
    protected void playHurtSound(DamageSource damageSource) {
        super.playHurtSound(damageSource);
        this.playSound(SoundEvents.ENTITY_ALLAY_HURT, 0.5F, 0.5F);
    }

    @Override
    protected void addFlapEffects() {
        this.playSound(SoundEvents.ENTITY_PARROT_FLY, 0.1F, 1.0F);
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
        return this.isInAir() && this.age % WING_FLAP_INTERVAL == 0 && !this.isRoosting();
    }

    @Override
    public boolean isInAir() {
        return !this.isOnGround();
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {}

    private void flapWings() {
        this.prevFlapProgress = this.flapProgress;
        this.prevMaxWingDeviation = this.maxWingDeviation;
        this.maxWingDeviation += (float) (!this.isOnGround() && !this.hasVehicle() ? 4 : -1) * 0.3F;
        this.maxWingDeviation = MathHelper.clamp(this.maxWingDeviation, 0.0F, 1.0F);
        if (!this.isOnGround() && this.flapSpeed < 1.0F) {
            this.flapSpeed = 1.0F;
        }

        this.flapSpeed *= 0.9F;
        Vec3d velocity = this.getVelocity();
        if (!this.isOnGround() && velocity.y < 0.0D) {
            this.setVelocity(velocity.multiply(1.0D, 0.6D, 1.0D));
        }

        this.flapProgress += this.flapSpeed * 2.0F;
    }

    // =====================================================================
    // Tree helpers
    // =====================================================================

    private static boolean isTreeBlock(BlockState state) {
        return state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.LOGS);
    }

    private static boolean isValidTreeStructure(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return false;

        int treeSides = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (isTreeBlock(world.getBlockState(pos.offset(direction)))) {
                treeSides++;
            }
        }
        return treeSides > 0 && treeSides < 4;
    }

    // =====================================================================
    // Goals
    // =====================================================================

    class MothBiteGoal extends BugMeleeGoal {
        MothBiteGoal(MothEntity moth, double speed, boolean pauseWhenMobIdle) {
            super(moth, speed, pauseWhenMobIdle);
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting()) {
                MothEntity.this.setRoosting(false);
            }
        }
    }

    class MothMateGoal extends AnimalMateGoal {
        MothMateGoal(MothEntity animal, double speed) {
            super(animal, speed);
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting()) {
                MothEntity.this.setRoosting(false);
            }
        }
    }

    class MothTemptGoal extends TemptGoal {
        MothTemptGoal(PathAwareEntity entity, double speed, Predicate<ItemStack> foodPredicate, boolean canBeScared) {
            super(entity, speed, foodPredicate, canBeScared);
        }

        @Override
        public void start() {
            super.start();
            if (MothEntity.this.isRoosting()) {
                MothEntity.this.setRoosting(false);
            }
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

        FlyToTreeGoal(MothEntity entity, double speed, int horizontalRange, int verticalRange) {
            this.mob = entity;
            this.speed = speed;
            this.horizontalRange = horizontalRange;
            this.verticalRange = verticalRange;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.mob.hasControllingPassenger()) return false;
            if (this.mob.random.nextInt(1200) != 0 && (this.mob.isRoosting() || this.mob.random.nextInt(15) != 0)) {
                return false;
            }
            Vec3d target = this.getWanderTarget();
            if (target == null) return false;
            this.targetX = target.x;
            this.targetY = target.y;
            this.targetZ = target.z;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return !this.mob.getNavigation().isIdle() && !this.mob.hasControllingPassenger();
        }

        @Override
        public void start() {
            this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
            if (this.mob.isRoosting()) {
                this.mob.setRoosting(false);
            }
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
            super.stop();
        }

        @Nullable
        protected Vec3d getWanderTarget() {
            Vec3d target = null;
            if (this.mob.isTouchingWater()) {
                target = FuzzyTargeting.find(this.mob, this.horizontalRange, this.verticalRange);
            } else if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
                target = this.getRandomValidTreePos(serverWorld, this.mob.getBlockPos());
            }
            return target == null ? this.randomNearbyPos() : target;
        }

        @Nullable
        private Vec3d getRandomValidTreePos(ServerWorld world, BlockPos origin) {
            int h = this.horizontalRange / 2;
            int v = this.verticalRange / 2;
            List<BlockPos> valid = new ArrayList<>();
            for (BlockPos pos : BlockPos.iterate(origin.add(-h, -v, -h), origin.add(h, v, h))) {
                if (isValidTreeStructure(world, pos)) {
                    valid.add(pos.toImmutable()); // iterate() reuses one mutable instance
                }
            }
            if (valid.isEmpty()) return null;
            return valid.get(world.random.nextInt(valid.size())).toBottomCenterPos();
        }

        private Vec3d randomNearbyPos() {
            return BlockPos.ofFloored(
                    this.mob.getX() + this.mob.random.nextInt(7) - this.mob.random.nextInt(7),
                    this.mob.getY() - this.mob.random.nextInt(6) + 2.0D,
                    this.mob.getZ() + this.mob.random.nextInt(7) - this.mob.random.nextInt(7)).toBottomCenterPos();
        }
    }

    static class LatchGoal extends Goal {
        protected final MothEntity mob;
        @Nullable
        protected Direction latchDirection;

        LatchGoal(MothEntity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canStart() {
            return this.mob.getWorld() instanceof ServerWorld serverWorld
                    && !this.mob.isRoosting()
                    && isValidTreeStructure(serverWorld, this.mob.getBlockPos())
                    && this.mob.random.nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinue() {
            // isRoosting() matters: pushing, damage, tempt, mate and FlyToTree only clear the flag,
            // so without it this goal would keep freezing a moth that thinks it is flying.
            boolean keep = this.mob.isRoosting()
                    && this.latchDirection != null
                    && this.mob.getWorld() instanceof ServerWorld serverWorld
                    && this.mob.getRandom().nextInt(6000) != 0
                    && !serverWorld.getBlockState(this.mob.getBlockPos().offset(this.latchDirection)).isAir()
                    && !this.mob.hasControllingPassenger()
                    && this.mob.getVelocity().length() < 0.05F;
            if (!keep) {
                this.mob.setRoosting(false);
            }
            return keep;
        }

        @Override
        public void start() {
            this.mob.setRoosting(true);
            this.mob.getNavigation().stop();

            if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
                this.latchDirection = this.getLatchDirection(serverWorld);
                double inset = this.mob.isBaby() ? 0.4D : 0.3D;
                Vec3d offset = new Vec3d(this.latchDirection.getOffsetX() * inset, 0, this.latchDirection.getOffsetZ() * inset);
                this.mob.setPosition(this.mob.getBlockPos().toBottomCenterPos().add(offset));
                this.alignForwardToDirection(this.latchDirection);
            }
        }

        @Override
        public void tick() {
            super.tick();
            this.mob.setVelocity(Vec3d.ZERO);
            if (this.latchDirection != null) {
                this.alignForwardToDirection(this.latchDirection);
            }
        }

        private Direction getLatchDirection(ServerWorld world) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                if (isTreeBlock(world.getBlockState(this.mob.getBlockPos().offset(direction)))) {
                    return direction;
                }
            }
            return Direction.DOWN;
        }

        private void alignForwardToDirection(Direction direction) {
            switch (direction) {
                case NORTH -> this.face(180.0F);
                case SOUTH -> this.face(0.0F);
                case WEST -> this.face(90.0F);
                case EAST -> this.face(-90.0F);
                default -> { }
            }
            this.mob.setPitch(0.0F);
        }

        private void face(float yaw) {
            this.mob.setHeadYaw(yaw);
            this.mob.setYaw(yaw);
        }
    }
}