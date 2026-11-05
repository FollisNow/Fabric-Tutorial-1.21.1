package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Random;

/**
 * Giant arm slam. The whole swarm turns into one huge arm rooted at the boss' feet, with the eyes on its base. The arm
 * unfolds upwards, hovers with its fist raised over the target, then slams the fist down on the ground.
 * Ticks count from the start of the pattern; the server goal shares the constants so damage lines up with the visuals.
 *
 *   0 .. RISE            the arm unfolds out of the body: a tube of sheets with a fist (a box of sheets) at its tip
 *   .. LOCK              the fist hovers overhead and follows the target; at LOCK the server fixes the impact point
 *   SLAM_START .. IMPACT the fist comes down fast (accelerating)
 *   IMPACT               screen shake, and about a quarter of the sheets pop off the fist and arm: they fly outward
 *                        and fall to lie on the ground (like the shattered cube)
 *   .. RECALL_START      the fist stays put on the ground
 *   RECALL_START .. end  the sheets go back to the body, the fist's first, spiralling in
 *
 * The arm is a cubic Bezier curve from the boss' feet to the fist. All its control points lie in the vertical plane
 * that contains the boss and the impact point, so the sheets on its surface can use a stable frame around it.
 */
public final class ArmSlamFormation implements PatternFormation {

    public static final int RISE = 30;
    public static final int LOCK = 44;
    public static final int SLAM_START = 60;
    public static final int SLAM_TICKS = 8;
    public static final int IMPACT = SLAM_START + SLAM_TICKS;
    public static final int HOLD = 26;
    public static final int RECALL_START = IMPACT + HOLD;
    public static final int RECALL_SPREAD = 14;
    public static final int SETTLE = 12;
    public static final int TOTAL = RECALL_START + RECALL_SPREAD + SETTLE;

    public static final double REACH = 13.0;          // how far from the boss the fist can land
    public static final float BLAST_RADIUS = 4.5f;    // how far around the impact point the blow hurts (the server uses it too)

    // arm geometry, in blocks
    private static final double RAISED_HEIGHT = 6.5;  // height of the fist while it is raised
    private static final double SHOULDER = 4.0;       // height of the arm's first control point
    private static final float R_BASE = 0.85f, R_WRIST = 0.6f, BULGE = 0.15f;
    private static final float FIST_X = 0.9f, FIST_Y = 0.8f, FIST_Z = 0.9f;   // half sizes of the fist
    private static final float FIST_SHARE = 0.35f;    // share of the sheets that make the fist, the rest make the tube
    private static final float POP_SHARE = 0.28f;     // share of the sheets that pop off at the impact
    private static final int OUT_TICKS = 5;           // popped sheets fly outward this long, then fall
    private static final float TAU = (float) (Math.PI * 2.0);

    private final HumanoidFormation body;
    private final int[] rank;            // -1 = eye
    private final int fistCount;
    private final boolean[] pops;
    private final int[] recallDelay;
    private final float[] swirlPhase, scatterDist, groundJitter, groundY;

    // popped sheets: where each was at the moment of impact, and the way it flies off
    private final boolean[] originSet;
    private final double[] ox, oy, oz;
    private final float[] dirX, dirZ;
    private final Target scratch = new Target();

    // per tick
    private int t;
    private Ground ground;
    private boolean init, slamSent;
    private double bx, by, bz;               // root of the arm: the boss' feet
    private double px, py, pz;               // where the fist lands (smoothed, then locked by the server)
    private double hx, hz;                   // horizontal unit vector from the root towards the impact
    private double sx, sz;                   // horizontal unit vector perpendicular to it
    private double fx, fy, fz;               // the fist's position
    private float grow;                      // 0..1: how far the arm has unfolded
    private final double[][] cp = new double[4][3];     // Bezier control points
    private final double[] fc = new double[3], ft = new double[3], fn = new double[3];   // frame: centre, tangent, normal

