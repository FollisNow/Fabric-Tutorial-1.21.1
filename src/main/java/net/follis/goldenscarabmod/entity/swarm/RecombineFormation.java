package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Played by a clone when it merges back into the boss. The server sends the point to merge into (the boss'
 * chest) as the pattern's aim point. Every sheet, eyes included, spirals into that point and shrinks to
 * nothing as it arrives; the server removes the clone entity right after, and the boss gets its sheets back.
 */
public final class RecombineFormation implements PatternFormation {

    public static final int FADE_START = 14;
    public static final int FADE_TICKS = 10;
    public static final int TOTAL = 30;

    private static final float TAU = (float) (Math.PI * 2.0);

    private final int[] delay;
    private final float[] phase;

    private int t;
    private double px, py, pz;
    private float bodyScale = 1f;

    public RecombineFormation(int count, long seed) {
        this.delay = new int[count];
        this.phase = new float[count];
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            delay[i] = r.nextInt(9);
            phase[i] = r.nextFloat() * TAU;
        }
    }

    @Override
    public boolean isFinished() {
        return t >= TOTAL;
    }

    @Override
    public float stiffnessScale(int i) {
        return 1.8f;
    }

    @Override
    public float noiseScale(int i) {
        return 0.5f;
    }

    @Override
    public float scaleLimit(int i) {
        // shrink away once the sheets have had time to reach the boss
        return t >= FADE_START ? Math.max(0f, 1f - (t - FADE_START) / (float) FADE_TICKS) : 1f;
    }

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        px = c.targetX;
        py = c.targetY;
        pz = c.targetZ;
        bodyScale = c.scale;
    }

    @Override
    public void target(int i, Target out) {
        // a spiral that tightens onto the merge point
        float p = MathHelper.clamp((t - delay[i]) / 12f, 0f, 1f);
        float amp = 1.4f * bodyScale * (1f - p);
        float a = phase[i] + t * 0.5f;
        float c = MathHelper.cos(a), s = MathHelper.sin(a);

        out.x = px + c * amp;
        out.y = py + MathHelper.sin(a * 0.7f) * amp * 0.6f;
        out.z = pz + s * amp;
        out.nx = c * 0.5f;
        out.ny = 0.8f;
        out.nz = s * 0.5f;
        out.spin = phase[i] + t * 0.4f;
    }
}