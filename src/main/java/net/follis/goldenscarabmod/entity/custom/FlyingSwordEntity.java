package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.effect.ModEffects;
import net.follis.goldenscarabmod.pattern.SwordPool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A flying sword of light that pierces whatever it hits. Depending on its mode it circles its owner,
 * waits in a formation behind its owner, homes in on targets, flies in a straight line, flocks,
 * or hovers over a target and then falls on it (the sword rain).
 */
public class FlyingSwordEntity extends ProjectileEntity {
    private enum Mode { ORBIT, FORMATION, HOMING, STRAIGHT, BOID, RAIN, DRIVEN, RETURN, DIVE }

    /** The colour of the sword, and what its hits do. */
    public enum Kind { GOLD, BLACK, RED }

    /** Pass as appearAt for a sword that is already visible when created, because it just left a boss's body: no growing in, no chime. */
    public static final int ALREADY_VISIBLE = -6;
    private static final Vec3d DOWN = new Vec3d(0, -1, 0);
    private static final int RECOLOR_TICKS = 24;   // a sword leaving the body hangs still this long while it turns to its colour

    // what the client needs in order to draw it
    private static final TrackedData<Integer> APPEAR_AT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFE_END = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> AIM = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> TINT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);   // boid
    private static final TrackedData<Integer> RECOLOR_AT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> RECOLOR_LEN = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // rain: the target (-1 for every other sword, which is how the client tells a rain sword), where the sword hovers
    // relative to the top of the target's head, and the tick it falls at
    private static final TrackedData<Integer> TARGET_NETWORK_ID = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> HOVER_OFFSET = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> FALL_AT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPIN_END = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);        // the tick the sword stops turning on itself
    private static final TrackedData<Vector3f> ROLL_NORMAL = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.VECTOR3F);  // driven swords: where the broad face of the blade looks
    // a sword in a star: the star's size and its place in it, which way the star travels, where the sword hung on the body,
