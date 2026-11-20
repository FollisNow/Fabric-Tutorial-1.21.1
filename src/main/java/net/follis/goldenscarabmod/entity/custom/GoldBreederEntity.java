package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.item.ModItems;
import net.follis.goldenscarabmod.util.IBugVariants;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Breeder golem.
 *
 * <ul>
 *   <li>Every PRODUCE_INTERVAL_TICKS it harvests PRODUCE_AMOUNT flowers (for bee-like bugs) and, offset by half an
 *       interval, PRODUCE_AMOUNT locust meat (for meat eaters), with the matching harvest animation, into its own
 *       inventory.</li>
 *   <li>Every few seconds it looks for two compatible bugs nearby. If it holds food they both accept, it aims its
 *       love gun at the first, fires, swings to the second and fires again. Each shot consumes the food and puts that
 *       bug in love; the bugs' own mate goals do the rest.</li>
 *   <li>The server runs the state machine; the client only reacts to entity statuses (animations) and to the tracked
 *       aim target (arm aiming, smoothed on the client).</li>
 * </ul>
 *
 * Deliberately NOT based on AbstractGoldWorkerEntity: it has no template / input -> output craft loop.
 */
public class GoldBreederEntity extends PathAwareEntity implements InventoryOwner {

    // =====================================================================
    // Tuning  (X units every Y minutes: change PRODUCE_AMOUNT / PRODUCE_INTERVAL_TICKS)
    // =====================================================================
    private static final int PRODUCE_INTERVAL_TICKS = 20 * 60 * 2; // Y = 2 minutes
    private static final int PRODUCE_AMOUNT = 2;                   // X
    private static final int MAX_STORED_PER_RESOURCE = 64;
    private static final int INVENTORY_SIZE = 9;

    /** Item produced for meat-eating bugs. Must be accepted by those bugs' isBreedingItem (e.g. in minecraft:meat). */
    private static Item meatItem() {
        return ModItems.LOCUST_GRASSHOPPER;
    }

    private static final int IDLE_CHECK_INTERVAL = 10;       // ticks between "anything to do?" checks
    private static final int BREED_EVERY_N_CHECKS = 10;      // => a breeding scan every 100 ticks
    private static final double BREED_RANGE = 8.0D;         // blocks around the golem it looks for bugs
    private static final double MAX_PAIR_DISTANCE = 6.0D;    // pair must be close: their mate goal searches ~8 blocks
    private static final int MAX_SAME_SPECIES_IN_RANGE = 12; // density cap, stops population explosions
    private static final int FOOD_PER_BUG = 1;

    // harvest timeline (the harvest animations are 1.0 s = 20 ticks)
    private static final int HARVEST_TICKS = 20;
    private static final int HARVEST_OUTPUT_TICK = 12;

    // shooting timeline (love gun animation = 0.4167 s, squashes at ~0.125 s and ~0.29 s after it starts)
    private static final int SHOOT_ANIM_START_TICK = 3; // arm is up by then
    private static final int SHOOT_A_TICK = 6;
    private static final int SHOOT_B_TICK = 9;
    private static final int SHOOT_LOWER_TICK = 12;
    private static final int SHOOT_END_TICK = 18;

    // client animation lengths (ticks)
    private static final int HARVEST_ANIM_TICKS = 20;
    private static final int LOVE_GUN_ANIM_TICKS = 9;

    // client aim smoothing
    private static final float AIM_TURN_DEGREES_PER_TICK = 50.0F;
    private static final float AIM_RAISE_PER_TICK = 0.34F;
    private static final float AIM_LOWER_PER_TICK = 0.20F;

    // gun position relative to the golem (blocks), used for aiming and for the beam
    private static final double GUN_SIDE = 0.44D;
    private static final double GUN_HEIGHT = 1.34D;

    // entity statuses used to start client animations
    private static final byte STATUS_HARVEST_FLOWERS = 100;
    private static final byte STATUS_HARVEST_MEAT = 101;
    private static final byte STATUS_LOVE_GUN = 102;

