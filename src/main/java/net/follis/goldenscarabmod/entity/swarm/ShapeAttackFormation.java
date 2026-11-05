package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Random;

/**
 * "Geometric shape" attack. Ticks count from the start of the pattern; the server goal shares the
 * constants below so visuals and damage line up. The server also decides, when the aim locks
 * (tick LOCK), where the shape ends up, how long the flight takes and whether it shatters.
 *
 *   0 .. WINDUP      part of the swarm leaves the body and assembles the shape in front of the boss
 *   WINDUP .. impact the shape is thrown
 *   impact .. recall what happens at the end depends on the shape:
 *                      SPEAR  fast, passes through the target's legs and is planted in the ground, perfectly still
 *                      CUBE   crashes on the target and shatters: every sheet flies outward from where it was
 *                             in the cube, then falls and lies on the ground
 *                      RING   slow, perfectly horizontal, flies until a wall or max distance (entities do not
 *                             stop it). On a wall it shatters like the cube; at max distance it hovers a moment
 *   recall .. total  sheets are called back to the body one by one, spiralling in
 *
 * Sheets that do not take part (the eyes and ~35% of the body) never leave the body.
 */
public final class ShapeAttackFormation implements PatternFormation {

    public static final int SPEAR = 0, RING = 1, CUBE = 2, SHAPE_COUNT = 3;

    public static final int LOCK = 20;       // last tick the aim can change; the server also sends the final path then
    public static final int WINDUP = 30;     // the throw starts

    // Chained shapes (in the later boss phases it fires three in a row). Every shape after the first has a
    // short wind-up, and every shape that is followed by another one skips the long hold and the recall: the next
    // shape simply picks its sheets up from wherever they are.
    public static final int VARIANT_SHORT_WINDUP = 1;   // this shape follows another one: quick assembly
    public static final int VARIANT_CHAINED = 2;        // another shape follows this one: no recall afterwards
    public static final int SHORT_WINDUP = 10, SHORT_LOCK = 4;
    public static final int CHAIN_HOLD = 6;             // ticks between an impact and the next shape's wind-up
    private static final int CHAIN_WAIT = 40;           // safety: if the next shape never comes, the sheets are recalled after this

    public static int windupFor(int variant) {
        return (variant & VARIANT_SHORT_WINDUP) != 0 ? SHORT_WINDUP : WINDUP;
    }

    public static int lockFor(int variant) {
        return (variant & VARIANT_SHORT_WINDUP) != 0 ? SHORT_LOCK : LOCK;
    }
    public static final int RECALL_SPREAD = 14;
    public static final int SETTLE = 12;

    public static final float RING_SPEED = 0.8f;          // blocks per tick, slow on purpose
    public static final float RING_MAX_DISTANCE = 24f;
    public static final float CUBE_BLAST_RADIUS = 2.8f;   // the cube's blast (the server uses it too)

    // the mild screen shake when a shape lands (the boss only, never its clone)
    private static final float IMPACT_SHAKE_STRENGTH = 0.35f;
    private static final double IMPACT_SHAKE_RADIUS = 20.0;

    private static final float SPEAR_TIP = 2.2f;          // tip distance from the spear's centre
    private static final float SPEAR_EMBED = 0.4f;        // how deep the tip sinks into the ground
    private static final int OUT_TICKS = 5;               // shattered sheets fly outward this long, then fall
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float PARTICIPATION = 0.65f;     // share of the non-orbiting sheets that join the shape

    // ------------------------------------------------------------ shared with the server goal

    public static int baseFlight(int shape) {
        return switch (shape) {
            case SPEAR -> 7;     // fast
            case RING -> 20;     // placeholder, the server computes the real one from the distance
            default -> 12;
        };
    }

    /** How long the shape stays at its end point before the sheets are called back. */
    public static int holdTicks(int shape, boolean shatters) {
        if (shape == SPEAR) return 18;
        return (shape == CUBE || shatters) ? 24 : 6;
    }

    /** Same, but a shape that is followed by another one only waits a moment. */
    public static int holdTicks(int shape, boolean shatters, int variant) {
        return (variant & VARIANT_CHAINED) != 0 ? CHAIN_HOLD : holdTicks(shape, shatters);
    }

    public static double anchorHeight(int shape) {
        return shape == RING ? 1.9 : 2.6;
    }

