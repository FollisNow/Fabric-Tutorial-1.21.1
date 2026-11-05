package net.follis.goldenscarabmod.entity.swarm;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/**
 * A formation answers one question: "where should sheet #i be right now?".
 * Humanoid idle is one formation; a spear, a ring, a wall, the flying form or the
 * pool on the ground will each be another one. The simulation (SwarmState) just
 * steers every sheet toward whatever the active formation says.
 */
public interface SwarmFormation {

    /** Called once per tick before target(): compute poses, bone matrices, etc. */
    void prepare(Context ctx);

    /** Fill the target position (world space), normal and spin for a sheet. */
    void target(int sheetIndex, Target out);

    /**
     * 0..1: how much of the target's own motion is fed into the sheet's velocity.
     * 0 = relaxed spring (trailing, organic). Near 1 = the sheet keeps up with a fast
     * moving target with almost no lag (used while a shape is thrown).
     */
    default float feedForward(int sheetIndex) {
        return 0f;
    }

    /** Multiplier on the sheet's spring stiffness (crisper shapes, faster returns). */
    default float stiffnessScale(int sheetIndex) {
        return 1f;
    }

    /** Upper limit on a sheet's size (1 = no limit). Lets a formation shrink sheets away, e.g. when recombining. */
    default float scaleLimit(int sheetIndex) {
        return 1f;
    }

    /** Multiplier on the idle shimmer of a sheet (0 = perfectly still, e.g. a planted spear). */
    default float noiseScale(int sheetIndex) {
        return 1f;
    }

    /** Lets a formation ask the world where the ground is (used when sheets scatter on the floor). */
    interface Ground {
        /** Height of the first solid surface below (x, y, z). */
        double heightAt(double x, double y, double z);

        /**
         * How far one can travel from (x, y, z) along the unit direction (dx, dy, dz) before hitting a block,
         * capped at max. Used so exploding shards stop at walls.
         */
        default double clearDistance(double x, double y, double z, double dx, double dy, double dz, double max) {
            return max;
        }
    }

    /** Snapshot of the boss, refreshed every tick. */
    final class Context {
        public double x, y, z;
        public float bodyYaw;    // radians, Minecraft convention
        public float headYaw;    // radians, relative to the body
        public float headPitch;  // radians, positive = looking down
        public float limbPos, limbSpeed;
        public float time;       // ticks since the entity appeared
        public float scale = 1f; // 1 for the boss, smaller for its clone: scales the body, shapes and sheets
        public boolean cloneBody; // true for the clone (it must not shake the screen, for instance)

        // the tentacle attack: the server's latest command for each tentacle (a counter that goes up per lash, and its aim)
        public final int[] tentacleSeq = new int[4];
        public final double[][] tentacleAim = new double[4][3];

        // attack patterns only
        public int patternTick;                    // ticks since the current pattern started
        public int patternFlight;                  // flight duration in ticks, decided by the server
        public boolean patternBreaks;              // does the shape shatter where it stops? (server decides)
        public Ground ground;                      // world ground lookup, may be null
        public Telegraph telegraph;                // red markers on the floor: ask for them every tick they should show

        // events a formation can raise for the client (read and cleared by SwarmState right after prepare)
        public boolean slam;                       // something just hit the ground hard: shake the screen
        public double slamX, slamY, slamZ, slamRadius;
        public float slamStrength;
        public double targetX, targetY, targetZ;   // point the pattern aims at (synced by the server)

        public void update(LivingEntity e) {
            x = e.getX();
            y = e.getY();
            z = e.getZ();
            bodyYaw = e.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
            headYaw = MathHelper.clamp(MathHelper.wrapDegrees(e.headYaw - e.bodyYaw), -75f, 75f)
                    * MathHelper.RADIANS_PER_DEGREE;
            headPitch = e.getPitch() * MathHelper.RADIANS_PER_DEGREE;
            limbPos = e.limbAnimator.getPos();
            limbSpeed = e.limbAnimator.getSpeed();
            time = e.age;
            GoldenSwarmEntity g = (e instanceof GoldenSwarmEntity b) ? b : null;
            scale = g != null ? g.getAttackScale() : 1f;
            cloneBody = g != null && g.isClone();
        }
    }

    /** Output of target(). */
    final class Target {
        public double x, y, z;
        public float nx, ny = 1f, nz;
        public float spin;
    }
}