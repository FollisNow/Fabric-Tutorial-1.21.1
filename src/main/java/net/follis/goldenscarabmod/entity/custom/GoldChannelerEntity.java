package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Hears noise through the vanilla {@link Vibrations} system (same as the Warden / sculk sensors): the little
 * vibration particle travels from the noise to the golem, takes time proportional to the distance, and wool
 * blocks occlude it. Sneaking players are ignored, as with a sculk sensor.
 *
 * When a noise that passes the filter ARRIVES and there are buttons within reach, the golem goes into a
 * frenzy: it presses the buttons ONE AT A TIME, alternating arms, one press animation per button. The
 * buttons nearest to where the noise came from are pressed first, then the rest, and it keeps looping over
 * the list until the frenzy is over.
 *
 * Right-click it with an EMPTY hand to cycle the filter (sneak + right-click cycles backwards).
 */
public class GoldChannelerEntity extends PathAwareEntity implements Vibrations {

    // ---- tuning ----
    private static final int LISTEN_RANGE = 16;            // how far it can hear (blocks)
    private static final double BUTTON_REACH = 4.0;        // how far it can press buttons (blocks)
    private static final int MAX_BUTTONS = 24;             // only the 24 buttons nearest to the noise are used
    private static final int PRESS_INTERVAL = 6;           // ticks between the START of two presses (arms alternate)
    private static final int PRESS_CONTACT_TICKS = 5;      // ticks from animation start to the piston hitting (0.25 s)
    private static final int MIN_FRENZY_TICKS = 100;       // at least 5 s...
    private static final int MAX_FRENZY_TICKS = 400;       // ...at most 20 s (scaled with the number of buttons)
    private static final int COOLDOWN_AFTER_FRENZY = 40;   // deaf for 2 s after a frenzy
    private static final int COOLDOWN_NO_BUTTONS = 20;     // throttle: heard something but nothing to press

    /** What it listens to. Right-click cycles through these in order. */
    public enum Filter {
        ALL(Items.SCULK_SENSOR),
        PLAYERS(Items.PLAYER_HEAD),
        MOBS(Items.ZOMBIE_HEAD),
        BLOCKS(Items.COBBLESTONE),
        PROJECTILES(Items.ARROW),
        EXPLOSIONS(Items.TNT);

        private static final Filter[] VALUES = values();

        private final Item icon;

        Filter(Item icon) {
            this.icon = icon;
        }

        /** Item shown on the golem's crest while this filter is selected. */
        public Item getIcon() {
            return this.icon;
        }

        public Filter next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }

        public Filter previous() {
            return VALUES[(this.ordinal() + VALUES.length - 1) % VALUES.length];
        }

        public static Filter byOrdinal(int ordinal) {
            return VALUES[MathHelper.clamp(ordinal, 0, VALUES.length - 1)];
        }

        public String translationKey() {
            return "entity." + GoldenScarabMod.MOD_ID + ".button_presser.filter."
                    + this.name().toLowerCase(Locale.ROOT);
        }

        /** Does a noise made by {@code source} with this game event pass the filter? */
        public boolean accepts(RegistryEntry<GameEvent> event, @Nullable Entity source) {
            return switch (this) {
                case ALL -> true;
                case PLAYERS -> source instanceof PlayerEntity;
                case MOBS -> source instanceof LivingEntity && !(source instanceof PlayerEntity);
                case BLOCKS -> isBlockEvent(event);
                case PROJECTILES -> source instanceof ProjectileEntity
                        || event.matches(GameEvent.PROJECTILE_SHOOT)
                        || event.matches(GameEvent.PROJECTILE_LAND);
                case EXPLOSIONS -> event.matches(GameEvent.EXPLODE);
            };
        }

