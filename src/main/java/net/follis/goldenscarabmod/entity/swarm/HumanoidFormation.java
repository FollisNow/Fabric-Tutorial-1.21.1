package net.follis.goldenscarabmod.entity.swarm;

import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Humanoid made of sheets. A tiny skeleton (torso, head, 2 arms, 2 legs) is posed
 * procedurally every tick; each sheet owns a "slot" glued to the surface of one
 * bone box, lying flat like a scale. A fraction of the sheets are loose orbiters
 * circling the body so it reads as a swarm and not as armour.
 *
 * Local space: +Y up, +Z forward, +X = the entity's left. Units are blocks.
 */
public final class HumanoidFormation implements SwarmFormation {

    private static final float TAU = (float) (Math.PI * 2.0);

    private static final int TORSO = 0, HEAD = 1, ARM_L = 2, ARM_R = 3, LEG_L = 4, LEG_R = 5, BONES = 6;

    // Joint position of each bone in the rest pose
    private static final float[][] PIVOT = {
            {0.00f, 1.00f, 0f},   // torso (waist)
            {0.00f, 2.05f, 0f},   // head (neck)
            {0.50f, 1.90f, 0f},   // left arm (shoulder)
            {-0.50f, 1.90f, 0f},  // right arm
            {0.17f, 1.00f, 0f},   // left leg (hip)
            {-0.17f, 1.00f, 0f},  // right leg
    };
    // Box centre relative to its joint
    private static final float[][] CENTER = {
            {0f, 0.50f, 0f}, {0f, 0.30f, 0f},
            {0f, -0.45f, 0f}, {0f, -0.45f, 0f},
            {0f, -0.50f, 0f}, {0f, -0.50f, 0f},
    };
    // Box half extents
    private static final float[][] HALF = {
            {0.35f, 0.50f, 0.20f}, {0.25f, 0.25f, 0.25f},
            {0.14f, 0.50f, 0.14f}, {0.14f, 0.50f, 0.14f},
            {0.15f, 0.50f, 0.15f}, {0.15f, 0.50f, 0.15f},
    };

    private static final class Slot {
        int bone;                    // -1 = free orbiter
        boolean eye;
        float lx, ly, lz;            // offset from the bone joint, bone-local, rest pose
        float nx, ny, nz;            // bone-local normal
        float depth;                 // 0 = buried under other sheets, 1 = outermost
        float spin0, spinRate;
        // orbiters only
        float radius, height, speed, angle0, wobble;
    }

    private final Slot[] slots;
    private final int orbiterStart, orbiterEnd;

    // pose, rebuilt every tick
    private final Matrix3f yaw = new Matrix3f();
    private final Matrix3f torsoRot = new Matrix3f();
    private final Matrix3f local = new Matrix3f();
    private final Matrix3f[] boneRot = new Matrix3f[BONES];
    private final Vector3f[] bonePivot = new Vector3f[BONES];
    private final Vector3f p = new Vector3f();
    private final Vector3f n = new Vector3f();
    private double ex, ey, ez;
    private float time;
    private float bodyScale = 1f;   // 1 = the boss; smaller for its clone
    private float armRaise; // 0 = arms down, 1 = both arms thrown forward/up (set by attack patterns)

    public void setArmRaise(float v) {
        this.armRaise = v;
    }

    public HumanoidFormation(int count, long seed) {
        for (int i = 0; i < BONES; i++) {
            boneRot[i] = new Matrix3f();
            bonePivot[i] = new Vector3f();
        }

        Random r = new Random(seed ^ 0x9E3779B97F4A7C15L);
        slots = new Slot[count];
        int idx = 0;

        // 0 and 1: the eyes
        slots[idx++] = eye(+1f);
        slots[idx++] = eye(-1f);

        // loose orbiting sheets
        orbiterStart = idx;
        int orbiters = (int) (count * 0.14f);
        for (int k = 0; k < orbiters; k++) slots[idx++] = orbiter(r);
        orbiterEnd = idx;

        // everything else is spread over the bone surfaces proportionally to their area
        int remaining = count - idx;
        float[] area = new float[BONES];
        float total = 0f;
        for (int b = 0; b < BONES; b++) {
            float[] h = HALF[b];
            area[b] = 8f * (h[0] * h[1] + h[0] * h[2] + h[1] * h[2]);
            total += area[b];
        }
        for (int b = 0; b < BONES; b++) {
            int num = (b == BONES - 1) ? count - idx : Math.round(remaining * area[b] / total);
            for (int k = 0; k < num && idx < count; k++) slots[idx++] = surface(b, r);
        }
        while (idx < count) slots[idx++] = surface(TORSO, r);
    }

    public boolean isEye(int i) {
        return slots[i].eye;
    }

    public boolean isOrbiter(int i) {
        return i >= orbiterStart && i < orbiterEnd;
    }

    public float depth(int i) {
        return slots[i].depth;
    }

    // ------------------------------------------------------------------ slots

    private static Slot eye(float side) {
        Slot s = new Slot();
        s.bone = HEAD;
        s.eye = true;
        s.lx = CENTER[HEAD][0] + side * 0.11f;
        s.ly = CENTER[HEAD][1] + 0.05f;
        s.lz = CENTER[HEAD][2] + HALF[HEAD][2] + 0.12f;  // sits in front of the gold layer
        s.nz = 1f;
        s.depth = 1f;
        return s;
    }

