package net.follis.goldenscarabmod.world.structure;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class TempleRooms {
    private TempleRooms() {}

    private static Identifier id(String n) { return Identifier.of(GoldenScarabMod.MOD_ID, "temple/" + n); }
    private static Port p(int cx, int cz, Dir side) { return new Port(cx, cz, side); }

    /** ids("hall", 0, 1, 2) -> temple/hall_0, temple/hall_1, temple/hall_2 */
    private static List<Identifier> ids(String base, int... variants) {
        return Arrays.stream(variants).mapToObj(i -> id(base + "_" + i)).toList();
    }
    private static Set<Identifier> treasure(String base, int... variants) {
        return Set.copyOf(ids(base, variants));
    }
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
    public static final RoomDef HALL = new RoomDef(ids("hall",  1, 2, 3, 4, 5, 7, 8), 1, 2,
            List.of(p(0, 0, Dir.N), p(0, 1, Dir.S)), 3);

    public static final RoomDef HALL_SIDE_END = new RoomDef(ids("hall_side_end",  0, 1, 2, 3), 1, 2,
            List.of(p(0,1,Dir.S), p(0,0,Dir.E), p(0,1,Dir.E)), 3);

    public static final RoomDef HALL_SIDES = new RoomDef(ids("hall_sides",  0, 1, 2, 3), 1, 2,
            List.of(p(0,0,Dir.E), p(0,1,Dir.E)), 3, treasure("hall_sides", 3));

    public static final RoomDef HALL_BEND = new RoomDef(ids("hall_bend",  0, 1, 2, 3), 1, 2,
            List.of(p(0,0,Dir.N), p(0,1,Dir.E)), 3);

    public static final RoomDef HALL_DEAD_END = new RoomDef(ids("hall_dead_end", 0, 1), 1, 2,
            List.of(p(0,1,Dir.S)), 2, treasure("hall_dead_end", 0));

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

    public static final RoomDef BOSS = new RoomDef(ids("boss", 0), 4, 4, List.of(), 1);

    // Rooms the generator can pick freely. Add STRAIGHT back once straight_* templates exist.
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