// whether the star is stuck, and the angle its spin stops at
    private static final TrackedData<Integer> STAR_COUNT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);   // 0: not in a star
    private static final TrackedData<Integer> STAR_SLOT = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Vector3f> STAR_FORWARD = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> STAR_START = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Boolean> STAR_STUCK = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> STAR_FINAL_SPIN = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // ---- flight tuning ----
    private static final double HOMING_SPEED = 0.95;          // blocks per tick
    private static final double HOMING_ACCELERATION = 0.05;
    private static final double TURN_RATE = 0.2;              // radians per tick: lower means wider loops after a pass
    private static final double TARGET_RANGE = 40.0;
    private static final double STRAIGHT_SPEED = 1.3;
    private static final double STRAIGHT_ACCELERATION = 0.2;
    private static final double MAX_FLIGHT_DISTANCE = 45.0;
    private static final int STUCK_TICKS = 30;
    /** How long a sword takes to fade out at the end of its life. */
    public static final int FADE_TICKS = 10;

    // ---- rain tuning ----
    private static final double RAIN_FOLLOW_FRACTION = 0.35;       // share of the remaining distance covered each tick while following
    /** At most this many blocks per tick while gliding to the hover point. */
    public static final double RAIN_MAX_FOLLOW_STEP = 1.5;
    /** For this many ticks before the fall the sword stops following its target and hangs still: the dodge window. 0 turns it off. */
    public static final int RAIN_LOCK_TICKS = 12;
    private static final int HOVER_BLEND_TICKS = 4;                // how long the drawn position takes to hand over to the real one when the fall starts
    private static final double FALL_START_SPEED = 0.5;            // blocks per tick when the fall begins...
    private static final double FALL_ACCELERATION = 0.3;           // ...gaining this much every tick...
    private static final double FALL_MAX_SPEED = 2.6;              // ...up to this

    // ---- boid tuning ---- (boid)
    private static final int EMERGE_TICKS = 8;                // time spent rising out of the floor, straight up
    private static final double EMERGE_SPEED = 0.2;
    private static final double BOID_SPEED = 0.25;
    private static final double WANDERER_SPEED = 0.4;
    private static final double HATRED_SPEED = 0.85;
    private static final double BOID_TURN = 0.11;             // radians per tick

    private static final double NEIGHBOUR_RADIUS = 14.0;
    private static final double SEPARATION_RADIUS = 2.0;
    private static final double W_COHESION = 1.4;
    private static final double W_ALIGNMENT = 1.4;

    private static final double WANDERER_AVOID_RADIUS = 7.0;  // how far a wanderer keeps from the flock
    private static final double WANDERER_SEEK_RANGE = 14.0;
    public static final double RING_FRACTION = 0.6;          // the flock circles at this share of the arena radius
    private static final double W_SEPARATION = 2.2;
    private static final double W_CIRCLE = 0.9;
    private static final double W_RING = 1.5;
    private static final double W_BOUNDS = 3.0;
    private static final double W_HEIGHT = 0.8;
    private static final double W_WANDER = 0.7;
    private static final double W_SEEK = 1.2;
    private static final double W_HATRED = 4.0;
    private static final double W_CHARGE = 4.0;           // the whole flock sent at one target
    private static final double CHARGE_SPEED = 0.8;
    private static final double CHARGE_TURN = 0.14;       // radians per tick

    private int ticks;   // ticks lived, counted on both sides
    private Mode mode = Mode.ORBIT;

    // ---- following the leader (the boss, during the flock charge) ----
    private static final double FOLLOW_SPREAD = 3.0;       // each sword keeps its own spot within this distance of the boss...
    private static final double FOLLOW_TRAIL = 4.0;        // ...and up to this far behind it
    private static final double W_FOLLOW = 3.0;
    private static final double W_FOLLOW_ALIGN = 1.2;
    private static final double FOLLOW_TURN = 0.2;         // radians per tick

    // ---- the sword star: values shared with SwordStarPattern ----
    public static final double STAR_RADIUS = 0.65;     // from the centre of the star to the middle of a blade
    public static final int STAR_GATHER_TICKS = 24;    // from leaving the body to taking its place in the star
    public static final double STAR_SPIN = 0.9;        // radians per tick
    private static final int STAR_SETTLE_TICKS = 8;    // after a hit the spin eases to a stop over this long

    // ---- coming back to the boss ----
    private static final int RETURN_HOLD_TICKS = 40;       // stuck in a block this long before being called back
    private static final int RETURN_TIMEOUT = 400;         // a sword that cannot find its way home gives up after this
    private static final double RETURN_SPEED = 1.1;
    private static final double RETURN_TURN = 0.5;         // radians per tick
    private static final double RETURN_ARRIVE = 1.0;       // closer than this to its place, it hangs on the body again

    // the client blends between the previous and the current aim when drawing
    private Vec3d aim = new Vec3d(0, -1, 0);
    private Vec3d prevAim = new Vec3d(0, -1, 0);
    // client only: where a rain sword hovers, worked out from the target as this game draws it
    private Vec3d hover = Vec3d.ZERO;
    private Vec3d prevHover = Vec3d.ZERO;
    private boolean hoverReady;
    // client only: the spin of a star, advanced here and interpolated when drawing
    private double starSpin, prevStarSpin, settleFrom, settleTo;
    private boolean starSettling;
    private int settleTicks;

    // ---- server only ----
    private Predicate<LivingEntity> targets = e -> true;
    private float damage = 4.0F;
    private int hitCooldown = 12;                              // ticks before the same sword can hit the same target again
    private final Map<UUID, Integer> nextHit = new HashMap<>();
    private boolean stuck;
    private int launchAt;
    private int slot;                                          // position in its group, used to vary pitch
    private double orbitRadius, orbitHeight, orbitPhase, orbitSpeed;
    private double orbitTilt, orbitYaw;                    // the plane of the orbit: tilted around the x axis, then turned around the vertical axis
    private Vec3d formationOffset = Vec3d.ZERO;
    private Vec3d lockedDirection = new Vec3d(0, 0, 1);
    private Vec3d direction = new Vec3d(0, 0, 1);             // current flight direction, a unit vector
    private double speed;
    private Vec3d launchPoint = Vec3d.ZERO;
    @Nullable private UUID targetId;
    private Vec3d syncedAim = Vec3d.ZERO;
    @Nullable private Vec3d lockPoint;     // server: the spot the swords will fall on
    @Nullable private Vec3d clientLock;    // client: the same spot, to aim the blade at it

    // coming back to the boss
    private boolean returning;                 // this sword goes home to the boss's body when its work is done
    private int recallAt = Integer.MAX_VALUE;  // the tick it starts going home, whatever it is doing
    private boolean lent;                      // the boss was told this sword will come back
    private boolean arrived;
    private int returnTicks;
    private int lastDriveTick;                 // driven swords: the last tick a controller moved this one

    // leaving a boss's body: the sword glides from where it hung to its place in the pattern
    @Nullable private Vec3d gatherStart;
    private int gatherTicks;

    // boid
    private Kind kind = Kind.GOLD;
    private boolean wanderer;
    private boolean detached;                                  // a wanderer that left a body instead of rising from the floor
    private Vec3d arenaCenter = Vec3d.ZERO;
    private double arenaRadius = 15.0;
    private double cruiseHeight = 2.0;                         // height above the arena floor this sword likes to fly at
    private double wanderPhase;
    private int emergeAt;
    @Nullable private UUID hatedId;
    @Nullable private UUID chargeId;   // the flock is sent at this target...
    private int chargeUntil;           // ...until this tick

    // following a leader
    @Nullable private UUID leaderId;
    private int followUntil;
    private Vec3d followOffset = Vec3d.ZERO;
    private double followTrail;
    private Vec3d lastLeaderPos = Vec3d.ZERO;
    private Vec3d leaderDir = new Vec3d(0, 0, 1);

    // rain
    private Vec3d hoverOffset = Vec3d.ZERO;
    private boolean fallStarted;
    private int fallTicks;

    public FlyingSwordEntity(EntityType<? extends FlyingSwordEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(APPEAR_AT, 0);
        builder.add(LIFE_END, 200);
        builder.add(AIM, new Vector3f(0, -1, 0));
        builder.add(TINT, 0);
        builder.add(RECOLOR_AT, 0);
        builder.add(RECOLOR_LEN, 0);
        builder.add(TARGET_NETWORK_ID, -1);
        builder.add(HOVER_OFFSET, new Vector3f());
        builder.add(FALL_AT, 0);
        builder.add(SPIN_END, Integer.MAX_VALUE);
        builder.add(ROLL_NORMAL, new Vector3f());
        builder.add(STAR_COUNT, 0);
        builder.add(STAR_SLOT, 0);
        builder.add(STAR_FORWARD, new Vector3f(0.0F, 0.0F, 1.0F));
        builder.add(STAR_START, new Vector3f());
        builder.add(STAR_STUCK, false);
        builder.add(STAR_FINAL_SPIN, 0.0F);
    }

    // ---- setting a sword up (server side) ----

    public void configure(LivingEntity owner, Predicate<LivingEntity> targets, float damage, int hitCooldown,
                          int slot, int appearAt, int lifeEnd, int launchAt) {
        this.setOwner(owner);
        this.targets = targets;
        this.damage = damage;
        this.hitCooldown = hitCooldown;
        this.slot = slot;
        this.launchAt = launchAt;
        this.dataTracker.set(APPEAR_AT, appearAt);
        this.dataTracker.set(LIFE_END, lifeEnd);
    }

    /** Circles the owner, then launches at its target at launchAt. */
    public void startOrbit(double radius, double height, double phase, double angularSpeed) {
        startOrbit(radius, height, phase, angularSpeed, 0.0, 0.0);
    }

    /** The same, in a tilted plane: tilt turns the circle around the x axis, then yaw turns it around the vertical axis. */
    public void startOrbit(double radius, double height, double phase, double angularSpeed, double tilt, double yaw) {
        this.mode = Mode.ORBIT;
        this.orbitRadius = radius;
        this.orbitHeight = height;
        this.orbitPhase = phase;
        this.orbitSpeed = angularSpeed;
        this.orbitTilt = tilt;
        this.orbitYaw = yaw;
        Entity owner = getOwner();
        if (owner != null) setPosition(orbitPoint(owner, 0));
    }

    /** Waits at a fixed offset from the owner, pointing along lockedDirection, then flies that way at launchAt. */
    public void startFormation(Vec3d offset, Vec3d lockedDirection) {
        this.mode = Mode.FORMATION;
        this.formationOffset = offset;
        this.lockedDirection = lockedDirection;
        Entity owner = getOwner();
        if (owner != null) setPosition(owner.getX() + offset.x, owner.getY() + offset.y, owner.getZ() + offset.z);
    }

    /**
     * Flocks in the arena. The sword waits under the floor until its appearAt tick, then rises out of it and joins in.
     * A wanderer keeps away from the rest of the flock instead of joining it.
     */
    public void startBoid(Kind kind, boolean wanderer, Vec3d arenaCenter, double arenaRadius, double cruiseHeight, int emergeAt) {
        this.mode = Mode.BOID;
        this.kind = kind;
        this.wanderer = wanderer;
        this.arenaCenter = arenaCenter;
        this.arenaRadius = arenaRadius;
        this.cruiseHeight = cruiseHeight;
        this.emergeAt = emergeAt;
        this.wanderPhase = getWorld().random.nextDouble() * Math.PI * 2.0;
        this.direction = new Vec3d(0, 1, 0);
        this.dataTracker.set(TINT, kind.ordinal());
    }

    /** A wanderer that leaves the body: it lifts off at `from`, hangs still while it turns to its colour, then starts wandering. */
    public void startDetachedWanderer(Kind kind, Vec3d arenaCenter, double arenaRadius, double cruiseHeight, Vec3d from) {
        startBoid(kind, true, arenaCenter, arenaRadius, cruiseHeight, 0);
        this.detached = true;
        this.direction = DOWN;   // the blade still hangs tip down, as it did on the body
        setPosition(from);
        dataTracker.set(RECOLOR_AT, EMERGE_TICKS);
        dataTracker.set(RECOLOR_LEN, RECOLOR_TICKS);
    }

    /**
     * Hovers over the target (offset is relative to the top of its head) and falls on it at tick fallAt.
     * With from set, the sword starts there, where it hung on the boss's body, and glides over; otherwise it starts over the target.
     */
    public void startRain(LivingEntity target, Vec3d offset, int fallAt, @Nullable Vec3d from) {
        this.mode = Mode.RAIN;
        this.targetId = target.getUuid();
        this.hoverOffset = offset;
        this.direction = DOWN;
        this.dataTracker.set(TARGET_NETWORK_ID, target.getId());
        this.dataTracker.set(HOVER_OFFSET, offset.toVector3f());
        this.dataTracker.set(FALL_AT, fallAt);
        setPosition(from != null ? from : hoverPoint(target));
    }

    /** The sword starts at `from`, where it hung on the boss's body, and glides to its place in the pattern over `ticks`. Call after startOrbit/startFormation. */
    public void gatherFrom(Vec3d from, int ticks) {
        this.gatherStart = from;
        this.gatherTicks = ticks;
        setPosition(from);
    }

    /** 0 to 1 over the glide, smoothed; 1 when there is no glide or it is over. */
    private double gatherProgress() {
        if (gatherStart == null || ticks >= gatherTicks) return 1.0;
        double k = (double) ticks / gatherTicks;
        return k * k * (3.0 - 2.0 * k);
    }

    /** Makes the sword fade out and disappear; a sword that means to come back goes home instead. */
    public void retire() {
        Entity owner = getOwner();
        if (returning && mode != Mode.RETURN && owner != null && owner.isAlive()) {
            startReturn();
            return;
        }
        int end = ticks + FADE_TICKS;
        if (end < getLifeEnd()) dataTracker.set(LIFE_END, end);
    }

    private void vanish(ServerWorld world) {
        Entity owner = getOwner();
        if (returning && mode != Mode.RETURN && owner != null && owner.isAlive()) {
            startReturn();
            return;
        }
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 5, 0.2, 0.2, 0.2, 0.03);
        discard();
    }

    // ---- for the renderer ----
    public int getTicks() { return ticks; }
    public int getAppearAt() { return dataTracker.get(APPEAR_AT); }
    public int getLifeEnd() { return dataTracker.get(LIFE_END); }
    public int getTint() { return dataTracker.get(TINT); }
    public int getRecolorAt() { return dataTracker.get(RECOLOR_AT); }
    public int getRecolorLength() { return dataTracker.get(RECOLOR_LEN); }
    public boolean isRain() { return dataTracker.get(TARGET_NETWORK_ID) >= 0; }
    public int getFallAt() { return dataTracker.get(FALL_AT); }
    public boolean isHovering() { return isRain() && ticks < getFallAt(); }
    public int getSpinEnd() { return dataTracker.get(SPIN_END); }

    /** Driven swords only: the direction the broad face of the blade looks in, or null for a sword that turns freely. */
    @Nullable
    public Vec3d getRollNormal() {
        Vector3f v = dataTracker.get(ROLL_NORMAL);
        return v.lengthSquared() < 1.0E-6F ? null : new Vec3d(v.x(), v.y(), v.z());
    }
    /**
     * Where to draw a rain sword. While it travels and hovers, that is the position worked out on this side from the
     * target as it is drawn here, which is smooth; once it falls, the real position from the server.
     */
    public Vec3d getRenderPosition(float tickDelta) {
        Vec3d real = getLerpedPos(tickDelta);
        if (!isRain() || !hoverReady) return real;

        float sinceFall = ticks + tickDelta - getFallAt();
        if (sinceFall >= HOVER_BLEND_TICKS) return real;

        Vec3d hovering = prevHover.lerp(hover, tickDelta);
        return sinceFall <= 0.0F ? hovering : hovering.lerp(real, sinceFall / HOVER_BLEND_TICKS);
    }

    /**
     * The direction the blade points in. A rain sword aims at the body of its target while it hovers; every other sword,
     * and a falling one, uses the aim from the server, blended between the last two ticks so turning looks smooth.
     */
    public Vec3d getAim(float tickDelta) {
        if (isHovering()) {
            if (clientLock != null) {   // locked: the blades point at the spot they are about to hit
                Vec3d toLock = clientLock.subtract(getRenderPosition(tickDelta));
                return toLock.lengthSquared() > 1.0E-4 ? toLock.normalize() : DOWN;
            }
            Entity target = getWorld().getEntityById(dataTracker.get(TARGET_NETWORK_ID));
            if (target instanceof LivingEntity living) {
                Vec3d toTarget = living.getLerpedPos(tickDelta).add(0, living.getHeight() * 0.5, 0)
                        .subtract(getRenderPosition(tickDelta));
                if (toTarget.lengthSquared() > 1.0E-4) return toTarget.normalize();
            }
            return DOWN;
        }
        Vec3d blended = prevAim.lerp(aim, tickDelta);
        return blended.lengthSquared() < 1.0E-6 ? aim : blended.normalize();
    }

    private Vec3d trackedAim() {
        Vector3f v = dataTracker.get(AIM);
        return new Vec3d(v.x(), v.y(), v.z());
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (getWorld().isClient) {
            // the server sends the position and velocity every tick, but network timing is never perfectly even.
            // Like vanilla projectiles, carry on along the last known velocity in between, so a late update is not a stall.
            setPosition(getPos().add(getVelocity()));
            prevAim = aim;
            aim = trackedAim();
            if (isHovering()) updateClientHover();   // the server moves the sword, the client only works out how to draw it
            if (getStarCount() > 0) tickStarClient();
            return;
        }
        ServerWorld world = (ServerWorld) getWorld();

        if (returning && mode != Mode.RETURN && ticks >= recallAt) startReturn();
        if (ticks >= getLifeEnd()) {
            vanish(world);
            return;
        }
        if (stuck && mode != Mode.RETURN) return;

        Entity owner = getOwner();
        Vec3d wanted = switch (mode) {
            case ORBIT -> tickOrbit(world, owner);
            case FORMATION -> tickFormation(world, owner);
            case HOMING -> tickHoming(world);
            case STRAIGHT -> tickStraight(world);
            case BOID -> tickBoid(world, owner);
            case RAIN -> tickRain(world);
            case DRIVEN -> tickDriven(world, owner);
            case RETURN -> tickReturn(world, owner);
            case DIVE -> tickDive(world);
        };
        if (wanted != null) syncAim(wanted);
    }

    /** Moves the sword and records the move as its velocity, which is what the clients extrapolate with. */
    private void moveTo(Vec3d target) {
        setVelocity(target.subtract(getPos()));
        setPosition(target);
    }
