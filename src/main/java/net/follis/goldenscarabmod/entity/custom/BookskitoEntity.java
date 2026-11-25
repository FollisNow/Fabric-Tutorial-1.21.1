package net.follis.goldenscarabmod.entity.custom;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.follis.goldenscarabmod.entity.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
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
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * A book-shaped insect. Its covers flap like wings in flight, it can perch vertically on a wall
 * (see {@link #isRoostBlock}) and it can rest on the ground.
 */
public class BookskitoEntity extends AngerableBugEntity implements Flutterer {
    private static final int WING_FLAP_INTERVAL = MathHelper.ceil(1.4959966F);
    private static final int ROOST_CHECK_INTERVAL = 5;

    public enum RestState {
        FLYING, ROOSTING, GROUNDED;

        static RestState byId(int id) {
            RestState[] all = values();
            return id >= 0 && id < all.length ? all[id] : FLYING;
        }
    }

    private static final TrackedData<Integer> DATA_ID_TYPE_VARIANT =
            DataTracker.registerData(BookskitoEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> REST_STATE =
            DataTracker.registerData(BookskitoEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public final AnimationState flyingAnimationState = new AnimationState();
    public final AnimationState roostingAnimationState = new AnimationState();
    public final AnimationState groundedAnimationState = new AnimationState();

    /** First slot an enchantment is valid in wins, so it is only ever applied once. */
    private static final EquipmentSlot[] ENCHANT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND
    };
    private static final float MENDING_HEAL_PER_XP = 1.0F;
    private static final float ENCHANT_PARTICLES_PER_ENCHANT = 0.25F;
    private static final float MAX_ENCHANT_PARTICLES_PER_TICK = 4.0F;
    private int enchantCount; // client-side cache, refreshed twice a second

    public BookskitoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
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
        this.goalSelector.add(0, new BookskitoBiteGoal(this, 1.4F, true));

        this.goalSelector.add(1, new BookskitoMateGoal(this, 1.15D));
        this.goalSelector.add(2, new BookskitoTemptGoal(this, 1.25D, this::foodSelector, false));

        this.goalSelector.add(4, new LatchGoal(this));
        this.goalSelector.add(5, new FlyToRoostGoal(this, 1.4F, 8, 6));
        this.goalSelector.add(6, new GroundRestGoal(this, 1.0D));
        this.goalSelector.add(7, new SwimGoal(this));

        this.addCombatTargetGoals();
    }

    private boolean foodSelector(ItemStack stack) {
        return stack.isOf(Items.PAPER)
                || stack.isOf(Items.BOOK) || stack.isOf(Items.WRITABLE_BOOK) || stack.isOf(Items.WRITTEN_BOOK)
                || stack.isOf(Items.LEATHER) || stack.isOf(Items.RABBIT_HIDE)
                || stack.isOf(Items.INK_SAC) || stack.isOf(Items.GLOW_INK_SAC)
                || stack.getItem() instanceof DyeItem; // all 16 dyes
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
        this.setVariant(this.random.nextInt(100) == 0 ? BookskitoVariant.GOLD : BookskitoVariant.REGULAR);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        BookskitoEntity baby = ModEntities.BOOKSKITO.create(world);
        if (baby == null || !(entity instanceof BookskitoEntity other)) {
            return baby;
        }

        BookskitoVariant mine = this.getVariant();
        BookskitoVariant theirs = other.getVariant();
        if ((mine == BookskitoVariant.GOLD && theirs == BookskitoVariant.GOLD) || this.random.nextInt(100) == 0) {
            baby.setVariant(BookskitoVariant.GOLD);
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
        if (this.getWorld().isClient) {
            this.updateAnimations();
            this.tickEnchantTrail();
        } else {
            // hovers like a bee, except while resting on the ground
            this.setNoGravity(this.getRestState() != RestState.GROUNDED);
            if (this.age % ROOST_CHECK_INTERVAL == 0 && this.isRoosting()
                    && this.getWorld() instanceof ServerWorld serverWorld
                    && !isValidRoost(serverWorld, this.getBlockPos())) {
                this.wakeUp();
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        Vec3d velocity = this.getVelocity();
        if (!this.isOnGround() && velocity.y < 0.0D && this.getRestState() == RestState.FLYING) {
            this.setVelocity(velocity.multiply(1.0D, 0.6D, 1.0D)); // glide down slowly
        }
        if (!this.getWorld().isClient && this.age % 5 == 0 && this.isEnchanted()
                && this.getHealth() < this.getMaxHealth() && this.hasXpRepairEnchantment()) {
            this.absorbExperience();
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.getWorld().isClient) {
            this.wakeUp();
        }
        return super.damage(source, amount);
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        super.pushAwayFrom(entity);
        if (!this.getWorld().isClient && !(entity instanceof BookskitoEntity)) {
            this.wakeUp();
        }
    }

    // =====================================================================
    // Variant / rest state / NBT
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_ID_TYPE_VARIANT, 0);
        builder.add(REST_STATE, RestState.FLYING.ordinal());
    }

    public BookskitoVariant getVariant() {
        return BookskitoVariant.byId(this.getTypeVariant() & 255);
    }

    @Override
    public int getTypeVariant() {
        return this.dataTracker.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariant(BookskitoVariant variant) {
        this.dataTracker.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    public RestState getRestState() {
        return RestState.byId(this.dataTracker.get(REST_STATE));
    }

    public void setRestState(RestState state) {
        this.dataTracker.set(REST_STATE, state.ordinal());
    }

    public boolean isRoosting() {
        return this.getRestState() == RestState.ROOSTING;
    }

    public boolean isGrounded() {
        return this.getRestState() == RestState.GROUNDED;
    }


    /** The gold variant is the enchanted one: it shimmers and accepts enchanted books. */
    public boolean isEnchanted() {
        return this.getVariant() == BookskitoVariant.GOLD;
    }

    /** Everything it currently has, merged from the hidden stacks. */
    public ItemEnchantmentsComponent getStoredEnchantments() {
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (EquipmentSlot slot : ENCHANT_SLOTS) {
            ItemEnchantmentsComponent held = this.getEquippedStack(slot)
                    .getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : held.getEnchantmentEntries()) {
                builder.set(entry.getKey(), entry.getIntValue());
            }
        }
        return builder.build();
    }

    /**
     * Takes what it can from an enchanted book, anvil style: a new enchantment is added unless it conflicts with one
     * it already has, a higher level replaces a lower one and two equal levels make one level more.
     * With simulate = true nothing changes (the client uses it to decide whether the interaction is accepted).
     */
    private boolean absorbBook(ItemStack book, boolean simulate) {
        ItemEnchantmentsComponent incoming = book.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        boolean gained = false;

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : incoming.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            ItemEnchantmentsComponent current = this.getStoredEnchantments();
            int have = current.getLevel(enchantment);

            int result;
            if (have > 0) {
                result = have == level ? Math.min(level + 1, enchantment.value().getMaxLevel()) : Math.max(have, level);
            } else {
                boolean conflict = false;
                for (RegistryEntry<Enchantment> existing : current.getEnchantments()) {
                    if (!Enchantment.canBeCombined(existing, enchantment)) {
                        conflict = true;
                        break;
                    }
                }
                if (conflict) continue;
                result = level;
            }

            if (result > have) {
                gained = true;
                if (!simulate) {
                    this.storeEnchantment(enchantment, result);
                }
            }
        }
        return gained;
    }

    /** Writes the enchantment into the hidden stack of the slot it belongs to. */
    private void storeEnchantment(RegistryEntry<Enchantment> enchantment, int level) {
        EquipmentSlot target = EquipmentSlot.MAINHAND;
        for (EquipmentSlot slot : ENCHANT_SLOTS) {
            if (enchantment.value().slotMatches(slot)) {
                target = slot;
                break;
            }
        }
        // an upgrade stays in the slot it already lives in
        for (EquipmentSlot slot : ENCHANT_SLOTS) {
            if (this.getEquippedStack(slot).getOrDefault(DataComponentTypes.ENCHANTMENTS,
                    ItemEnchantmentsComponent.DEFAULT).getLevel(enchantment) > 0) {
                target = slot;
                break;
            }
        }

        ItemStack stack = this.getEquippedStack(target).copy();
        if (stack.isEmpty()) {
            stack = new ItemStack(Items.BOOK); // invisible: this mob has no equipment renderer
        }
        EnchantmentHelper.apply(stack, builder -> builder.set(enchantment, level));
        this.equipStack(target, stack);
        this.setEquipmentDropChance(target, 0.0F); // the hidden stacks must never drop
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.isEnchanted() && stack.isOf(Items.ENCHANTED_BOOK)) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                if (!this.absorbBook(stack, false)) {
                    return ActionResult.PASS;
                }
                stack.decrementUnlessCreative(1, player);
                this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F, 1.2F);
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getBodyY(0.5), this.getZ(), 20, 0.3, 0.3, 0.3, 0.5);
                return ActionResult.SUCCESS;
            }
            return this.absorbBook(stack, true) ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return super.interactMob(player, hand);
    }

