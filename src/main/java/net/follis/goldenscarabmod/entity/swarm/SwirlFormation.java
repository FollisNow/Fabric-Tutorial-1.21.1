package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Swirl relocation: the whole boss (eyes included) comes apart into a sheet swirl and travels to another
 * spot. The server moves the real entity along the path (invisible: it is made of the sheets), so on the
 * client the swirl simply follows the entity's position; only the flight duration is synced.
 *
 *   0 .. DISASSEMBLE       the body unravels into a spinning column of sheets around the boss, faster and faster
 *   DISASSEMBLE .. arrive  the swirl travels as a comet: a thick corkscrew head and a long thin trail behind it
 *   arrive .. +SPREAD      sheets peel off the swirl one by one and spiral back into the humanoid shape
 *   .. total               settle
 */
public final class SwirlFormation implements PatternFormation {

    public static final int DISASSEMBLE = 24;
    public static final float SPEED = 1.1f;            // blocks per tick during the trip
    public static final int REASSEMBLE_SPREAD = 10;
    public static final int SETTLE = 14;
    public static final double CHEST = 1.2;            // height above the feet of the swirl's centre
    public static final float HIT_RADIUS = 2.2f;       // half width of the corridor that hurts (the server uses it too)

    private static final float TRAIL = 4.5f;           // length of the comet's tail
    private static final float TAU = (float) (Math.PI * 2.0);

    public static int travelTicks(double distance) {
        return Math.max(6, (int) Math.ceil(distance / SPEED));
    }

    public static int totalTicks(int flight) {
        return DISASSEMBLE + flight + REASSEMBLE_SPREAD + SETTLE;
    }

    private final HumanoidFormation body;
    private final float[] along;            // 0 = head of the comet, 1 = end of its tail
    private final float[] angle;            // phase around the column / the path
    private final float[] height;           // 0..1 position in the column
    private final float[] swirlPhase;
    private final int[] reassembleDelay;

    private int t;
    private int flight = 12;
    private boolean init;
    private double cx, cy, cz, lastX, lastY, lastZ;
    private double originX, originY, originZ;   // where the trip started
    private final Vector3f dir = new Vector3f(0f, 0f, 1f);   // direction of travel (smoothed)
    private final Vector3f want = new Vector3f();
    private final Vector3f p1 = new Vector3f(), p2 = new Vector3f();