// ---- the sword star ----

    /** The plane of a star: its normal is "up" made perpendicular to the direction of travel. */
    public static Vec3d starNormal(Vec3d forward) {
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d normal = up.subtract(forward.multiply(up.dotProduct(forward)));
        return normal.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : normal.normalize();
    }

    /** The direction of a spoke of the star at this angle; angle 0 points straight ahead. forward must be a unit vector. */
    public static Vec3d starSpoke(Vec3d forward, double angle) {
        Vec3d side = starNormal(forward).crossProduct(forward);
        return forward.multiply(Math.cos(angle)).add(side.multiply(Math.sin(angle)));
    }

    private static double smooth01(double x) {
        x = MathHelper.clamp(x, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    /** Where a sword of a star is drawn, the way its blade points, and the normal of the star's plane. */
    public record StarPose(Vec3d position, Vec3d aim, Vec3d normal) {}

    public int getStarCount() {
        return dataTracker.get(STAR_COUNT);
    }

    /** For the renderer: the pose of this sword in its star, worked out here from the centre of the star, or null if it is in none. */
    @Nullable
    public StarPose getStarPose(float tickDelta) {
        int count = getStarCount();
        if (count <= 0) return null;
        Vector3f f = dataTracker.get(STAR_FORWARD);
        Vec3d forward = new Vec3d(f.x(), f.y(), f.z());
        if (forward.lengthSquared() < 1.0E-6) return null;
        forward = forward.normalize();

        double spin = MathHelper.lerp(tickDelta, prevStarSpin, starSpin);   // the angle is interpolated, not the position: a perfect circle
        double progress = smooth01((ticks + tickDelta) / STAR_GATHER_TICKS);   // 0 on the body, 1 in the star
        Vec3d spoke = starSpoke(forward, 2.0 * Math.PI * dataTracker.get(STAR_SLOT) / count + spin);
        Vec3d place = getLerpedPos(tickDelta).add(spoke.multiply(STAR_RADIUS * (0.3 + 0.7 * progress)));

        Vector3f s = dataTracker.get(STAR_START);
        Vec3d position = progress >= 1.0 ? place : new Vec3d(s.x(), s.y(), s.z()).lerp(place, progress);   // it glides out of the body
        Vec3d aim = progress >= 1.0 ? spoke : DOWN.lerp(spoke, progress);
        return new StarPose(position, aim, starNormal(forward));
    }

    private void tickStarClient() {
        prevStarSpin = starSpin;
        if (!dataTracker.get(STAR_STUCK)) {
            starSpin += STAR_SPIN;
            return;
        }
        if (!starSettling) {   // the star just hit a block: the spin eases to a stop on the angle the server says
            starSettling = true;
            double turn = 2.0 * Math.PI;
            double target = dataTracker.get(STAR_FINAL_SPIN);
            settleFrom = starSpin;
            settleTo = target + turn * Math.ceil((starSpin - target) / turn);   // the same angle as the server's, never backwards
            settleTicks = 0;
        }
        if (settleTicks < STAR_SETTLE_TICKS) {
            settleTicks++;
            double k = settleTicks / (double) STAR_SETTLE_TICKS;
            starSpin = settleFrom + (settleTo - settleFrom) * (1.0 - (1.0 - k) * (1.0 - k));
        }
    }

// ---- server side, for the controller of the star ----

    /** The sword becomes part of a star. The entity sits at the centre of the star; the clients draw the blade in its place. */
    public void joinStar(int slot, int count, Vec3d start, Vec3d center) {
        this.mode = Mode.DRIVEN;
        this.lastDriveTick = ticks;
        dataTracker.set(SPIN_END, 0);   // it never turns on itself
        dataTracker.set(STAR_SLOT, slot);
        dataTracker.set(STAR_COUNT, count);
        dataTracker.set(STAR_START, start.toVector3f());
        setPosition(center);
    }

    /** Moves the centre of the star, and tells the clients which way it travels. */
    public void driveStar(Vec3d center, Vec3d forward) {
        moveTo(center);   // the velocity lets the clients carry on smoothly: the centre moves slowly
        dataTracker.set(STAR_FORWARD, forward.toVector3f());
        lastDriveTick = ticks;
    }

    /** Lets the blade hit what it passed through between these two points. */
    public void sweepHits(Vec3d from, Vec3d to) {
        if (getWorld() instanceof ServerWorld world) pierce(world, from, to);
    }

    /** The star hit a block: it stops, with its spin ending on finalSpin. */
    public void stuckInStar(double finalSpin) {
        dataTracker.set(STAR_FINAL_SPIN, (float) finalSpin);
        dataTracker.set(STAR_STUCK, true);
        setVelocity(Vec3d.ZERO);
    }

    /** The sword leaves the star: from now on it is drawn at its own position, which is where its blade was. */
    public void leaveStar(Vec3d bladePosition, Vec3d bladeAim) {
        dataTracker.set(STAR_COUNT, 0);
        setPosition(bladePosition);
        setVelocity(Vec3d.ZERO);
        Vec3d aimNow = bladeAim.normalize();
        dataTracker.set(AIM, aimNow.toVector3f());
        syncedAim = aimNow;
    }

    /** Dives from `from` to a point in the ground, arriving after `ticks`, and stabs in. A returning sword then goes home after a while. */
    public void startDive(Vec3d from, Vec3d to, int ticks) {
        this.mode = Mode.DIVE;
        setPosition(from);
        Vec3d delta = to.subtract(from);
        double distance = delta.length();
        this.direction = distance < 1.0E-4 ? DOWN : delta.multiply(1.0 / distance);
        this.speed = distance / Math.max(1, ticks);   // a steady speed, so every sword of the group arrives together
        this.launchPoint = from;
    }

    @Nullable
    private Vec3d tickDive(ServerWorld world) {
        advance(world, false);   // pierces what it dives through and stabs into the ground
        if (!stuck && getPos().squaredDistanceTo(launchPoint) > MAX_FLIGHT_DISTANCE * MAX_FLIGHT_DISTANCE) {
            vanish(world);
            return null;
        }
        return direction;
    }

    /** This flock sword follows the leader for this many ticks instead of flocking: it takes a spot of its own around and behind it. */
    public void startFollow(LivingEntity leader, int duration) {
        if (mode != Mode.BOID) return;
        var random = getWorld().random;
        Vec3d v;
        do {
            v = new Vec3d(random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 2.0 - 1.0);
        } while (v.lengthSquared() > 1.0);
        leaderId = leader.getUuid();
        followUntil = ticks + duration;
        followOffset = v.multiply(FOLLOW_SPREAD);
        followTrail = random.nextDouble() * FOLLOW_TRAIL;
        lastLeaderPos = leader.getPos();
    }

    /** The sword goes back to flocking. */
    public void stopFollow() {
        leaderId = null;
    }

    @Nullable
    private LivingEntity leaderToFollow(ServerWorld world) {
        if (leaderId == null) return null;
        if (ticks >= followUntil) {
            leaderId = null;
            return null;
        }
        return world.getEntity(leaderId) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** Keeps to its spot around and behind the leader, at the leader's pace and heading. */
    private void steerFollow(LivingEntity leader) {
        Vec3d leaderPos = leader.getPos();
        Vec3d move = leaderPos.subtract(lastLeaderPos);   // how far the leader went this tick
        lastLeaderPos = leaderPos;
        double leaderSpeed = move.length();
        if (leaderSpeed > 0.05) leaderDir = move.multiply(1.0 / leaderSpeed);

        double bodyHeight = leader instanceof SwordPool pool ? pool.bodyCenterHeight() : leader.getHeight() * 0.5;
        Vec3d spot = leaderPos.add(0.0, bodyHeight, 0.0).add(followOffset).subtract(leaderDir.multiply(followTrail));
        Vec3d toSpot = spot.subtract(getPos());
        double distance = toSpot.length();

        Vec3d force = limit(toSpot.multiply(1.0 / 5.0), 1.0).multiply(W_FOLLOW).add(leaderDir.multiply(W_FOLLOW_ALIGN));
        steer(force, FOLLOW_TURN);
        double wantedSpeed = MathHelper.clamp(leaderSpeed + distance * 0.1, 0.25, 1.3);   // faster when it falls behind
        speed += MathHelper.clamp(wantedSpeed - speed, -0.08, 0.08);
    }


    // ---- orbiting ----

    private Vec3d orbitPoint(Entity owner, int t) {
        double angle = orbitPhase + orbitSpeed * t;
        Vec3d offset = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle))
                .rotateX((float) orbitTilt).rotateY((float) orbitYaw).multiply(orbitRadius);
        return new Vec3d(owner.getX() + offset.x,
                owner.getY() + orbitHeight + offset.y + Math.sin(t * 0.12 + orbitPhase) * 0.25,
                owner.getZ() + offset.z);
    }

    @Nullable
    private Vec3d tickOrbit(ServerWorld world, @Nullable Entity owner) {
        if (owner == null || !owner.isAlive()) {
            vanish(world);
            return null;
        }
        double angle = orbitPhase + orbitSpeed * ticks;
        double g = gatherProgress();
        Vec3d point = orbitPoint(owner, ticks);
        moveTo(g >= 1.0 ? point : gatherStart.lerp(point, g));
        Vec3d tangent = new Vec3d(-Math.sin(angle), 0.0, Math.cos(angle))
                .rotateX((float) orbitTilt).rotateY((float) orbitYaw).multiply(Math.signum(orbitSpeed));
        announceAppearance(world);

        if (ticks >= launchAt) {
            LivingEntity target = pickTarget(world);
            if (target == null) {
                launchAt = ticks + 10;   // nobody to hit yet: keep circling
            } else {
                mode = Mode.HOMING;
                targetId = target.getUuid();
                direction = tangent;
                speed = Math.abs(orbitSpeed) * orbitRadius;   // carries on at the speed it was circling at
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                        SoundCategory.HOSTILE, 0.8F, 1.3F);
            }
        }
        return g >= 1.0 ? tangent : DOWN.lerp(tangent, g);   // the blade turns from hanging down to its orbit direction
    }

    // ---- waiting in formation ----

    @Nullable
    private Vec3d tickFormation(ServerWorld world, @Nullable Entity owner) {
        if (owner == null || !owner.isAlive()) {
            vanish(world);
            return null;
        }
        Vec3d spot = new Vec3d(owner.getX() + formationOffset.x, owner.getY() + formationOffset.y, owner.getZ() + formationOffset.z);
        double g = gatherProgress();
        moveTo(g >= 1.0 ? spot : gatherStart.lerp(spot, g));
        announceAppearance(world);

        if (ticks >= launchAt) {
            mode = Mode.STRAIGHT;
            direction = lockedDirection;
            speed = 0.3;
            launchPoint = getPos();
            if (slot == 0) {   // one sound for the whole wall
                world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(),
                        SoundCategory.HOSTILE, 3.0F, 0.8F);
            }
        }
        return g >= 1.0 ? lockedDirection : DOWN.lerp(lockedDirection, g);
    }

    // ---- homing ----

    @Nullable
    private Vec3d tickHoming(ServerWorld world) {
        LivingEntity target = currentTarget(world);
        if (target == null) {
            target = pickTarget(world);
            targetId = target == null ? null : target.getUuid();
        }
        if (target != null) {
            steer(target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(getPos()), TURN_RATE);
        }
        speed = Math.min(HOMING_SPEED, speed + HOMING_ACCELERATION);
        advance(world, true);
        return direction;
    }

    /** Turns toward the wanted direction, by no more than turnRate radians this tick. */
    private void steer(Vec3d wanted, double turnRate) {
        if (wanted.lengthSquared() < 1.0E-6) return;
        Vec3d desired = wanted.normalize();
        double angle = Math.acos(MathHelper.clamp(direction.dotProduct(desired), -1.0, 1.0));
        if (angle < 1.0E-4) return;

        double k = Math.min(1.0, turnRate / angle);
        Vec3d blended = direction.multiply(1.0 - k).add(desired.multiply(k));
        if (blended.lengthSquared() < 1.0E-6) {
            blended = direction.crossProduct(new Vec3d(0, 1, 0));   // the target is exactly behind: pick a side to turn to
        }
        direction = blended.normalize();
    }

    @Nullable
    private LivingEntity currentTarget(ServerWorld world) {
        if (targetId == null) return null;
        if (world.getEntity(targetId) instanceof LivingEntity living && canStab(living)
                && living.squaredDistanceTo(this) < TARGET_RANGE * TARGET_RANGE * 2.25) {
            return living;
        }
        return null;
    }

    @Nullable
    private LivingEntity pickTarget(ServerWorld world) {
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class,
                new Box(getPos(), getPos()).expand(TARGET_RANGE), this::canStab);
        if (candidates.isEmpty()) return null;
        return candidates.get(world.getRandom().nextInt(candidates.size()));   // random, so the swords spread over several players
    }

    // ---- flying straight ----

    @Nullable
    private Vec3d tickStraight(ServerWorld world) {
        speed = Math.min(STRAIGHT_SPEED, speed + STRAIGHT_ACCELERATION);
        advance(world, false);
        if (!stuck && getPos().squaredDistanceTo(launchPoint) > MAX_FLIGHT_DISTANCE * MAX_FLIGHT_DISTANCE) {
            vanish(world);
            return null;
        }
        return direction;
    }

    // ---- the sword rain: hovering over a target, then falling on it ----

    /** Where the sword hovers: over the top of the target's head, at its own spot of the ring. */
    private Vec3d hoverPoint(LivingEntity target) {
        return new Vec3d(target.getX() + hoverOffset.x, target.getBoundingBox().maxY + hoverOffset.y, target.getZ() + hoverOffset.z);
    }

    @Nullable
    private LivingEntity rainTarget(ServerWorld world) {
        if (targetId != null && world.getEntity(targetId) instanceof LivingEntity living && living.isAlive()) return living;
        return null;
    }

    private static Vec3d bodyPoint(LivingEntity e) {
        return e.getPos().add(0, e.getHeight() * 0.5, 0);
    }

    @Nullable
    private Vec3d tickRain(ServerWorld world) {
        if (!fallStarted) {
            if (ticks < getFallAt()) {
                LivingEntity target = rainTarget(world);
                if (target != null) {
                    if (ticks < getFallAt() - RAIN_LOCK_TICKS) {
                        Vec3d step = hoverPoint(target).subtract(getPos()).multiply(RAIN_FOLLOW_FRACTION);
                        if (step.length() > RAIN_MAX_FOLLOW_STEP) step = step.normalize().multiply(RAIN_MAX_FOLLOW_STEP);
                        moveTo(getPos().add(step));
                    } else {
                        // the last moments before the fall: the sword hangs still over where the target was, so it can be dodged
                        moveTo(getPos());
                        if (lockPoint == null) lockPoint = bodyPoint(target);
                    }
                    announceAppearance(world);
                    if (ticks > getAppearAt() && ticks % 6 == 0) {
                        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 0.7, getZ(), 1, 0.1, 0.3, 0.1, 0.0);
                    }
                    return (lockPoint != null ? lockPoint : bodyPoint(target)).subtract(getPos());
                }
                // the target is gone: swords still to appear vanish, the others fall where they are
                if (ticks < getAppearAt()) {
                    discard();
                    return null;
                }
                dataTracker.set(FALL_AT, ticks);
            }
            startFall(world);
        }
        speed = Math.min(FALL_MAX_SPEED, FALL_START_SPEED + FALL_ACCELERATION * fallTicks++);
        advance(world, false);   // pierces what it falls through and stabs into the ground
        return direction;
    }

    /** The swords fall on the spot the target was at when they locked, not on where it has run to since. */
    private void startFall(ServerWorld world) {
        fallStarted = true;
        fallTicks = 0;
        LivingEntity target = rainTarget(world);
        Vec3d aimPoint = lockPoint != null ? lockPoint : target != null ? bodyPoint(target) : null;
        Vec3d toPoint = aimPoint == null ? Vec3d.ZERO : aimPoint.subtract(getPos());
        direction = toPoint.lengthSquared() > 1.0E-4 ? toPoint.normalize() : DOWN;
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 1.0F, 1.6F);
    }

    /** The same following as on the server, but from the target's position as this game sees it. */
    private void updateClientHover() {
        if (!hoverReady) {
            hover = getPos();   // the first position the server told us about: where the sword left the body
            prevHover = hover;
            hoverReady = true;
            return;
        }
        Entity target = getWorld().getEntityById(dataTracker.get(TARGET_NETWORK_ID));
        if (ticks >= getFallAt() - RAIN_LOCK_TICKS) {   // locked: it hangs still over where the target was
            prevHover = hover;
            if (clientLock == null && target instanceof LivingEntity living) clientLock = bodyPoint(living);
            return;
        }
        Vec3d desired = getPos();
        if (target instanceof LivingEntity living) {
            Vector3f o = dataTracker.get(HOVER_OFFSET);
            desired = new Vec3d(living.getX() + o.x(), living.getBoundingBox().maxY + o.y(), living.getZ() + o.z());
        }
        prevHover = hover;
        Vec3d step = desired.subtract(hover).multiply(RAIN_FOLLOW_FRACTION);
        if (step.length() > RAIN_MAX_FOLLOW_STEP) step = step.normalize().multiply(RAIN_MAX_FOLLOW_STEP);
        hover = hover.add(step);
    }

    // ---- flocking ---- (boid)

    @Nullable
    private Vec3d tickBoid(ServerWorld world, @Nullable Entity owner) {
        if (owner == null || !owner.isAlive()) retire();

        int since = ticks - emergeAt;
        if (since < 0) return null;   // still under the floor, waiting for its turn

        if (since == 0) {
            if (!detached) {   // a sword from the floor rises out of it; one from the body is already in the air
                direction = new Vec3d(0, 1, 0);
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(getBlockPos())),
                        getX(), getY() + 1.0, getZ(), 12, 0.3, 0.1, 0.3, 0.1);
            }
            announceAppearance(world);
        }
        if (since < EMERGE_TICKS) {
            moveTo(getPos().add(0, EMERGE_SPEED, 0));   // straight up
            return direction;
        }
        int hoverTicks = detached ? RECOLOR_TICKS : 0;   // a detached sword hangs still while it changes colour
        if (since < EMERGE_TICKS + hoverTicks) {
            moveTo(getPos());                            // zero velocity, so the clients do not keep drifting it upward
            return direction;
        }
        if (since == EMERGE_TICKS + hoverTicks) speed = EMERGE_SPEED;

        steerBoid(world);
        advance(world, true);   // bounces off blocks
        return direction;
    }

    /** The three flocking rules, plus the arena, plus whatever the sword is after. */
    private void steerBoid(ServerWorld world) {
        LivingEntity leader = leaderToFollow(world);
        if (leader != null) {   // following the boss: nothing else matters
            steerFollow(leader);
            return;
        }
        Vec3d pos = getPos();
        Vec3d separation = Vec3d.ZERO, alignment = Vec3d.ZERO, centre = Vec3d.ZERO;
        int mates = 0;

        for (FlyingSwordEntity o : world.getEntitiesByClass(FlyingSwordEntity.class, getBoundingBox().expand(NEIGHBOUR_RADIUS),
                o -> o != this && o.mode == Mode.BOID && o.ticks >= o.emergeAt + EMERGE_TICKS && o.getOwner() == getOwner())) {
            Vec3d away = pos.subtract(o.getPos());
            double d = away.length();
            if (d < 1.0E-4) continue;

            // a wanderer keeps well away from the flock; the flock keeps only its own spacing
            double radius = (wanderer && !o.wanderer) ? WANDERER_AVOID_RADIUS : SEPARATION_RADIUS;
            if (d < radius) separation = separation.add(away.normalize().multiply((radius - d) / radius));

            if (!wanderer && !o.wanderer) {
                alignment = alignment.add(o.direction);
                centre = centre.add(o.getPos());
                mates++;
            }
        }

        Vec3d force = separation.multiply(W_SEPARATION);
        if (mates > 0) {
            force = force.add(alignment.multiply(1.0 / mates).multiply(W_ALIGNMENT));
            Vec3d toMates = centre.multiply(1.0 / mates).subtract(pos);
            force = force.add(limit(toMates.multiply(1.0 / NEIGHBOUR_RADIUS), 1.0).multiply(W_COHESION));
        }

        // the arena: circle it at a fixed distance from the centre, and turn back at the edge
        Vec3d toCenter = new Vec3d(arenaCenter.x - pos.x, 0, arenaCenter.z - pos.z);
        double dist = toCenter.length();
        if (dist > 1.0E-3) {
            Vec3d inward = toCenter.multiply(1.0 / dist);
            Vec3d tangent = new Vec3d(inward.z, 0, -inward.x);
            double ring = arenaRadius * RING_FRACTION;
            force = force.add(tangent.multiply(W_CIRCLE * (wanderer ? 0.3 : 1.0)))
                    .add(inward.multiply(MathHelper.clamp((dist - ring) / arenaRadius, -1.0, 1.0) * W_RING));
            if (dist > arenaRadius * 0.95) force = force.add(inward.multiply(W_BOUNDS));
        }
        double wantedY = arenaCenter.y + cruiseHeight + Math.sin(ticks * 0.03 + wanderPhase) * 0.8;
        force = force.add(0, MathHelper.clamp(wantedY - pos.y, -1.0, 1.0) * W_HEIGHT, 0);

        if (wanderer) {
            force = force.add(new Vec3d(Math.cos(ticks * 0.045 + wanderPhase),
                    Math.sin(ticks * 0.031 + wanderPhase * 2.0) * 0.3,
                    Math.sin(ticks * 0.052 + wanderPhase)).multiply(W_WANDER));
        }

// whoever carries the hatred effect is hunted by everything except the red sword that gave it
        double wantedSpeed = wanderer ? WANDERER_SPEED : BOID_SPEED;
        LivingEntity hated = kind == Kind.RED ? null : hatedTarget(world);
        if (hated != null) {
            force = force.multiply(0.25).add(directionTo(hated).multiply(W_HATRED));
            wantedSpeed = HATRED_SPEED;
        } else if (wanderer) {
            LivingEntity near = nearestTarget(world, WANDERER_SEEK_RANGE);
            if (near != null) force = force.add(directionTo(near).multiply(W_SEEK));
        }

        if (force.lengthSquared() > 1.0E-6) steer(force, BOID_TURN);
        speed += MathHelper.clamp(wantedSpeed - speed, -0.03, 0.03);
    }

    private Vec3d directionTo(LivingEntity e) {
        Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(getPos());
        return to.lengthSquared() < 1.0E-6 ? Vec3d.ZERO : to.normalize();
    }

    private static Vec3d limit(Vec3d v, double max) {
        double length = v.length();
        return length > max ? v.multiply(max / length) : v;
    }

    @Nullable
    private LivingEntity nearestTarget(ServerWorld world, double range) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(range), this::canStab)) {
            double d = e.squaredDistanceTo(this);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    /** The nearest valid target that carries the hatred effect, looked up every few ticks. */
    @Nullable
    private LivingEntity hatedTarget(ServerWorld world) {
        if (ticks % 5 == 0) {
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class,
                    new Box(arenaCenter, arenaCenter).expand(arenaRadius + 12.0), this::canStab)) {
                if (!e.hasStatusEffect(ModEffects.HATRED)) continue;
                double d = e.squaredDistanceTo(this);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = e;
                }
            }
            hatedId = best == null ? null : best.getUuid();
        }
        if (hatedId != null && world.getEntity(hatedId) instanceof LivingEntity e
                && e.isAlive() && e.hasStatusEffect(ModEffects.HATRED)) {
            return e;
        }
        return null;
    }
