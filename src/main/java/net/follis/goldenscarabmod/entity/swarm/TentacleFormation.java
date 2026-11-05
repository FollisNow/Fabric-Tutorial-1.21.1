package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Random;

/**
 * Four tentacles of sheets, rooted at the boss' feet, one in each quarter around it. The boss keeps its body (about a
 * third of its sheets stay on it); the rest make the tentacles. Ticks count from the start of the pattern.
 *
 *   0 .. GROW        the tentacles grow out of the ground around the boss
 *   GROW .. END      each tentacle lives its own life, driven by commands the server sends it (one per lash):
 *                      idle    sways around, tip held up in front of the boss
 *                      windup  rears back and up (it follows its target, until a few ticks before the lash)
 *                      lash    the tip whips down onto the target's position, fast
 *                      recover returns to the idle pose
 *   END ..           the tentacles retract and the sheets go back to the body
 *
 * Each tentacle is a cubic Bezier curve from its root to its tip, with a wave travelling along it, and the sheets sit
 * on a tube around it. The four tentacles do not know about each other: the server gives each one its own quarter of
 * the surroundings, so they never go for the same target.
 *
 * The direction of the first tentacle (in degrees, same convention as atan2(dz, dx)) arrives as the pattern's variant;
 * the others follow at 90 degree steps.
 */
public final class TentacleFormation implements PatternFormation {

    public static final int TENTACLES = 4;
    public static final int GROW = 20;
    public static final int ACTIVE = 200;               // how long they stay out and fight after growing
    public static final int END = GROW + ACTIVE;
    public static final int WINDUP = 14, LASH = 5, RECOVER = 12;   // one lash: the server and the clients share these
    public static final int RECALL_SPREAD = 14, SETTLE = 12;
    public static final int TOTAL = END + RECALL_SPREAD + SETTLE;

    public static final double REACH = 8.0;             // how far from the boss a tentacle can lash
    public static final float HIT_RADIUS = 1.7f;        // around the tip, where the lash hurts

    private static final double ROOT_OFFSET = 0.7;      // roots sit this far from the boss' centre, towards their quarter
    private static final double IDLE_DIST = 3.2, IDLE_HEIGHT = 2.4, REAR_HEIGHT = 4.6;
    private static final float R_BASE = 0.5f, R_TIP = 0.12f;
    private static final float PARTICIPATION = 0.7f;    // the rest of the sheets stay on the body
    private static final float TAU = (float) (Math.PI * 2.0);

    private final HumanoidFormation body;
    private final float theta0;               // direction of the first tentacle, radians
    private final int[] rank;                 // -1 = stays on the body
    private final int[] tentacle;             // which tentacle the sheet belongs to
    private final float[] along;              // 0 = root, 1 = tip
    private final float[] phi;                // angle around the tube
    private final int[] recallDelay;
    private final float[] swirlPhase;

    // per tick
    private int t;
    private Ground ground;
    private boolean seqInit;
    private double bx, by, bz;                // the boss' feet
    private float extent;                     // 0..1: how far the tentacles are grown
    private final int[] lastSeq = new int[TENTACLES];
    private final int[] cmdStart = new int[TENTACLES];          // tick at which the tentacle's current lash command began
    private final double[][] aim = new double[TENTACLES][3];    // where each tentacle lashes
    private final boolean[] lashing = new boolean[TENTACLES];
    private final double[][][] cp = new double[TENTACLES][4][3];  // Bezier control points of each tentacle
    private final double[] waveAmp = new double[TENTACLES];
    private final double[] latX = new double[TENTACLES], latZ = new double[TENTACLES];   // sideways direction of each tentacle
    private final double[] fc = new double[3], ft = new double[3], fn1 = new double[3], fn2 = new double[3];   // frame at a point

    public TentacleFormation(HumanoidFormation body, int count, long seed, int directionDegrees) {
        this.body = body;
        this.theta0 = (float) Math.toRadians(directionDegrees);
        this.rank = new int[count];
        this.tentacle = new int[count];
        this.along = new float[count];
        this.phi = new float[count];
        this.recallDelay = new int[count];
        this.swirlPhase = new float[count];
        Arrays.fill(rank, -1);

        Random r = new Random(seed);

        // the sheets that make the tentacles, shuffled so each tentacle is fed from the whole body
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
        for (int k = 0; k < n; k++) {
            int i = pool[k];
            rank[i] = k;
            tentacle[i] = k % TENTACLES;
            int local = k / TENTACLES;   // position within its own tentacle
            along[i] = (float) frac(0.5 + local * 0.7548776662466927);
            phi[i] = (float) (frac(0.5 + local * 0.5698402909980532) * TAU);
            recallDelay[i] = (int) ((1f - along[i]) * 8f) + r.nextInt(5);   // tips come home first
        }
        for (int i = 0; i < count; i++) swirlPhase[i] = r.nextFloat() * TAU;
    }