    /** Where the shape is assembled: in front of the boss, towards the target. Identical on both sides. */
    public static Vec3d anchor(int shape, float bodyScale, double bx, double by, double bz, double tx, double tz, float bodyYaw) {
        double hx = tx - bx, hz = tz - bz;
        double hl = Math.sqrt(hx * hx + hz * hz);
        if (hl < 1e-3) {
            hx = -Math.sin(bodyYaw);
            hz = Math.cos(bodyYaw);
            hl = 1.0;
        }
        return new Vec3d(bx + hx / hl * 1.7 * bodyScale, by + anchorHeight(shape) * bodyScale, bz + hz / hl * 1.7 * bodyScale);
    }

    // ------------------------------------------------------------------------------- state

    private final HumanoidFormation body;
    private final int shape;
    private final int variant;   // VARIANT_* bits
    private final int windup;    // length of this shape's wind-up, in ticks
    private final int[] rank;            // position in the shape, -1 = stays on the body
    private final int[] assembleDelay;   // ticks before the sheet leaves the body
    private final int[] recallDelay;     // ticks after recallStart before the sheet goes home
    private final float[] swirlPhase;    // random angle per sheet (spiral, spin, direction jitter)
    private final float[] scatterDist;   // random base distance a shattered sheet travels outward
    private final float[] groundJitter;  // tiny height offset so lying sheets do not z-fight
    private final float[] groundY;       // cached floor height under each lying sheet (NaN = not looked up yet)

    // shattering: where each sheet was in the shape at the moment of impact, and its outward direction
    private final boolean[] originSet;
    private final double[] ox, oy, oz;
    private final float[] dirX, dirZ;
    private final Target scratch = new Target();

    // timeline, derived from what the server announced
    private int flight, impactTick, recallStart, total;
    private boolean shatters;

    // per-tick state
    private int t;
    private boolean aimInit, launched;
    private Ground ground;
    private double cx, cy, cz;                 // centre of the shape
    private double launchX, launchY, launchZ;  // where the throw started
    private double endX, endY, endZ;           // where the shape's centre ends up
    private float scale = 1f, spin;
    private float bodyScale = 1f;   // 1 = the boss, smaller for its clone: shapes and distances shrink with it
    private boolean shakeSent;
    private final Vector3f aim = new Vector3f(0f, 0f, 1f);
    private final Vector3f desired = new Vector3f();
    private final Vector3f right = new Vector3f(), upv = new Vector3f(), fwd = new Vector3f();
    private final Matrix3f tumble = new Matrix3f();
    private final Vector3f lp = new Vector3f(), ln = new Vector3f();

    public ShapeAttackFormation(HumanoidFormation body, int count, int shape, long seed, int variant) {
        this.body = body;
        this.shape = shape;
        this.variant = variant;
        this.windup = windupFor(variant);
        this.rank = new int[count];
        this.assembleDelay = new int[count];
        this.recallDelay = new int[count];
        this.swirlPhase = new float[count];
        this.scatterDist = new float[count];
        this.groundJitter = new float[count];
        this.groundY = new float[count];
        this.originSet = new boolean[count];
        this.ox = new double[count];
        this.oy = new double[count];
        this.oz = new double[count];
        this.dirX = new float[count];
        this.dirZ = new float[count];
        Arrays.fill(rank, -1);
        Arrays.fill(groundY, Float.NaN);

        Random r = new Random(seed);

        // pick the participants, then shuffle so the shape is fed from the whole body
        int[] pool = new int[count];
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (body.isEye(i)) continue;
            if (body.isOrbiter(i) || r.nextFloat() < PARTICIPATION) pool[n++] = i;
        }
        for (int k = n - 1; k > 0; k--) {
            int j = r.nextInt(k + 1);
            int tmp = pool[k];
            pool[k] = pool[j];
            pool[j] = tmp;
        }
        for (int k = 0; k < n; k++) rank[pool[k]] = k;

        for (int i = 0; i < count; i++) {
            // a chained shape starts at once (the sheets are already out); only a first shape staggers its assembly
            assembleDelay[i] = windup < WINDUP ? 0 : r.nextInt(14);
            recallDelay[i] = r.nextInt(RECALL_SPREAD + 1);
            swirlPhase[i] = r.nextFloat() * TAU;
            scatterDist[i] = 0.6f + 3.2f * (float) Math.sqrt(r.nextFloat());
            groundJitter[i] = r.nextFloat() * 0.05f;
        }

