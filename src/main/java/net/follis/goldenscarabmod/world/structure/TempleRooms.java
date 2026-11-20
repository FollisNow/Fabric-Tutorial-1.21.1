package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Room data types (Dir, Port, Shape, RoomDef) and the catalogue of rooms the generator can use. */
public final class TempleRooms {
    private TempleRooms() {}

    // ================================================================ data types
    public enum Dir {
        N(0, 0, -1), E(1, 0, 0), S(0, 0, 1), W(-1, 0, 0), UP(0, 1, 0), DOWN(0, -1, 0);
        public final int dx, dy, dz;
        Dir(int dx, int dy, int dz) { this.dx = dx; this.dy = dy; this.dz = dz; }
        public Dir opposite() {
            return switch (this) { case N -> S; case S -> N; case E -> W; case W -> E; case UP -> DOWN; case DOWN -> UP; };
        }
        public Dir rotateCW() {
            return switch (this) { case N -> E; case E -> S; case S -> W; case W -> N; default -> this; };
        }
    }

    /** A doorway on one side of footprint cell (cx, cz). */
    public record Port(int cx, int cz, Dir side) {}

    public record Shape(int w, int d, List<Port> ports) {}

    public record RoomDef(List<Identifier> templates, int w, int d, List<Port> ports, int weight,
                          Set<Identifier> treasureTemplates, boolean miniboss) {

        /** Every door must be inside the footprint and open to the outside of the room. */
        public RoomDef {
            for (Port q : ports) {
                boolean inside = q.cx() >= 0 && q.cx() < w && q.cz() >= 0 && q.cz() < d;
                int nx = q.cx() + q.side().dx, nz = q.cz() + q.side().dz;
                boolean opensIntoRoom = q.side().dy == 0 && nx >= 0 && nx < w && nz >= 0 && nz < d;
                if (!inside || opensIntoRoom)
                    throw new IllegalArgumentException("Bad door " + q + " in room " + templates.get(0));
            }
        }

        public RoomDef(List<Identifier> templates, int w, int d, List<Port> ports, int weight) {
            this(templates, w, d, ports, weight, Set.of(), false);
        }

        public RoomDef(List<Identifier> templates, int w, int d, List<Port> ports, int weight,
                       Set<Identifier> treasureTemplates) {
            this(templates, w, d, ports, weight, treasureTemplates, false);
        }

        /** allTreasure = true flags every variant of this room as a treasure room. */
        public RoomDef(List<Identifier> templates, int w, int d, List<Port> ports, int weight, boolean allTreasure) {
            this(templates, w, d, ports, weight, allTreasure ? Set.copyOf(templates) : Set.<Identifier>of(), false);
        }

        /** A miniboss room: every variant counts toward the per-floor miniboss limit. */
        public static RoomDef miniboss(List<Identifier> templates, int w, int d, List<Port> ports, int weight) {
            return new RoomDef(templates, w, d, ports, weight, Set.of(), true);
        }

        public boolean deadEnd() { return ports.size() == 1; }

        public boolean isTreasure(int variant) { return treasureTemplates.contains(templates.get(variant)); }

        public boolean hasTreasure() { return !treasureTemplates.isEmpty(); }

        public Identifier pickTemplate(Random rng) { return templates.get(rng.nextInt(templates.size())); }

        /** Footprint and doorways after rotating clockwise by quarterTurns. */
        public Shape shape(int quarterTurns) {
            int w = this.w, d = this.d;
            List<Port> p = ports;
            for (int i = 0; i < quarterTurns; i++) {
                List<Port> next = new ArrayList<>();
                for (Port q : p) next.add(new Port(d - 1 - q.cz(), q.cx(), q.side().rotateCW()));
                p = next;
                int t = w; w = d; d = t;
            }
            return new Shape(w, d, p);
        }
    }

    // ================================================================ helpers
    private static Identifier id(String n) { return Identifier.of(GoldenScarabMod.MOD_ID, "temple/" + n); }
    private static Port p(int cx, int cz, Dir side) { return new Port(cx, cz, side); }

    /** ids("hall", 0, 1, 2) -> temple/hall_0, temple/hall_1, temple/hall_2 */
    private static List<Identifier> ids(String base, int... variants) {
        return Arrays.stream(variants).mapToObj(i -> id(base + "_" + i)).toList();
    }
    private static Set<Identifier> treasure(String base, int... variants) {
        return Set.copyOf(ids(base, variants));
    }

    // ================================================================ catalogue
    // Door sides are for the orientation the template was saved in. All variants of a room share the same doors.

    // ---- 1x1 ----
    public static final RoomDef DEAD_END = new RoomDef(ids("dead_end", 0, 1), 1, 1,
            List.of(p(0, 0, Dir.S)), 3);

