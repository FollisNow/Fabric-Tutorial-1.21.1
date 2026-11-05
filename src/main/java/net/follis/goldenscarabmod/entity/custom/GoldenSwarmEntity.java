package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.entity.ai.ApproachTargetGoal;
import net.follis.goldenscarabmod.entity.ai.ArmSlamGoal;
import net.follis.goldenscarabmod.entity.ai.BurstAttackGoal;
import net.follis.goldenscarabmod.entity.ai.CloneSummonGoal;
import net.follis.goldenscarabmod.entity.ai.ShapeAttackGoal;
import net.follis.goldenscarabmod.entity.ai.TentacleGoal;
import net.follis.goldenscarabmod.entity.ai.TranslocateAttackGoal;
import net.follis.goldenscarabmod.entity.swarm.SwarmState;
import net.follis.goldenscarabmod.entity.swarm.TentacleFormation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GoldenSwarmEntity extends PathAwareEntity {

    public static final int PATTERN_NONE = 0;
    public static final int PATTERN_SHAPE_THROW = 1;
    public static final int PATTERN_BURST = 2;
    public static final int PATTERN_TRANSLOCATE = 3;
    public static final int PATTERN_CLONE = 4;
    public static final int PATTERN_ARM_SLAM = 5;
    public static final int PATTERN_TENTACLES = 6;
    /** Played by a clone while it merges back into the boss. Never picked by the scheduler. */
    public static final int PATTERN_RECOMBINE = 7;
    /** Number of attack patterns the boss picks from (ids 1..ATTACK_PATTERNS). Bump when adding one. */
    private static final int ATTACK_PATTERNS = 6;

    // ---- boss phases, from its health ---------------------------------------------------------------
    //   phase 1: health above 75%      phase 2: 25% to 75%      phase 3: 25% or less
    public static final float PHASE_2_THRESHOLD = 0.75f;
    public static final float PHASE_3_THRESHOLD = 0.25f;

    /**
     * Relative odds of each attack in each phase. First index = phase (1..3, row 0 unused), second index = the pattern
     * id: {unused, SHAPE_THROW, BURST, TRANSLOCATE, CLONE, ARM_SLAM, TENTACLES}. Only the attacks that can start right now (in range, in
     * sight...) take part in the draw, so the odds are shared out among whatever is usable at that moment.
     * A weight of 0 disables that attack in that phase.
     *
     * Phase 1: no burst. Phase 2: everything (and the shape attack fires three shapes, see getShapeLegs).
     * Phase 3: burst and relocation are heavily favoured (76% of the draws when both can start), no more clones.
     */
    private static final int[][] PHASE_WEIGHTS = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 40, 0, 20, 20, 15, 15},
            {0, 40, 20, 20, 20, 20, 20},
            {0, 25, 40, 40, 0, 20, 25},
    };

    /**
     * Per phase: an attack that is cast as soon as it can start, before any draw (0 = none). Above 75% that is the
     * clone. For a strict "whenever it can" in the last phase, put PATTERN_BURST or PATTERN_TRANSLOCATE in the
     * last slot (the boss then does nothing else while that attack is usable).
     */
    private static final int[] PHASE_PRIORITY = {0, PATTERN_CLONE, 0, 0};

    // the alert played when the boss drops into a new phase: a row of chain-break sounds (plus a sheet flurry on the clients)
    public static final int ALERT_SOUNDS = 5;
    public static final int ALERT_INTERVAL = 4;      // ticks between two sounds
    private static final float ALERT_VOLUME = 3.0f;

    // The shockwave of a phase change: with each chain sound everything near the boss is thrown back,
    // hard at first and less and less (the clients draw a ring of markers expanding with each sound).
    public static final double REPEL_RADIUS = 8.0;
    private static final float[] REPEL_STRENGTH = {1.6f, 1.3f, 1.0f, 0.8f, 0.6f};

    // the boss bar: notched every 5%, so there is a mark at exactly 75% and 25% where the phases change
    private static final String BOSS_NAME = "The Golden Swarm";
    private static final BossBar.Color[] PHASE_BAR_COLORS = {null, BossBar.Color.YELLOW, BossBar.Color.RED, BossBar.Color.PURPLE};

    // When each attack may start: distance to the target, in blocks. (The shape throw's range was 3-16 in its goal.)
    public static final double SHAPE_MIN_DIST = 3.0, SHAPE_MAX_DIST = 16.0;
    public static final double BURST_MAX_DIST = 8.0;                       // the burst only starts when the target is inside the blast
    public static final double TRANSLOCATE_MIN_DIST = 3.0, TRANSLOCATE_MAX_DIST = 18.0;
    public static final double CLONE_MAX_DIST = 20.0;
    public static final double ARM_MIN_DIST = 2.5, ARM_MAX_DIST = 13.0;   // the arm's reach is 13 blocks
    public static final double TENTACLE_REACH = TentacleFormation.REACH;  // any foe this close (not only the target) is enough

    // ---- clone settings -------------------------------------------------------------------------
    public static final float CLONE_SCALE = 0.6f;               // the clone is this much smaller than the boss
    private static final float CLONE_HEALTH_FRACTION = 0.25f;   // of the boss' max health
    private static final int CLONE_LIFETIME_TICKS = 400;        // 20 seconds, then it recombines
    private static final float CLONE_DAMAGE_MULTIPLIER = 0.5f; // its attacks hurt half as much...
    private static final float CLONE_COOLDOWN_MULTIPLIER = 0.5f; // ...but come twice as often
    private static final int RECOMBINE_TICKS = 26;              // how long the merge animation lasts
    private static final int RECOMBINE_RELEASE = 14;            // when the boss gets its sheets back

    // What the server tells the clients so they can play the matching visuals.
    // The sheets themselves are never networked, only this tiny state.
    private static final TrackedData<Integer> PATTERN_ID =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PATTERN_SEQ =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PATTERN_SHAPE =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> PATTERN_TARGET =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> PATTERN_FLIGHT =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> PATTERN_BREAKS =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PATTERN_VARIANT =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // clone state, also needed by the clients (size of the body, share of the swarm, where the sheets come from)
    private static final TrackedData<Boolean> IS_CLONE =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> SHEET_SHARE =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Vector3f> CLONE_ORIGIN =
            DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.VECTOR3F);

    // The tentacles each live their own life, so each has its own pair: where it is lashing, and a counter that goes up
    // once per lash. The clients start a tentacle's windup when they see its counter change.
    private static final List<TrackedData<Vector3f>> TENTACLE_AIM = registerTentacleAims();
    private static final List<TrackedData<Integer>> TENTACLE_SEQ = registerTentacleSeqs();

    private static List<TrackedData<Vector3f>> registerTentacleAims() {
        List<TrackedData<Vector3f>> list = new ArrayList<>();
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {
            list.add(DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.VECTOR3F));
        }
        return list;
    }

    private static List<TrackedData<Integer>> registerTentacleSeqs() {
        List<TrackedData<Integer>> list = new ArrayList<>();
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {
            list.add(DataTracker.registerData(GoldenSwarmEntity.class, TrackedDataHandlerRegistry.INTEGER));
        }
        return list;
    }

    /**
     * Visual-only swarm simulation. Exists on the CLIENT only (null on the server):
     * the server never knows about individual sheets, it only runs the boss logic.
     */
    public final SwarmState swarm;

    /** The boss bar shown to players who are close to the boss. Server only; the clone has none. */
    private final ServerBossBar bossBar;

    // server only
    private UUID ownerUuid;     // clone: the boss it came from
    private UUID cloneUuid;     // boss: its current clone
    private int cloneAge;
    private int recombineTicks; // > 0 while the clone is merging back

    // Must be public: ModEntities lives in another package and uses GoldenSwarmEntity::new
    public GoldenSwarmEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.swarm = world.isClient ? new SwarmState(this.random.nextLong()) : null;
        this.bossBar = world.isClient ? null
                : new ServerBossBar(Text.literal(BOSS_NAME), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_20);
        planNextPattern();
    }

    public static DefaultAttributeContainer.Builder createGoldenSwarmAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PATTERN_ID, PATTERN_NONE);
        builder.add(PATTERN_SEQ, 0);
        builder.add(PATTERN_SHAPE, 0);
        builder.add(PATTERN_TARGET, new Vector3f());
        builder.add(PATTERN_FLIGHT, 12);
        builder.add(PATTERN_BREAKS, false);
        builder.add(PATTERN_VARIANT, 0);
        builder.add(IS_CLONE, false);
        builder.add(SHEET_SHARE, 1.0f);
        builder.add(CLONE_ORIGIN, new Vector3f());
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {
            builder.add(TENTACLE_AIM.get(k), new Vector3f());
            builder.add(TENTACLE_SEQ.get(k), 0);
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        // Attacks (they take over movement and looking while they run)
        this.goalSelector.add(2, new ShapeAttackGoal(this));
        this.goalSelector.add(2, new BurstAttackGoal(this));
        this.goalSelector.add(2, new TranslocateAttackGoal(this));
        this.goalSelector.add(2, new CloneSummonGoal(this));
        this.goalSelector.add(2, new ArmSlamGoal(this));
        this.goalSelector.add(2, new TentacleGoal(this));
        // No melee: the boss only walks into range, its attacks are the sheet patterns
        this.goalSelector.add(4, new ApproachTargetGoal(this, 1.0, 7.0));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        // Testing setup: the boss hunts hostile mobs and leaves players alone.
        // To make it a real boss later, target PlayerEntity here and in isFoe().
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, HostileEntity.class, true));
    }

    /** Who the boss' attacks are allowed to hurt. */
    public boolean isFoe(LivingEntity other) {
        return other instanceof HostileEntity;
    }

    // ------------------------------------------------------------------------- clone: size and stats

    public boolean isClone() {
        return this.dataTracker.get(IS_CLONE);
    }

    /** Share of the whole swarm this body owns: 1 normally, 0.5 for the boss and its clone while both exist. */
    public float getSheetShare() {
        return this.dataTracker.get(SHEET_SHARE);
    }

    private void setSheetShare(float share) {
        this.dataTracker.set(SHEET_SHARE, share);
    }

    /** Where a freshly summoned clone's sheets come from (the boss' chest). */
    public Vector3f getCloneOrigin() {
        return this.dataTracker.get(CLONE_ORIGIN);
    }

    /** Size of everything the body does: its shapes, its reach, its sheets. */
    public float getAttackScale() {
        return isClone() ? CLONE_SCALE : 1f;
    }

    public float getDamageMultiplier() {
        return isClone() ? CLONE_DAMAGE_MULTIPLIER : 1f;
    }

    public float getCooldownMultiplier() {
        return isClone() ? CLONE_COOLDOWN_MULTIPLIER : 1f;
    }

    // The clone's hitbox is shrunk with vanilla's scale attribute (see setupAsClone): LivingEntity builds its
    // dimensions from it, and the attribute is synced to the clients by vanilla, so nothing is overridden here.

    // ------------------------------------------------------------ attack scheduling (server)
    // One shared cooldown and one "planned" pattern, so the boss alternates between its attacks
    // instead of the goal with the highest priority always winning. Each attack goal only starts
    // when canStartAttack(its id) is true and calls finishAttack() when it ends.

    private int attackReadyAge = 40;   // no attack for the first two seconds after spawning
    private int plannedPattern;      // 0 = nothing chosen yet
    private int forcedPattern;       // debug: an attack to use next, whatever the draw says (0 = none)
    private int planInvalidTicks;    // how long the chosen attack has been unusable (conditions flicker, so wait a bit)

    private static final int AIRBORNE_TICKS = 8;   // off the ground this long (0.4 s) counts as airborne: small hops do not
    private int airTicks;

    /** True when the boss has been off the ground for a moment (fell off a ledge, was launched, ...). Server only. */
    public boolean isAirborne() {
        return airTicks >= AIRBORNE_TICKS;
    }

    public void resetAirTicks() {
        airTicks = 0;
    }

    public boolean canStartAttack(int pattern) {
        if (isClone()) {
            // the clone only knows the shape attack
            return pattern == PATTERN_SHAPE_THROW && this.age >= attackReadyAge;
        }
        if (isAirborne()) {
            // While airborne the boss does one thing: relocate. It does not wait for the cooldown or for
            // the planned attack, and none of its other attacks can start until it is back on the ground.
            return pattern == PATTERN_TRANSLOCATE;
        }
        return this.age >= attackReadyAge && plannedPattern == pattern;
    }

    public void finishAttack(int cooldownTicks) {
        attackReadyAge = this.age + cooldownTicks;
        planInvalidTicks = 0;
        planNextPattern();
    }

    // ------------------------------------------------------------------------- phases

    public static int phaseFor(float healthFraction) {
        if (healthFraction > PHASE_2_THRESHOLD) return 1;
        return healthFraction > PHASE_3_THRESHOLD ? 2 : 3;
    }

    /** 1, 2 or 3 depending on health. Works on both sides (health is synced). A clone always counts as phase 1. */
    public int getPhase() {
        return isClone() ? 1 : phaseFor(this.getHealth() / Math.max(1f, this.getMaxHealth()));
    }

    /** How many shapes one shape attack fires in a row: three from phase 2 on, one before that (and for a clone). */
    public int getShapeLegs() {
        return getPhase() >= 2 ? 3 : 1;
    }

    private int alertPhase = -1;      // the phase seen last tick (-1 = not seen yet)
    private int alertSoundsLeft;
    private int alertTimer;

    /**
     * When the boss drops into a new phase: five chain-break sounds in a row. (The clients see the same health
     * change and play the sheet flurry themselves, see SwarmState.) Never for a clone or a dying boss.
     */
    private void updatePhaseAlert() {
        if (isClone() || this.isDead()) return;

        int phase = getPhase();
        if (alertPhase < 0) {
            alertPhase = phase;   // first tick: no alert for the phase it starts in
        } else if (phase > alertPhase) {
            alertSoundsLeft = ALERT_SOUNDS;
            alertTimer = 0;
        }
        alertPhase = phase;       // also follows healing back up, so crossing a threshold again alerts again

        if (alertSoundsLeft > 0 && --alertTimer <= 0) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, ALERT_VOLUME, 1.0f);
            repelNearby(ALERT_SOUNDS - alertSoundsLeft);   // wave 0..4, in time with the sound
            alertSoundsLeft--;
            alertTimer = ALERT_INTERVAL;
        }
    }

    /** One wave of the phase shockwave: everything alive near the boss is pushed away from it (and lifted a little). */
    private void repelNearby(int wave) {
        float strength = REPEL_STRENGTH[Math.min(wave, REPEL_STRENGTH.length - 1)];
        for (LivingEntity other : this.getWorld().getEntitiesByClass(LivingEntity.class,
                this.getBoundingBox().expand(REPEL_RADIUS),
                e -> e != this && !(e instanceof GoldenSwarmEntity) && e.isAlive() && !e.isSpectator())) {
            double dist = this.distanceTo(other);
            if (dist > REPEL_RADIUS) continue;
            float falloff = 1f - 0.5f * (float) (dist / REPEL_RADIUS);   // full strength at the boss, half at the edge
            other.takeKnockback(strength * falloff, this.getX() - other.getX(), this.getZ() - other.getZ());
            other.setVelocity(other.getVelocity().add(0.0, 0.25 * falloff, 0.0));
            other.velocityModified = true;
        }
    }

    /** True if some foe (not only the current target) stands within r blocks of the boss. */
    private boolean hasFoeWithin(double r) {
        for (LivingEntity e : this.getWorld().getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(r),
                other -> other != this && other.isAlive() && isFoe(other))) {
            if (this.distanceTo(e) <= r) return true;
        }
        return false;
    }

    /** Forget the current choice: the next one is drawn when the boss is ready and has a target (see updatePlan). */
    private void planNextPattern() {
        plannedPattern = 0;
    }

    /**
     * Single place with every attack's start conditions (distance to the target, line of sight, ...).
     * The attack goals ask this too, so what the planner picks is always something the goal agrees to start.
     */
    public boolean isPatternUsable(int pattern) {
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) return false;
        double d = Math.sqrt(this.squaredDistanceTo(t));
        return switch (pattern) {
            case PATTERN_SHAPE_THROW -> d >= SHAPE_MIN_DIST && d <= SHAPE_MAX_DIST && this.canSee(t);
            case PATTERN_BURST -> d <= BURST_MAX_DIST && this.canSee(t);
            case PATTERN_TRANSLOCATE -> d >= TRANSLOCATE_MIN_DIST && d <= TRANSLOCATE_MAX_DIST;
            case PATTERN_CLONE -> !isClone() && !hasClone() && this.isOnGround() && d <= CLONE_MAX_DIST;
            case PATTERN_ARM_SLAM -> d >= ARM_MIN_DIST && d <= ARM_MAX_DIST && this.canSee(t);
            case PATTERN_TENTACLES -> hasFoeWithin(TENTACLE_REACH);
            default -> false;
        };
    }

    /** Weighted random pick among the attacks that can start right now. Leaves plannedPattern at 0 if none can. */
    private void choosePattern() {
        int phase = getPhase();

        // an attack that is cast as soon as it can start (above 75%: the clone)
        int priority = isClone() ? 0 : PHASE_PRIORITY[phase];
        if (priority != 0 && isPatternUsable(priority)) {
            plannedPattern = priority;
            return;
        }

        int[] weight = new int[ATTACK_PATTERNS + 1];
        int total = 0;
        for (int p = PATTERN_SHAPE_THROW; p <= ATTACK_PATTERNS; p++) {
            if (isClone() && p != PATTERN_SHAPE_THROW) continue;   // the clone only knows the shape attack
            if (PHASE_WEIGHTS[phase][p] > 0 && isPatternUsable(p)) {
                weight[p] = PHASE_WEIGHTS[phase][p];
                total += weight[p];
            }
        }
        if (total == 0) {
            plannedPattern = 0;
            return;
        }
        int roll = this.random.nextInt(total);
        for (int p = PATTERN_SHAPE_THROW; p <= ATTACK_PATTERNS; p++) {
            roll -= weight[p];
            if (roll < 0) {
                plannedPattern = p;
                return;
            }
        }
    }

    /**
     * Keeps the chosen attack in line with the situation. Draws a new one when nothing is chosen, or when the
     * chosen one has been unusable for half a second (the target moved out of its range, hid behind a wall...).
     * No more sitting idle for seconds waiting for an attack that cannot start.
     */
    private void updatePlan() {
        if (forcedPattern != 0) {   // debug command: this attack goes next, as soon as it can start
            plannedPattern = forcedPattern;
            return;
        }
        if (this.getTarget() == null || this.age < attackReadyAge) return;
        if (plannedPattern != 0 && isPatternUsable(plannedPattern)) {
            planInvalidTicks = 0;
            return;
        }
        if (plannedPattern != 0 && ++planInvalidTicks < 10) return;   // conditions flicker: give it a moment
        if (plannedPattern == 0 && this.age % 5 != 0) return;          // nothing was usable: look again every 5 ticks
        planInvalidTicks = 0;
        choosePattern();
    }

    // ------------------------------------------------------------------------- clone: lifecycle (server)

    public boolean hasClone() {
        return cloneUuid != null;
    }

    /**
     * Boss: split off a clone at the given spot. The boss gives half of its sheets to it, the clone's sheets
     * fly in from the boss' chest, and the clone has a quarter of the boss' health. Returns false if it failed.
     */
    public boolean spawnClone(Vec3d spot) {
        if (!(this.getWorld() instanceof ServerWorld world) || isClone() || hasClone()) return false;

        Entity created = this.getType().create(world);
        if (!(created instanceof GoldenSwarmEntity clone)) return false;

        clone.setupAsClone(this.getUuid(), this.getPos().add(0.0, 1.4, 0.0));
        clone.refreshPositionAndAngles(spot.x, spot.y, spot.z, this.getYaw(), 0.0f);
        clone.bodyYaw = this.bodyYaw;
        clone.headYaw = this.headYaw;

        double hp = Math.max(1.0, this.getMaxHealth() * CLONE_HEALTH_FRACTION);
        if (clone.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            clone.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(hp);
        }
        clone.setHealth((float) hp);
        clone.setPersistent();
        clone.setTarget(this.getTarget());

        if (!world.spawnEntity(clone)) return false;
        this.cloneUuid = clone.getUuid();
        setSheetShare(0.5f);
        return true;
    }

    private void setupAsClone(UUID owner, Vec3d sheetOrigin) {
        this.ownerUuid = owner;
        this.dataTracker.set(IS_CLONE, true);
        this.dataTracker.set(SHEET_SHARE, 0.5f);
        this.dataTracker.set(CLONE_ORIGIN, new Vector3f((float) sheetOrigin.x, (float) sheetOrigin.y, (float) sheetOrigin.z));
        EntityAttributeInstance scale = this.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale != null) {
            scale.setBaseValue(CLONE_SCALE);   // smaller hitbox, eye height and nameplate; synced to the clients by vanilla
        }
        this.calculateDimensions();
        this.attackReadyAge = this.age + 20;   // it waits a second before its first attack
        planNextPattern();
    }

    /** Clone: start merging back into the boss. */
    private void beginRecombine(GoldenSwarmEntity owner) {
        if (recombineTicks > 0) return;
        recombineTicks = 1;
        Vec3d point = (owner != null ? owner.getPos() : this.getPos()).add(0.0, 1.2, 0.0);
        this.setAiDisabled(true);
        this.setInvulnerable(true);
        this.setTarget(null);
        this.getNavigation().stop();
        this.beginPattern(PATTERN_RECOMBINE, 0, point, 1, false);
    }

    /** Boss: the clone is gone, the sheets come back. */
    public void cloneEnded() {
        cloneUuid = null;
        setSheetShare(1.0f);
    }

    private GoldenSwarmEntity findOwner() {
        if (ownerUuid == null || !(this.getWorld() instanceof ServerWorld world)) return null;
        return world.getEntity(ownerUuid) instanceof GoldenSwarmEntity owner ? owner : null;
    }

    private void tickClone() {
        if (recombineTicks > 0) {
            recombineTicks++;
            if (recombineTicks == RECOMBINE_RELEASE) {
                GoldenSwarmEntity owner = findOwner();
                if (owner != null) owner.cloneEnded();
            }
            if (recombineTicks >= RECOMBINE_TICKS) {
                this.discard();
            }
            return;
        }

        cloneAge++;
        GoldenSwarmEntity owner = findOwner();
        if (owner == null || !owner.isAlive() || cloneAge >= CLONE_LIFETIME_TICKS) {
            beginRecombine(owner);
        }
    }

    /** Boss: if the clone vanished some other way (killed by a command, unloaded...), take the sheets back. */
    private void checkClone() {
        if (!(this.getWorld() instanceof ServerWorld world)) return;
        Entity c = world.getEntity(cloneUuid);
        if (c == null || !c.isAlive()) {
            cloneEnded();
        }
    }

    /** A hit that would kill the clone makes it recombine instead. Damage to the clone never touches the boss. */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isClone() && recombineTicks == 0 && !this.getWorld().isClient && this.getHealth() - amount <= 0f) {
            beginRecombine(findOwner());
            return false;
        }
        return super.damage(source, amount);
    }

    /** Clones are temporary: never written to disk. */
    @Override
    public boolean shouldSave() {
        return !isClone() && super.shouldSave();
    }

    // ------------------------------------------------------------ pattern sync (server -> client)

    /** Server: announce a new pattern. Clients start it when they see the sequence number change. */
    public void beginPattern(int patternId, int shape, Vec3d target, int flightTicks, boolean breaks) {
        beginPattern(patternId, shape, target, flightTicks, breaks, 0);
    }

    /** Same, with variant bits (ShapeAttackFormation.VARIANT_*) used by the chained shape attack. */
    public void beginPattern(int patternId, int shape, Vec3d target, int flightTicks, boolean breaks, int variant) {
        if (patternId == forcedPattern) forcedPattern = 0;   // the forced attack has started: back to the normal draw
        this.dataTracker.set(PATTERN_VARIANT, variant);
        this.dataTracker.set(PATTERN_SHAPE, shape);
        this.dataTracker.set(PATTERN_TARGET, new Vector3f((float) target.x, (float) target.y, (float) target.z));
        this.dataTracker.set(PATTERN_FLIGHT, flightTicks);
        this.dataTracker.set(PATTERN_BREAKS, breaks);
        this.dataTracker.set(PATTERN_SEQ, this.dataTracker.get(PATTERN_SEQ) + 1);
        this.dataTracker.set(PATTERN_ID, patternId);
    }

    /**
     * Server: final path of the current shape, sent when the aim locks (before the throw starts):
     * where it ends up, how many ticks the flight lasts, and whether it shatters on arrival.
     */
    public void setPatternPath(Vec3d impact, int flightTicks, boolean breaks) {
        this.dataTracker.set(PATTERN_TARGET, new Vector3f((float) impact.x, (float) impact.y, (float) impact.z));
        this.dataTracker.set(PATTERN_FLIGHT, flightTicks);
        this.dataTracker.set(PATTERN_BREAKS, breaks);
    }

    /** Server: update the point the current pattern aims at. */
    public void setPatternTarget(Vec3d target) {
        this.dataTracker.set(PATTERN_TARGET, new Vector3f((float) target.x, (float) target.y, (float) target.z));
    }

    public void endPattern() {
        this.dataTracker.set(PATTERN_ID, PATTERN_NONE);
    }

    public int getPatternId() {
        return this.dataTracker.get(PATTERN_ID);
    }

    public int getPatternSeq() {
        return this.dataTracker.get(PATTERN_SEQ);
    }

    public int getPatternShape() {
        return this.dataTracker.get(PATTERN_SHAPE);
    }

    public Vector3f getPatternTarget() {
        return this.dataTracker.get(PATTERN_TARGET);
    }

    public int getPatternFlight() {
        return this.dataTracker.get(PATTERN_FLIGHT);
    }

    public boolean getPatternBreaks() {
        return this.dataTracker.get(PATTERN_BREAKS);
    }

    public int getPatternVariant() {
        return this.dataTracker.get(PATTERN_VARIANT);
    }

    public int getTentacleSeq(int k) {
        return this.dataTracker.get(TENTACLE_SEQ.get(k));
    }

    public Vector3f getTentacleAim(int k) {
        return this.dataTracker.get(TENTACLE_AIM.get(k));
    }

    /** Server: tentacle k starts a lash at this point. Clients start its windup when they see its counter change. */
    public void commandTentacle(int k, Vec3d aim) {
        updateTentacleAim(k, aim);
        this.dataTracker.set(TENTACLE_SEQ.get(k), this.dataTracker.get(TENTACLE_SEQ.get(k)) + 1);
    }

    /** Server: move the point tentacle k is aiming at (only useful while it is still rearing back). */
    public void updateTentacleAim(int k, Vec3d aim) {
        this.dataTracker.set(TENTACLE_AIM.get(k), new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
    }

    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.swarm != null) {
            this.swarm.tick(this);
            return;
        }

        // ---- server only from here ----
        boolean grounded = this.isOnGround() || this.isTouchingWater() || this.isInLava() || this.hasVehicle();
        airTicks = grounded ? 0 : airTicks + 1;

        updatePlan();
        updatePhaseAlert();
        updateBossBar();

        if (isClone()) {
            tickClone();
        } else if (cloneUuid != null && this.age % 10 == 0) {
            checkClone();
        }
    }

    // ------------------------------------------------------------------------- boss bar

    private void updateBossBar() {
        if (bossBar == null || isClone()) return;
        bossBar.setPercent(MathHelper.clamp(this.getHealth() / Math.max(1f, this.getMaxHealth()), 0f, 1f));
        bossBar.setColor(PHASE_BAR_COLORS[getPhase()]);   // yellow, then red, then purple as the phases change
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar != null && !isClone()) bossBar.addPlayer(player);
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

    // -------------------------------------------------------------------------- debug (see GoldenSwarmCommands)

    /** Make this attack the next one, as soon as it can start (it still needs a target in its range). */
    public void debugForcePattern(int pattern) {
        forcedPattern = pattern;
        plannedPattern = pattern;
        attackReadyAge = this.age;
    }

    public int debugPlannedPattern() {
        return plannedPattern;
    }

    public int debugTicksUntilReady() {
        return Math.max(0, attackReadyAge - this.age);
    }

    /** The swarm can spread far beyond the hitbox (thrown shapes), so don't frustum-cull it too early. */
    @Override
    public Box getVisibilityBoundingBox() {
        return this.getBoundingBox().expand(18.0, 6.0, 18.0);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_CHAIN_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_CHAIN_BREAK;
    }
}