// ---- Mending: any enchantment with the repair-with-xp effect heals it from XP orbs instead ----

    private boolean hasXpRepairEnchantment() {
        for (RegistryEntry<Enchantment> enchantment : this.getStoredEnchantments().getEnchantments()) {
            if (enchantment.value().effects().contains(EnchantmentEffectComponentTypes.REPAIR_WITH_XP)) {
                return true;
            }
        }
        return false;
    }

    private void absorbExperience() {
        List<ExperienceOrbEntity> orbs = this.getWorld().getEntitiesByClass(ExperienceOrbEntity.class,
                this.getBoundingBox().expand(1.0D), orb -> !orb.isRemoved());
        for (ExperienceOrbEntity orb : orbs) {
            if (this.getHealth() >= this.getMaxHealth()) return;
            this.heal(orb.getExperienceAmount() * MENDING_HEAL_PER_XP);
            orb.discard();
            this.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, 0.5F + this.random.nextFloat() * 0.5F);
        }
    }
    /** Client only: one glyph every 4 ticks per enchantment held, capped so a heavily enchanted one stays cheap. */
    private void tickEnchantTrail() {
        if (!this.isEnchanted()) return;
        if (this.age % 10 == 0) {
            this.enchantCount = this.getStoredEnchantments().getEnchantments().size();
        }
        if (this.enchantCount <= 0) return;

        float expected = Math.min(this.enchantCount * ENCHANT_PARTICLES_PER_ENCHANT, MAX_ENCHANT_PARTICLES_PER_TICK);
        int amount = (int) expected;
        if (this.random.nextFloat() < expected - amount) {
            amount++;
        }

        for (int i = 0; i < amount; i++) {
            this.getWorld().addParticle(ParticleTypes.ENCHANT,
                    this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5),
                    (this.random.nextDouble() - 0.5D) * 0.8D,
                    this.random.nextDouble() * 0.5D,
                    (this.random.nextDouble() - 0.5D) * 0.8D);
        }
    }

    /** Leaves whatever resting state it is in and goes back to flying. */
    public void wakeUp() {
        if (this.getRestState() != RestState.FLYING) {
            this.setRestState(RestState.FLYING);
        }
    }

    /**
     * Rest states are deliberately NOT saved: they only mean something while the matching goal is running
     * (same reasoning as the moth's roosting). A reloaded bookskito just takes off and settles again.
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
    // Sounds / animation / flight (all placeholders copied from the moth)
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

    /** Client only: the model reads these animation states. */
    private void updateAnimations() {
        switch (this.getRestState()) {
            case ROOSTING -> {
                this.flyingAnimationState.stop();
                this.groundedAnimationState.stop();
                this.roostingAnimationState.startIfNotRunning(this.age);
            }
            case GROUNDED -> {
                this.flyingAnimationState.stop();
                this.roostingAnimationState.stop();
                this.groundedAnimationState.startIfNotRunning(this.age);
            }
            default -> {
                this.roostingAnimationState.stop();
                this.groundedAnimationState.stop();
                this.flyingAnimationState.startIfNotRunning(this.age);
            }
        }
    }

    public boolean isFlappingWings() {
        return this.isInAir() && this.age % WING_FLAP_INTERVAL == 0 && this.getRestState() == RestState.FLYING;
    }

    @Override
    public boolean isInAir() {
        return !this.isOnGround();
    }

    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {}

    // =====================================================================
    // Roost helpers
    // =====================================================================

    /** What it can perch on. Swap this one method to change that (e.g. back to logs and leaves). */
    private static boolean isRoostBlock(BlockState state) {
        return state.isOf(Blocks.BOOKSHELF) || state.isOf(Blocks.CHISELED_BOOKSHELF);
    }

    private static boolean isValidRoost(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return false;

        int roostSides = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (isRoostBlock(world.getBlockState(pos.offset(direction)))) {
                roostSides++;
            }
        }
        return roostSides > 0 && roostSides < 4;
    }

    // =====================================================================
    // Goals
    // =====================================================================

    class BookskitoBiteGoal extends BugMeleeGoal {
        BookskitoBiteGoal(BookskitoEntity bookskito, double speed, boolean pauseWhenMobIdle) {
            super(bookskito, speed, pauseWhenMobIdle);
        }

        @Override
        public void start() {
            super.start();
            BookskitoEntity.this.wakeUp();
        }
    }

    class BookskitoMateGoal extends AnimalMateGoal {
        BookskitoMateGoal(BookskitoEntity animal, double speed) {
            super(animal, speed);
        }

        @Override
        public void start() {
            super.start();
            BookskitoEntity.this.wakeUp();
        }
    }

    class BookskitoTemptGoal extends TemptGoal {
        BookskitoTemptGoal(PathAwareEntity entity, double speed, Predicate<ItemStack> foodPredicate, boolean canBeScared) {
            super(entity, speed, foodPredicate, canBeScared);
        }

        @Override
        public void start() {
            super.start();
            BookskitoEntity.this.wakeUp();
        }
    }

    /** Flies to a perch (or just somewhere nearby): also the idle wandering. */
    static class FlyToRoostGoal extends Goal {
        protected final BookskitoEntity mob;
        protected double targetX;
        protected double targetY;
        protected double targetZ;
        protected final double speed;
        protected final int horizontalRange;
        protected final int verticalRange;

        FlyToRoostGoal(BookskitoEntity entity, double speed, int horizontalRange, int verticalRange) {
            this.mob = entity;
            this.speed = speed;
            this.horizontalRange = horizontalRange;
            this.verticalRange = verticalRange;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            // while flying: often looks for a new spot; while resting: rarely
            if (this.mob.random.nextInt(1200) != 0
                    && (this.mob.getRestState() != RestState.FLYING || this.mob.random.nextInt(15) != 0)) {
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
            return !this.mob.getNavigation().isIdle();
        }

        @Override
        public void start() {
            this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, this.speed);
            this.mob.wakeUp();
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
                target = this.getRandomValidRoostPos(serverWorld, this.mob.getBlockPos());
            }
            return target == null ? this.randomNearbyPos() : target;
        }

        @Nullable
        private Vec3d getRandomValidRoostPos(ServerWorld world, BlockPos origin) {
            int h = this.horizontalRange / 2;
            int v = this.verticalRange / 2;
            List<BlockPos> valid = new ArrayList<>();
            for (BlockPos pos : BlockPos.iterate(origin.add(-h, -v, -h), origin.add(h, v, h))) {
                if (isValidRoost(world, pos)) {
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

    /** Sticks to a wall once it is next to a perch. */
    static class LatchGoal extends Goal {
        protected final BookskitoEntity mob;
        @Nullable
        protected Direction latchDirection;

        LatchGoal(BookskitoEntity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canStart() {
            return this.mob.getWorld() instanceof ServerWorld serverWorld
                    && this.mob.getRestState() == RestState.FLYING
                    && isValidRoost(serverWorld, this.mob.getBlockPos())
                    && this.mob.random.nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinue() {
            // isRoosting() matters: damage, pushing, tempt, mate and FlyToRoost only change the state,
            // so without it this goal would keep freezing a bookskito that thinks it is flying.
            boolean keep = this.mob.isRoosting()
                    && this.latchDirection != null
                    && this.mob.getWorld() instanceof ServerWorld serverWorld
                    && this.mob.getRandom().nextInt(6000) != 0
                    && !serverWorld.getBlockState(this.mob.getBlockPos().offset(this.latchDirection)).isAir()
                    && this.mob.getVelocity().length() < 0.05F;
            if (!keep) {
                this.mob.wakeUp();
            }
            return keep;
        }

        @Override
        public void start() {
            this.mob.setRestState(RestState.ROOSTING);
            this.mob.getNavigation().stop();

            if (this.mob.getWorld() instanceof ServerWorld serverWorld) {
                this.latchDirection = this.getLatchDirection(serverWorld);
                double inset = this.mob.isBaby() ? 0.42D : 0.34D;
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
                if (isRoostBlock(world.getBlockState(this.mob.getBlockPos().offset(direction)))) {
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

    /** Lands on a nearby floor spot, rests there for a while, then takes off again. */
    static class GroundRestGoal extends Goal {
        private final BookskitoEntity mob;
        private final double speed;
        @Nullable
        private BlockPos target;
        private int restTicks;

        GroundRestGoal(BookskitoEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.mob.getRestState() != RestState.FLYING
                    || this.mob.getTarget() != null
                    || this.mob.isTouchingWater()
                    || this.mob.getRandom().nextInt(300) != 0) {
                return false;
            }
            this.target = this.findLandingSpot();
            return this.target != null;
        }

        @Override
        public boolean shouldContinue() {
            if (this.mob.getRestState() == RestState.GROUNDED) {
                return this.restTicks > 0 && this.mob.getTarget() == null;
            }
            // approaching: woken up or interrupted means target is cleared or the state changed
            return this.target != null && this.mob.getRestState() == RestState.FLYING;
        }

        @Override
        public void start() {
            this.restTicks = 0;
            if (this.target != null) {
                Vec3d spot = Vec3d.ofBottomCenter(this.target);
                this.mob.getNavigation().startMovingTo(spot.x, spot.y, spot.z, this.speed);
            }
        }

        @Override
        public void tick() {
            if (this.mob.getRestState() == RestState.GROUNDED) {
                this.restTicks--;
                Vec3d velocity = this.mob.getVelocity();
                this.mob.setVelocity(0.0D, velocity.y, 0.0D);
            } else if (this.target != null) {
                if (this.mob.squaredDistanceTo(Vec3d.ofBottomCenter(this.target)) < 1.5D) {
                    this.mob.getNavigation().stop();
                    this.mob.setRestState(RestState.GROUNDED);
                    this.restTicks = 200 + this.mob.getRandom().nextInt(400);
                    this.target = null; // so a wake-up does not make it land again
                } else if (this.mob.getNavigation().isIdle()) {
                    this.target = null; // path ended short of the spot: give up
                }
            }
        }

        @Override
        public void stop() {
            if (this.mob.getRestState() == RestState.GROUNDED) {
                this.mob.wakeUp();
            }
            this.mob.getNavigation().stop();
            this.target = null;
        }

        @Nullable
        private BlockPos findLandingSpot() {
            World world = this.mob.getWorld();
            BlockPos origin = this.mob.getBlockPos();
            for (int i = 0; i < 12; i++) {
                BlockPos pos = origin.add(
                        this.mob.getRandom().nextInt(9) - 4,
                        -this.mob.getRandom().nextInt(5),
                        this.mob.getRandom().nextInt(9) - 4);
                BlockPos below = pos.down();
                if (world.isAir(pos) && world.isAir(pos.up())
                        && world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP)) {
                    return pos;
                }
            }
            return null;
        }
    }
}