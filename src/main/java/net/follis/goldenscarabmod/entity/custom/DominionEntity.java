package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.entity.ai.ApproachTargetGoal;
import net.follis.goldenscarabmod.entity.ai.SwordAttackGoal;
import net.follis.goldenscarabmod.pattern.SwordFlock;
import net.follis.goldenscarabmod.pattern.SwordPatterns;
import net.follis.goldenscarabmod.pattern.SwordPool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The Dominion: a cube with a cone of swords hanging under it, like the rods of a blaze. The swords are its ammunition
 * (see SwordPool), it can fire a beam from its bottom, crash down like a chandelier, and, when it changes phase, fly
 * with the flock and charge through its target with it.
 */
public class DominionEntity extends PathAwareEntity implements SwordPool {
    // ---- who the boss hunts and who its swords hurt ----
    /** Hostile mobs for now, for testing. Later: SwordPatterns.PLAYERS (and PlayerEntity in initGoals). */
    public static final Predicate<LivingEntity> VALID_TARGETS = e -> e instanceof HostileEntity;

    // ---- phases and boss bar, same as the Golden Swarm ----
    public static final float PHASE_2_THRESHOLD = 0.75f;
    public static final float PHASE_3_THRESHOLD = 0.25f;
    private static final String BOSS_NAME = "The Dominion";
    private static final BossBar.Color[] PHASE_BAR_COLORS = {null, BossBar.Color.YELLOW, BossBar.Color.RED, BossBar.Color.PURPLE};

    // ---- sword layout tuning (blocks, relative to the entity's feet) ----
    public static final double BODY_CENTER_Y = 3.0;     // the cube's centre: pivot y = -24 px puts it 3 blocks up
    private static final int RINGS = 4;
    private static final double RING_TOP = 0.0;         // highest ring, level with the cube's centre
    private static final double RING_BOTTOM = -2.0;     // lowest ring: the swords hang under the cube
    private static final double TOP_RADIUS = 0.85;      // the top ring is the widest...
    private static final double BOTTOM_RADIUS = 0.25;   // ...and the rings get tighter going down, into a cone
    public static final double SWORD_SPACING = 0.7;     // along a ring: sets how many swords each ring holds
    private static final double[] RING_SPEED = {-0.14, 0.08, -0.11, 0.06};   // radians per tick, alternating directions
    private static final double BOB = 0.12;

    // ---- swords as ammunition ----
    private static final int SWORD_REGEN_TICKS = 8;     // one sword comes back this often
    private static final double FLOCK_ARENA_RADIUS = 16.0;

    // ---- particles ----
    private static final DustParticleEffect GOLD_DUST = new DustParticleEffect(new Vector3f(1.0F, 0.78F, 0.15F), 1.0F);

    // ---- the beam cannon ----
    public static final int CANNON_NONE = 0, CANNON_RISE = 1, CANNON_CHARGE = 2, CANNON_FIRE = 3, CANNON_RECOVER = 4;
    public static final int CANNON_RISE_TICKS = 24;      // rising and tilting toward the target
    public static final int CANNON_CHARGE_TICKS = 30;    // swords spinning up: the warning
    public static final int CANNON_FIRE_TICKS = 24;
    public static final int CANNON_RECOVER_TICKS = 24;
    public static final int CANNON_TOTAL_TICKS = CANNON_RISE_TICKS + CANNON_CHARGE_TICKS + CANNON_FIRE_TICKS + CANNON_RECOVER_TICKS;
    private static final int CANNON_LOCK_TICKS = 10;     // the aim freezes this many ticks before the shot
    private static final double CANNON_RISE_HEIGHT = 1.5;
    private static final double CANNON_MAX_SPIN = 6.0;   // the swords turn this many times faster at the peak
    public static final double BEAM_RANGE = 40.0;
    public static final double MUZZLE_DISTANCE = 2.8;    // from the middle of the cube, along the aim: just past the tips of the swords
    private static final double BEAM_HIT_RADIUS = 0.7;
    private static final float BEAM_DAMAGE = 3.0F;       // per hit
    private static final int BEAM_HIT_INTERVAL = 4;      // ticks before the same target can be hit again

    // ---- scripted actions: while one runs the boss moves on its own and ignores its goals ----
    public static final int ACTION_NONE = 0;
    // the chandelier: it rises, bounces up and down, crashes, and its swords dive into the ground
    public static final int CHANDELIER_RISE = 1, CHANDELIER_HOVER = 2, CHANDELIER_CRASH = 3, CHANDELIER_IMPACT = 4;
    // the flock charge: it joins the flock, flies with it, charges through the target with it, flies on a little, then leaves the flock
    public static final int FLOCK_JOIN = 11, FLOCK_FLY = 12, FLOCK_CHARGE = 13, FLOCK_END = 14;

    private static final int CHANDELIER_RISE_TICKS = 24;
    private static final int CHANDELIER_BOUNCE_TICKS = 39;     // the up and down motion: three bounces, each bigger than the last
    private static final int CHANDELIER_IMPACT_TICKS = 40;     // after the crash: stunned, while the swords are in the ground
    private static final int CHANDELIER_STAB_TICKS = 4;        // from the swords leaving the body to being in the ground
    public static final int CHANDELIER_TOTAL_TICKS = CHANDELIER_RISE_TICKS + CHANDELIER_BOUNCE_TICKS + 12 + CHANDELIER_IMPACT_TICKS;
    private static final double CHANDELIER_HEIGHT = 3.0;       // how far above the ground it rises
    private static final double CHANDELIER_STOMP_RADIUS = 3.5;
    private static final float CHANDELIER_STOMP_DAMAGE = 8.0F;