    public static final RoomDef STRAIGHT = new RoomDef(ids("straight", 0, 1, 2, 3), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.S)), 4, treasure("straight", 3));

    public static final RoomDef CORNER = new RoomDef(ids("corner", 0), 1, 1,
            List.of(p(0, 0, Dir.S), p(0, 0, Dir.E)), 3);

    public static final RoomDef TEE = new RoomDef(ids("tee", 0, 1, 2, 3), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.E), p(0, 0, Dir.S)), 2);

    public static final RoomDef CROSS = new RoomDef(ids("cross", 0, 1), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.E), p(0, 0, Dir.S), p(0, 0, Dir.W)), 1, treasure("cross", 0));

    // ---- 1x2 (1 module wide on X, 2 deep on Z; doors on the short ends) ----
    public static final RoomDef HALL = new RoomDef(ids("hall", 1, 2, 3, 4, 5, 7, 8), 1, 2,
            List.of(p(0, 0, Dir.N), p(0, 1, Dir.S)), 3);

    public static final RoomDef HALL_SIDE_END = new RoomDef(ids("hall_side_end", 0, 1, 2, 3), 1, 2,
            List.of(p(0, 1, Dir.S), p(0, 0, Dir.E), p(0, 1, Dir.E)), 3);

    public static final RoomDef HALL_SIDES = new RoomDef(ids("hall_sides", 0, 1, 2, 3), 1, 2,
            List.of(p(0, 0, Dir.E), p(0, 1, Dir.E)), 3, treasure("hall_sides", 3));

    public static final RoomDef HALL_BEND = new RoomDef(ids("hall_bend", 0, 1, 2, 3), 1, 2,
            List.of(p(0, 0, Dir.N), p(0, 1, Dir.E)), 3);

    public static final RoomDef HALL_DEAD_END = new RoomDef(ids("hall_dead_end", 0, 1), 1, 2,
            List.of(p(0, 1, Dir.S)), 2, treasure("hall_dead_end", 0));

    // ---- 2x2: two doors per wall, one per module ----
    public static final RoomDef GREAT_HALL = new RoomDef(ids("great_hall", 0, 1, 2, 3, 4, 5), 2, 2, List.of(
            p(0, 0, Dir.N), p(1, 0, Dir.N),
            p(0, 1, Dir.S), p(1, 1, Dir.S),
            p(0, 0, Dir.W), p(0, 1, Dir.W),
            p(1, 0, Dir.E), p(1, 1, Dir.E)), 1, treasure("great_hall", 4));

    // ---- vertical ----
    public static final RoomDef STAIRS = new RoomDef(ids("stairs", 0, 1, 2), 1, 1,
            List.of(p(0, 0, Dir.S), p(0, 0, Dir.UP)), 2);

    public static final RoomDef STAIRS_STRAIGHT = new RoomDef(ids("stairs_straight", 0, 1, 2), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.S), p(0, 0, Dir.UP)), 2);

    public static final RoomDef STAIRS_CORNER = new RoomDef(ids("stairs_corner", 0, 1, 2), 1, 1,
            List.of(p(0, 0, Dir.S), p(0, 0, Dir.E), p(0, 0, Dir.UP)), 2);

    public static final RoomDef STAIRS_TEE = new RoomDef(ids("stairs_tee", 0, 1, 2), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.E), p(0, 0, Dir.S), p(0, 0, Dir.UP)), 2);

    public static final RoomDef STAIRS_CROSS = new RoomDef(ids("stairs_cross", 0, 1, 2), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.E), p(0, 0, Dir.S), p(0, 0, Dir.W), p(0, 0, Dir.UP)), 2);

    // No floor; always sits above a stairwell
    public static final RoomDef GROUNDLESS = new RoomDef(ids("groundless", 0), 1, 1,
            List.of(p(0, 0, Dir.N), p(0, 0, Dir.E), p(0, 0, Dir.S), p(0, 0, Dir.W), p(0, 0, Dir.DOWN)), 1);

    // ---- special rooms, handled by the generator directly (not in ALL) ----
    // Saved with the entrance wall on the SOUTH side, which has no ports (it opens to the outside)
    public static final RoomDef ENTRANCE = new RoomDef(ids("entrance", 0), 2, 2, List.of(
            p(0, 0, Dir.N), p(1, 0, Dir.N),
            p(0, 0, Dir.W), p(0, 1, Dir.W),
            p(1, 0, Dir.E), p(1, 1, Dir.E)), 1);

    // 2x2 arena with a single door, so it fits at the end of any corridor with three free cells beside it
    public static final RoomDef MINIBOSS = RoomDef.miniboss(ids("miniboss", 0), 2, 2,
            List.of(p(0, 0, Dir.N)), 1);

    public static final RoomDef PENULTIMATE = new RoomDef(ids("penultimate", 0), 1, 1,
            List.of(p(0, 0, Dir.S)), 1);

    public static final RoomDef BOSS = new RoomDef(ids("boss", 0), 6, 6, List.of(), 1);

    // Rooms the generator can pick freely.
    public static final List<RoomDef> ALL = List.of(
            DEAD_END, CORNER, CROSS,
            HALL_DEAD_END, HALL, HALL_SIDE_END, HALL_SIDES, HALL_BEND,
            GREAT_HALL, MINIBOSS,
            STAIRS, STAIRS_STRAIGHT, STAIRS_CORNER, STAIRS_TEE, STAIRS_CROSS,
            GROUNDLESS, TEE, STRAIGHT);

    public static RoomDef find(Identifier template) {
        for (RoomDef d : ALL) if (d.templates().contains(template)) return d;
        for (RoomDef d : List.of(ENTRANCE, PENULTIMATE, BOSS)) if (d.templates().contains(template)) return d;
        return null;
    }
}