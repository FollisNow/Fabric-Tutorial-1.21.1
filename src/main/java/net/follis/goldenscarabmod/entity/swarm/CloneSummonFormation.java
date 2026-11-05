package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Random;

/**
 * What the boss does while it summons its clone. Ticks count from the start of the pattern; the server
 * goal spawns the clone at tick GATHER, the moment the ball pops.
 *
 *   0 .. GATHER            the body's sheets are pulled into a tight, vibrating ball at the chest, arms raised
 *   GATHER                 the ball pops outward; the clone appears and its sheets fly in from here
 *   GATHER .. total        the boss' remaining sheets flow back into its body (the other half dissolves,
 *                          handled by the swarm's share system, not by this formation)
 */
public final class CloneSummonFormation implements PatternFormation {

    public static final int GATHER = 36;
    public static final int COMPRESS_TICKS = 28;
    public static final int RELEASE_SPREAD = 10;
    public static final int SETTLE = 14;
    public static final int TOTAL = GATHER + RELEASE_SPREAD + SETTLE;

    private static final float BALL_START = 1.1f;
    private static final float BALL_END = 0.35f;
    private static final float TAU = (float) (Math.PI * 2.0);

    private final HumanoidFormation body;
    private final boolean[] joins;       // every sheet except the eyes
    private final float[] dx, dy, dz;    // direction of the sheet on the ball
    private final int[] releaseDelay;
    private final float[] swirlPhase;

    private int t;
    private double cx, cy, cz;
    private float rotCos = 1f, rotSin = 0f;

    public CloneSummonFormation(HumanoidFormation body, int count, long seed) {
        this.body = body;
        this.joins = new boolean[count];
        this.dx = new float[count];
        this.dy = new float[count];
        this.dz = new float[count];
        this.releaseDelay = new int[count];
        this.swirlPhase = new float[count];
        Arrays.fill(joins, false);

        Random r = new Random(seed);
        int n = 0;
        for (int i = 0; i < count; i++) {
            if (!body.isEye(i)) n++;
        }
        int k = 0;
        for (int i = 0; i < count; i++) {
            if (body.isEye(i)) continue;
            joins[i] = true;
            // evenly spread directions over the sphere (Fibonacci sphere)
            float yv = 1f - 2f * (k + 0.5f) / n;
            float rr = (float) Math.sqrt(Math.max(0f, 1f - yv * yv));
            float phi = k * 2.3999632f;
            dx[i] = MathHelper.cos(phi) * rr;
            dy[i] = yv;
            dz[i] = MathHelper.sin(phi) * rr;
            releaseDelay[i] = r.nextInt(RELEASE_SPREAD + 1);
            swirlPhase[i] = r.nextFloat() * TAU;
            k++;
        }
    }

    @Override
    public boolean isFinished() {
        return t >= TOTAL;
    }

    @Override
    public float stiffnessScale(int i) {
        if (!joins[i] || t >= GATHER + releaseDelay[i]) return 1f;
        return 1.5f;
    }

    @Override
    public float noiseScale(int i) {
        if (!joins[i] || t >= GATHER + releaseDelay[i]) return 1f;
        return 2.5f + 3f * Math.min(1f, t / (float) GATHER);   // the ball shakes harder and harder
    }

    @Override
    public void prepare(Context c) {
        t = c.patternTick;

        float raise = t < GATHER
                ? ease(Math.min(1f, t / 14f))
                : 1f - ease(Math.min(1f, (t - GATHER) / 14f));
        body.setArmRaise(raise);
        body.prepare(c);

        cx = c.x;
        cy = c.y + 1.4;
        cz = c.z;

        float ang = Math.min(t, GATHER) * 0.12f;
        rotCos = MathHelper.cos(ang);
        rotSin = MathHelper.sin(ang);
    }

    @Override
    public void target(int i, Target out) {
        if (!joins[i]) {
            body.target(i, out);
            return;
        }

        if (t >= GATHER + releaseDelay[i]) {
            // the ball is gone: back into the humanoid on a shrinking spiral
            body.target(i, out);
            float p = MathHelper.clamp((t - GATHER - releaseDelay[i]) / 12f, 0f, 1f);
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

        float radius = BALL_START + (BALL_END - BALL_START) * ease(Math.min(1f, t / (float) COMPRESS_TICKS));
        if (t >= GATHER - 2 && t < GATHER + 3) {
            radius += 0.5f;   // the pop, right when the clone appears
        }

        out.x = cx + ux * radius;
        out.y = cy + uy * radius;
        out.z = cz + uz * radius;
        out.nx = ux;   // a spiky ball: every sheet stands on end
        out.ny = uy;
        out.nz = uz;
        out.spin = swirlPhase[i] + t * 0.25f;
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}