    private static Slot orbiter(Random r) {
        Slot s = new Slot();
        s.bone = -1;
        s.radius = 0.85f + r.nextFloat() * 0.9f;
        s.height = (r.nextFloat() * 2f - 1f) * 0.9f;
        s.speed = (0.025f + r.nextFloat() * 0.06f) * (r.nextBoolean() ? 1f : -1f);
        s.angle0 = r.nextFloat() * TAU;
        s.wobble = 0.10f + r.nextFloat() * 0.25f;
        s.spin0 = r.nextFloat() * TAU;
        s.spinRate = (r.nextFloat() - 0.5f) * 0.3f;
        s.depth = 1f;
        return s;
    }

    private static Slot surface(int bone, Random r) {
        float[] h = HALF[bone];
        float ax = h[1] * h[2], ay = h[0] * h[2], az = h[0] * h[1];
        float pick = r.nextFloat() * (ax + ay + az);
        int axis = pick < ax ? 0 : (pick < ax + ay ? 1 : 2);
        float sign = r.nextBoolean() ? 1f : -1f;

        float[] pos = {
                (r.nextFloat() * 2f - 1f) * h[0],
                (r.nextFloat() * 2f - 1f) * h[1],
                (r.nextFloat() * 2f - 1f) * h[2]};
        float outward = 0.01f + r.nextFloat() * 0.05f;
        pos[axis] = sign * (h[axis] + outward);

        // face normal + random tilt so the scales flare a little instead of lying like tiles
        float[] nor = new float[3];
        nor[axis] = sign;
        for (int k = 0; k < 3; k++) nor[k] += (r.nextFloat() - 0.5f) * 0.7f;
        float len = (float) Math.sqrt(nor[0] * nor[0] + nor[1] * nor[1] + nor[2] * nor[2]);

        Slot s = new Slot();
        s.bone = bone;
        s.lx = CENTER[bone][0] + pos[0];
        s.ly = CENTER[bone][1] + pos[1];
        s.lz = CENTER[bone][2] + pos[2];
        s.nx = nor[0] / len;
        s.ny = nor[1] / len;
        s.nz = nor[2] / len;
        s.depth = (outward - 0.01f) / 0.05f;
        s.spin0 = r.nextFloat() * TAU;
        return s;
    }

    // ------------------------------------------------------------------- pose

    @Override
    public void prepare(Context c) {
        bodyScale = c.scale;
        ex = c.x;
        ey = c.y;
        ez = c.z;
        time = c.time;
        yaw.rotationY(-c.bodyYaw);

        float amp = Math.min(1f, c.limbSpeed * 1.6f);
        float swing = MathHelper.cos(c.limbPos * 0.6662f) * amp;
        float bob = MathHelper.sin(time * 0.09f) * 0.02f;

        // torso: leans forward while walking, bobs while breathing
        torsoRot.rotationYXZ(0f, 0.18f * amp, 0f);
        boneRot[TORSO].set(torsoRot);
        bonePivot[TORSO].set(PIVOT[TORSO][0], PIVOT[TORSO][1] + bob, PIVOT[TORSO][2]);

        // head follows where the entity looks
        local.rotationYXZ(-c.headYaw, c.headPitch, 0f);
        boneRot[HEAD].set(torsoRot).mul(local);
        childPivot(HEAD);

        // arms: slight outward sway at rest, counter-swing when walking
        float sway = MathHelper.sin(time * 0.07f) * 0.04f;
        local.rotationYXZ(0f, -swing * 0.72f - armRaise * 2.3f, 0.10f + sway);
        boneRot[ARM_L].set(torsoRot).mul(local);
        childPivot(ARM_L);
        local.rotationYXZ(0f, swing * 0.72f - armRaise * 2.3f, -(0.10f + sway));
        boneRot[ARM_R].set(torsoRot).mul(local);
        childPivot(ARM_R);

        // legs
        boneRot[LEG_L].rotationYXZ(0f, swing * 0.9f, 0f);
        bonePivot[LEG_L].set(PIVOT[LEG_L][0], PIVOT[LEG_L][1], PIVOT[LEG_L][2]);
        boneRot[LEG_R].rotationYXZ(0f, -swing * 0.9f, 0f);
        bonePivot[LEG_R].set(PIVOT[LEG_R][0], PIVOT[LEG_R][1], PIVOT[LEG_R][2]);
    }

    /** Children of the torso inherit its rotation around the waist. */
    private void childPivot(int bone) {
        p.set(PIVOT[bone][0] - PIVOT[TORSO][0], PIVOT[bone][1] - PIVOT[TORSO][1], PIVOT[bone][2] - PIVOT[TORSO][2]);
        torsoRot.transform(p);
        p.add(bonePivot[TORSO]);
        bonePivot[bone].set(p);
    }

    @Override
    public void target(int i, Target out) {
        Slot s = slots[i];

        if (s.bone >= 0) {
            Matrix3f rot = boneRot[s.bone];
            p.set(s.lx, s.ly, s.lz);
            rot.transform(p);
            p.add(bonePivot[s.bone]);
            n.set(s.nx, s.ny, s.nz);
            rot.transform(n);
            out.spin = s.spin0;
        } else {
            float a = s.angle0 + s.speed * time;
            p.set(MathHelper.cos(a) * s.radius,
                    1.45f + s.height + s.wobble * MathHelper.sin(time * 0.05f + s.angle0 * 3f),
                    MathHelper.sin(a) * s.radius);
            n.set(MathHelper.cos(a) * 0.8f, MathHelper.sin(s.angle0) * 0.6f, MathHelper.sin(a) * 0.8f).normalize();
            out.spin = s.spin0 + s.spinRate * time;
        }

        p.mul(bodyScale);   // uniform scale about the feet (positions only, normals are unaffected)
        yaw.transform(p);
        yaw.transform(n);
        out.x = ex + p.x;
        out.y = ey + p.y;
        out.z = ez + p.z;
        out.nx = n.x;
        out.ny = n.y;
        out.nz = n.z;
    }
}