        configure(baseFlight(shape), shape == CUBE);
    }

    private void configure(int flightTicks, boolean breaks) {
        this.flight = Math.max(1, flightTicks);
        this.shatters = shape == CUBE || breaks;
        this.impactTick = windup + flight;
        int hold = holdTicks(shape, shatters, variant);
        // a chained shape does not recall: it waits for the next shape (and only recalls if that never comes)
        this.recallStart = impactTick + hold + ((variant & VARIANT_CHAINED) != 0 ? CHAIN_WAIT : 0);
        this.total = recallStart + RECALL_SPREAD + SETTLE;
    }

    public boolean isFinished() {
        return t >= total;
    }

    // ----------------------------------------------------------------- steering hooks

    @Override
    public float feedForward(int i) {
        return (rank[i] >= 0 && t > windup && t < impactTick) ? 0.85f : 0f;
    }

    @Override
    public float stiffnessScale(int i) {
        if (rank[i] < 0 || t < assembleDelay[i] || t >= recallStart) return 1f;
        if (shatters && t >= impactTick + OUT_TICKS) return 1f;   // falling sheets use a looser spring
        return t >= windup ? 1.8f : 1.3f;
    }

    @Override
    public float noiseScale(int i) {
        if (rank[i] < 0 || t < assembleDelay[i] || t >= recallStart + recallDelay[i]) return 1f;
        if (t >= impactTick && (shape == SPEAR || shatters)) return 0.1f;  // planted spear, sheets lying on the floor
        return t >= windup ? 0.5f : 1f;
    }

    // ------------------------------------------------------------------------ pose

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        ground = c.ground;
        // the server finalises flight time / shatter flag at tick LOCK, well before the throw
        if (!launched) configure(c.patternFlight, c.patternBreaks);

        // the boss throws its arms up while charging, keeps them out during the throw, lowers them while recalling
        float raise;
        if (t < windup) raise = (variant & VARIANT_SHORT_WINDUP) != 0 ? 1f : ease(Math.min(1f, t / 18f));   // chained: the arms are already up
        else if (t < recallStart) raise = 1f;
        else raise = 1f - ease(Math.min(1f, (t - recallStart) / 16f));
        body.setArmRaise(raise);
        body.prepare(c);

        double tx = c.targetX, ty = c.targetY, tz = c.targetZ;
        bodyScale = c.scale;
        Vec3d a = anchor(shape, bodyScale, c.x, c.y, c.z, tx, tz, c.bodyYaw);
        boolean flat = shape == RING;   // the ring never moves or aims vertically

        if (t < windup) {
            cx = a.x;
            cy = a.y + (flat ? 0.0 : Math.sin(t * 0.2) * 0.08 * bodyScale);
            cz = a.z;
            desired.set((float) (tx - cx), flat ? 0f : (float) (ty - cy), (float) (tz - cz));
            if (desired.lengthSquared() < 1e-4f) desired.set(0f, 0f, 1f);
            desired.normalize();
            if (!aimInit) {
                aim.set(desired);
                aimInit = true;
            } else {
                aim.lerp(desired, 0.25f).normalize();
            }
            scale = 0.35f + 0.65f * ease(Math.min(1f, t / (float) Math.min(16, windup)));
            spin += 0.2f;
        } else {
            if (!launched) {
                // lock the flight line from wherever the shape is right now
                launched = true;
                launchX = cx;
                launchY = cy;
                launchZ = cz;
                aim.set((float) (tx - launchX), flat ? 0f : (float) (ty - launchY), (float) (tz - launchZ));
                if (aim.lengthSquared() < 1e-4f) aim.set(0f, 0f, 1f);
                aim.normalize();
                if (shape == SPEAR) {
                    // the spear's tip ends up just below the ground at the impact point
                    endX = tx - aim.x * (SPEAR_TIP - SPEAR_EMBED) * bodyScale;
                    endY = ty - aim.y * (SPEAR_TIP - SPEAR_EMBED) * bodyScale;
                    endZ = tz - aim.z * (SPEAR_TIP - SPEAR_EMBED) * bodyScale;
                } else {
                    endX = tx;
                    endY = flat ? launchY : ty;   // the ring keeps its height for the whole flight
                    endZ = tz;
                }
            }
            float u = Math.min(1f, (t - windup) / (float) flight);
            double e = (shape == CUBE) ? Math.pow(u, 1.6) : u;  // cube accelerates, spear and ring keep a steady speed
            cx = launchX + (endX - launchX) * e;
            cy = launchY + (endY - launchY) * e;
            cz = launchZ + (endZ - launchZ) * e;
            scale = 1f;

            boolean planted = shape == SPEAR && t >= impactTick;  // frozen: no spin, no expansion
            if (!planted) spin += (shape == RING) ? 0.3f : 0.45f;
        }

        // orthonormal basis (right, up, forward) around the aim direction
        fwd.set(aim);
        if (Math.abs(fwd.y) > 0.97f) right.set(1f, 0f, 0f);
        else right.set(0f, 1f, 0f);
        right.cross(fwd).normalize();
        upv.set(fwd).cross(right);

        tumble.rotation(spin * 0.5f, 0.53f, 0.8f, 0.28f);

        // A mild screen shake when the shape lands. Only the boss itself shakes the screen, never its clone. (A ring that
        // reaches its maximum distance does not crash into anything, so it does not shake either.)
        if (t == impactTick && !shakeSent && !c.cloneBody && (shape != RING || shatters)) {
            shakeSent = true;
            c.slam = true;
            c.slamX = tx;
            c.slamY = ty;
            c.slamZ = tz;
            c.slamStrength = IMPACT_SHAKE_STRENGTH;
            c.slamRadius = IMPACT_SHAKE_RADIUS;
        }

        // ground markers from the aim lock until the impact: the danger zone of the shape that is about to land
        int lockTick = lockFor(variant);
        if (c.telegraph != null && t >= lockTick && t < impactTick) {
            float fade = ease(Math.min(1f, (t - lockTick) / 5f));
            switch (shape) {
                case SPEAR -> c.telegraph.line(cx, cz, tx, tz, ty, 0.9, 0.0, fade);                    // its line of flight
                case RING -> {                                                                         // the corridor it sweeps
                    double w = 1.6 * bodyScale;
                    c.telegraph.line(cx, cz, tx, tz, c.y, 0.9, w, fade);
                    c.telegraph.line(cx, cz, tx, tz, c.y, 0.9, -w, fade);
                }
                default -> c.telegraph.ring(tx, ty, tz, CUBE_BLAST_RADIUS * bodyScale, 40, fade, t * 0.04f);   // the cube's blast
            }
        }
    }

    @Override
    public void target(int i, Target out) {
        int rk = rank[i];
        if (rk < 0 || t < assembleDelay[i]) {
            body.target(i, out);
            return;
        }

        if (t >= recallStart + recallDelay[i]) {
            // called back: fly home on a shrinking spiral (from wherever the sheet is: shape, air or floor)
            body.target(i, out);
            float p = MathHelper.clamp((t - recallStart - recallDelay[i]) / 12f, 0f, 1f);
            float amp = 1.6f * bodyScale * (1f - p);
            float a = swirlPhase[i] + t * 0.45f;
            out.x += MathHelper.cos(a) * amp;
            out.y += MathHelper.sin(a * 0.7f) * amp * 0.6f + amp * 0.4f;
            out.z += MathHelper.sin(a) * amp;
            return;
        }

        if (shatters && t >= impactTick) {
            scatterPoint(i, out);
            return;
        }

        shapePoint(rk, i, out);
    }

    // ----------------------------------------------------------------------- shattering

    /**
     * The shape broke. Each sheet starts from the spot it occupied in the shape at the moment of impact,
     * flies straight outward (away from the shape's centre, horizontally, at constant height), then falls
     * and lies flat on the ground where it ended up.
     */
    private void scatterPoint(int i, Target out) {
        if (!originSet[i]) {
            shapePoint(rank[i], i, scratch);   // where the sheet is in the shape right now
            ox[i] = scratch.x;
            oy[i] = scratch.y;
            oz[i] = scratch.z;

            // outward = away from the centre of the shape, with a little jitter so sheets right above
            // the centre (top and bottom of a ring, for instance) still pick a direction
            float hx = (float) (ox[i] - endX), hz = (float) (oz[i] - endZ);
            hx += MathHelper.cos(swirlPhase[i]) * 0.35f;
            hz += MathHelper.sin(swirlPhase[i]) * 0.35f;
            float hl = MathHelper.sqrt(hx * hx + hz * hz);
            if (hl < 1e-4f) {
                hx = 1f;
                hz = 0f;
                hl = 1f;
            }
            dirX[i] = hx / hl;
            dirZ[i] = hz / hl;
            originSet[i] = true;
        }

        float travel = scatterDist[i] * 0.6f * bodyScale;
        float tau = t - impactTick;

        if (tau < OUT_TICKS) {
            // outward only: same height as in the shape
            float p = Math.min(1f, (tau + 1f) / OUT_TICKS);
            float b = 1f - (1f - p) * (1f - p);
            out.x = ox[i] + dirX[i] * travel * b;
            out.y = oy[i];
            out.z = oz[i] + dirZ[i] * travel * b;
            out.nx = dirX[i] * 0.7f;
            out.ny = 0.7f;
            out.nz = dirZ[i] * 0.7f;
            out.spin = swirlPhase[i] + t * 0.5f;
        } else {
            // then straight down to the floor under the landing spot
            double fx = ox[i] + dirX[i] * travel, fz = oz[i] + dirZ[i] * travel;
            if (Float.isNaN(groundY[i])) {
                groundY[i] = ground != null ? (float) ground.heightAt(fx, oy[i], fz) : (float) (oy[i] - 1.5);
            }
            out.x = fx;
            out.y = groundY[i] + 0.03f + groundJitter[i];
            out.z = fz;
            out.nx = dirX[i] * 0.15f;   // lying flat, very slightly tilted
            out.ny = 1f;
            out.nz = dirZ[i] * 0.15f;
            out.spin = swirlPhase[i] * 3f;
        }
    }

    // ----------------------------------------------------------------------- shapes

    private void shapePoint(int rk, int i, Target out) {
        // R2 low-discrepancy sequence: evenly spread points for any sheet count
        double uu = frac(0.5 + rk * 0.7548776662466927);
        double vv = frac(0.5 + rk * 0.5698402909980532);

        float x, y, z, nx, ny, nz;
        switch (shape) {
            case SPEAR -> {
                // thin shaft + leaf-shaped head, tip pointing along the aim direction
                float phi = (float) (vv * TAU) + spin;
                float rad, zz, nzc;
                if (uu < 0.35) {
                    float s = (float) (uu / 0.35);
                    zz = -1.6f + 1.8f * s;
                    rad = 0.10f;
                    nzc = 0f;
                } else {
                    float s = (float) ((uu - 0.35) / 0.65);
                    zz = 0.2f + 2.0f * s;
                    rad = 0.55f * (1f - s) + 0.03f;
                    nzc = 0.3f;
                }
                float cp = MathHelper.cos(phi), sp = MathHelper.sin(phi);
                x = cp * rad;
                y = sp * rad;
                z = zz;
                nx = cp;
                ny = sp;
                nz = nzc;
            }
            case RING -> {
                // torus facing the direction of travel, sheets circulating around it
                float th = (float) (uu * TAU) + spin * 0.6f;
                float ps = (float) (vv * TAU);
                float cr = 1.3f + 0.3f * MathHelper.cos(ps);
                x = cr * MathHelper.cos(th);
                y = cr * MathHelper.sin(th);
                z = 0.3f * MathHelper.sin(ps) * 0.7f;
                nx = MathHelper.cos(ps) * MathHelper.cos(th);
                ny = MathHelper.cos(ps) * MathHelper.sin(th);
                nz = MathHelper.sin(ps);
            }
            default -> {
                // hollow cube, tumbling
                float h = 0.85f;
                float a = (float) (uu * 2.0 - 1.0) * h, b = (float) (vv * 2.0 - 1.0) * h;
                switch (rk % 6) {
                    case 0 -> { x = h;  y = a;  z = b;  nx = 1f;  ny = 0f;  nz = 0f; }
                    case 1 -> { x = -h; y = a;  z = b;  nx = -1f; ny = 0f;  nz = 0f; }
                    case 2 -> { x = a;  y = h;  z = b;  nx = 0f;  ny = 1f;  nz = 0f; }
                    case 3 -> { x = a;  y = -h; z = b;  nx = 0f;  ny = -1f; nz = 0f; }
                    case 4 -> { x = a;  y = b;  z = h;  nx = 0f;  ny = 0f;  nz = 1f; }
                    default -> { x = a; y = b;  z = -h; nx = 0f;  ny = 0f;  nz = -1f; }
                }
                lp.set(x, y, z);
                tumble.transform(lp);
                ln.set(nx, ny, nz);
                tumble.transform(ln);
                x = lp.x;
                y = lp.y;
                z = lp.z;
                nx = ln.x;
                ny = ln.y;
                nz = ln.z;
            }
        }

        float nl = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl > 1e-4f) {
            nx /= nl;
            ny /= nl;
            nz /= nl;
        }

        out.x = cx + (right.x * x + upv.x * y + fwd.x * z) * scale * bodyScale;
        out.y = cy + (right.y * x + upv.y * y + fwd.y * z) * scale * bodyScale;
        out.z = cz + (right.z * x + upv.z * y + fwd.z * z) * scale * bodyScale;
        out.nx = right.x * nx + upv.x * ny + fwd.x * nz;
        out.ny = right.y * nx + upv.y * ny + fwd.y * nz;
        out.nz = right.z * nx + upv.z * ny + fwd.z * nz;
        out.spin = swirlPhase[i] + spin * 0.5f;
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}