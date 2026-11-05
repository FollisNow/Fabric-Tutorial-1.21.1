package net.follis.goldenscarabmod.entity.swarm;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Client-side simulation of all the sheets of one boss. Runs once per game tick
 * (called from GoldenSwarmEntity#tick) and keeps the previous tick's values so the
 * renderer can interpolate with partialTick. No client-only classes are imported,
 * so it is safe to load on a dedicated server (it just never gets instantiated there).
 *
 * Every sheet is a damped spring chasing the target given by the active formation.
 * The server only announces "pattern #seq, shape X, aimed at P" through the entity's
 * tracked data; the client starts the matching formation and runs it on its own clock.
 */
public class SwarmState {

    public static final int SHEET_COUNT = 1500;

    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float MAX_SPEED = 3.2f;      // blocks per tick
    private static final float FOLLOW = 0.6f;         // share of the boss' own movement given to the sheets for free
    private static final float SCATTER_RADIUS = 3.0f; // spawn animation: sheets start scattered and fly in

    // health scaling: the boss sheds sheets as it gets hurt
    private static final float MIN_SHEET_FRACTION = 0.25f; // never fewer than this share of its sheets, however low its health
    // Everything that sheds or collapses is paced relative to the number of sheets (these numbers were tuned for 520),
    // so the cascades take the same time whatever SHEET_COUNT is.
    private static final float COUNT_SCALE = SHEET_COUNT / 520f;
    private static final int SHED_PER_TICK = Math.round(10 * COUNT_SCALE);       // a big hit sheds sheets in a cascade of this many per tick
    private static final int DISSOLVE_PER_TICK = Math.round(30 * COUNT_SCALE);   // splitting the swarm with a clone
    private static final int COLLAPSE_PER_TICK = Math.round(40 * COUNT_SCALE);   // death
    private static final int DEBRIS_TICKS = 40;            // a shed sheet falls, lies on the ground, then shrinks away

    // phase-change flurry: purely cosmetic sheets thrown off the body (the body itself loses nothing)
    private static final int FX_POOL = 128;
    private static final int FLURRY_WAVES = 5;             // one wave per chain-break sound...
    private static final int FLURRY_WAVE_INTERVAL = 4;     // ...which the server plays every 4 ticks
    private static final int FLURRY_WAVE_SHEETS = 18;

    public static final class Sheet {
        // world position, current and previous tick
        public double x, y, z, px, py, pz;
        public float vx, vy, vz;
        // orientation: surface normal + rotation around it
        public float nx, ny = 1f, nz, pnx, pny = 1f, pnz;
        public float spin, pspin;

        // look
        public float halfW, halfH;
        public int texU, texV;               // top-left pixel of the 4x4 patch sampled from the texture
        public float ao;                     // base "ambient occlusion" shade
        public final float[] corner = new float[4];
        public boolean eye;

        // health scaling: a shed sheet turns into debris, then disappears
        public float scale = 1f, pscale = 1f;   // size multiplier; 0 = invisible (gone, or just starting to grow back)
        boolean active = true;
        int dying;                              // ticks of debris life left
        boolean dissolve;                       // true: shrinks away in place instead of falling (swarm split with a clone)
        float groundY, spinVel;

        // behaviour
        float stiffness, damping, noiseAmp, phase;
        double ptx, pty, ptz;                // last tick's target, for feed-forward
    }

    public final Sheet[] sheets = new Sheet[SHEET_COUNT];
    public final Sheet[] fxSheets = new Sheet[FX_POOL];   // cosmetic debris, drawn like the others
    private final Telegraph telegraph = new Telegraph();
    public final Sheet[] telegraphSheets = telegraph.sheets;   // red ground markers, drawn in the glowing pass

    private final SwarmFormation.Context ctx = new SwarmFormation.Context();
    private final SwarmFormation.Target target = new SwarmFormation.Target();
    private final Random rng;
    private final HumanoidFormation humanoid;
    private SwarmFormation formation;
    private PatternFormation attack;
    private GoldenSwarmEntity owner;
    private final HashMap<Long, Double> groundCache = new HashMap<>();
    private int groundCacheAge;
    private final SwarmFormation.Ground groundProbe = new SwarmFormation.Ground() {
        @Override
        public double heightAt(double x, double y, double z) {
            return probeGround(x, y, z);
        }

        @Override
        public double clearDistance(double x, double y, double z, double dx, double dy, double dz, double max) {
            return probeClear(x, y, z, dx, dy, dz, max);
        }
    };
    private int activeSeq = -1;
    private int patternTick;
    private boolean ready;
    private double lastX, lastY, lastZ;

    private final int[] removalOrder;   // non-eye sheets in the order they fall off as health drops
    private final int nonEye;
    private int removed;                // how many of removalOrder are currently gone
    private float lastShare = 1f;       // share of the swarm this body owned last tick
    private int lastPhase = -1;         // boss phase seen last tick
    private int flurryWavesLeft, flurryTimer;
    private int shockTick = -1;         // ticks into the phase-change shockwave rings (-1 = none)
    private long chainSeed;             // seed of the first shape of a chain: the next shapes reuse it so they use the same sheets
    private boolean dissolving;         // the share just dropped (a clone took half): sheets shrink away instead of falling

    public SwarmState(long seed) {
        this.rng = new Random(seed);
        this.humanoid = new HumanoidFormation(SHEET_COUNT, seed);
        this.formation = humanoid;
        this.ctx.ground = groundProbe;
        this.telegraph.ground = groundProbe;
        this.ctx.telegraph = telegraph;

        for (int i = 0; i < SHEET_COUNT; i++) {
            Sheet s = new Sheet();
            s.eye = humanoid.isEye(i);

            if (s.eye) {
                s.halfW = 0.075f;
                s.halfH = 0.032f;
                s.stiffness = 0.45f;
                s.damping = 0.5f;
                s.noiseAmp = 0f;
                s.ao = 1f;
                for (int k = 0; k < 4; k++) s.corner[k] = 1f;
                s.texU = 4;
                s.texV = 4;
            } else {
                float base = 0.075f + rng.nextFloat() * 0.06f;
                s.halfW = base * (0.8f + rng.nextFloat() * 0.5f);
                s.halfH = base * (0.8f + rng.nextFloat() * 0.5f);
                float looseness = humanoid.isOrbiter(i) ? 0.6f : 1f;
                s.stiffness = (0.22f + rng.nextFloat() * 0.18f) * looseness;
                s.damping = 0.55f + rng.nextFloat() * 0.12f;
                s.noiseAmp = 0.012f + rng.nextFloat() * 0.03f;
                s.phase = rng.nextFloat() * TAU;
                // fake ambient occlusion: sheets buried under others are darker,
                // plus a random darkening per corner for a soft gradient on each sheet
                s.ao = 0.62f + 0.38f * humanoid.depth(i);
                for (int k = 0; k < 4; k++) s.corner[k] = 0.82f + rng.nextFloat() * 0.18f;
                s.texU = rng.nextInt(13);
                s.texV = rng.nextInt(13);
            }
            sheets[i] = s;
        }

        // random order, so the body thins out evenly instead of losing one limb first
        int[] pool = new int[SHEET_COUNT];
        int n = 0;
        for (int i = 0; i < SHEET_COUNT; i++) {
            if (!sheets[i].eye) pool[n++] = i;
        }
        for (int k = n - 1; k > 0; k--) {
            int j = rng.nextInt(k + 1);
            int tmp = pool[k];
            pool[k] = pool[j];
            pool[j] = tmp;
        }
        this.nonEye = n;
        this.removalOrder = Arrays.copyOf(pool, n);

        for (int i = 0; i < FX_POOL; i++) {
            Sheet f = new Sheet();
            f.active = false;
            f.scale = 0f;      // invisible until a flurry uses it
            f.pscale = 0f;
            fxSheets[i] = f;
        }
    }

    public boolean isReady() {
        return ready;
    }

    /** Where is the floor below this point? Used when shattered sheets fall and lie on the ground. */
    private double probeGround(double x, double y, double z) {
        if (owner == null) return y - 0.9;

        // Hundreds of sheets land in the same few spots, so remember the answers: one raycast per half-block cell
        // (half blocks, so stairs and slabs still read right) and per 3-block height band (floors above each other).
        long key = (((long) Math.floor(x * 2.0) & 0x3FFFFFFL) << 38)
                | (((long) Math.floor(z * 2.0) & 0x3FFFFFFL) << 12)
                | ((long) Math.floor(y / 3.0) & 0xFFFL);
        Double known = groundCache.get(key);
        if (known != null) return known;

        World world = owner.getWorld();
        Vec3d from = new Vec3d(x, y + 1.5, z);
        Vec3d to = new Vec3d(x, y - 12.0, z);
        BlockHitResult hit = world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        double floor = hit.getType() == HitResult.Type.MISS ? y - 0.9 : hit.getPos().y;
        groundCache.put(key, floor);
        return floor;
    }

    /** Free distance along a unit direction from a point, capped at max (exploding shards stop at walls). */
    private double probeClear(double x, double y, double z, double dx, double dy, double dz, double max) {
        if (owner == null) return max;
        Vec3d from = new Vec3d(x, y, z);
        Vec3d to = from.add(dx * max, dy * max, dz * max);
        BlockHitResult hit = owner.getWorld().raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        return hit.getType() == HitResult.Type.MISS ? max : from.distanceTo(hit.getPos());
    }

    // ------------------------------------------------------------------ health scaling

    /**
     * How many sheets should currently be gone. Health is already synced to the clients by vanilla,
     * so nothing is sent for this. The boss keeps MIN_SHEET_FRACTION of its sheets at the lowest health,
     * and drops all of them (eyes aside) when it dies.
     */
    private int desiredRemoved(GoldenSwarmEntity e) {
        boolean dead = e.isDead();
        float hp = dead ? 0f : MathHelper.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
        float floor = dead ? 0f : MIN_SHEET_FRACTION;
        float share = MathHelper.clamp(e.getSheetShare(), 0f, 1f);   // 0.5 while a clone owns the other half
        float fraction = share * (floor + (1f - floor) * hp);
        return nonEye - Math.round(fraction * nonEye);
    }

    private void updateHealth(GoldenSwarmEntity e) {
        float share = e.getSheetShare();
        if (share < lastShare - 0.001f) dissolving = true;   // half the swarm went to a clone: shrink away, do not fall
        lastShare = share;

        int want = desiredRemoved(e);
        int budget = e.isDead() ? COLLAPSE_PER_TICK : (dissolving ? DISSOLVE_PER_TICK : SHED_PER_TICK);   // death: the whole swarm collapses quickly
        while (removed < want && budget-- > 0) shed(sheets[removalOrder[removed++]], dissolving);
        while (removed > want && budget-- > 0) restore(sheets[removalOrder[--removed]]);
        if (removed >= want) dissolving = false;
    }

    /** The sheet falls off the boss: it is thrown outward a little, then drops to the floor as debris. */
    private void shed(Sheet s, boolean dissolve) {
        s.active = false;
        s.dissolve = dissolve;
        s.dying = dissolve ? 8 : DEBRIS_TICKS;
        if (dissolve) {
            s.vx = s.vy = s.vz = 0f;
            return;   // it just shrinks away where it is
        }

        double ox = s.x - ctx.x, oz = s.z - ctx.z;
        float ol = (float) Math.sqrt(ox * ox + oz * oz);
        float dirX, dirZ;
        if (ol < 1e-3f) {
            float a = rng.nextFloat() * TAU;
            dirX = MathHelper.cos(a);
            dirZ = MathHelper.sin(a);
        } else {
            dirX = (float) (ox / ol);
            dirZ = (float) (oz / ol);
        }
        float push = 0.08f + rng.nextFloat() * 0.2f;
        s.vx = dirX * push;
        s.vz = dirZ * push;
        s.vy = 0.08f + rng.nextFloat() * 0.2f;
        s.spinVel = (rng.nextFloat() - 0.5f) * 0.6f;
        s.groundY = (float) probeGround(s.x, s.y, s.z);
    }

    /** The boss healed: the sheet grows back at its chest and flies to its place from there. */
    private void restore(Sheet s) {
        s.active = true;
        s.dissolve = false;
        s.dying = 0;
        s.scale = 0f;
        s.pscale = 0f;
        s.x = ctx.x + (rng.nextFloat() - 0.5f) * 0.6;
        s.y = ctx.y + 1.4 + (rng.nextFloat() - 0.5f) * 0.6;
        s.z = ctx.z + (rng.nextFloat() - 0.5f) * 0.6;
        s.px = s.x;
        s.py = s.y;
        s.pz = s.z;
        s.vx = s.vy = s.vz = 0f;
        s.ptx = Double.NaN;   // no previous target yet: no feed-forward on its first tick
        s.pty = Double.NaN;
        s.ptz = Double.NaN;
    }

    private void stepDebris(Sheet s) {
        if (s.dying <= 0) {
            s.scale = 0f;
            return;
        }
        s.dying--;

        if (s.dissolve) {
            s.scale = Math.max(0f, s.dying / 8f);   // shrinks away where it is
            return;
        }

        if (s.y > s.groundY + 0.03) {
            // falling: gravity, air drag, tumbling
            s.vy -= 0.05f;
            s.vx *= 0.94f;
            s.vz *= 0.94f;
            s.vy *= 0.98f;
            s.x += s.vx;
            s.y += s.vy;
            s.z += s.vz;
            s.spin += s.spinVel;
            if (s.y <= s.groundY + 0.03) {
                s.y = s.groundY + 0.03;
                s.vx = s.vy = s.vz = 0f;
            }
        } else {
            // landed: lie flat on the ground
            s.y = s.groundY + 0.03;
            s.nx += (0f - s.nx) * 0.3f;
            s.ny += (1f - s.ny) * 0.3f;
            s.nz += (0f - s.nz) * 0.3f;
            float nl = (float) Math.sqrt(s.nx * s.nx + s.ny * s.ny + s.nz * s.nz);
            if (nl > 1e-4f) {
                s.nx /= nl;
                s.ny /= nl;
                s.nz /= nl;
            }
        }

        if (s.dying < 10) s.scale = s.dying / 10f;   // shrink away at the end
    }

    /**
     * The shape attack. The second and third shape of a chain reuse the first one's seed, so they are made of the
     * same sheets: the swarm flows from one shape into the next instead of going home in between.
     */
    private ShapeAttackFormation newShapeAttack(GoldenSwarmEntity e) {
        int variant = e.getPatternVariant();
        boolean continuing = (variant & ShapeAttackFormation.VARIANT_SHORT_WINDUP) != 0 && chainSeed != 0L;
        long seed = continuing ? chainSeed : rng.nextLong();
        chainSeed = seed;
        return new ShapeAttackFormation(humanoid, SHEET_COUNT, e.getPatternShape(), seed, variant);
    }

    // --------------------------------------------------------------- phase-change flurry

    /**
     * When the boss drops into a new phase (75%, 25% of its health) a flurry of gold sheets is thrown off its body,
     * matching the chain-break sounds the server plays. The sheets are extra cosmetic ones: the body keeps all of its own.
     */
    private void updateFlurry(GoldenSwarmEntity e) {
        if (!e.isClone() && !e.isDead()) {
            int phase = e.getPhase();
            if (lastPhase >= 0 && phase > lastPhase) {
                flurryWavesLeft = FLURRY_WAVES;
                flurryTimer = 0;
                shockTick = 0;
            }
            lastPhase = phase;   // follows healing back up, so crossing a threshold again triggers it again
        }

        if (flurryWavesLeft > 0 && --flurryTimer <= 0) {
            for (int k = 0; k < FLURRY_WAVE_SHEETS; k++) spawnFlurrySheet();
            flurryWavesLeft--;
            flurryTimer = FLURRY_WAVE_INTERVAL;
        }
    }

    /** One cosmetic sheet, copied from a random sheet of the body and thrown off it harder than ordinary shedding. */
    private void spawnFlurrySheet() {
        Sheet fx = null;
        for (Sheet candidate : fxSheets) {
            if (candidate.dying <= 0 && candidate.scale <= 0.02f) {
                fx = candidate;
                break;
            }
        }
        if (fx == null) return;   // pool exhausted: skip

        Sheet src = sheets[rng.nextInt(SHEET_COUNT)];
        for (int tries = 0; tries < 6 && !src.active; tries++) {
            src = sheets[rng.nextInt(SHEET_COUNT)];
        }
        if (!src.active) return;

        fx.x = src.x;
        fx.y = src.y;
        fx.z = src.z;
        fx.px = fx.x;
        fx.py = fx.y;
        fx.pz = fx.z;
        fx.nx = src.nx;
        fx.ny = src.ny;
        fx.nz = src.nz;
        fx.pnx = fx.nx;
        fx.pny = fx.ny;
        fx.pnz = fx.nz;
        fx.spin = src.spin;
        fx.pspin = fx.spin;
        fx.halfW = src.halfW;
        fx.halfH = src.halfH;
        fx.texU = src.texU;
        fx.texV = src.texV;
        fx.ao = src.ao;
        System.arraycopy(src.corner, 0, fx.corner, 0, 4);
        fx.scale = 1f;
        fx.pscale = 1f;

        shed(fx, false);        // flung outward, then falls and lies on the ground like shed sheets do
        fx.vx *= 2.2f;
        fx.vz *= 2.2f;
        fx.vy += 0.15f;
    }

    private void stepFx() {
        for (Sheet f : fxSheets) {
            f.px = f.x;
            f.py = f.y;
            f.pz = f.z;
            f.pnx = f.nx;
            f.pny = f.ny;
            f.pnz = f.nz;
            f.pspin = f.spin;
            f.pscale = f.scale;
            if (f.dying > 0 || f.scale > 0.02f) stepDebris(f);   // idle pool entries cost nothing
        }
    }

    /** Force a formation (debug / future patterns). Attack patterns are started via tracked data. */
    public void setFormation(SwarmFormation formation) {
        this.formation = formation;
    }

    private void init(GoldenSwarmEntity e) {
        ctx.update(e);
        boolean fromOrigin = e.isClone();
        Vector3f origin = e.getCloneOrigin();
        float bodyScale = ctx.scale;
        lastShare = e.getSheetShare();
        lastPhase = e.getPhase();   // no flurry for the phase the boss starts in
        formation.prepare(ctx);
        lastX = ctx.x;
        lastY = ctx.y;
        lastZ = ctx.z;

        for (int i = 0; i < SHEET_COUNT; i++) {
            Sheet s = sheets[i];
            formation.target(i, target);

            if (fromOrigin) {
                // a clone: its sheets come from the boss' chest and fly over to it
                s.x = origin.x + (rng.nextFloat() - 0.5f) * 0.8f;
                s.y = origin.y + (rng.nextFloat() - 0.5f) * 0.8f;
                s.z = origin.z + (rng.nextFloat() - 0.5f) * 0.8f;
            } else {
                // start scattered above the final position, they assemble on their own
                float a = rng.nextFloat() * TAU;
                float r = 0.5f + rng.nextFloat() * SCATTER_RADIUS;
                s.x = target.x + Math.cos(a) * r;
                s.y = target.y + 0.5 + rng.nextFloat() * SCATTER_RADIUS;
                s.z = target.z + Math.sin(a) * r;
            }
            s.halfW *= bodyScale;   // a clone's sheets are smaller too
            s.halfH *= bodyScale;
            s.vx = s.vy = s.vz = 0f;
            s.ptx = target.x;
            s.pty = target.y;
            s.ptz = target.z;

            float nx = rng.nextFloat() - 0.5f, ny = rng.nextFloat() - 0.5f, nz = rng.nextFloat() - 0.5f;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-4f;
            s.nx = nx / len;
            s.ny = ny / len;
            s.nz = nz / len;
            s.spin = rng.nextFloat() * TAU;

            s.px = s.x;
            s.py = s.y;
            s.pz = s.z;
            s.pnx = s.nx;
            s.pny = s.ny;
            s.pnz = s.nz;
            s.pspin = s.spin;
        }

        // start with the right number of sheets for the current health: no shedding animation on load
        int startRemoved = desiredRemoved(e);
        for (int k = 0; k < startRemoved; k++) {
            Sheet gone = sheets[removalOrder[k]];
            gone.active = false;
            gone.dying = 0;
            gone.scale = 0f;
            gone.pscale = 0f;
        }
        removed = startRemoved;
        ready = true;
    }

    public void tick(GoldenSwarmEntity e) {
        this.owner = e;
        if (++groundCacheAge >= 40) {   // the world can change: forget the floor heights every two seconds
            groundCacheAge = 0;
            groundCache.clear();
        }
        if (!ready) {
            init(e);
            return;
        }

        ctx.update(e);
        for (int k = 0; k < TentacleFormation.TENTACLES; k++) {   // the tentacles' own lash commands
            ctx.tentacleSeq[k] = e.getTentacleSeq(k);
            Vector3f a = e.getTentacleAim(k);
            ctx.tentacleAim[k][0] = a.x;
            ctx.tentacleAim[k][1] = a.y;
            ctx.tentacleAim[k][2] = a.z;
        }
        updateHealth(e);
        updateFlurry(e);

        // did the server announce a new attack?
        int seq = e.getPatternSeq();
        if (e.getPatternId() != GoldenSwarmEntity.PATTERN_NONE && seq != activeSeq) {
            activeSeq = seq;
            patternTick = 0;
            PatternFormation next = switch (e.getPatternId()) {
                case GoldenSwarmEntity.PATTERN_SHAPE_THROW -> newShapeAttack(e);
                case GoldenSwarmEntity.PATTERN_BURST ->
                        new BurstFormation(humanoid, SHEET_COUNT, rng.nextLong());
                case GoldenSwarmEntity.PATTERN_TRANSLOCATE ->
                        new SwirlFormation(humanoid, SHEET_COUNT, rng.nextLong());
                case GoldenSwarmEntity.PATTERN_CLONE ->
                        new CloneSummonFormation(humanoid, SHEET_COUNT, rng.nextLong());
                case GoldenSwarmEntity.PATTERN_RECOMBINE ->
                        new RecombineFormation(SHEET_COUNT, rng.nextLong());
                case GoldenSwarmEntity.PATTERN_ARM_SLAM ->
                        new ArmSlamFormation(humanoid, SHEET_COUNT, rng.nextLong());
                case GoldenSwarmEntity.PATTERN_TENTACLES ->
                        new TentacleFormation(humanoid, SHEET_COUNT, rng.nextLong(), e.getPatternVariant());
                default -> null;
            };
            if (next != null) {
                attack = next;
                formation = next;
            }
        }
        if (attack != null) {
            if (attack.isFinished()) {
                attack = null;
                formation = humanoid;
                humanoid.setArmRaise(0f);
            } else {
                Vector3f tg = e.getPatternTarget();
                ctx.targetX = tg.x;
                ctx.targetY = tg.y;
                ctx.targetZ = tg.z;
                ctx.patternFlight = e.getPatternFlight();
                ctx.patternBreaks = e.getPatternBreaks();
                ctx.patternTick = patternTick++;
            }
        }

        double dx = ctx.x - lastX, dy = ctx.y - lastY, dz = ctx.z - lastZ;
        lastX = ctx.x;
        lastY = ctx.y;
        lastZ = ctx.z;
        // teleported / big jump: carry the sheets along completely instead of lagging behind
        float follow = (dx * dx + dy * dy + dz * dz) > 36.0 ? 1f : FOLLOW;

        telegraph.begin();
        formation.prepare(ctx);
        if (shockTick >= 0) {   // phase change: a ring expands from the boss with each of the five chain sounds
            float p = (shockTick % GoldenSwarmEntity.ALERT_INTERVAL + 1) / (float) GoldenSwarmEntity.ALERT_INTERVAL;
            telegraph.ring(ctx.x, ctx.y, ctx.z, GoldenSwarmEntity.REPEL_RADIUS * (0.25 + 0.75 * p), 48, 1f - 0.4f * p, 0f);
            if (++shockTick >= GoldenSwarmEntity.ALERT_SOUNDS * GoldenSwarmEntity.ALERT_INTERVAL) shockTick = -1;
        }
        telegraph.end();
        if (ctx.slam) {
            ctx.slam = false;
            if (SwarmFx.slamHandler != null) {   // set by the client (the renderer); nothing happens on a server
                SwarmFx.slamHandler.slam(ctx.slamX, ctx.slamY, ctx.slamZ, ctx.slamStrength, ctx.slamRadius);
            }
        }
        float t = ctx.time;

        for (int i = 0; i < SHEET_COUNT; i++) {
            Sheet s = sheets[i];

            // remember last tick for render interpolation
            s.px = s.x;
            s.py = s.y;
            s.pz = s.z;
            s.pnx = s.nx;
            s.pny = s.ny;
            s.pnz = s.nz;
            s.pspin = s.spin;
            s.pscale = s.scale;

            // a shed sheet is debris: it falls, lies on the ground and shrinks away
            if (!s.active) {
                stepDebris(s);
                continue;
            }
            if (s.scale < 1f) s.scale = Math.min(1f, s.scale + 0.1f);   // growing back after the boss healed
            float limit = formation.scaleLimit(i);   // a formation can shrink sheets away (a recombining clone)
            if (limit < s.scale) s.scale = limit;

            // the boss moved: drag the sheets along (the rest of the distance is the trailing lag)
            s.x += dx * follow;
            s.y += dy * follow;
            s.z += dz * follow;

            formation.target(i, target);

            // feed-forward: when the target itself is moving fast (thrown shape), push the sheet
            // with it so it does not trail far behind. (1/damping - 1) cancels the damping lag.
            float ff = formation.feedForward(i);
            float fx = 0f, fy = 0f, fz = 0f;
            if (ff > 0f && !Double.isNaN(s.ptx)) {
                float g = ff * (1f / s.damping - 1f);
                fx = (float) (target.x - s.ptx) * g;
                fy = (float) (target.y - s.pty) * g;
                fz = (float) (target.z - s.ptz) * g;
            }
            s.ptx = target.x;
            s.pty = target.y;
            s.ptz = target.z;

            double tx = target.x, ty = target.y, tz = target.z;
            float na = s.noiseAmp * formation.noiseScale(i);
            if (na > 0f) {
                // cheap shimmer so the swarm never looks frozen (the formation can quiet it down)
                tx += na * Math.sin(t * 0.31f + s.phase);
                ty += na * Math.sin(t * 0.27f + s.phase * 1.7f);
                tz += na * Math.sin(t * 0.23f + s.phase * 2.3f);
            }

            // damped spring toward the target
            float k = s.stiffness * formation.stiffnessScale(i);
            s.vx = (s.vx + (float) (tx - s.x) * k + fx) * s.damping;
            s.vy = (s.vy + (float) (ty - s.y) * k + fy) * s.damping;
            s.vz = (s.vz + (float) (tz - s.z) * k + fz) * s.damping;

            float sp2 = s.vx * s.vx + s.vy * s.vy + s.vz * s.vz;
            if (sp2 > MAX_SPEED * MAX_SPEED) {
                float c = MAX_SPEED / (float) Math.sqrt(sp2);
                s.vx *= c;
                s.vy *= c;
                s.vz *= c;
            }
            s.x += s.vx;
            s.y += s.vy;
            s.z += s.vz;

            // orientation: chase the target normal, plus a flutter proportional to velocity
            float tnx = target.nx + s.vx * 0.6f;
            float tny = target.ny + s.vy * 0.6f;
            float tnz = target.nz + s.vz * 0.6f;
            float tl = (float) Math.sqrt(tnx * tnx + tny * tny + tnz * tnz);
            if (tl > 1e-4f) {
                tnx /= tl;
                tny /= tl;
                tnz /= tl;
            }
            s.nx += (tnx - s.nx) * 0.25f;
            s.ny += (tny - s.ny) * 0.25f;
            s.nz += (tnz - s.nz) * 0.25f;
            float nl = (float) Math.sqrt(s.nx * s.nx + s.ny * s.ny + s.nz * s.nz);
            if (nl > 1e-4f) {
                s.nx /= nl;
                s.ny /= nl;
                s.nz /= nl;
            }

            float d = target.spin - s.spin;
            d -= TAU * (float) Math.floor((d + Math.PI) / TAU);  // shortest way around
            s.spin += d * 0.2f;
        }

        stepFx();
    }
}