    private static final int FLOCK_JOIN_TICKS = 30;
    private static final int FLOCK_FLY_TICKS = 160;            // 8 seconds with the flock
    private static final int FLOCK_DASH_TICKS = 36;        // how long the dash lasts at full speed
    private static final int FLOCK_FOLLOW_TICKS = 30;      // after the dash it slows down while the flock goes on following it, then it leaves the flock
    private static final int FLOCK_END_TICKS = 24;
    private static final int FLOCK_WAIT_TICKS = 100;       // if there is nobody to charge when the time comes, it keeps flying with the flock this long
    private static final double FLOCK_CHARGE_RANGE = 40.0; // how far it looks for targets to charge into
    private static final double FLOCK_DASH_CONE = 0.3;     // during the dash it only bends toward targets within about 70 degrees of its heading    private static final int FLOCK_END_TICKS = 24;
    private static final double FLOCK_HEIGHT = 0.5;            // the boss's feet above the floor of the arena while it flies with the flock
    private static final double FLOCK_DASH_SPEED = 0.9;        // blocks per tick
    private static final double FLOCK_DASH_TURN = 0.05;        // radians per tick: how much it corrects its course toward the target...
    private static final double FLOCK_DASH_LOCK = 8.0;         // ...until it is this close: then it goes straight, so it can be dodged
    private static final double FLOCK_CONTACT_RADIUS = 3.2;    // while it dashes, whatever its body goes through is hit
    private static final float FLOCK_CONTACT_DAMAGE = 6.0F;

    // ---- what the server tells the clients ----
    private static final TrackedData<Integer> SWORD_COUNT =
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CANNON_PHASE =
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CANNON_PHASE_AGE =      // the server clock when the phase began
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CANNON_START_AGE =      // the server clock when this cannon began, -1 when none is running
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> CANNON_SPIN_BASE =        // the extra spin of the cannons that are over
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> CANNON_AIM =           // the direction the bottom of the body points in while it is tilted
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> SERVER_CLOCK =
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ACTION_PHASE =
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ACTION_PHASE_AGE =      // the server clock when the phase began
            DataTracker.registerData(DominionEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** The boss bar shown to players who are close to the boss. Server only. */
    private final ServerBossBar bossBar;
    private boolean flockStarted;
    private int lent;   // swords that left the body and are on their way back: the body does not regrow them

    // sword layout, built once
    private int[] ringCounts = new int[0];
    private int[] slotRing = new int[0];
    private int[] slotIndex = new int[0];

    // the cannon
    @Nullable private UUID cannonTargetId;
    private double cannonBaseY;
    private final Map<UUID, Integer> beamNextHit = new HashMap<>();

    // client: the server's age minus this side's age
    private int clockOffset;
    private boolean clockSynced;

    // the scripted actions
    @Nullable private UUID actionTargetId;
    private double actionBaseY;
    private final Map<UUID, Integer> contactNextHit = new HashMap<>();
    private int lastPhase;                 // the boss phase seen last tick: 0 until the first tick
    private boolean pendingFlockCharge;    // the phase just changed and the charge has not started yet
    private Vec3d dashDir = new Vec3d(0, 0, 1);
    private double dashSpeed;
    private boolean dashPassed;            // the boss went through the target
    private int passedAge;

    public DominionEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = world.isClient ? null
                : new ServerBossBar(Text.literal(BOSS_NAME), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_20);
        this.dataTracker.set(SWORD_COUNT, getMaxSwords());   // starts with every sword
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SWORD_COUNT, 0);
        builder.add(CANNON_PHASE, CANNON_NONE);
        builder.add(CANNON_PHASE_AGE, 0);
        builder.add(CANNON_START_AGE, -1);
        builder.add(CANNON_SPIN_BASE, 0.0F);
        builder.add(CANNON_AIM, new Vector3f(0.0F, -1.0F, 0.0F));
        builder.add(SERVER_CLOCK, 0);
        builder.add(ACTION_PHASE, ACTION_NONE);
        builder.add(ACTION_PHASE_AGE, 0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // no melee: the boss only walks into range, its swords do the damage
        this.goalSelector.add(2, new SwordAttackGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(3, new ApproachTargetGoal(this, 1.0, 10.0));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // testing setup: the boss hunts hostile mobs and leaves players alone
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, HostileEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    // ---- phases ----

    public static int phaseFor(float healthFraction) {
        if (healthFraction > PHASE_2_THRESHOLD) return 1;
        return healthFraction > PHASE_3_THRESHOLD ? 2 : 3;
    }

    /** 1, 2 or 3 depending on health. Works on both sides, since health is synced. */
    public int getPhase() {
        return phaseFor(this.getHealth() / Math.max(1f, this.getMaxHealth()));
    }

    // ---- per tick ----

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            updateBossBar();
            if (age % 10 == 0) dataTracker.set(SERVER_CLOCK, age);
            tickCannon();
            tickAction();
            checkPhaseChange();
        }
    }