    public SwirlFormation(HumanoidFormation body, int count, long seed) {
        this.body = body;
        this.along = new float[count];
        this.angle = new float[count];
        this.height = new float[count];
        this.swirlPhase = new float[count];
        this.reassembleDelay = new int[count];

        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            along[i] = (float) Math.pow(r.nextFloat(), 1.4);   // more sheets near the head than in the tail
            angle[i] = r.nextFloat() * TAU;
            height[i] = r.nextFloat();
            swirlPhase[i] = r.nextFloat() * TAU;
            reassembleDelay[i] = r.nextInt(REASSEMBLE_SPREAD + 1);
        }
    }

    @Override
    public boolean isFinished() {
        return t >= totalTicks(flight);
    }

    private int arrive() {
        return DISASSEMBLE + flight;
    }

    // ----------------------------------------------------------------- steering hooks

    @Override
    public float feedForward(int i) {
        return (t >= DISASSEMBLE && t < arrive()) ? 0.9f : 0f;
    }

    @Override
    public float stiffnessScale(int i) {
        if (t >= arrive() + reassembleDelay[i]) return 1f;
        return (t >= DISASSEMBLE && t < arrive()) ? 1.7f : 1.3f;
    }

    @Override
    public float noiseScale(int i) {
        if (t >= arrive() + reassembleDelay[i]) return 1f;
        return (t >= DISASSEMBLE && t < arrive()) ? 0.4f : 1.5f;
    }

    // ------------------------------------------------------------------------ pose

    @Override
    public void prepare(Context c) {
        t = c.patternTick;
        flight = Math.max(1, c.patternFlight);

        body.setArmRaise(0f);
        body.prepare(c);   // the humanoid pose the sheets come back to

        cx = c.x;
        cy = c.y + CHEST;
        cz = c.z;
        if (!init) {
            init = true;
            dir.set(-MathHelper.sin(c.bodyYaw), 0f, MathHelper.cos(c.bodyYaw));
            lastX = cx;
            lastY = cy;
            lastZ = cz;
            originX = c.x;
            originY = c.y;
            originZ = c.z;
        }

        // direction of travel from how the entity actually moved, smoothed
        want.set((float) (cx - lastX), (float) (cy - lastY), (float) (cz - lastZ));
        if (want.lengthSquared() > 0.04f) {
            want.normalize();
            dir.lerp(want, 0.35f).normalize();
        }
        lastX = cx;
        lastY = cy;
        lastZ = cz;

        // two unit vectors perpendicular to the direction of travel
        if (Math.abs(dir.y) > 0.95f) p1.set(1f, 0f, 0f);
        else p1.set(0f, 1f, 0f);
        p1.cross(dir).normalize();
        p2.set(dir).cross(p1);

        // ground markers: the corridor the swirl is about to sweep, from the boss to where it will land
        if (c.telegraph != null && t < arrive()) {
            float fade = ease(Math.min(1f, t / 8f));
            double w = HIT_RADIUS * 0.9;
            c.telegraph.line(originX, originZ, c.targetX, c.targetZ, originY, 0.9, w, fade);
            c.telegraph.line(originX, originZ, c.targetX, c.targetZ, originY, 0.9, -w, fade);
        }
    }

    @Override
    public void target(int i, Target out) {
        int arrive = arrive();

        if (t >= arrive + reassembleDelay[i]) {
            // back into the humanoid, on a shrinking spiral
            body.target(i, out);
            float p = MathHelper.clamp((t - arrive - reassembleDelay[i]) / 12f, 0f, 1f);
            float amp = 1.6f * (1f - p);
            float a = swirlPhase[i] + t * 0.45f;
            out.x += MathHelper.cos(a) * amp;
            out.y += MathHelper.sin(a * 0.7f) * amp * 0.6f + amp * 0.4f;
            out.z += MathHelper.sin(a) * amp;
            return;
        }

        if (t >= DISASSEMBLE && t < arrive) {
            comet(i, out);
        } else {
            column(i, out);   // before the trip, and while waiting to be called back at the destination
        }
    }

    /** A column of sheets spinning around the boss, widening and speeding up. */
    private void column(int i, Target out) {
        float u = ease(Math.min(1f, t / (float) DISASSEMBLE));
        float ang = angle[i] + t * 0.12f + t * t * 0.0035f;                      // spins faster and faster
        float r = (0.5f + 1.0f * u) * (0.75f + 0.25f * MathHelper.sin(swirlPhase[i] * 3f + t * 0.1f));
        float y = (height[i] - 0.5f) * (2.2f + 0.6f * u);

        float c = MathHelper.cos(ang), s = MathHelper.sin(ang);
        out.x = cx + c * r;
        out.y = cy + y;
        out.z = cz + s * r;
        // plates lying almost flat, banking with the spin
        out.nx = c * 0.35f;
        out.ny = 0.9f;
        out.nz = s * 0.35f;
        out.spin = swirlPhase[i] + t * 0.4f;
    }

    /** The travelling swirl: a corkscrew around the line of travel, thick at the head, thin at the tail. */
    private void comet(int i, Target out) {
        float a = along[i];
        float back = a * TRAIL;
        float rad = 1.2f * (1f - 0.65f * a);
        float phi = angle[i] + a * 5f + t * 0.55f;
        float cp = MathHelper.cos(phi), sp = MathHelper.sin(phi);

        // unit vector pointing away from the axis of the corkscrew
        float rx = p1.x * cp + p2.x * sp;
        float ry = p1.y * cp + p2.y * sp;
        float rz = p1.z * cp + p2.z * sp;

        out.x = cx - dir.x * back + rx * rad;
        out.y = cy - dir.y * back + ry * rad;
        out.z = cz - dir.z * back + rz * rad;
        out.nx = rx;   // ribbons: the sheet's plane contains the direction of travel
        out.ny = ry;
        out.nz = rz;
        out.spin = swirlPhase[i] + t * 0.3f;
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}