    private static final TrackedData<Integer> DATA_AIM_TARGET =
            DataTracker.registerData(GoldBreederEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Ordinal of the current BreedMode (synced so the client can draw the matching icon on the gun). */
    private static final TrackedData<Integer> DATA_BREED_MODE =
            DataTracker.registerData(GoldBreederEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Which pairs the golem is allowed to breed. Right-click the golem to cycle. */
    public enum BreedMode {
        /** Any two compatible bugs of the same species. */
        ALL,
        /** Same species AND same variant (keeps a variant pure instead of mixing). */
        SAME_VARIANT;

        public BreedMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public static BreedMode byOrdinal(int ordinal) {
            BreedMode[] all = values();
            return all[MathHelper.clamp(ordinal, 0, all.length - 1)];
        }

        /** Icon drawn on the gun. TODO: swap these placeholders for your own icon items. */
        public Item icon() {
            return switch (this) {
                case ALL -> Items.PINK_DYE;
                case SAME_VARIANT -> Items.SPYGLASS;
            };
        }
    }

    private enum State { IDLE, HARVEST, SHOOT }

    private enum Resource { FLOWERS, MEAT }

    // ---- client animation states (read by the model) ----
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState harvestCropAnimationState = new AnimationState();
    public final AnimationState harvestMeatAnimationState = new AnimationState();
    public final AnimationState loveGunAnimationState = new AnimationState();
    /** Idle loop of the decorative locust in the glass bottle. */
    public final AnimationState bottleLocustAnimationState = new AnimationState();

    private final SimpleInventory inventory = new SimpleInventory(INVENTORY_SIZE);

    // ---- server state ----
    private State state = State.IDLE;
    private int stateTicks;
    private int idleChecks;
    private int breedChecks;
    private int flowerTimer = PRODUCE_INTERVAL_TICKS;
    private int meatTimer = PRODUCE_INTERVAL_TICKS + PRODUCE_INTERVAL_TICKS / 2;
    @Nullable private Resource harvesting;
    @Nullable private AnimalEntity shotA;
    @Nullable private AnimalEntity shotB;
    @Nullable private Item foodA;
    @Nullable private Item foodB;

    // ---- client state ----
    private int harvestStopTick;
    private int loveGunStopTick;
    private float aimYaw, prevAimYaw;       // degrees, relative to the body, positive = to the golem's right
    private float aimPitch, prevAimPitch;   // degrees, positive = up
    private float aimBlend, prevAimBlend;   // 0 = arm hanging, 1 = fully aimed

    public GoldBreederEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent(); // holds items: never despawn
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AbstractGoldWorkerEntity.createWorkerAttributes();
    }

    // =====================================================================
    // Goals
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5) {
            @Override
            public boolean canStart() {
                return !GoldBreederEntity.this.isBusy() && super.canStart();
            }
        });
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F) {
            @Override
            public boolean canStart() {
                return !GoldBreederEntity.this.isBusy() && super.canStart();
            }
        });
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    public boolean isBusy() {
        return this.state != State.IDLE;
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
        if (this.flowerTimer > 0) this.flowerTimer--;
        if (this.meatTimer > 0) this.meatTimer--;

        switch (this.state) {
            case IDLE -> this.tickIdle();
            case HARVEST -> this.tickHarvest();
            case SHOOT -> this.tickShoot();
        }
    }

    private void setState(State newState) {
        this.state = newState;
        this.stateTicks = 0;
        if (newState != State.IDLE) {
            this.getNavigation().stop(); // stand still while working
        }
    }

    private void tickIdle() {
        if (++this.idleChecks < IDLE_CHECK_INTERVAL) return;
        this.idleChecks = 0;

        if (++this.breedChecks >= BREED_EVERY_N_CHECKS) {
            this.breedChecks = 0;
            if (!this.inventory.isEmpty() && this.tryStartBreeding()) return;
        }

        if (this.flowerTimer <= 0 && this.stored(Resource.FLOWERS) < MAX_STORED_PER_RESOURCE) {
            this.startHarvest(Resource.FLOWERS);
        } else if (this.meatTimer <= 0 && this.stored(Resource.MEAT) < MAX_STORED_PER_RESOURCE) {
            this.startHarvest(Resource.MEAT);
        }
    }

    // ---- harvesting ----

    private void startHarvest(Resource resource) {
        this.harvesting = resource;
        if (resource == Resource.FLOWERS) {
            this.flowerTimer = PRODUCE_INTERVAL_TICKS;
            this.getWorld().sendEntityStatus(this, STATUS_HARVEST_FLOWERS);
        } else {
            this.meatTimer = PRODUCE_INTERVAL_TICKS;
            this.getWorld().sendEntityStatus(this, STATUS_HARVEST_MEAT);
        }
        this.setState(State.HARVEST);
    }

    private void tickHarvest() {
        int t = this.stateTicks++;
        if (t == HARVEST_OUTPUT_TICK && this.harvesting != null) {
            this.produce(this.harvesting);
        } else if (t >= HARVEST_TICKS) {
            this.harvesting = null;
            this.setState(State.IDLE);
        }
    }

    private void produce(Resource resource) {
        ItemStack stack = resource == Resource.MEAT
                ? new ItemStack(meatItem(), PRODUCE_AMOUNT)
                : this.randomFlowers(PRODUCE_AMOUNT);
        if (stack.isEmpty() || !this.inventory.canInsert(stack)) return;

        this.inventory.addStack(stack);
        // only one sound per harvest: crop breaking for bee food, a stab for meat
        if (resource == Resource.MEAT) {
            this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 0.8F, 1.3F);
        } else {
            this.playSound(SoundEvents.BLOCK_CROP_BREAK, 0.9F, 1.0F);
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** A random flower accepted by bee-like bugs (minecraft:bee_food, so modded flowers are included). */
    private ItemStack randomFlowers(int count) {
        return Registries.ITEM.getEntryList(ItemTags.BEE_FOOD)
                .flatMap(list -> list.getRandom(this.random))
                .map(entry -> new ItemStack(entry.value(), count))
                .orElse(ItemStack.EMPTY);
    }

    private int stored(Resource resource) {
        int total = 0;
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.getStack(i);
            if (stack.isEmpty()) continue;
            boolean matches = resource == Resource.MEAT ? stack.isOf(meatItem()) : stack.isIn(ItemTags.BEE_FOOD);
            if (matches) total += stack.getCount();
        }
        return total;
    }

    // ---- breeding ----

    /** Arthropod, adult, not already in love, and (for tameables like the spiderling) tamed and standing. */
    private boolean isBreedCandidate(AnimalEntity bug) {
        if (bug.isBaby() || bug.getBreedingAge() != 0 || bug.isInLove()) return false;
        return !(bug instanceof TameableEntity tameable && (!tameable.isTamed() || tameable.isInSittingPose()));
    }

    /** Item in the inventory this bug accepts, keeping `reserved` (already promised to the other bug) available. */
    @Nullable
    private Item pickFood(AnimalEntity bug, @Nullable Item reserved) {
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.getStack(i);
            if (stack.isEmpty() || !bug.isBreedingItem(stack)) continue;
            Item item = stack.getItem();
            int available = this.inventory.count(item) - (item == reserved ? FOOD_PER_BUG : 0);
            if (available >= FOOD_PER_BUG) return item;
        }
        return null;
    }

    public BreedMode getBreedMode() {
        return BreedMode.byOrdinal(this.dataTracker.get(DATA_BREED_MODE));
    }

    public void setBreedMode(BreedMode mode) {
        this.dataTracker.set(DATA_BREED_MODE, mode.ordinal());
    }

    /**
     * ALL accepts any pair. SAME_VARIANT compares IBugVariants#getTypeVariant(); bugs that have no variant
     * (e.g. vanilla bees) always count as "same".
     */
    private boolean matchesMode(AnimalEntity a, AnimalEntity b) {
        if (this.getBreedMode() == BreedMode.ALL) return true;
        return !(a instanceof IBugVariants first)
                || !(b instanceof IBugVariants second)
                || first.getTypeVariant() == second.getTypeVariant();
    }

    private boolean tryStartBreeding() {
        Box area = this.getBoundingBox().expand(BREED_RANGE, BREED_RANGE / 2.0, BREED_RANGE);
        List<AnimalEntity> bugs = this.getWorld().getEntitiesByClass(AnimalEntity.class, area,
                bug -> bug.isAlive() && bug.getType().isIn(EntityTypeTags.ARTHROPOD));
        if (bugs.size() < 2) return false;

        Map<Class<?>, Integer> perSpecies = new HashMap<>();
        for (AnimalEntity bug : bugs) {
            perSpecies.merge(bug.getClass(), 1, Integer::sum);
        }

        List<AnimalEntity> candidates = new ArrayList<>();
        for (AnimalEntity bug : bugs) {
            if (this.isBreedCandidate(bug)) candidates.add(bug);
        }
        for (int i = candidates.size() - 1; i > 0; i--) { // shuffle so the same pair isn't always picked
            Collections.swap(candidates, i, this.random.nextInt(i + 1));
        }

        for (int i = 0; i < candidates.size(); i++) {
            AnimalEntity a = candidates.get(i);
            if (perSpecies.getOrDefault(a.getClass(), 0) >= MAX_SAME_SPECIES_IN_RANGE) continue;
            Item first = this.pickFood(a, null);
            if (first == null) continue;

            for (int j = i + 1; j < candidates.size(); j++) {
                AnimalEntity b = candidates.get(j);
                if (b.getClass() != a.getClass()
                        || a.squaredDistanceTo(b) > MAX_PAIR_DISTANCE * MAX_PAIR_DISTANCE
                        || !this.matchesMode(a, b)) continue;
                Item second = this.pickFood(b, first);
                if (second == null) continue;

                this.shotA = a;
                this.shotB = b;
                this.foodA = first;
                this.foodB = second;
                this.setState(State.SHOOT);
                return true;
            }
        }
        return false;
    }

    private void tickShoot() {
        int t = this.stateTicks++;
        if (t == 0) {
            if (!this.isUsable(this.shotA) || !this.isUsable(this.shotB)) {
                this.finishShooting();
                return;
            }
            this.setAimTarget(this.shotA); // raises the arm and starts turning
        } else if (t == SHOOT_ANIM_START_TICK) {
            this.getWorld().sendEntityStatus(this, STATUS_LOVE_GUN);
        } else if (t == SHOOT_A_TICK) {
            this.fire(this.shotA, this.foodA, 1.6F);
            this.setAimTarget(this.shotB); // swing to the second bug
        } else if (t == SHOOT_B_TICK) {
            this.fire(this.shotB, this.foodB, 1.9F);
        } else if (t == SHOOT_LOWER_TICK) {
            this.setAimTarget(null);
        } else if (t >= SHOOT_END_TICK) {
            this.finishShooting();
        }
    }

    private boolean isUsable(@Nullable AnimalEntity bug) {
        return bug != null && bug.isAlive() && !bug.isRemoved() && !bug.isInLove();
    }

    private void finishShooting() {
        this.setAimTarget(null);
        this.shotA = null;
        this.shotB = null;
        this.foodA = null;
        this.foodB = null;
        this.setState(State.IDLE);
    }

    private void fire(@Nullable AnimalEntity bug, @Nullable Item food, float pitch) {
        if (food == null || !this.isUsable(bug)) return;
        // the item is only consumed if the shot actually goes off
        if (this.inventory.removeItem(food, FOOD_PER_BUG).getCount() < FOOD_PER_BUG) return;

        bug.lovePlayer(null); // no player: the bug just becomes "in love" (hearts are sent by lovePlayer)
        this.playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 0.9F, pitch); // "pew"

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d from = this.gunPos(0.5);
            Vec3d to = bug.getPos().add(0, bug.getHeight() * 0.6, 0);
            Vec3d delta = to.subtract(from);
            int points = Math.max(2, (int) (delta.length() / 0.75));
            for (int i = 1; i <= points; i++) {
                Vec3d p = from.add(delta.multiply(i / (double) points));
                serverWorld.spawnParticles(ParticleTypes.HEART, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void setAimTarget(@Nullable Entity target) {
        this.dataTracker.set(DATA_AIM_TARGET, target == null ? -1 : target.getId());
    }

    /** Gun position in world space: left of the body, at shoulder height, `forward` blocks ahead. */
    private Vec3d gunPos(double forward) {
        double yaw = Math.toRadians(this.bodyYaw);
        double leftX = Math.cos(yaw);
        double leftZ = Math.sin(yaw);
        double fwdX = -Math.sin(yaw);
        double fwdZ = Math.cos(yaw);
        return new Vec3d(
                this.getX() + leftX * GUN_SIDE + fwdX * forward,
                this.getY() + GUN_HEIGHT,
                this.getZ() + leftZ * GUN_SIDE + fwdZ * forward);
    }

    // ---------------- client ----------------

    private void tickClient() {
        this.idleAnimationState.startIfNotRunning(this.age);
        this.bottleLocustAnimationState.startIfNotRunning(this.age);

        if (this.harvestStopTick > 0 && this.age >= this.harvestStopTick) {
            this.harvestCropAnimationState.stop();
            this.harvestMeatAnimationState.stop();
            this.harvestStopTick = 0;
        }
        if (this.loveGunStopTick > 0 && this.age >= this.loveGunStopTick) {
            this.loveGunAnimationState.stop();
            this.loveGunStopTick = 0;
        }

        this.tickAim();
    }

    private void tickAim() {
        this.prevAimYaw = this.aimYaw;
        this.prevAimPitch = this.aimPitch;
        this.prevAimBlend = this.aimBlend;

        int id = this.dataTracker.get(DATA_AIM_TARGET);
        Entity target = id < 0 ? null : this.getWorld().getEntityById(id);
        if (target == null) {
            this.aimBlend = Math.max(0.0F, this.aimBlend - AIM_LOWER_PER_TICK);
            return;
        }

        Vec3d to = new Vec3d(target.getX(), target.getBodyY(0.6), target.getZ()).subtract(this.gunPos(0.0));
        float worldYaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
        float relativeYaw = MathHelper.wrapDegrees(worldYaw - this.bodyYaw);
        float elevation = (float) (MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0 / Math.PI);

        this.aimYaw += MathHelper.clamp(MathHelper.wrapDegrees(relativeYaw - this.aimYaw),
                -AIM_TURN_DEGREES_PER_TICK, AIM_TURN_DEGREES_PER_TICK);
        this.aimPitch += MathHelper.clamp(elevation - this.aimPitch,
                -AIM_TURN_DEGREES_PER_TICK, AIM_TURN_DEGREES_PER_TICK);
        this.aimBlend = Math.min(1.0F, this.aimBlend + AIM_RAISE_PER_TICK);
    }

    public float getAimYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevAimYaw, this.aimYaw);
    }

    public float getAimPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevAimPitch, this.aimPitch);
    }

    public float getAimBlend(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevAimBlend, this.aimBlend);
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case STATUS_HARVEST_FLOWERS -> {
                this.harvestMeatAnimationState.stop();
                this.harvestCropAnimationState.start(this.age);
                this.harvestStopTick = this.age + HARVEST_ANIM_TICKS;
            }
            case STATUS_HARVEST_MEAT -> {
                this.harvestCropAnimationState.stop();
                this.harvestMeatAnimationState.start(this.age);
                this.harvestStopTick = this.age + HARVEST_ANIM_TICKS;
            }
            case STATUS_LOVE_GUN -> {
                this.loveGunAnimationState.start(this.age);
                this.loveGunStopTick = this.age + LOVE_GUN_ANIM_TICKS;
            }
            default -> super.handleStatus(status);
        }
    }

    // =====================================================================
    // Inventory / interaction
    // =====================================================================

    @Override
    public SimpleInventory getInventory() {
        return this.inventory;
    }

    /** Right-click cycles the breeding mode (name tags are left alone so you can still rename it). */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || player.getStackInHand(hand).isOf(Items.NAME_TAG)) {
            return super.interactMob(player, hand);
        }
        if (!this.getWorld().isClient()) {
            BreedMode next = this.getBreedMode().next();
            this.setBreedMode(next);
            this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.0F + 0.2F * next.ordinal());
        }
        return ActionResult.SUCCESS;
    }

    // =====================================================================
    // Data / NBT / sounds
    // =====================================================================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_AIM_TARGET, -1);
        builder.add(DATA_BREED_MODE, BreedMode.ALL.ordinal());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.writeInventory(nbt, this.getRegistryManager());
        nbt.putInt("FlowerTimer", this.flowerTimer);
        nbt.putInt("MeatTimer", this.meatTimer);
        nbt.putString("BreedMode", this.getBreedMode().name());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.readInventory(nbt, this.getRegistryManager());
        if (nbt.contains("FlowerTimer")) this.flowerTimer = nbt.getInt("FlowerTimer");
        if (nbt.contains("MeatTimer")) this.meatTimer = nbt.getInt("MeatTimer");
        BreedMode mode = BreedMode.ALL;
        String savedMode = nbt.getString("BreedMode");
        for (BreedMode candidate : BreedMode.values()) {
            if (candidate.name().equals(savedMode)) mode = candidate;
        }
        this.setBreedMode(mode);
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