    public ArmSlamFormation(HumanoidFormation body, int count, long seed) {
        this.body = body;
        this.rank = new int[count];
        this.pops = new boolean[count];
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

        // every sheet except the eyes, shuffled so the arm is fed from the whole body
        int[] pool = new int[count];
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (!body.isEye(i)) pool[n++] = i;
        }
        for (int k = n - 1; k > 0; k--) {
            int j = r.nextInt(k + 1);
            int tmp = pool[k];
            pool[k] = pool[j];
            pool[j] = tmp;
        }
        for (int k = 0; k < n; k++) rank[pool[k]] = k;
        int fistSheets = Math.round(n * FIST_SHARE);
        this.fistCount = fistSheets;

        for (int i = 0; i < count; i++) {
            swirlPhase[i] = r.nextFloat() * TAU;
            scatterDist[i] = 0.6f + 3.2f * (float) Math.sqrt(r.nextFloat());
            groundJitter[i] = r.nextFloat() * 0.05f;
            if (rank[i] < 0) {
                recallDelay[i] = 8 + r.nextInt(5);   // the eyes go home last
                continue;
            }
            pops[i] = r.nextFloat() < POP_SHARE;
            // the fist is recalled first, then the arm from the tip down to the root
            float u = rank[i] < fistSheets ? 1f : (float) frac(0.5 + rank[i] * 0.7548776662466927);
            recallDelay[i] = (int) ((1f - u) * 8f) + r.nextInt(5);
        }
    }

    @Override
    public boolean isFinished() {
        return t >= TOTAL;
    }

    // ----------------------------------------------------------------- steering hooks

    @Override
    public float feedForward(int i) {
        return (rank[i] >= 0 && t > SLAM_START && t <= IMPACT) ? 0.9f : 0f;
    }

    @Override
    public float stiffnessScale(int i) {
        if (t >= RECALL_START + recallDelay[i]) return 1f;
        if (pops[i] && t >= IMPACT + OUT_TICKS) return 1f;   // popped sheets fall with a looser spring
        return t < SLAM_START ? 1.4f : 2.0f;
    }

    @Override
    public float noiseScale(int i) {
        if (t >= RECALL_START + recallDelay[i]) return 1f;
        return t >= IMPACT ? 0.1f : 0.7f;                    // after the slam everything stays put
    }

    // ------------------------------------------------------------------------ pose

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        ground = c.ground;

        body.setArmRaise(0f);
        body.prepare(c);   // the humanoid the sheets go back to

        bx = c.x;
        by = c.y;
        bz = c.z;

        // the impact point follows what the server sends (the aim, then the locked point), smoothed
        if (!init) {
            px = c.targetX;
            py = c.targetY;
            pz = c.targetZ;
            init = true;
        } else {
            px += (c.targetX - px) * 0.25;
            py += (c.targetY - py) * 0.25;
            pz += (c.targetZ - pz) * 0.25;
        }
        double dx = px - bx, dz = pz - bz;
        double hl = Math.sqrt(dx * dx + dz * dz);
        if (hl > REACH) {                       // the arm only reaches so far
            px = bx + dx / hl * REACH;
            pz = bz + dz / hl * REACH;
            dx = px - bx;
            dz = pz - bz;
            hl = REACH;
        }
        if (hl < 1e-3) {
            hx = -Math.sin(c.bodyYaw);
            hz = Math.cos(c.bodyYaw);
        } else {
            hx = dx / hl;
            hz = dz / hl;
        }
        sx = -hz;
        sz = hx;
        double dist = Math.max(hl, 2.5);

        grow = ease(Math.min(1f, t / (float) RISE));

        // the raised fist: over the arm's root, leaning a little towards the target, swaying slightly
        double sway = Math.sin(t * 0.12) * 0.3;
        double wx = bx + hx * dist * 0.3 + sx * sway;
        double wy = by + RAISED_HEIGHT + Math.sin(t * 0.09) * 0.2;
        double wz = bz + hz * dist * 0.3 + sz * sway;

        if (t < SLAM_START) {
            fx = wx;
            fy = wy;
            fz = wz;
        } else {
            // the slam: accelerating from the raised position down to the ground at the impact point
            float u = Math.min(1f, (t - SLAM_START) / (float) SLAM_TICKS);
            double e = u * u;
            fx = wx + (px - wx) * e;
            fy = wy + (py + FIST_Y - wy) * e;
            fz = wz + (pz - wz) * e;
        }

        // the arm's curve: up from the feet, over, and down onto the fist
        set(cp[0], bx, by + 0.2, bz);
        set(cp[1], bx + hx * 0.3, by + SHOULDER, bz + hz * 0.3);
        set(cp[2], fx - hx * 1.2, fy + 3.5, fz - hz * 1.2);
        set(cp[3], fx, fy, fz);

        // ground marker, from the moment the aim is locked: the reach of the blow, and a ring closing in on the centre
        if (c.telegraph != null && t >= LOCK && t < IMPACT + 2) {
            float fade = ease(Math.min(1f, (t - LOCK) / 6f));
            float closing = Math.max(0f, 1f - (t - LOCK) / (float) (IMPACT - LOCK));
            c.telegraph.ring(px, py, pz, BLAST_RADIUS, 56, fade, 0f);
            c.telegraph.ring(px, py, pz, BLAST_RADIUS * closing, 28, fade, t * 0.05f);
        }

        if (t == IMPACT && !slamSent) {
            slamSent = true;
            c.slam = true;                  // the swarm tells the client to shake the screen
            c.slamX = px;
            c.slamY = py;
            c.slamZ = pz;
            c.slamStrength = 1.0f;
            c.slamRadius = 32.0;
        }
    }

    @Override
    public void target(int i, Target out) {
        if (t >= RECALL_START + recallDelay[i]) {
            // back into the humanoid on a shrinking spiral
            body.target(i, out);
            float p = MathHelper.clamp((t - RECALL_START - recallDelay[i]) / 12f, 0f, 1f);
            float amp = 1.6f * (1f - p);
            float a = swirlPhase[i] + t * 0.45f;
            out.x += MathHelper.cos(a) * amp;
            out.y += MathHelper.sin(a * 0.7f) * amp * 0.6f + amp * 0.4f;
            out.z += MathHelper.sin(a) * amp;
            return;
        }

        if (rank[i] < 0) {
            eyePoint(i, out);
            return;
        }

        if (pops[i] && t >= IMPACT) {
            popPoint(i, out);
            return;
        }

        armSheet(i, out);
    }

    // ----------------------------------------------------------------------- the arm

    /** The eyes stay at the base of the arm, looking towards the target. */
    private void eyePoint(int i, Target out) {
        float side = i == 0 ? 1f : -1f;
        out.x = bx + hx * 0.75 + sx * 0.28 * side;
        out.y = by + 1.1;
        out.z = bz + hz * 0.75 + sz * 0.28 * side;
        out.nx = (float) hx;
        out.ny = 0f;
        out.nz = (float) hz;
        out.spin = 0f;
    }

    /** A sheet of the arm: on the fist (a box) or on the tube, depending on its rank. */
    private void armSheet(int i, Target out) {
        int rk = rank[i];
        double uu = frac(0.5 + rk * 0.7548776662466927);
        double vv = frac(0.5 + rk * 0.5698402909980532);
        out.spin = swirlPhase[i] + t * 0.15f;

        if (rk < fistCount) {
            fistPoint(rk, uu, vv, out);
        } else {
            // the tube grows out of the root as the arm unfolds
            float u = (float) uu * 0.92f * grow;
            frame(u);
            float phi = (float) (vv * TAU) + swirlPhase[i] * 0.2f;
            double radius = R_BASE + (R_WRIST - R_BASE) * u + BULGE * Math.sin(Math.PI * u);
            double cphi = Math.cos(phi), sphi = Math.sin(phi);
            double rx = cphi * sx + sphi * fn[0];
            double ry = sphi * fn[1];
            double rz = cphi * sz + sphi * fn[2];
            out.x = fc[0] + rx * radius;
            out.y = fc[1] + ry * radius;
            out.z = fc[2] + rz * radius;
            out.nx = (float) rx;
            out.ny = (float) ry;
            out.nz = (float) rz;
        }
    }

    /** The fist: the surface of a box at the tip of the arm, its long axis along the arm. */
    private void fistPoint(int k, double uu, double vv, Target out) {
        frame(grow);
        float s = Math.max(0.25f, grow);   // it grows with the arm
        float hxs = FIST_X * s, hys = FIST_Y * s, hzs = FIST_Z * s;
        float a = (float) (uu * 2.0 - 1.0), b = (float) (vv * 2.0 - 1.0);

        float lx, ly, lz, nx, ny, nz;      // local: x along the side axis, y along the arm, z along the other normal
        switch (k % 6) {
            case 0 -> { lx = hxs;  ly = a * hys; lz = b * hzs; nx = 1f;  ny = 0f;  nz = 0f; }
            case 1 -> { lx = -hxs; ly = a * hys; lz = b * hzs; nx = -1f; ny = 0f;  nz = 0f; }
            case 2 -> { lx = a * hxs; ly = hys;  lz = b * hzs; nx = 0f;  ny = 1f;  nz = 0f; }
            case 3 -> { lx = a * hxs; ly = -hys; lz = b * hzs; nx = 0f;  ny = -1f; nz = 0f; }
            case 4 -> { lx = a * hxs; ly = b * hys; lz = hzs;  nx = 0f;  ny = 0f;  nz = 1f; }
            default -> { lx = a * hxs; ly = b * hys; lz = -hzs; nx = 0f; ny = 0f;  nz = -1f; }
        }

        out.x = fc[0] + sx * lx + ft[0] * ly + fn[0] * lz;
        out.y = fc[1] + ft[1] * ly + fn[1] * lz;
        out.z = fc[2] + sz * lx + ft[2] * ly + fn[2] * lz;
        out.nx = (float) (sx * nx + ft[0] * ny + fn[0] * nz);
        out.ny = (float) (ft[1] * ny + fn[1] * nz);
        out.nz = (float) (sz * nx + ft[2] * ny + fn[2] * nz);
    }

    /**
     * Frame of the curve at u: its centre point, its unit tangent, and the unit normal in the arm's vertical plane.
     * (The other axis around the arm is the horizontal "side" vector, constant.)
     */
    private void frame(float u) {
        double w = 1.0 - u;
        double a = w * w * w, b = 3 * w * w * u, c = 3 * w * u * u, d = (double) u * u * u;
        double t0 = w * w, t1 = 2 * w * u, t2 = (double) u * u;
        for (int k = 0; k < 3; k++) {
            fc[k] = a * cp[0][k] + b * cp[1][k] + c * cp[2][k] + d * cp[3][k];
            ft[k] = 3 * (t0 * (cp[1][k] - cp[0][k]) + t1 * (cp[2][k] - cp[1][k]) + t2 * (cp[3][k] - cp[2][k]));
        }
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
        // normal = tangent x side, with side = (sx, 0, sz)
        fn[0] = ft[1] * sz;
        fn[1] = ft[2] * sx - ft[0] * sz;
        fn[2] = -ft[1] * sx;
    }

    // ---------------------------------------------------------------------- popping off

    /**
     * Some sheets pop off at the impact. Each starts from the spot it occupied on the arm at that moment, flies
     * straight outward (away from the impact point, level), then falls and lies flat on the ground.
     */
    private void popPoint(int i, Target out) {
        if (!originSet[i]) {
            armSheet(i, scratch);   // where the sheet is on the arm right now
            ox[i] = scratch.x;
            oy[i] = scratch.y;
            oz[i] = scratch.z;
            float hxo = (float) (ox[i] - px) + MathHelper.cos(swirlPhase[i]) * 0.35f;
            float hzo = (float) (oz[i] - pz) + MathHelper.sin(swirlPhase[i]) * 0.35f;
            float hl = MathHelper.sqrt(hxo * hxo + hzo * hzo);
            if (hl < 1e-4f) {
                hxo = 1f;
                hzo = 0f;
                hl = 1f;
            }
            dirX[i] = hxo / hl;
            dirZ[i] = hzo / hl;
            originSet[i] = true;
        }

        float travel = scatterDist[i];
        float tau = t - IMPACT;

        if (tau < OUT_TICKS) {
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
            double gx = ox[i] + dirX[i] * travel, gz = oz[i] + dirZ[i] * travel;
            if (Float.isNaN(groundY[i])) {
                groundY[i] = ground != null ? (float) ground.heightAt(gx, oy[i], gz) : (float) (py);
            }
            out.x = gx;
            out.y = groundY[i] + 0.03f + groundJitter[i];
            out.z = gz;
            out.nx = dirX[i] * 0.15f;
            out.ny = 1f;
            out.nz = dirZ[i] * 0.15f;
            out.spin = swirlPhase[i] * 3f;
        }
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