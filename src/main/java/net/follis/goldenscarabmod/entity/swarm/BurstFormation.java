package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Random;

/**
 * Fragmentation burst. Ticks count from the start of the pattern; the server goal shares the constants
 * and shellRadius() so the damage front lines up with the visuals.
 *
 *   0 .. WINDUP            most of the body's sheets are pulled into a tight, jittering ball at the boss' chest
 *   WINDUP .. +EXPAND      the ball explodes: every sheet flies away in its own direction, edge-on like shrapnel.
 *                          A shard stops early when it meets a block (the client raycasts the world for it).
 *   .. RECALL_START        the shards hang in the air, slowly tumbling
 *   RECALL_START .. TOTAL  sheets are called back to the body, the nearest ones first, spiralling in
 *
 * The eyes and ~15% of the body never leave, so the boss keeps a silhouette while it blows up.
 */
public final class BurstFormation implements PatternFormation {

    public static final int WINDUP = 80;             // 4 seconds: time for players to find cover
    public static final int TICK_INTERVAL = 20;      // "ticking bomb": one tick sound (and ball pulse) per second, 3 times
    public static final int COMPRESS_TICKS = 40;     // the ball is fully formed after 2 seconds, then it just gets more unstable
    public static final int EXPAND = 7;
    public static final int HOLD = 12;
    public static final int RECALL_SPREAD = 14;
    public static final int SETTLE = 12;
    public static final int RECALL_START = WINDUP + EXPAND + HOLD;
    public static final int TOTAL = RECALL_START + RECALL_SPREAD + SETTLE;

    public static final float REACH = 9f;               // how far the shards fly, at most
    public static final double CENTER_HEIGHT = 1.4;     // the ball and the explosion are centred this high above the boss' feet

    private static final float PARTICIPATION = 0.85f;   // share of the non-orbiting sheets that explode
    private static final float BALL_START = 1.1f;       // radius of the ball when it starts to form...
    private static final float BALL_END = 0.3f;         // ...and just before it explodes
    private static final float TAU = (float) (Math.PI * 2.0);

    // the screen shake when the ball explodes: milder than the arm slam's (strength 1.0), and it carries further
    private static final float SHAKE_STRENGTH = 0.5f;
    private static final double SHAKE_RADIUS = 24.0;

    // a faint jolt with each tick of the countdown. The shake grows with the square of the strength, so a value that
    // looks small on paper (0.3) is only about a ninth of the strongest shake: noticeable, never annoying.
    private static final float JOLT_STRENGTH = 0.3f;
    private static final double JOLT_RADIUS = 16.0;

    /** Radius of the explosion front t ticks after the pattern started. The server uses it to hurt things as it passes. */
    public static float shellRadius(int t) {
        if (t <= WINDUP) return 0f;
        float u = Math.min(1f, (t - WINDUP) / (float) EXPAND);
        return REACH * (1f - (1f - u) * (1f - u));
    }

    private final HumanoidFormation body;
    private final int[] rank;            // -1 = stays on the body
    private final int[] recallDelay;
    private final float[] swirlPhase;
    private final float[] baseReach;     // how far this shard would fly in open air
    private final float[] finalReach;    // after checking the world for walls (NaN = not computed yet)
    private final float[] dx, dy, dz;    // flight direction (unit vector)

    private int t;
    private Ground ground;
    private boolean frozen;
    private boolean shakeSent;
    private double cx, cy, cz;           // centre of the ball / explosion, frozen at the moment of the burst
    private float rotCos = 1f, rotSin = 0f;
    private int sliceCursor;             // next sheet whose reach is still to be computed (see prepare)