    @Override
    public void tickMovement() {
        if (getWorld().isClient) {
            // golden motes inside the cone of swords, in place of the blaze's smoke
            for (int i = 0; i < 2; i++) {
                double h = random.nextDouble();   // 0 = bottom of the cone, 1 = top
                double y = getY() + BODY_CENTER_Y + RING_BOTTOM + h * (RING_TOP - RING_BOTTOM);
                double radius = (BOTTOM_RADIUS + (TOP_RADIUS - BOTTOM_RADIUS) * h) * random.nextDouble();
                double angle = random.nextDouble() * Math.PI * 2.0;
                getWorld().addParticle(GOLD_DUST, getX() + Math.cos(angle) * radius, y, getZ() + Math.sin(angle) * radius,
                        0.0, -0.02, 0.0);
            }
            if (random.nextInt(6) == 0) {   // an occasional bright spark
                getWorld().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.8,
                        getY() + BODY_CENTER_Y - random.nextDouble() * 1.5, getZ() + (random.nextDouble() - 0.5) * 0.8,
                        0.0, -0.03, 0.0);
            }
        } else if (age % SWORD_REGEN_TICKS == 0 && getSwordCount() + lent < getMaxSwords()) {
            addSword();
        }
        super.tickMovement();
    }

    /** While a cannon or a scripted action runs the boss does nothing else: its goals stop, and it does not walk off. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || getCannonPhase() != CANNON_NONE || getActionPhase() != ACTION_NONE;
    }

    // ---- the clock ----

    /** The boss's age as the server counts it, with the fraction of the tick: the same number on both sides. */
    public float clock(float tickDelta) {
        return age + clockOffset + tickDelta;
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (data == SERVER_CLOCK && getWorld().isClient) {
            int offset = dataTracker.get(SERVER_CLOCK) - age;
            if (!clockSynced || Math.abs(offset - clockOffset) > 2) {
                clockOffset = offset;
                clockSynced = true;
            }
        }
    }

    // ---- boss bar ----

    private void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setPercent(MathHelper.clamp(this.getHealth() / Math.max(1f, this.getMaxHealth()), 0f, 1f));
        bossBar.setColor(PHASE_BAR_COLORS[getPhase()]);   // yellow, then red, then purple as the phases change
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar != null) bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (bossBar != null) bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        if (bossBar != null) bossBar.setName(name != null ? name : Text.literal(BOSS_NAME));
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (bossBar != null) bossBar.clearPlayers();
    }

    // ---- the swords: ammunition ----

    public int getSwordCount() { return dataTracker.get(SWORD_COUNT); }

    public int getMaxSwords() {
        ensureLayout();
        return slotRing.length;
    }

    /**
     * Uses up one sword. Returns the slot it was in, so an attack can call getSwordOffset(slot, 0)
     * and spawn its own sword right where the drawn one was, or -1 if none is left.
     */
    public int takeSword() {
        int count = getSwordCount();
        if (count <= 0) return -1;
        dataTracker.set(SWORD_COUNT, count - 1);
        return count - 1;
    }

    /** Uses up several swords at once for an attack. Returns false, and takes nothing, if there are not enough. */
    public boolean takeSwords(int n) {
        int count = getSwordCount();
        if (count < n) return false;
        dataTracker.set(SWORD_COUNT, count - n);
        return true;
    }

    /** Gives one sword back. Returns false if the body is already full. */
    public boolean addSword() {
        int count = getSwordCount();
        if (count >= getMaxSwords()) return false;
        dataTracker.set(SWORD_COUNT, count + 1);
        return true;
    }

    // ---- SwordPool: what the patterns take swords from, and give them back to ----

    @Override
    public int swordsAvailable() {
        return getSwordCount();
    }

    /** Takes the last sword off the body and returns where it was hanging, so the pattern's sword starts exactly there. */
    @Override
    @Nullable
    public Vec3d takeSwordPosition() {
        int slot = takeSword();
        return slot < 0 ? null : getPos().add(getSwordOffset(slot, 0.0F));
    }

    @Override
    public double bodyCenterHeight() {
        return BODY_CENTER_Y;
    }

    @Override
    public void lendSword() {
        lent++;
    }

    @Override
    public void forgetLent() {
        lent = Math.max(0, lent - 1);
    }

    /** The place on the body where the next sword will hang. */
    @Override
    public Vec3d returnPosition() {
        int slot = Math.min(getSwordCount(), getMaxSwords() - 1);
        return getPos().add(getSwordOffset(slot, 0.0F));
    }

    @Override
    public boolean takeBack() {
        lent = Math.max(0, lent - 1);
        return addSword();
    }

    /** The flock of swords circling the arena starts the first time the boss has a target. */
    public void ensureFlock() {
        if (flockStarted || !(getWorld() instanceof ServerWorld world)) return;
        flockStarted = true;
        SwordFlock.start(world, this, getPos(), FLOCK_ARENA_RADIUS, VALID_TARGETS);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Swords", getSwordCount());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Swords")) dataTracker.set(SWORD_COUNT, MathHelper.clamp(nbt.getInt("Swords"), 0, getMaxSwords()));
        this.setNoGravity(false);   // a boss saved in the middle of an action must not stay weightless
    }

    // ---- where the swords are ----

    /** Where a sword is, relative to the entity's feet. Used by the renderer, and by attacks that launch the sword. */
    public Vec3d getSwordOffset(int slot, float tickDelta) {
        ensureLayout();
        int r = slotRing[slot], i = slotIndex[slot];
        float t = clock(tickDelta);
        double spin = t + getCannonExtraSpin(tickDelta);   // the cannon makes the rings turn faster
        double angle = 2.0 * Math.PI * i / ringCounts[r] + r * 0.8 + spin * RING_SPEED[r % RING_SPEED.length];
        double radius = ringRadius(r);
        double ringY = BODY_CENTER_Y + RING_TOP + (RING_BOTTOM - RING_TOP) * r / (RINGS - 1);
        double y = ringY + Math.cos((i * 2 + r + t) * 0.25) * BOB;   // each sword bobs on its own, like blaze rods
        Vec3d local = new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);

        if (getBodyTiltAmount(tickDelta) <= 0.0F) return local;
        // tilted: the whole cone turns about the middle of the cube, so its tip points where the body points
        Vector3f v = new Vector3f((float) local.x, (float) (local.y - BODY_CENTER_Y), (float) local.z);
        getBodyTilt(tickDelta).transform(v);
        return new Vec3d(v.x, v.y + BODY_CENTER_Y, v.z);
    }

    private double ringRadius(int ring) {
        return TOP_RADIUS + (BOTTOM_RADIUS - TOP_RADIUS) * ring / (RINGS - 1);
    }

    /** Builds the rings once. Slots are numbered round-robin across the rings, so a missing sword is never one whole ring. */
    private void ensureLayout() {
        if (slotRing.length > 0) return;

        ringCounts = new int[RINGS];
        int total = 0, widest = 0;
        for (int r = 0; r < RINGS; r++) {
            ringCounts[r] = Math.max(3, (int) Math.round(2.0 * Math.PI * ringRadius(r) / SWORD_SPACING));
            total += ringCounts[r];
            widest = Math.max(widest, ringCounts[r]);
        }

        slotRing = new int[total];
        slotIndex = new int[total];
        int slot = 0;
        for (int i = 0; i < widest; i++) {
            for (int r = 0; r < RINGS; r++) {
                if (i < ringCounts[r]) {
                    slotRing[slot] = r;
                    slotIndex[slot] = i;
                    slot++;
                }
            }
        }
    }

    // ---- the tilt of the body: the cannon and the flight ----

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** How far the body is tilted, whatever tilts it: the cannon or the flight. */
    public float getBodyTiltAmount(float tickDelta) {
        return Math.max(getCannonTilt(tickDelta), getFlightTilt(tickDelta));
    }

    /** The turn that takes "straight down" to the aim of the body, in proportion to the tilt. Used for the cube and for the swords. */
    public Quaternionf getBodyTilt(float tickDelta) {
        float tilt = getBodyTiltAmount(tickDelta);
        Quaternionf q = new Quaternionf();
        if (tilt <= 0.0F) return q;
        Quaternionf full = new Quaternionf().rotationTo(new Vector3f(0.0F, -1.0F, 0.0F), new Vector3f(dataTracker.get(CANNON_AIM)));
        return q.slerp(full, tilt);
    }

    /** The direction every sword on the body points in: straight down, or along the barrel while the body is tilted. */
    public Vec3d getSwordAim(float tickDelta) {
        Vector3f v = new Vector3f(0.0F, -1.0F, 0.0F);
        getBodyTilt(tickDelta).transform(v);
        return new Vec3d(v.x, v.y, v.z);
    }

    /** Turns the aim of the body, which the tilt follows, a share of the way toward a direction. */
    private void steerBodyAim(Vec3d wanted, double share) {
        if (wanted.lengthSquared() < 1.0E-6) return;
        Vec3d next = getCannonAim().lerp(wanted.normalize(), share);
        if (next.lengthSquared() < 1.0E-6) return;
        dataTracker.set(CANNON_AIM, next.normalize().toVector3f());
    }

    // ---- the beam cannon ----

    public int getCannonPhase() {
        return dataTracker.get(CANNON_PHASE);
    }

    private void setCannonPhase(int phase) {
        dataTracker.set(CANNON_PHASE, phase);
        dataTracker.set(CANNON_PHASE_AGE, age);
    }

    /** Ticks since the current phase began, with the fraction of the tick. */
    public float cannonPhaseTime(float tickDelta) {
        return clock(tickDelta) - dataTracker.get(CANNON_PHASE_AGE);
    }

    public Vec3d getCannonAim() {
        Vector3f v = dataTracker.get(CANNON_AIM);
        return new Vec3d(v.x(), v.y(), v.z());
    }

    /** 0 = upright, 1 = fully tilted at the target. */
    public float getCannonTilt(float tickDelta) {
        float time = cannonPhaseTime(tickDelta);
        return switch (getCannonPhase()) {
            case CANNON_RISE -> smooth(time / CANNON_RISE_TICKS);
            case CANNON_CHARGE, CANNON_FIRE -> 1.0F;
            case CANNON_RECOVER -> 1.0F - smooth(time / CANNON_RECOVER_TICKS);
            default -> 0.0F;
        };
    }

    /** 0 to 1: the beam grows in over the first ticks of the shot and shrinks away at the end. */
    public float getBeamStrength(float tickDelta) {
        if (getCannonPhase() != CANNON_FIRE) return 0.0F;
        float time = cannonPhaseTime(tickDelta);
        return Math.min(MathHelper.clamp(time / 3.0F, 0.0F, 1.0F), MathHelper.clamp((CANNON_FIRE_TICKS - time) / 4.0F, 0.0F, 1.0F));
    }

    /** Where the beam leaves: past the tips of the swords, along the aim. */
    public Vec3d cannonMuzzle(Vec3d feet, Vec3d aim) {
        return feet.add(0.0, BODY_CENTER_Y, 0.0).add(aim.multiply(MUZZLE_DISTANCE));
    }

    /** How far the beam goes from start along aim before it hits a block. */
    public double beamLength(Vec3d start, Vec3d aim) {
        Vec3d end = start.add(aim.multiply(BEAM_RANGE));
        BlockHitResult hit = getWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? BEAM_RANGE : hit.getPos().distanceTo(start);
    }

    /**
     * How many extra ticks of turning the swords have gained from the cannons so far, in ticks of normal speed.
     * Worked out from the clock, so the server and the client agree without sending anything every tick.
     */
    public double getCannonExtraSpin(float tickDelta) {
        double base = dataTracker.get(CANNON_SPIN_BASE);
        int start = dataTracker.get(CANNON_START_AGE);
        if (start < 0) return base;
        return base + spinExtra(clock(tickDelta) - start);
    }

    /** The speed-up is 1x until the charge, then rises to CANNON_MAX_SPIN, holds through the shot and falls back. This is its integral. */
    private static double spinExtra(double t) {
        double extra = CANNON_MAX_SPIN - 1.0;
        if (t <= CANNON_RISE_TICKS) return 0.0;
        double t1 = t - CANNON_RISE_TICKS;
        double charge = CANNON_CHARGE_TICKS;
        if (t1 <= charge) {
            double p = t1 / charge;
            return extra * charge * p * p / 2.0;
        }
        double base1 = extra * charge / 2.0;
        double t2 = t1 - charge;
        if (t2 <= CANNON_FIRE_TICKS) return base1 + extra * t2;
        double base2 = base1 + extra * CANNON_FIRE_TICKS;
        double t3 = t2 - CANNON_FIRE_TICKS;
        double recover = CANNON_RECOVER_TICKS;
        if (t3 <= recover) {
            double p = t3 / recover;
            return base2 + extra * recover * (p - p * p / 2.0);
        }
        return base2 + extra * recover / 2.0;
    }

    /** Starts the cannon against this target. Returns how many ticks it lasts, or 0 if it cannot start. */
    public int startCannon(LivingEntity target) {
        if (getWorld().isClient || getCannonPhase() != CANNON_NONE || getActionPhase() != ACTION_NONE) return 0;
        cannonTargetId = target.getUuid();
        cannonBaseY = getY();
        beamNextHit.clear();
        dataTracker.set(CANNON_START_AGE, age);
        dataTracker.set(CANNON_AIM, aimAt(target).toVector3f());
        setCannonPhase(CANNON_RISE);
        setNoGravity(true);
        getNavigation().stop();
        return CANNON_TOTAL_TICKS;
    }

    private Vec3d aimAt(LivingEntity target) {
        Vec3d to = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(getPos().add(0.0, BODY_CENTER_Y, 0.0));
        return to.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, -1.0, 0.0) : to.normalize();
    }

    /** Turns the aim a share of the way toward the target. */
    private void steerCannonAim(@Nullable LivingEntity target, double share) {
        if (target == null) return;
        steerBodyAim(aimAt(target), share);
    }

    private void holdHeight(double wantedY) {
        Vec3d v = getVelocity();
        setVelocity(v.x * 0.5, MathHelper.clamp((wantedY - getY()) * 0.25, -0.25, 0.25), v.z * 0.5);
    }

    private void tickCannon() {
        int phase = getCannonPhase();
        if (phase == CANNON_NONE || !(getWorld() instanceof ServerWorld world)) return;

        int t = age - dataTracker.get(CANNON_PHASE_AGE);   // ticks into this phase
        LivingEntity target = null;
        if (cannonTargetId != null && world.getEntity(cannonTargetId) instanceof LivingEntity living && living.isAlive()) {
            target = living;
        }
        if (phase != CANNON_RECOVER) holdHeight(cannonBaseY + CANNON_RISE_HEIGHT);

        switch (phase) {
            case CANNON_RISE -> {
                steerCannonAim(target, 0.3);
                if (t >= CANNON_RISE_TICKS) {
                    setCannonPhase(CANNON_CHARGE);
                    world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                            SoundCategory.HOSTILE, 3.0F, 1.4F);
                }
            }
            case CANNON_CHARGE -> {
                if (t < CANNON_CHARGE_TICKS - CANNON_LOCK_TICKS) steerCannonAim(target, 0.2);   // then the aim is frozen: time to dodge
                Vec3d muzzle = cannonMuzzle(getPos(), getCannonAim());
                if (t % 5 == 0) {
                    world.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.BLOCK_BEACON_POWER_SELECT,
                            SoundCategory.HOSTILE, 2.0F, 0.8F + 1.2F * t / CANNON_CHARGE_TICKS);
                }
                world.spawnParticles(ParticleTypes.END_ROD, muzzle.x, muzzle.y, muzzle.z, 2, 0.3, 0.3, 0.3, 0.05);
                if (t >= CANNON_CHARGE_TICKS) {
                    setCannonPhase(CANNON_FIRE);
                    world.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                            SoundCategory.HOSTILE, 4.0F, 1.0F);
                }
            }
            case CANNON_FIRE -> {
                fireBeam(world);
                if (t >= CANNON_FIRE_TICKS) {
                    setCannonPhase(CANNON_RECOVER);
                    setNoGravity(false);   // it sinks back to the ground
                }
            }
            case CANNON_RECOVER -> {
                if (t >= CANNON_RECOVER_TICKS) endCannon();
            }
            default -> {}
        }
    }

    private void fireBeam(ServerWorld world) {
        Vec3d aim = getCannonAim();
        Vec3d start = cannonMuzzle(getPos(), aim);
        double length = beamLength(start, aim);
        Vec3d end = start.add(aim.multiply(length));

        Box sweep = new Box(start, end).expand(BEAM_HIT_RADIUS + 0.5);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, sweep,
                e -> e != this && e.isAlive() && !e.isSpectator() && VALID_TARGETS.test(e))) {
            Box hitbox = e.getBoundingBox().expand(BEAM_HIT_RADIUS);
            if (!hitbox.contains(start) && hitbox.raycast(start, end).isEmpty()) continue;
            if (age < beamNextHit.getOrDefault(e.getUuid(), 0)) continue;

            beamNextHit.put(e.getUuid(), age + BEAM_HIT_INTERVAL);
            e.timeUntilRegen = 0;
            e.damage(getDamageSources().indirectMagic(this, this), BEAM_DAMAGE);
        }

        if (age % 2 == 0) {
            for (double d = 1.0; d < length; d += 2.0) {
                Vec3d p = start.add(aim.multiply(d));
                world.spawnParticles(GOLD_DUST, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.0);
            }
        }
        world.spawnParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 6, 0.3, 0.3, 0.3, 0.15);
    }

    private void endCannon() {
        dataTracker.set(CANNON_SPIN_BASE, dataTracker.get(CANNON_SPIN_BASE) + (float) spinExtra(CANNON_TOTAL_TICKS));
        dataTracker.set(CANNON_START_AGE, -1);
        dataTracker.set(CANNON_PHASE, CANNON_NONE);
        setNoGravity(false);
        cannonTargetId = null;
    }

    // ---- scripted actions: the chandelier and the flock charge ----

    public int getActionPhase() {
        return dataTracker.get(ACTION_PHASE);
    }

    private void setActionPhase(int phase) {
        dataTracker.set(ACTION_PHASE, phase);
        dataTracker.set(ACTION_PHASE_AGE, age);
    }

    /** Ticks since the current phase began, with the fraction of the tick. */
    public float actionPhaseTime(float tickDelta) {
        return clock(tickDelta) - dataTracker.get(ACTION_PHASE_AGE);
    }

    /** The flock charge tilts the body as the cannon does: its bottom leads, in the direction the boss flies. */
    public float getFlightTilt(float tickDelta) {
        float time = actionPhaseTime(tickDelta);
        return switch (getActionPhase()) {
            case FLOCK_JOIN -> smooth(time / FLOCK_JOIN_TICKS);
            case FLOCK_FLY, FLOCK_CHARGE -> 1.0F;
            case FLOCK_END -> 1.0F - smooth(time / FLOCK_END_TICKS);
            default -> 0.0F;
        };
    }

    private boolean canStartAction() {
        return getCannonPhase() == CANNON_NONE && getActionPhase() == ACTION_NONE;
    }

    /** Starts the chandelier. Returns how many ticks it lasts, or 0 if it cannot start. */
    public int startChandelier() {
        if (getWorld().isClient || !canStartAction() || !isOnGround()) return 0;
        actionBaseY = getY();
        setNoGravity(true);
        getNavigation().stop();
        setActionPhase(CHANDELIER_RISE);
        return CHANDELIER_TOTAL_TICKS;
    }

    /** The transition for a change of phase: joins the flock, flies with it, charges through the target with it, leaves it. */
    /** The transition for a change of phase: joins the flock, flies with it, charges through whatever targets there are, leaves it. */
    private boolean startFlockCharge() {
        if (!canStartAction()) return false;
        ensureFlock();
        SwordFlock.fillUp(this);   // the whole flock takes part, not only the swords that are out already
        contactNextHit.clear();
        setNoGravity(true);
        getNavigation().stop();
        setActionPhase(FLOCK_JOIN);
        return true;
    }

    /** When the boss drops into a new phase the flock charge is due: it starts as soon as the boss is in a fight and is free. */
    private void checkPhaseChange() {
        int phase = getPhase();
        if (lastPhase != 0 && phase > lastPhase) pendingFlockCharge = true;
        lastPhase = phase;

        if (pendingFlockCharge && !isDead()) {
            LivingEntity target = getTarget();   // only to know the boss is in a fight: the charge picks its own targets when it happens
            if (target != null && target.isAlive() && startFlockCharge()) pendingFlockCharge = false;
        }
    }

    /** The height above the start during the elastic bounce: three bounces, each bigger than the last, ending at the top of the highest. */
    private static double bounceHeight(int t) {
        double progress = t / (double) CHANDELIER_BOUNCE_TICKS;
        double amplitude = 0.25 + 0.65 * progress;
        return CHANDELIER_HEIGHT + amplitude * Math.sin(2.0 * Math.PI * t / 12.0);
    }

    private void tickAction() {
        int phase = getActionPhase();
        if (phase == ACTION_NONE || !(getWorld() instanceof ServerWorld world)) return;

        int t = age - dataTracker.get(ACTION_PHASE_AGE);   // ticks into this phase

        switch (phase) {
            case CHANDELIER_RISE -> {
                driveHeight(actionBaseY + CHANDELIER_HEIGHT * smooth((t + 1.0F) / CHANDELIER_RISE_TICKS));
                if (t % 6 == 0) {
                    world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                            SoundCategory.HOSTILE, 2.0F, 0.6F);
                }
                if (t >= CHANDELIER_RISE_TICKS) setActionPhase(CHANDELIER_HOVER);
            }
            case CHANDELIER_HOVER -> {
                driveHeight(actionBaseY + bounceHeight(t + 1));   // the elastic motion: it bounces up and down
                if (t % 6 == 3) {   // at the top and at the bottom of each bounce, a note that goes higher every time
                    world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                            SoundCategory.HOSTILE, 2.0F, 0.6F + 1.2F * t / CHANDELIER_BOUNCE_TICKS);
                }
                if (t % 4 == 0) markGround(world);   // the ring on the ground shows where it will land
                if (t >= CHANDELIER_BOUNCE_TICKS) {
                    setActionPhase(CHANDELIER_CRASH);
                    world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                            SoundCategory.HOSTILE, 3.0F, 0.7F);
                }
            }
            case CHANDELIER_CRASH -> {
                setVelocity(0.0, -Math.min(2.2, 0.5 + 0.25 * t), 0.0);   // it falls faster and faster
                if (isOnGround() || t > 40) chandelierImpact(world);
            }
            case CHANDELIER_IMPACT -> {
                if (t == CHANDELIER_STAB_TICKS) chandelierStab(world);   // the swords are in the ground: this is the moment of the impact
                if (t >= CHANDELIER_IMPACT_TICKS) endAction();
            }

            case FLOCK_JOIN -> {
                if (!flyWithFlock(0.7)) {
                    endAction();
                    return;
                }
                if (t >= FLOCK_JOIN_TICKS) setActionPhase(FLOCK_FLY);
            }
            case FLOCK_FLY -> {
                if (!flyWithFlock(0.45)) {
                    endAction();
                    return;
                }
                if (t >= FLOCK_FLY_TICKS) {
                    // the charge: whoever is there now, in any direction. Nobody yet: it keeps flying with the flock for a while
                    Vec3d body = getPos().add(0.0, BODY_CENTER_Y, 0.0);
                    LivingEntity first = findChargeTarget(world, body, dashDir, -1.0);
                    if (first != null) {
                        beginDash(body, first);
                        SwordFlock.follow(this, FLOCK_DASH_TICKS + FLOCK_FOLLOW_TICKS + 40);   // the whole flock follows the boss from now on
                        setActionPhase(FLOCK_CHARGE);
                        world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                                SoundCategory.HOSTILE, 3.0F, 0.6F);
                    } else if (t >= FLOCK_FLY_TICKS + FLOCK_WAIT_TICKS) {
                        startFlockEnd();   // nobody came: it leaves the flock without charging
                    }
                }
            }
            case FLOCK_CHARGE -> {
                dashThrough(world, t);
                if (t >= FLOCK_DASH_TICKS + FLOCK_FOLLOW_TICKS) quitFlock(world);
            }
            case FLOCK_END -> {
                if (t >= FLOCK_END_TICKS) endAction();
            }
            default -> {}
        }
    }
    private void endAction() {
        setActionPhase(ACTION_NONE);
        setNoGravity(false);
    }

    private void startFlockEnd() {
        setVelocity(Vec3d.ZERO);
        setNoGravity(false);   // it sinks back to the ground while the body straightens
        setActionPhase(FLOCK_END);
    }

    /** A ring of sparks on the ground, where the boss will land. */
    private void markGround(ServerWorld world) {
        for (int i = 0; i < 16; i++) {
            double a = 2.0 * Math.PI * i / 16;
            world.spawnParticles(ParticleTypes.CRIT, getX() + Math.cos(a) * CHANDELIER_STOMP_RADIUS, actionBaseY + 0.1,
                    getZ() + Math.sin(a) * CHANDELIER_STOMP_RADIUS, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * The boss lands: the swords of its body leave it and dive into the ground around it, and the wave of ground swords is
     * set so that its first ring comes up at the very moment they are in the ground.
     */
    private void chandelierImpact(ServerWorld world) {
        setNoGravity(false);
        setVelocity(Vec3d.ZERO);
        setActionPhase(CHANDELIER_IMPACT);

        int stabIn = SwordPatterns.stabIntoGround(world, this, this, VALID_TARGETS, CHANDELIER_STAB_TICKS);
        SwordPatterns.groundWave(world, this, getPos(), VALID_TARGETS, stabIn);
    }

    /** The swords are in the ground: what is below is hit, and the impact is heard and seen. */
    private void chandelierStab(ServerWorld world) {
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(CHANDELIER_STOMP_RADIUS),
                e -> e != this && e.isAlive() && !e.isSpectator() && VALID_TARGETS.test(e))) {
            if (distanceTo(e) > CHANDELIER_STOMP_RADIUS) continue;
            e.timeUntilRegen = 0;
            e.damage(getDamageSources().indirectMagic(this, this), CHANDELIER_STOMP_DAMAGE);
            e.takeKnockback(1.2, getX() - e.getX(), getZ() - e.getZ());
        }
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 3.0F, 0.5F);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 3.0F, 0.6F);
        var floor = new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(getBlockPos().down()));
        for (int i = 0; i < 32; i++) {
            double a = 2.0 * Math.PI * i / 32;
            world.spawnParticles(floor, getX() + Math.cos(a) * 1.5, getY() + 0.2, getZ() + Math.sin(a) * 1.5, 3, 0.2, 0.1, 0.2, 0.15);
        }
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 20, 1.0, 0.3, 1.0, 0.15);
    }

    /** Moves the boss straight to this height next tick (within a limit): exact, so it can follow a curve. */
    private void driveHeight(double wantedY) {
        Vec3d v = getVelocity();
        setVelocity(v.x * 0.5, MathHelper.clamp(wantedY - getY(), -0.9, 0.9), v.z * 0.5);
    }

    private void flyToward(Vec3d want, double maxSpeed) {
        Vec3d v = want.subtract(getPos()).multiply(0.15);   // slows down as it gets there
        if (v.length() > maxSpeed) v = v.normalize().multiply(maxSpeed);
        setVelocity(v);
    }

    /** Turns a unit vector toward another by no more than maxAngle radians. */
    private static Vec3d turnToward(Vec3d current, Vec3d desired, double maxAngle) {
        double angle = Math.acos(MathHelper.clamp(current.dotProduct(desired), -1.0, 1.0));
        if (angle < 1.0E-4) return current;
        double k = Math.min(1.0, maxAngle / angle);
        Vec3d blended = current.multiply(1.0 - k).add(desired.multiply(k));
        return blended.lengthSquared() < 1.0E-6 ? current : blended.normalize();
    }

    /**
     * Flies as one of the flock: at the flock's own place on its circle and at its height, at its pace, with the bottom of
     * the body leading. False if there is no flock.
     */
    private boolean flyWithFlock(double maxSpeed) {
        Vec3d center = SwordFlock.circleCenter(this);
        if (center == null) return false;
        double radius = SwordFlock.circleRadius(this);
        double angle = SwordFlock.flockAngle(this);   // where the swords of the flock are, around the centre of the circle
        if (Double.isNaN(angle)) angle = Math.atan2(getZ() - center.z, getX() - center.x);
        double flockY = SwordFlock.flockHeight(this);
        double feetY = Double.isNaN(flockY) ? center.y + FLOCK_HEIGHT : Math.max(center.y + FLOCK_HEIGHT, flockY - 2.5);

        flyToward(new Vec3d(center.x + Math.cos(angle) * radius, feetY, center.z + Math.sin(angle) * radius), maxSpeed);
        Vec3d v = getVelocity();
        if (v.x * v.x + v.z * v.z > 0.0025) steerBodyAim(new Vec3d(v.x, 0.0, v.z), 0.2);   // the body lies level along the way it flies
        return true;
    }

    /**
     * The best target to charge into from here: the nearest foe that is in front of the heading, and the straighter ahead
     * the better. minFacing is the cosine of the widest angle allowed: 0.3 is about 70 degrees, -1 means any direction.
     */
    /**
     * The best target to charge into from here: the nearest foe that is in front of the heading, and the straighter ahead
     * the better. Only the horizontal side counts, since the dash is level. minFacing is the cosine of the widest angle
     * allowed: 0.3 is about 70 degrees, -1 means any direction. heading must be a horizontal unit vector.
     */
    @Nullable
    private LivingEntity findChargeTarget(ServerWorld world, Vec3d body, Vec3d heading, double minFacing) {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(body, body).expand(FLOCK_CHARGE_RANGE),
                e -> e != this && e.isAlive() && !e.isSpectator() && VALID_TARGETS.test(e))) {
            double dx = e.getX() - body.x, dz = e.getZ() - body.z;
            double distance = Math.hypot(dx, dz);
            if (distance < 1.0E-3 || distance > FLOCK_CHARGE_RANGE) continue;

            double facing = (dx * heading.x + dz * heading.z) / distance;   // 1 = straight ahead
            if (facing < minFacing) continue;
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private void beginDash(Vec3d body, LivingEntity first) {
        Vec3d to = new Vec3d(first.getX() - body.x, 0.0, first.getZ() - body.z);   // level: the dash does not dive or climb
        dashDir = to.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : to.normalize();
        dashSpeed = 0.3;   // it sets off at about the pace of the flock
    }

    /**
     * The dash: level, a fixed length of time at full speed, through whatever is in the way, then it slows down while the flock
     * goes on following it. It bends toward the best target in front of it, whoever that is; it never stops for a target.
     * The body lies along the dash, and only its height follows the target, gently, so the body passes through it.
     */
    private void dashThrough(ServerWorld world, int t) {
        Vec3d body = getPos().add(0.0, BODY_CENTER_Y, 0.0);
        double wantedY = getY();   // it keeps its height unless there is a target to fly level with

        if (t < FLOCK_DASH_TICKS) {
            LivingEntity aimAt = findChargeTarget(world, body, dashDir, FLOCK_DASH_CONE);
            if (aimAt != null) {
                Vec3d to = new Vec3d(aimAt.getX() - body.x, 0.0, aimAt.getZ() - body.z);
                double distance = to.length();
                // a little homing; closer than the lock distance it goes straight, which is the last moment to step aside
                if (distance > FLOCK_DASH_LOCK) dashDir = turnToward(dashDir, to.multiply(1.0 / distance), FLOCK_DASH_TURN);
                wantedY = aimAt.getY();
            }
            dashSpeed = Math.min(FLOCK_DASH_SPEED, dashSpeed + 0.04);
        } else {
            dashSpeed *= 0.93;   // the dash is over: it slows down, and the flock still follows
        }
        setVelocity(dashDir.x * dashSpeed, MathHelper.clamp((wantedY - getY()) * 0.25, -0.5, 0.5), dashDir.z * dashSpeed);
        steerBodyAim(dashDir, 0.3);   // dashDir is level, so the body lies flat along it

        // whatever the body goes through is hit, whoever it is
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(body, body).expand(FLOCK_CONTACT_RADIUS),
                e -> e != this && e.isAlive() && !e.isSpectator() && VALID_TARGETS.test(e))) {
            if (e.getPos().add(0.0, e.getHeight() * 0.5, 0.0).distanceTo(body) > FLOCK_CONTACT_RADIUS) continue;
            if (age < contactNextHit.getOrDefault(e.getUuid(), 0)) continue;
            contactNextHit.put(e.getUuid(), age + 10);
            e.timeUntilRegen = 0;
            e.damage(getDamageSources().indirectMagic(this, this), FLOCK_CONTACT_DAMAGE);
        }
    }
    /** The boss leaves the flock: the swords go back to circling and the boss lands. */
    private void quitFlock(ServerWorld world) {
        SwordFlock.release(this);
        world.playSound(null, getX(), getY() + BODY_CENTER_Y, getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.HOSTILE, 2.5F, 1.0F);
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + BODY_CENTER_Y, getZ(), 24, 1.0, 1.0, 1.0, 0.2);
        startFlockEnd();
    }

    // ---- rendering helpers ----

    /** The swords and the beam reach outside the hitbox: without this the boss would vanish when only its hitbox is off screen. */
    @Override
    public Box getVisibilityBoundingBox() {
        Box box = super.getVisibilityBoundingBox().expand(TOP_RADIUS + 2.0, 4.0, TOP_RADIUS + 2.0);
        return getCannonPhase() == CANNON_NONE ? box : box.expand(BEAM_RANGE);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}