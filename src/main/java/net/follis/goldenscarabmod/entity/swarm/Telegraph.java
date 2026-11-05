package net.follis.goldenscarabmod.entity.swarm;

/**
 * Red markers lying on the ground that show where the next attack will land: a ring for a blast, two lines for a
 * corridor. A formation asks for them every tick it wants them visible (c.telegraph.ring(...), c.telegraph.line(...));
 * whatever it does not ask for again that tick disappears. They are drawn from a pool of cosmetic sheets that is
 * separate from the body and from the phase flurry, and rendered with the eyes' glowing red texture.
 */
public final class Telegraph {

    private static final int POOL = 256;
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float SIZE = 0.2f;        // half size of a marker sheet
    private static final double LIFT = 0.05;       // just above the floor, so it does not flicker against it
    private static final double SNAP_SQ = 2.5 * 2.5; // a marker that moved further than this in one tick did not move: it was reassigned

    public final SwarmState.Sheet[] sheets = new SwarmState.Sheet[POOL];
    private int used;
    SwarmFormation.Ground ground;                   // set by SwarmState

    Telegraph() {
        for (int i = 0; i < POOL; i++) {
            SwarmState.Sheet s = new SwarmState.Sheet();
            s.eye = true;                           // drawn in the glowing red pass
            s.active = false;
            s.halfW = SIZE;
            s.halfH = SIZE;
            s.texU = 4;
            s.texV = 4;
            s.ao = 1f;
            for (int k = 0; k < 4; k++) s.corner[k] = 1f;
            s.ny = 1f;
            s.pny = 1f;
            s.scale = 0f;
            s.pscale = 0f;
            s.ptx = Double.NaN;                     // used as the cache of the last floor lookup (x, z) -> y
            sheets[i] = s;
        }
    }

    void begin() {
        used = 0;
    }

    /** Everything that was not asked for this tick disappears. */
    void end() {
        for (int i = used; i < POOL; i++) {
            SwarmState.Sheet s = sheets[i];
            s.pscale = s.scale;
            s.scale = 0f;
        }
    }

    /**
     * A circle of markers on the floor around (cx, cz). refY is roughly the height of the floor there (the search
     * for the actual floor starts just above it); size is 0..1 (0 = invisible), use it to fade in and to pulse.
     */
    public void ring(double cx, double refY, double cz, double radius, int count, float size, float phase) {
        if (size <= 0f || radius <= 0.01) return;
        for (int k = 0; k < count; k++) {
            float a = TAU * k / count + phase;
            place(cx + Math.cos(a) * radius, refY, cz + Math.sin(a) * radius, size, a * 2f);
        }
    }

    /**
     * A row of markers on the floor from (ax, az) to (bx, bz), shifted sideways by lateral blocks (use two calls with
     * +w and -w to mark a corridor of half width w).
     */
    public void line(double ax, double az, double bx, double bz, double refY, double spacing, double lateral, float size) {
        if (size <= 0f) return;
        double dx = bx - ax, dz = bz - az;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.5) return;
        double ux = dx / len, uz = dz / len;
        double sideX = -uz, sideZ = ux;
        // The markers are fixed to the END of the line (where things land), at constant spacing, and only the far
        // ones drop out as the line shortens. Spreading a fixed number of markers over the current length instead
        // would make every one of them slide a little whenever the length changes.
        int n = (int) Math.min(40, Math.max(1, len / spacing));
        for (int k = 0; k < n; k++) {
            double back = k * spacing;
            place(bx - ux * back + sideX * lateral, refY, bz - uz * back + sideZ * lateral, size, k * 0.7f);
        }
    }

    private void place(double x, double refY, double z, float size, float spin) {
        if (used >= POOL) return;
        SwarmState.Sheet s = sheets[used++];

        // the floor height is cached per sheet: markers that stay where they are cost no raycast
        double floor;
        if (!Double.isNaN(s.ptx) && Math.abs(s.ptx - x) < 0.01 && Math.abs(s.ptz - z) < 0.01) {
            floor = s.pty;
        } else {
            floor = ground != null ? ground.heightAt(x, refY, z) : refY;
            s.ptx = x;
            s.ptz = z;
            s.pty = floor;
        }

        // The renderer blends between last tick's position and this one, exactly as it does for the body's sheets,
        // so the markers glide at the frame rate. A marker that has just appeared, or was handed to a different
        // spot (it jumped far in one tick), snaps instead of streaking across the arena for a tick.
        double y = floor + LIFT;
        double jx = x - s.x, jy = y - s.y, jz = z - s.z;
        boolean continuing = s.scale > 0.02f && jx * jx + jy * jy + jz * jz < SNAP_SQ;
        s.px = continuing ? s.x : x;
        s.py = continuing ? s.y : y;
        s.pz = continuing ? s.z : z;
        s.x = x;
        s.y = y;
        s.z = z;
        s.nx = 0f;
        s.ny = 1f;
        s.nz = 0f;
        s.pnx = 0f;
        s.pny = 1f;
        s.pnz = 0f;
        s.pspin = s.spin;
        s.spin = spin;
        s.pscale = s.scale;
        s.scale = size;
    }
}