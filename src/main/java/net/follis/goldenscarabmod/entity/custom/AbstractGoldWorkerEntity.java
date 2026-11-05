package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.entity.ai.MoveToItemGoal;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared logic for the "template + input -> output" golems (Gold Carver, Gold Panner).
 *
 * <ul>
 *   <li>The item in the MAIN HAND is the "template": it decides WHAT the golem produces.</li>
 *   <li>Slot 0 of the inventory is the input it gathered from the ground.</li>
 *   <li>The state machine runs on the SERVER only. The client just reacts to tracked data.</li>
 * </ul>
 *
 * Subclasses only describe what is valid and what a craft does.
 */
public abstract class AbstractGoldWorkerEntity extends PathAwareEntity implements InventoryOwner {

    /** One craft: how many input items it eats and what it spits out. */
    public record Craft(int inputCount, ItemStack output) {}

    public enum State {
        IDLE, PICKUP, CRAFT;

        private static final State[] VALUES = values(); // values() clones the array on every call, so cache it

        public static State byOrdinal(int ordinal) {
            return VALUES[MathHelper.clamp(ordinal, 0, VALUES.length - 1)];
        }
    }

    // ---- tuning ----
    private static final int IDLE_CHECK_INTERVAL = 10;     // ticks between "can I start working?" checks
    private static final int PICKUP_TICKS = 20;            // length of the PICKUP phase (anim is 10 ticks)
    private static final int LEFTOVER_SPIT_TICKS = 200;    // 10 s: input that can never be processed gets spat out
    private static final int IDLE_ANIM_INTERVAL = 40;      // client only: how often the idle animation plays

    // ---- synced data ----
    private static final TrackedData<Integer> DATA_STATE =
            DataTracker.registerData(AbstractGoldWorkerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Incremented at the start of every craft cycle so the client can restart the craft animation. */
    private static final TrackedData<Integer> DATA_CRAFT_CYCLE =
            DataTracker.registerData(AbstractGoldWorkerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // ---- client-side animation states (read by the models) ----
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState pickupAnimationState = new AnimationState();
    public final AnimationState craftAnimationState = new AnimationState();

    private final SimpleInventory inventory = new SimpleInventory(1);

    // server-side timers
    private int stateTicks;
    private int leftoverTicks;
    // client-side timer
    private int idleAnimTimer = IDLE_ANIM_INTERVAL / 2;

    // (input item) -> craft for the CURRENT template item. Avoids scanning recipes every tick.
    private final Map<Item, Optional<Craft>> craftCache = new HashMap<>();
    @Nullable
    private Item cacheTemplateItem;
    private int cacheVersion;

    protected AbstractGoldWorkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // These hold player items, so never despawn them.
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createWorkerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20);
    }

    // =====================================================================
    // What subclasses have to provide
    // =====================================================================

    /** Is this an item the golem is allowed to gather as input? */
    protected abstract boolean isValidInput(ItemStack stack);

    /** Is this an item a player may hand over as the template? */
    protected abstract boolean isValidTemplate(ItemStack stack);

    /**
     * Compute the craft for this input/template pair, or null if the combination is impossible.
     * Must only depend on the ITEMS (not counts/components): the result is cached.
     * The output item should be the template item, otherwise the golem will gather its own output.
     */
    @Nullable
    protected abstract Craft computeCraft(ItemStack input, ItemStack template);

    /** Length of one craft cycle in ticks. Match it to the craft animation length (seconds * 20). */
    protected abstract int getCraftCycleTicks();

    /** Tick inside the cycle at which the input is consumed and the output is thrown. */
    protected abstract int getCraftOutputTick();

    protected abstract SoundEvent getCraftSound();

    protected abstract void spawnCraftParticles(ServerWorld world);