        private static boolean isBlockEvent(RegistryEntry<GameEvent> event) {
            return event.matches(GameEvent.BLOCK_PLACE)
                    || event.matches(GameEvent.BLOCK_DESTROY)
                    || event.matches(GameEvent.BLOCK_OPEN)
                    || event.matches(GameEvent.BLOCK_CLOSE)
                    || event.matches(GameEvent.BLOCK_ACTIVATE)
                    || event.matches(GameEvent.BLOCK_DEACTIVATE)
                    || event.matches(GameEvent.BLOCK_CHANGE)
                    || event.matches(GameEvent.CONTAINER_OPEN)
                    || event.matches(GameEvent.CONTAINER_CLOSE);
        }
    }

    /** The selected filter, synced so the client can draw its icon on the crest. */
    private static final TrackedData<Integer> DATA_FILTER =
            DataTracker.registerData(GoldChannelerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DATA_FRENZY =
            DataTracker.registerData(GoldChannelerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /**
     * "A press just started": value = counter * 2 + (1 if it is the right arm). It changes on every press, so the
     * client can start the matching arm animation once per button.
     */
    private static final TrackedData<Integer> DATA_PRESS =
            DataTracker.registerData(GoldChannelerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // client-side animation states (read by the model)
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState pressLeftAnimationState = new AnimationState();
    public final AnimationState pressRightAnimationState = new AnimationState();

    // ---- vanilla vibration plumbing (same layout as WardenEntity) ----
    private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler;
    private final Vibrations.Callback vibrationCallback = new NoiseCallback();
    private Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();

    // ---- server-side state ----
    private boolean noisePending;
    @Nullable
    private Vec3d noiseOrigin;                 // where the noise that woke us up was made
    private int cooldown;
    private int frenzyTicks;
    private final List<BlockPos> queue = new ArrayList<>();   // buttons, nearest-to-the-noise first
    private int queueIndex;
    private int pressTimer;                    // ticks until the next press starts
    @Nullable
    private BlockPos pendingPress;             // button whose punch is in flight
    private int pendingPressTicks;
    private boolean nextArmRight;
    private int pressCounter;
    @Nullable
    private Vec3d lookTarget;

    public GoldChannelerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent(); // a placed device: never despawn
        this.gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16);
    }

    // =====================================================================
    // Goals
    // =====================================================================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FrenzyGoal()); // holds MOVE/LOOK/JUMP while frenzied: preempts the rest
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    /** Stand still and turn towards the button being pressed, so the arm that punches is facing it. */
    private class FrenzyGoal extends Goal {
        FrenzyGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
        }

        @Override
        public boolean canStart() {
            return GoldChannelerEntity.this.isFrenzied();
        }

        @Override
        public boolean shouldContinue() {
            return GoldChannelerEntity.this.isFrenzied();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void start() {
            GoldChannelerEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            GoldChannelerEntity self = GoldChannelerEntity.this;
            Vec3d target = self.lookTarget;
            if (target == null) return;

            self.getLookControl().lookAt(target.x, target.y, target.z);

            double dx = target.x - self.getX();
            double dz = target.z - self.getZ();
            if (dx * dx + dz * dz > 0.01) { // not when the button is straight above/below
                float wanted = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                float diff = MathHelper.wrapDegrees(wanted - self.getYaw()); // shortest way round, -180..180
                self.setYaw(self.getYaw() + MathHelper.clamp(diff, -30.0F, 30.0F)); // at most 30 degrees per tick
                self.setBodyYaw(self.getYaw());
            }
        }
    }

    // =====================================================================
    // Hearing (vanilla vibration system)
    // =====================================================================

    @Override
    public Vibrations.ListenerData getVibrationListenerData() {
        return this.vibrationListenerData;
    }

    @Override
    public Vibrations.Callback getVibrationCallback() {
        return this.vibrationCallback;
    }

    @Override
    public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            callback.accept(this.gameEventHandler, serverWorld);
        }
    }

    /**
     * The vanilla default of {@code canAccept} already handles: the "vibrations" tag, spectators, sneaking
     * players and wool-like blocks that dampen vibrations. {@link #acceptsNoise} adds our own rules.
     */
    private class NoiseCallback implements Vibrations.Callback {
        private final PositionSource positionSource =
                new EntityPositionSource(GoldChannelerEntity.this, GoldChannelerEntity.this.getStandingEyeHeight());

        @Override
        public int getRange() {
            return LISTEN_RANGE;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public boolean triggersAvoidCriterion() {
            return false;
        }

        /** Called when the vibration is picked up, BEFORE the particle starts travelling. */
        @Override
        public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event,
                               GameEvent.Emitter emitter) {
            return GoldChannelerEntity.this.acceptsNoise(event, emitter);
        }

        /** Called when the vibration ARRIVES (after the distance-based delay). {@code pos} = where it was made. */
        @Override
        public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event,
                           @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
            GoldChannelerEntity.this.onNoiseHeard(pos);
        }

        @Override
        public void onListen() {
            GoldChannelerEntity.this.updateEventHandler(EntityGameEventHandler::onEntitySetPosCallback);
        }
    }

    private boolean acceptsNoise(RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
        if (!this.isAlive() || this.noisePending || this.cooldown > 0 || this.isFrenzied()) return false;

        Entity source = emitter.sourceEntity();
        if (source == this) return false;

        // Never react to buttons: it would hear its own presses (and the releases) and never stop.
        BlockState affected = emitter.affectedState();
        if (affected != null && affected.getBlock() instanceof ButtonBlock) return false;

        return this.getFilter().accepts(event, source);
    }

    private void onNoiseHeard(BlockPos pos) {
        if (this.isFrenzied() || this.cooldown > 0) return; // busy by the time it arrived
        this.noiseOrigin = Vec3d.ofCenter(pos);
        this.noisePending = true;
    }

    // =====================================================================
    // Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vibrations.Ticker.tick(serverWorld, this.vibrationListenerData, this.vibrationCallback);
            this.tickServer();
        } else if (!this.isFrenzied()) {
            this.idleAnimationState.startIfNotRunning(this.age);
        }
    }

    private void tickServer() {
        if (this.cooldown > 0) this.cooldown--;

        if (this.isFrenzied()) {
            this.tickFrenzy();
        } else if (this.noisePending) {
            this.noisePending = false;
            if (this.cooldown <= 0) this.tryStartFrenzy();
        }
    }

    private void tryStartFrenzy() {
        this.buildQueue();
        if (this.queue.isEmpty()) { // nothing to press: stay calm, and don't rescan on every footstep
            this.cooldown = COOLDOWN_NO_BUTTONS;
            this.noiseOrigin = null;
            return;
        }
        this.queueIndex = 0;
        this.pressTimer = 0;
        this.pendingPress = null;
        this.frenzyTicks = MathHelper.clamp(this.queue.size() * PRESS_INTERVAL * 2, MIN_FRENZY_TICKS, MAX_FRENZY_TICKS);
        this.getNavigation().stop();
        this.dataTracker.set(DATA_FRENZY, true);
        this.playSound(SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, 1.0F, 1.0F);
    }

    private void tickFrenzy() {
        this.frenzyTicks--;

        // a punch lands
        if (this.pendingPress != null && --this.pendingPressTicks <= 0) {
            this.firePress(this.pendingPress);
            this.pendingPress = null;
        }

        if (this.frenzyTicks > 0) {
            if (--this.pressTimer <= 0) {
                this.startNextPress();
                this.pressTimer = PRESS_INTERVAL;
            }
        } else if (this.pendingPress == null) { // let the last punch land before stopping
            this.endFrenzy();
        }
    }

    private void endFrenzy() {
        this.dataTracker.set(DATA_FRENZY, false);
        this.cooldown = COOLDOWN_AFTER_FRENZY;
        this.queue.clear();
        this.lookTarget = null;
        this.noiseOrigin = null;
    }

    /**
     * Scans the buttons within reach and sorts them by distance to where the noise was made: with buttons on
     * every wall, the ones on the side of the noise come first.
     */
    private void buildQueue() {
        this.queue.clear();

        World world = this.getWorld();
        Vec3d origin = this.getEyePos();
        final Vec3d reference = this.noiseOrigin != null ? this.noiseOrigin : origin;
        BlockPos center = this.getBlockPos();
        int r = MathHelper.ceil(BUTTON_REACH);
        double reachSq = BUTTON_REACH * BUTTON_REACH;

        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            if (!(world.getBlockState(pos).getBlock() instanceof ButtonBlock)) continue;
            if (Vec3d.ofCenter(pos).squaredDistanceTo(origin) > reachSq) continue;
            found.add(pos.toImmutable()); // iterate() reuses one mutable BlockPos
        }
        found.sort(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(reference)));

        this.queue.addAll(found.subList(0, Math.min(found.size(), MAX_BUTTONS)));
    }

    /** Picks the next button that is not currently pressed, turns towards it and starts an arm animation. */
    private void startNextPress() {
        int size = this.queue.size();
        World world = this.getWorld();

        for (int i = 0; i < size; i++) {
            BlockPos pos = this.queue.get(this.queueIndex);
            this.queueIndex = (this.queueIndex + 1) % size;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof ButtonBlock)) continue; // broken meanwhile
            if (state.get(ButtonBlock.POWERED)) continue;             // still pressed: try the next one

            this.pendingPress = pos;
            this.pendingPressTicks = PRESS_CONTACT_TICKS;
            this.lookTarget = Vec3d.ofCenter(pos);

            boolean right = this.nextArmRight;
            this.nextArmRight = !right;
            this.pressCounter++;
            this.dataTracker.set(DATA_PRESS, this.pressCounter * 2 + (right ? 1 : 0)); // client starts the animation
            return;
        }
        // every button is already pressed: wait for one to release
    }

    private void firePress(BlockPos pos) {
        World world = this.getWorld();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ButtonBlock button && !state.get(ButtonBlock.POWERED)) {
            button.powerOn(state, world, pos, null);
        }
    }

    // =====================================================================
    // State / client sync
    // =====================================================================

    public boolean isFrenzied() {
        return this.dataTracker.get(DATA_FRENZY);
    }

    public Filter getFilter() {
        return Filter.byOrdinal(this.dataTracker.get(DATA_FILTER));
    }

    private void setFilter(Filter filter) {
        this.dataTracker.set(DATA_FILTER, filter.ordinal());
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (!this.getWorld().isClient()) return;

        if (DATA_FRENZY.equals(data)) {
            if (this.isFrenzied()) {
                this.idleAnimationState.stop();
            } else {
                this.pressLeftAnimationState.stop();
                this.pressRightAnimationState.stop();
                this.idleAnimationState.start(this.age);
            }
        } else if (DATA_PRESS.equals(data)) {
            if ((this.dataTracker.get(DATA_PRESS) & 1) == 1) {
                this.pressRightAnimationState.start(this.age);
            } else {
                this.pressLeftAnimationState.start(this.age);
            }
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_FILTER, Filter.ALL.ordinal());
        builder.add(DATA_FRENZY, false);
        builder.add(DATA_PRESS, 0);
    }

    // =====================================================================
    // Right-click: cycle the filter
    // =====================================================================

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        // Empty hand only, so name tags, leads etc. keep working
        if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) {
            return super.interactMob(player, hand);
        }
        if (!this.getWorld().isClient()) {
            Filter selected = player.isSneaking() ? this.getFilter().previous() : this.getFilter().next();
            this.setFilter(selected);
            this.cooldown = 0;
            this.noisePending = false;
            this.playSound(SoundEvents.BLOCK_COMPARATOR_CLICK, 0.8F, 0.5F + 0.15F * selected.ordinal());
        }
        return ActionResult.SUCCESS;
    }

    // =====================================================================
    // NBT / sounds
    // =====================================================================

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Filter", this.getFilter().ordinal());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setFilter(Filter.byOrdinal(nbt.getInt("Filter")));
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ALLAY_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ALLAY_DEATH;
    }
}