// ---- driven by a controller, and coming home ----

    /** Starts in the mode where a controller (the sword star) puts the sword wherever it wants, every tick. */
    public void startDriven(Vec3d from) {
        this.mode = Mode.DRIVEN;
        this.lastDriveTick = ticks;
        dataTracker.set(SPIN_END, 0);   // it does not turn on itself while a controller moves it
        setPosition(from);
    }

    /** For a controller (the sword star): puts the sword at pos with its blade along aimDirection and its broad face looking along faceNormal. */
    public void driveTo(Vec3d pos, Vec3d aimDirection, Vec3d faceNormal, boolean hits) {
        if (!(getWorld() instanceof ServerWorld world)) return;
        if (hits) pierce(world, getPos(), pos);
        setPosition(pos);
        // no velocity: the clients carry on along the last step, which on a fast circle throws the swords outward
        setVelocity(Vec3d.ZERO);
        syncAim(aimDirection);
        dataTracker.set(ROLL_NORMAL, faceNormal.toVector3f());   // only sent when it changes
        lastDriveTick = ticks;
    }
    /** The flock is sent at this target for this many ticks: this sword stops flocking and goes for it. Then it flocks again. */
    public void startCharge(LivingEntity target, int duration) {
        if (mode != Mode.BOID) return;
        chargeId = target.getUuid();
        chargeUntil = ticks + duration;
    }

    @Nullable
    private LivingEntity chargeTarget(ServerWorld world) {
        if (chargeId == null) return null;
        if (ticks >= chargeUntil) {
            chargeId = null;
            return null;
        }
        return world.getEntity(chargeId) instanceof LivingEntity living && living.isAlive() ? living : null;
    }
    /**
     * This sword goes home to the boss's body when its work is done: at recallAt, when it stops in a block for a while,
     * when it flies too far, or when it is retired. Call after configure(). The boss does not regrow it while it is away.
     */
    public void markReturning(int recallAt) {
        this.returning = true;
        this.recallAt = recallAt;
        dataTracker.set(LIFE_END, Integer.MAX_VALUE);   // it never fades by itself: it goes home instead
        if (getOwner() instanceof SwordPool pool) {
            pool.lendSword();
            this.lent = true;
        }
    }

    /** Calls the sword home now. */
    public void recall() {
        if (returning && mode != Mode.RETURN) startReturn();
    }

    private void startReturn() {
        mode = Mode.RETURN;
        stuck = false;
        targets = e -> false;   // a sword on its way home hurts nobody
        speed = 0.25;
        returnTicks = 0;
        dataTracker.set(LIFE_END, ticks + RETURN_TIMEOUT);
        dataTracker.set(ROLL_NORMAL, new Vector3f());
        dataTracker.set(STAR_COUNT, 0);
    }

    @Nullable
    private Vec3d tickDriven(ServerWorld world, @Nullable Entity owner) {
        if (owner == null || !owner.isAlive()) {
            vanish(world);
            return null;
        }
        if (ticks - lastDriveTick > 5) startReturn();   // nobody moves it any more: go home
        return null;
    }

    @Nullable
    private Vec3d tickReturn(ServerWorld world, @Nullable Entity owner) {
        if (!(owner instanceof SwordPool pool) || !owner.isAlive()) {
            vanish(world);
            return null;
        }
        returnTicks++;
        Vec3d to = pool.returnPosition().subtract(getPos());
        double distance = to.length();

        if (distance < RETURN_ARRIVE) {
            arrived = true;
            pool.takeBack();
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.02);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.HOSTILE, 0.5F, 1.4F);
            discard();
            return null;
        }
        steer(to, RETURN_TURN);
        speed = Math.min(Math.min(RETURN_SPEED, 0.25 + returnTicks * 0.06), 0.3 + distance * 0.25);   // speeds up, slows down near home
        moveTo(getPos().add(direction.multiply(Math.min(speed, distance))));
        // the blade swings to hang tip down as it nears the body
        return distance < 3.0 ? direction.lerp(DOWN, 1.0 - distance / 3.0) : direction;
    }

    @Override
    public void remove(RemovalReason reason) {
        // a sword that was lent and vanished without coming back: the boss stops waiting for it
        if (!getWorld().isClient && lent && !arrived && getOwner() instanceof SwordPool pool) pool.forgetLent();
        super.remove(reason);
    }
    // ---- moving, piercing, hitting blocks ----

    private void advance(ServerWorld world, boolean bounceOffBlocks) {
        Vec3d start = getPos();
        Vec3d end = start.add(direction.multiply(speed));

        BlockHitResult block = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        boolean blocked = block.getType() != HitResult.Type.MISS;

        pierce(world, start, blocked ? block.getPos() : end);

        if (!blocked) {
            moveTo(end);
        } else if (bounceOffBlocks) {
            Vec3d normal = Vec3d.of(block.getSide().getVector());
            direction = direction.subtract(normal.multiply(2.0 * direction.dotProduct(normal))).normalize();
            setPosition(block.getPos().add(normal.multiply(0.2)));
            setVelocity(direction.multiply(speed));   // the clients carry on along the new direction
            world.spawnParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.2);
        } else {
            stuck = true;
            dataTracker.set(SPIN_END, ticks);
            setPosition(block.getPos().add(direction.multiply(0.2)));
            setVelocity(Vec3d.ZERO);                  // and they stop here
            if (returning) recallAt = Math.min(recallAt, ticks + RETURN_HOLD_TICKS);   // it hangs in the block a while, then goes home
            else dataTracker.set(LIFE_END, Math.min(getLifeEnd(), ticks + STUCK_TICKS));
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.1);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_TRIDENT_HIT_GROUND,
                    SoundCategory.HOSTILE, 0.8F, 1.3F);
        }
    }

    /** Hits everything the sword passes through between from and to. Each target is hit again only after hitCooldown ticks. */
    private void pierce(ServerWorld world, Vec3d from, Vec3d to) {
        Box sweep = new Box(from, to).expand(0.8);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, sweep, this::canStab)) {
            Box hitbox = e.getBoundingBox().expand(0.3);
            if (!hitbox.contains(from) && hitbox.raycast(from, to).isEmpty()) continue;
            if (ticks < nextHit.getOrDefault(e.getUuid(), 0)) continue;

            nextHit.put(e.getUuid(), ticks + hitCooldown);
            e.timeUntilRegen = 0;   // several swords can hit on the same tick, and each hit counts
            e.damage(getDamageSources().thrown(this, getOwner()), damage);
            applyHitEffect(e);
            world.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getBodyY(0.5), e.getZ(), 6, 0.2, 0.3, 0.2, 0.3);
            world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ITEM_TRIDENT_HIT,
                    SoundCategory.HOSTILE, 0.8F, 1.5F);
        }
    }

    /** What the black and red swords leave behind. */
    private void applyHitEffect(LivingEntity e) {
        switch (kind) {
            case BLACK -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0));
            case RED -> e.addStatusEffect(new StatusEffectInstance(ModEffects.HATRED, 200, 0));
            default -> {}
        }
    }

    private boolean canStab(LivingEntity e) {
        return e != getOwner() && e.isAlive() && !e.isSpectator() && targets.test(e);
    }

    // ---- small helpers ----

    private void announceAppearance(ServerWorld world) {
        if (ticks != getAppearAt()) return;
        world.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.02);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.HOSTILE, 1.2F, 0.8F + 0.015F * (slot % 30));
    }

    /** The aim only goes to the clients when it has changed noticeably. */
    private void syncAim(Vec3d wanted) {
        if (wanted.lengthSquared() < 1.0E-6) return;
        Vec3d n = wanted.normalize();
        if (n.dotProduct(syncedAim) < 0.995) {
            dataTracker.set(AIM, n.toVector3f());
            syncedAim = n;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}