    @Override
    public boolean isFinished() {
        return t >= TOTAL;
    }

    // ----------------------------------------------------------------- steering hooks

    @Override
    public float feedForward(int i) {
        return (rank[i] >= 0 && lashing[tentacle[i]]) ? 0.85f : 0f;
    }

    @Override
    public float stiffnessScale(int i) {
        if (rank[i] < 0 || t >= END + recallDelay[i]) return 1f;
        return lashing[tentacle[i]] ? 2.0f : 1.4f;
    }

    @Override
    public float noiseScale(int i) {
        if (rank[i] < 0 || t >= END + recallDelay[i]) return 1f;
        return lashing[tentacle[i]] ? 0.4f : 0.8f;
    }

    // ------------------------------------------------------------------------ pose

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        ground = c.ground;

        body.setArmRaise(0f);
        body.prepare(c);

        bx = c.x;
        by = c.y;
        bz = c.z;

        float grow = ease(Math.min(1f, t / (float) GROW));
        float retract = t >= END ? ease(Math.min(1f, (t - END) / 16f)) : 0f;
        extent = grow * (1f - retract);

        if (!seqInit) {   // commands that were already sent before this pattern started are not ours
            for (int k = 0; k < TENTACLES; k++) {
                lastSeq[k] = c.tentacleSeq[k];
                cmdStart[k] = -1000;
            }
            seqInit = true;
        }