    public BurstFormation(HumanoidFormation body, int count, long seed) {
        this.body = body;
        this.rank = new int[count];
        this.recallDelay = new int[count];
        this.swirlPhase = new float[count];
        this.baseReach = new float[count];
        this.finalReach = new float[count];
        this.dx = new float[count];
        this.dy = new float[count];
        this.dz = new float[count];
        Arrays.fill(rank, -1);
        Arrays.fill(finalReach, Float.NaN);

        Random r = new Random(seed);

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

            // evenly spread directions over the sphere (Fibonacci sphere) with a little jitter
            float yv = 1f - 2f * (k + 0.5f) / n;
            float rr = (float) Math.sqrt(Math.max(0f, 1f - yv * yv));
            float phi = k * 2.3999632f;
            float x = MathHelper.cos(phi) * rr + (r.nextFloat() - 0.5f) * 0.12f;
            float y = yv + (r.nextFloat() - 0.5f) * 0.12f;
            float z = MathHelper.sin(phi) * rr + (r.nextFloat() - 0.5f) * 0.12f;
            float len = MathHelper.sqrt(x * x + y * y + z * z);
            dx[i] = x / len;
            dy[i] = y / len;
            dz[i] = z / len;

            baseReach[i] = REACH * (0.55f + 0.45f * r.nextFloat());
            swirlPhase[i] = r.nextFloat() * TAU;
            // the nearest shards come home first, so the cloud collapses from the inside out
            recallDelay[i] = (int) (baseReach[i] / REACH * 9f) + r.nextInt(5);
        }
    }

    @Override
    public boolean isFinished() {
        return t >= TOTAL;
    }

    // ----------------------------------------------------------------- steering hooks

    @Override
    public float feedForward(int i) {
        return (rank[i] >= 0 && t > WINDUP && t <= WINDUP + EXPAND) ? 0.9f : 0f;
    }

    @Override
    public float stiffnessScale(int i) {
        if (rank[i] < 0 || t >= RECALL_START + recallDelay[i]) return 1f;
        return t < WINDUP ? 1.5f : 2.0f;
    }

    @Override
    public float noiseScale(int i) {
        if (rank[i] < 0 || t >= RECALL_START + recallDelay[i]) return 1f;
        // the ball vibrates more and more as the countdown runs out; the hanging shards are calmer
        return t < WINDUP ? 2.5f + 3f * (t / (float) WINDUP) : 0.6f;
    }

    // ------------------------------------------------------------------------ pose

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        ground = c.ground;

        float raise;
        if (t < WINDUP) raise = ease(Math.min(1f, t / 14f));
        else if (t < RECALL_START) raise = 1f;
        else raise = 1f - ease(Math.min(1f, (t - RECALL_START) / 16f));
        body.setArmRaise(raise);
        body.prepare(c);

        // the centre follows the boss until the burst, then stays where the explosion happened
        if (!frozen) {
            cx = c.x;
            cy = c.y + CENTER_HEIGHT;
            cz = c.z;
            if (t >= WINDUP) frozen = true;
        }

        // the moment the ball explodes: ask the client for a screen shake (the swarm forwards it, see SwarmFx)
        if (t == WINDUP && !shakeSent) {
            shakeSent = true;
            c.slam = true;
            c.slamX = cx;
            c.slamY = cy;
            c.slamZ = cz;
            c.slamStrength = SHAKE_STRENGTH;
            c.slamRadius = SHAKE_RADIUS;
        }

        // the countdown: a faint jolt on each tick, in time with the chain sounds the server plays
        if (t >= TICK_INTERVAL && t < WINDUP && t % TICK_INTERVAL == 0) {
            c.slam = true;
            c.slamX = cx;
            c.slamY = cy;
            c.slamZ = cz;
            c.slamStrength = JOLT_STRENGTH;
            c.slamRadius = JOLT_RADIUS;
        }

        // ground marker: the edge of the blast. It fades in, and swells on every tick of the countdown.
        if (c.telegraph != null && t >= 8 && t < WINDUP + 3) {
            float fade = ease(Math.min(1f, (t - 8) / 8f));
            float swell = (t >= TICK_INTERVAL && t % TICK_INTERVAL < 6) ? 1f : 0.7f;
            c.telegraph.ring(cx, cy - CENTER_HEIGHT, cz, REACH, 72, fade * swell, t * 0.02f);
        }

        // How far each shard can fly depends on the walls around, which costs one raycast per sheet. Doing them all at
        // the moment of the explosion would freeze a frame, so they are worked out during the charge instead, a few
        // per tick: the boss does not move, and the ball's final rotation (and so every shard's direction) is known.
        if (ground != null && t >= 6 && t < WINDUP - 2 && sliceCursor < rank.length) {
            float finalAng = WINDUP * 0.12f;
            float cs = MathHelper.cos(finalAng), sn = MathHelper.sin(finalAng);
            double centreX = c.x, centreY = c.y + CENTER_HEIGHT, centreZ = c.z;
            int budget = Math.max(8, rank.length / 60);
            while (budget > 0 && sliceCursor < rank.length) {
                int i = sliceCursor++;
                if (rank[i] < 0 || !Float.isNaN(finalReach[i])) continue;
                budget--;
                float ux = dx[i] * cs - dz[i] * sn, uy = dy[i], uz = dx[i] * sn + dz[i] * cs;
                finalReach[i] = computeReach(i, centreX, centreY, centreZ, ux, uy, uz);
            }
        }

        // the ball slowly rotates while charging; the burst uses the final rotation
        float ang = Math.min(t, WINDUP) * 0.12f;
        rotCos = MathHelper.cos(ang);
        rotSin = MathHelper.sin(ang);
    }

    @Override
    public void target(int i, Target out) {
        int rk = rank[i];
        if (rk < 0) {
            body.target(i, out);
            return;
        }

        if (t >= RECALL_START + recallDelay[i]) {
            // called back: fly home on a shrinking spiral
            body.target(i, out);
            float p = MathHelper.clamp((t - RECALL_START - recallDelay[i]) / 12f, 0f, 1f);
            float amp = 1.6f * (1f - p);
            float a = swirlPhase[i] + t * 0.45f;
            out.x += MathHelper.cos(a) * amp;
            out.y += MathHelper.sin(a * 0.7f) * amp * 0.6f + amp * 0.4f;
            out.z += MathHelper.sin(a) * amp;
            return;
        }

        float ux = dx[i] * rotCos - dz[i] * rotSin;
        float uy = dy[i];
        float uz = dx[i] * rotSin + dz[i] * rotCos;

        float radius;
        if (t < WINDUP) {
            // compress into the ball
            radius = BALL_START + (BALL_END - BALL_START) * ease(Math.min(1f, t / (float) COMPRESS_TICKS));
            // each tick of the countdown makes the ball jolt outward, then settle back
            int sinceTick = t % TICK_INTERVAL;
            if (t >= TICK_INTERVAL && sinceTick < 6) {
                radius += 0.35f * (1f - sinceTick / 6f);
            }
        } else {
            if (Float.isNaN(finalReach[i])) finalReach[i] = computeReach(i, cx, cy, cz, ux, uy, uz);   // normally already done
            float u = Math.min(1f, (t - WINDUP) / (float) EXPAND);
            float e = 1f - (1f - u) * (1f - u);     // fast at first, then slowing down
            radius = BALL_END + (finalReach[i] - BALL_END) * e;
        }

        out.x = cx + ux * radius;
        out.y = cy + uy * radius;
        out.z = cz + uz * radius;

        // shards fly edge-on: the sheet's normal is perpendicular to its flight direction
        // (half of them flat-ish, half upright, for variety)
        float n1x = -uz, n1y = 0f, n1z = ux;
        float l1 = MathHelper.sqrt(n1x * n1x + n1z * n1z);
        if (l1 < 0.1f) {
            n1x = 1f;
            n1z = 0f;
        } else {
            n1x /= l1;
            n1z /= l1;
        }
        if ((rk & 1) == 0) {
            out.nx = n1x;
            out.ny = n1y;
            out.nz = n1z;
        } else {
            // n2 = u x n1
            out.nx = uy * n1z - uz * n1y;
            out.ny = uz * n1x - ux * n1z;
            out.nz = ux * n1y - uy * n1x;
        }
        out.spin = swirlPhase[i] + t * 0.25f;
    }

    /** How far this shard can really fly: its normal reach, shortened if a block is in the way. */
    private float computeReach(int i, double centreX, double centreY, double centreZ, float ux, float uy, float uz) {
        float base = baseReach[i];
        if (ground == null) return base;
        double clear = ground.clearDistance(centreX, centreY, centreZ, ux, uy, uz, base + 0.2);
        return (float) Math.max(0.6, Math.min(base, clear - 0.15));
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}