    // =====================================================================
    // Goals
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MoveToItemGoal(this, 1.0, 12.0,
                () -> !this.isBusy() && this.hasItemTemplate(),
                this::canGather));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5) {
            @Override
            public boolean canStart() {
                return !AbstractGoldWorkerEntity.this.isBusy() && super.canStart(); // cheap check first
            }
        });
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    // =====================================================================
    // Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            this.tickClient();
        } else {
            this.tickServer();
        }
    }

    // ---------------- server ----------------

    private void tickServer() {
        switch (this.getCurrentState()) {
            case IDLE -> this.tickIdle();
            case PICKUP -> this.tickPickup();
            case CRAFT -> this.tickCraft();
        }
    }

    private void tickIdle() {
        this.tickLeftover();
        if (++this.stateTicks < IDLE_CHECK_INTERVAL) return;
        this.stateTicks = 0;
        if (this.canWork()) {
            this.setState(State.PICKUP);
        }
    }

    private void tickPickup() {
        if (!this.canWork()) {
            this.setState(State.IDLE);
            return;
        }
        if (++this.stateTicks >= PICKUP_TICKS) {
            this.setState(State.CRAFT);
        }
    }

    private void tickCraft() {
        if (!this.canWork()) { // template taken away, or input ran out
            this.setState(State.IDLE);
            return;
        }
        if (this.stateTicks == 0) {
            this.dataTracker.set(DATA_CRAFT_CYCLE, this.dataTracker.get(DATA_CRAFT_CYCLE) + 1);
        }
        if (this.stateTicks == this.getCraftOutputTick()) {
            this.produce();
        }
        if (++this.stateTicks >= this.getCraftCycleTicks()) {
            this.stateTicks = 0;
        }
    }

    /** Input that will never be processed (wrong count / no recipe for this template) is given back. */
    private void tickLeftover() {
        ItemStack input = this.inventory.getStack(0);
        ItemStack template = this.getMainHandStack();
        boolean stuck = false;
        if (!input.isEmpty() && !template.isEmpty()) {
            Craft craft = this.craftFor(input, template);
            stuck = craft == null || input.getCount() < craft.inputCount();
        }
        if (!stuck) {
            this.leftoverTicks = 0;
            return;
        }
        if (++this.leftoverTicks >= LEFTOVER_SPIT_TICKS) {
            this.leftoverTicks = 0;
            this.throwOutput(this.inventory.removeStack(0));
        }
    }

    private boolean canWork() {
        ItemStack template = this.getMainHandStack();
        ItemStack input = this.inventory.getStack(0);
        if (template.isEmpty() || input.isEmpty()) return false;
        Craft craft = this.craftFor(input, template);
        return craft != null && input.getCount() >= craft.inputCount();
    }

    private void produce() {
        ItemStack input = this.inventory.getStack(0);
        Craft craft = this.craftFor(input, this.getMainHandStack());
        if (craft == null || input.getCount() < craft.inputCount()) return;

        this.inventory.removeStack(0, craft.inputCount());
        this.throwOutput(craft.output().copy());
        this.playSound(this.getCraftSound(), 0.8F, 1.5F);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            this.spawnCraftParticles(serverWorld);
        }
    }

    /** Pops the stack out above the golem (respecting max stack size). Server only. */
    private void throwOutput(ItemStack stack) {
        World world = this.getWorld();
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(Math.min(stack.getCount(), stack.getMaxCount()));
            ItemEntity item = new ItemEntity(world, this.getX(), this.getEyeY() - 0.2, this.getZ(), part);
            item.setVelocity(
                    (this.random.nextDouble() - 0.5) * 0.1,
                    0.3,
                    (this.random.nextDouble() - 0.5) * 0.1);
            item.setToDefaultPickupDelay();
            item.setThrower(this);
            world.spawnEntity(item);
        }
    }

    private void setState(State state) {
        this.stateTicks = 0;
        this.leftoverTicks = 0;
        if (state != State.IDLE) {
            this.getNavigation().stop(); // stand still while working
        }
        this.dataTracker.set(DATA_STATE, state.ordinal());
    }

    // ---------------- client ----------------

    private void tickClient() {
        if (this.getCurrentState() == State.IDLE && --this.idleAnimTimer <= 0) {
            this.idleAnimationState.start(this.age);
            this.idleAnimTimer = IDLE_ANIM_INTERVAL;
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (!this.getWorld().isClient()) return;

        if (DATA_STATE.equals(data)) {
            State state = this.getCurrentState();
            if (state == State.IDLE) {
                this.idleAnimTimer = IDLE_ANIM_INTERVAL / 2;
            } else {
                this.idleAnimationState.stop();
            }
            if (state == State.PICKUP) {
                this.pickupAnimationState.startIfNotRunning(this.age);
            } else {
                this.pickupAnimationState.stop();
            }
            if (state == State.CRAFT) {
                this.craftAnimationState.startIfNotRunning(this.age);
            } else {
                this.craftAnimationState.stop();
            }
        } else if (DATA_CRAFT_CYCLE.equals(data) && this.getCurrentState() == State.CRAFT) {
            this.craftAnimationState.start(this.age); // new cycle -> replay the animation
        }
    }

    // =====================================================================
    // Craft cache
    // =====================================================================

    @Nullable
    protected Craft craftFor(ItemStack input, ItemStack template) {
        if (input.isEmpty() || template.isEmpty()) return null;
        int version = this.getCraftCacheVersion();
        if (template.getItem() != this.cacheTemplateItem || version != this.cacheVersion) {
            this.craftCache.clear();
            this.cacheTemplateItem = template.getItem();
            this.cacheVersion = version;
        }
        return this.craftCache
                .computeIfAbsent(input.getItem(), item -> Optional.ofNullable(this.computeCraft(input, template)))
                .orElse(null);
    }

    /** Subclasses whose crafts depend on reloadable data return a number that changes on reload. */
    protected int getCraftCacheVersion() {
        return 0;
    }

    // =====================================================================
    // Getters
    // =====================================================================

    public State getCurrentState() {
        return State.byOrdinal(this.dataTracker.get(DATA_STATE));
    }

    /** True while the golem is picking up / crafting (it should not walk around). */
    public boolean isBusy() {
        return this.getCurrentState() != State.IDLE;
    }

    public boolean hasItemTemplate() {
        return !this.getMainHandStack().isEmpty(); // no ItemStack copy, unlike copyWithCount(1)
    }

    @Override
    public SimpleInventory getInventory() {
        return this.inventory;
    }

    // =====================================================================
    // Item gathering
    // =====================================================================

    /**
     * Called by {@link MoveToItemGoal} (pickup is done by the goal itself, so the mobGriefing gamerule
     * and vanilla's loot scan are not involved: feeding a golem isn't griefing).
     */
    @Override
    public boolean canGather(ItemStack stack) {
        ItemStack template = this.getMainHandStack();
        return !template.isEmpty()
                && this.isValidInput(stack)
                && !ItemStack.areItemsEqual(template, stack) // never re-grab our own output
                && this.craftFor(stack, template) != null
                && this.inventory.canInsert(stack);
    }

    // =====================================================================
    // Player interaction
    // =====================================================================

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        ItemStack template = this.getMainHandStack();

        // give a template
        if (template.isEmpty() && !held.isEmpty() && this.isValidTemplate(held)) {
            if (!this.getWorld().isClient()) {
                this.setStackInHand(Hand.MAIN_HAND, held.copyWithCount(1));
                held.decrementUnlessCreative(1, player);
                this.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.0F, 1.0F);
            }
            return ActionResult.SUCCESS;
        }

        // take the template back (empty hand)
        if (!template.isEmpty() && hand == Hand.MAIN_HAND && held.isEmpty()) {
            if (!this.getWorld().isClient()) {
                ItemStack back = template.copy();
                this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.playSound(SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, 2.0F, 1.0F);
                if (!player.isCreative()) { // creative players keep theirs: avoids a dupe
                    player.giveItemStack(back);
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    // =====================================================================
    // Drops / NBT / data tracker / sounds
    // =====================================================================

    @Override
    protected void dropInventory() {
        super.dropInventory();
        this.inventory.clearToList().forEach(this::dropStack);
        ItemStack template = this.getEquippedStack(EquipmentSlot.MAINHAND);
        if (!template.isEmpty()
                && !EnchantmentHelper.hasAnyEnchantmentsWith(template, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
            this.dropStack(template);
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_STATE, State.IDLE.ordinal());
        builder.add(DATA_CRAFT_CYCLE, 0);
    }

    // The state is transient (timers aren't saved either), so a reloaded golem simply starts IDLE
    // and re-evaluates. Only the inventory needs to persist.
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeInventory(nbt, this.getRegistryManager());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readInventory(nbt, this.getRegistryManager());
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PARROT_AMBIENT;
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
}