        for (int k = 0; k < TENTACLES; k++) {
            // a new lash command from the server: this tentacle starts its windup now
            if (c.tentacleSeq[k] != lastSeq[k]) {
                lastSeq[k] = c.tentacleSeq[k];
                cmdStart[k] = t;
            }
            int tau = t - cmdStart[k];
            boolean busy = tau >= 0 && tau < WINDUP + LASH + RECOVER;
            // the aim follows what the server sends until just before the lash, then it stays where it was
            if (!busy || tau < WINDUP - 4) {
                System.arraycopy(c.tentacleAim[k], 0, aim[k], 0, 3);
            }
            lashing[k] = busy && tau >= WINDUP && tau < WINDUP + LASH + 2;

            // this tentacle's root, and its resting, rearing and lashing positions
            double a = theta0 + k * (Math.PI / 2.0);
            double dx = Math.cos(a), dz = Math.sin(a);
            latX[k] = -dz;
            latZ[k] = dx;
            double rx = bx + dx * ROOT_OFFSET, ry = by + 0.3, rz = bz + dz * ROOT_OFFSET;

            double sway = Math.sin(t * 0.07 + k * 1.7);
            double idleX = rx + dx * IDLE_DIST + latX[k] * sway * 0.8;
            double idleY = ry + IDLE_HEIGHT + Math.sin(t * 0.09 + k) * 0.4;
            double idleZ = rz + dz * IDLE_DIST + latZ[k] * sway * 0.8;
            double rearX = rx + dx * 0.8, rearY = ry + REAR_HEIGHT, rearZ = rz + dz * 0.8;

            // the lash lands on the aim point, but never further than the tentacle can reach
            double ax = aim[k][0], ay = aim[k][1], az = aim[k][2];
            double hx = ax - bx, hz = az - bz;
            double hl = Math.sqrt(hx * hx + hz * hz);
            if (hl > REACH) {
                ax = bx + hx / hl * REACH;
                az = bz + hz / hl * REACH;
            }

            double tx, ty, tz, arch, wave;
            if (!busy) {
                tx = idleX;
                ty = idleY;
                tz = idleZ;
                arch = 0.8;
                wave = 0.35;
            } else if (tau < WINDUP) {
                // rear back and up
                float u = ease(tau / (float) WINDUP);
                tx = idleX + (rearX - idleX) * u;
                ty = idleY + (rearY - idleY) * u;
                tz = idleZ + (rearZ - idleZ) * u;
                arch = 0.8 + 0.2 * u;
                wave = 0.35 - 0.15 * u;
            } else if (tau < WINDUP + LASH) {
                // the whip: fast, accelerating, the curve straightens as it goes
                float u = (tau - WINDUP + 1) / (float) LASH;
                double e = u * u;
                tx = rearX + (ax - rearX) * e;
                ty = rearY + (ay - rearY) * e;
                tz = rearZ + (az - rearZ) * e;
                arch = 1.0 - e;
                wave = 0.05;
            } else {
                // back to the resting pose
                float u = ease((tau - WINDUP - LASH) / (float) RECOVER);
                tx = ax + (idleX - ax) * u;
                ty = ay + (idleY - ay) * u;
                tz = az + (idleZ - az) * u;
                arch = 0.8 * u;
                wave = 0.05 + 0.3 * u;
            }

            // grown/retracted: the whole tentacle shrinks towards its root
            double ex = rx + (tx - rx) * extent, ey = ry + (ty - ry) * extent, ez = rz + (tz - rz) * extent;
            double rise = arch * extent;
            set(cp[k][0], rx, ry, rz);
            set(cp[k][1], rx + (ex - rx) * 0.25, ry + (ey - ry) * 0.25 + 1.8 * rise, rz + (ez - rz) * 0.25);
            set(cp[k][2], rx + (ex - rx) * 0.7, ry + (ey - ry) * 0.7 + 1.4 * rise, rz + (ez - rz) * 0.7);
            set(cp[k][3], ex, ey, ez);
            waveAmp[k] = wave * extent;

            // ground marker: where this tentacle is about to hit, from the moment it starts rearing until it lands
            if (c.telegraph != null && busy && tau < WINDUP + LASH) {
                float fade = ease(Math.min(1f, (tau + 1) / 4f));
                c.telegraph.ring(ax, ay, az, HIT_RADIUS, 16, fade, t * 0.05f);
            }
        }
    }

    @Override
    public void target(int i, Target out) {
        int rk = rank[i];
        if (rk < 0) {
            body.target(i, out);   // the boss keeps its body (and its eyes)
            return;
        }

        if (t >= END + recallDelay[i]) {
            // back into the humanoid on a shrinking spiral
            body.target(i, out);
            float p = MathHelper.clamp((t - END - recallDelay[i]) / 12f, 0f, 1f);
            float amp = 1.6f * (1f - p);
            float a = swirlPhase[i] + t * 0.45f;
            out.x += MathHelper.cos(a) * amp;
            out.y += MathHelper.sin(a * 0.7f) * amp * 0.6f + amp * 0.4f;
            out.z += MathHelper.sin(a) * amp;
            return;
        }

        int k = tentacle[i];
        float u = along[i];
        frame(k, u);

        float radius = (R_BASE + (R_TIP - R_BASE) * u) * (0.4f + 0.6f * extent);
        double angle = phi[i] + k;
        double cphi = Math.cos(angle), sphi = Math.sin(angle);
        double rx = cphi * fn1[0] + sphi * fn2[0];
        double ry = cphi * fn1[1] + sphi * fn2[1];
        double rz = cphi * fn1[2] + sphi * fn2[2];

        out.x = fc[0] + rx * radius;
        out.y = fc[1] + ry * radius;
        out.z = fc[2] + rz * radius;
        out.nx = (float) rx;
        out.ny = (float) ry;
        out.nz = (float) rz;
        out.spin = swirlPhase[i] + t * 0.2f;
    }

    /**
     * Frame of tentacle k at u: the centre point (with the travelling wave added), the unit tangent, and two unit normals
     * around the tube.
     */
    private void frame(int k, float u) {
        double w = 1.0 - u;
        double a = w * w * w, b = 3 * w * w * u, c = 3 * w * u * u, d = (double) u * u * u;
        double t0 = w * w, t1 = 2 * w * u, t2 = (double) u * u;
        double[][] p = cp[k];
        for (int m = 0; m < 3; m++) {
            fc[m] = a * p[0][m] + b * p[1][m] + c * p[2][m] + d * p[3][m];
            ft[m] = 3 * (t0 * (p[1][m] - p[0][m]) + t1 * (p[2][m] - p[1][m]) + t2 * (p[3][m] - p[2][m]));
        }

        // a wave running along the tentacle towards its tip, growing with the distance from the root
        double amp = waveAmp[k] * u * (1.0 - 0.3 * u);
        double wave = Math.sin(u * 5.5 - t * 0.35 + k * 1.9);
        fc[0] += latX[k] * amp * wave;
        fc[2] += latZ[k] * amp * wave;
        fc[1] += amp * 0.5 * Math.sin(u * 4.1 - t * 0.29 + 1.3);

        double len = Math.sqrt(ft[0] * ft[0] + ft[1] * ft[1] + ft[2] * ft[2]);
        if (len < 1e-6) {
            ft[0] = 0;
            ft[1] = 1;
            ft[2] = 0;
        } else {
            ft[0] /= len;
            ft[1] /= len;
            ft[2] /= len;
        }

        // first normal: tangent x up (horizontal); if the tentacle points straight up, use its sideways direction instead
        double n1x = -ft[2], n1z = ft[0];
        double l1 = Math.sqrt(n1x * n1x + n1z * n1z);
        if (l1 < 0.1) {
            n1x = latX[k];
            n1z = latZ[k];
        } else {
            n1x /= l1;
            n1z /= l1;
        }
        fn1[0] = n1x;
        fn1[1] = 0;
        fn1[2] = n1z;
        // second normal: tangent x first normal
        fn2[0] = ft[1] * n1z;
        fn2[1] = ft[2] * n1x - ft[0] * n1z;
        fn2[2] = -ft[1] * n1x;
    }

    // -------------------------------------------------------------------------- helpers

    private static void set(double[] v, double x, double y, double z) {
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}