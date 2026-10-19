package net.follis.goldenscarabmod.world.structure;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TempleLayout {

    // ================================================================ density knobs
    /** Fraction of each floor's module cells the temple tries to cover before that floor starts closing doorways. */
    private static final double FILL = 0.95;
    /** Chance to continue from the newest doorway (long branches) instead of a random one (side branches). */
    private static final double DFS_BIAS = 0.6;
    /** How many unresolved doorways to keep open per floor, per module of floor width. Higher = more branching. */
    private static final double FRONTIER_PER_SIZE = 1.5;
    /** Weight multiplier for dead ends while the temple is still growing (1.0 = no penalty). */
    private static final double DEAD_END_FACTOR = 0.3;
    /** Stairwells start getting a weight boost once their floor is this far along (fraction of its own fill target). */
    private static final double STAIRS_BOOST_FROM = 0.5;
    /** Weight multiplier per free cell that a placement would cut off for good (1.0 disables the check). */
    private static final double SEAL_PENALTY = 0.75;

    // ================================================================ coverage targets
    /** A layout is accepted if the whole temple is at least this covered... */
    private static final double MIN_COVERAGE_TOTAL = 0.85;
    /** ...and every floor is at least this covered. If no attempt qualifies, the best one is used. */
    private static final double MIN_COVERAGE_FLOOR = 0.80;

    // ================================================================ rules
    public enum RepeatRule {
        OFF,
        /** No two connected rooms of the same type (same RoomDef), whatever their variant. */
        SAME_TYPE,
        /** No two connected rooms built from the same template. Different variants of one type may touch. */
        SAME_ROOM
    }
    /** Rule 1: what counts as "the same room twice in a row". */
    private static final RepeatRule REPEAT_RULE = RepeatRule.SAME_TYPE;
    /** Rule 1 escape hatch: if a cell has no legal option that avoids a repeat, allow one instead of failing the attempt. */
    private static final boolean REPEAT_FALLBACK = false;
    private static final double CLASH_WEIGHT = 0.02;
    /** Rule 2: each earlier use of a room TYPE on the same floor multiplies its weight by this (1.0 disables). */
    private static final double SAME_FLOOR_DECAY = 1.0;
    /** Rule 2: same, but per individual room (template variant). */
    private static final double SAME_ROOM_FLOOR_DECAY = 0.6;
    /** Rule 3: treasure rooms per floor, inclusive. The minimum is only enforced if a treasure room exists in the pool. */
    private static final int TREASURE_MIN_PER_FLOOR = 1;
    private static final int TREASURE_MAX_PER_FLOOR = 3;
    /** Rule 3: distance between two treasure rooms on the same floor (Manhattan, nearest cells; 1 = adjacent). Scaled down on narrow floors. */
    public static final int TREASURE_MIN_DISTANCE = 4;
    /** Rule 4: stairwells (rooms with an up doorway) per floor. The minimum is implied: you cannot climb without one. */
    private static final int STAIRS_MAX_PER_FLOOR = 2;
    /** Rule 4: distance between two stairwells on the same floor. Scaled down on narrow floors. */
    public static final int STAIRS_MIN_DISTANCE = 4;
    /** Rule 4: distance between a stairwell and where players arrive on its floor (entrance / groundless rooms). Scaled down on narrow floors. */
    public static final int STAIRS_MIN_ARRIVAL_DISTANCE = 4;
    /** Rule 5: miniboss rooms per floor, inclusive. 0 as a minimum means a floor may have none. */
    private static final int MINIBOSS_MIN_PER_FLOOR = 0;
    private static final int MINIBOSS_MAX_PER_FLOOR = 1;

    // ================================================================ debug
    public static boolean DEBUG = false;
    public static final Map<String, Integer> FAILURES = new TreeMap<>();

    // ================================================================ types
    public interface Mask { boolean contains(Cell c); }

    public record Cell(int x, int y, int z) {
        Cell offset(Dir d) { return new Cell(x + d.dx, y + d.dy, z + d.dz); }
    }

    public record Placed(RoomDef def, int turns, Shape shape, int x, int y, int z, int variant) {
        public Placed(RoomDef def, int turns, Shape shape, int x, int y, int z) {
            this(def, turns, shape, x, y, z, 0);
        }
        public Identifier template() { return def.templates().get(variant); }
    }

    private record Candidate(RoomDef def, int turns, int x, int z, int variant, double weight, int opens, boolean clash, int sealed) {}

    public record Params(List<RoomDef> rooms, RoomDef entrance, RoomDef penultimate, RoomDef boss,
                         int size, int floors, int targetRooms) {   // targetRooms is unused; FILL replaces it
        public Mask mask() {
            return c -> c.y() >= 0 && c.y() < floors
                    && c.x() >= c.y() && c.x() < size - c.y()
                    && c.z() >= c.y() && c.z() < size - c.y();
        }
    }

    // ================================================================ state
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private final Map<Cell, Placed> cells = new HashMap<>();
    private final List<Placed> placed = new ArrayList<>();
    private final List<Cell> open = new ArrayList<>();
    private final Map<Integer, Integer> floorCells = new HashMap<>();
    private final Map<Integer, Map<RoomDef, Integer>> typeCounts = new HashMap<>();
    private final Map<Integer, Map<Identifier, Integer>> roomCounts = new HashMap<>();
    private final Map<Integer, List<Placed>> treasureRooms = new HashMap<>();
    private final Map<Integer, List<Placed>> stairsRooms = new HashMap<>();
    private final Map<Integer, List<Placed>> arrivals = new HashMap<>();
    private final Map<Integer, Integer> minibosses = new HashMap<>();
    private final boolean[][][] covered;   // [floor][x][z]: a room is here
    private final boolean[][][] isOpen;    // [floor][x][z]: free, and a doorway points at it
    private final Mask mask;
    private final int size;

    private TempleLayout(Params p) {
        this.mask = p.mask();
        this.size = p.size();
        this.covered = new boolean[p.floors() + 1][p.size()][p.size()];
        this.isOpen = new boolean[p.floors() + 1][p.size()][p.size()];
    }

    // ================================================================ entry point
    /**
     * Returns the rooms of a temple, or null if every attempt failed. The boss room is appended last.
     * The first attempt that meets the coverage targets is used; if none does, the best one is.
     */
    public static List<Placed> generate(Params p, Random rng, int attempts) {
        List<Placed> chosen = null;
        double chosenCoverage = -1;
        for (int i = 0; i < attempts; i++) {
            TempleLayout layout = new TempleLayout(p);
            if (!layout.run(p, rng)) continue;
            if (layout.coverageIsEnough(p)) { chosen = layout.placed; break; }
            double coverage = layout.totalCoverage(p);
            if (coverage > chosenCoverage) { chosen = layout.placed; chosenCoverage = coverage; }
        }
        if (chosen == null) return null;
        chosen.add(new Placed(p.boss(), 0, p.boss().shape(0), p.floors(), p.floors(), p.floors(),
                rng.nextInt(p.boss().templates().size())));
        return chosen;
    }

    // ================================================================ generation
    private boolean run(Params p, Random rng) {
        putEntrance(p, rng);
        boolean penultimatePlaced = false;
        int highest = 0;

        while (!open.isEmpty()) {
            // Mostly continue from the newest doorway (long branches), sometimes a random one (side branches)
            Cell n = open.remove(rng.nextFloat() < DFS_BIAS ? open.size() - 1 : rng.nextInt(open.size()));
            if (cells.containsKey(n)) continue;

            // Every floor has its own progress clock
            int floorWidth = p.size() - 2 * n.y();
            double floorProgress = cellsOn(n.y()) / (floorWidth * floorWidth * FILL);
            boolean topFloor = n.y() == p.floors() - 1;
            boolean closing = floorProgress >= 1.0 && (topFloor ? penultimatePlaced : highest > n.y());
            boolean forced = needsRoomAbove(n);
            int doomedNow = doomedCells(n.y());

            // Branching control, per floor. The target ramps up quickly, stays high until ~90%, then falls.
            long openHere = open.stream().filter(c -> c.y() == n.y()).count();
            double shape = Math.max(0.0, Math.min(1.0, Math.min(floorProgress * 8, (1.0 - floorProgress) * 6)));
            int desiredHere = 2 + (int) (floorWidth * FRONTIER_PER_SIZE * 0.5 * shape);
            boolean wantMore = openHere < desiredHere;

            List<RoomDef> pool = new ArrayList<>(p.rooms());
            Placed entrance = placed.get(0);
            boolean farEnough = Math.abs(n.x() - entrance.x()) + Math.abs(n.z() - entrance.z()) >= p.size() / 2;
            if (!penultimatePlaced && topFloor && farEnough) pool.add(p.penultimate());

            // 1. every (room, rotation, position, variant) that fits, honouring rules 1, 3, 4 and 5
            List<Candidate> raw = collect(n, pool, forced, p, true, doomedNow);
            if (raw.isEmpty() && REPEAT_FALLBACK) raw = collect(n, pool, forced, p, false, doomedNow);
            if (raw.isEmpty()) {
                if (DEBUG) recordFailure(n, closing, !collect(n, pool, forced, p, false, doomedNow).isEmpty());
                return false;
            }

            // 2. weight them; a type's weight is split across its placements and variants, so it means what it says
            Map<RoomDef, Integer> perDef = new IdentityHashMap<>();
            for (Candidate cand : raw) perDef.merge(cand.def(), 1, Integer::sum);

            List<Candidate> cands = new ArrayList<>();
            for (Candidate cand : raw) {
                RoomDef def = cand.def();
                double w = def.weight();

                if (def == p.penultimate()) {
                    w = 200;
                } else {
                    w *= Math.pow(SAME_FLOOR_DECAY, usesOfType(def, n.y()));                // rule 2, per type
                    if (hasUp(def) && n.y() == highest && highest < p.floors() - 1) {
                        if (floorProgress >= STAIRS_BOOST_FROM) w *= 8;
                        else if (openHere <= 2) w *= 50;   // this floor is about to run out of doorways: climb now
                    }
                }
                w *= Math.pow(SAME_ROOM_FLOOR_DECAY, usesOfRoom(def.templates().get(cand.variant()), n.y()));   // rule 2, per room
                if (cand.clash()) w *= CLASH_WEIGHT;                                                            // rule 1 fallback
                w *= Math.pow(SEAL_PENALTY, cand.sealed());                                                     // do not cut off free cells

                if (def.deadEnd() && !closing) w *= DEAD_END_FACTOR;

                // rule 3: below the minimum on this floor, treasure variants get steadily more likely
                if (def.isTreasure(cand.variant()) && treasureOn(n.y()) < TREASURE_MIN_PER_FLOOR)
                    w *= 1 + 10 * Math.min(1.0, floorProgress);

                // rule 5: same for miniboss rooms (only matters if the minimum is above 0)
                if (def.miniboss() && minibossOn(n.y()) < MINIBOSS_MIN_PER_FLOOR)
                    w *= 1 + 10 * Math.min(1.0, floorProgress);

                // branching control: few open doorways on this floor -> favour rooms that open more
                if (cand.opens() == 0) w *= wantMore ? 0.25 : 3.0;
                else if (cand.opens() >= 2) w *= wantMore ? 2.5 : 0.5;

                cands.add(new Candidate(def, cand.turns(), cand.x(), cand.z(), cand.variant(),
                        w / perDef.get(def), cand.opens(), cand.clash(), cand.sealed()));
            }

            if (closing) {   // this floor is full: prefer rooms that open the fewest new doorways
                int min = cands.stream().mapToInt(Candidate::opens).min().getAsInt();
                cands.removeIf(cand -> cand.opens() > min);
            }

            Candidate pick = pickWeighted(cands, rng);
            Placed room = new Placed(pick.def(), pick.turns(), pick.def().shape(pick.turns()),
                    pick.x(), n.y(), pick.z(), pick.variant());
            put(room);
            if (pick.def() == p.penultimate()) penultimatePlaced = true;
            highest = Math.max(highest, n.y());

            if (!fillAbove(room, p.rooms(), rng)) return false;
            if (hasUp(room.def())) highest = Math.max(highest, n.y() + 1);
        }

        boolean ok = penultimatePlaced && highest >= p.floors() - 1;
        if (!ok && DEBUG) FAILURES.merge("frontier emptied: highest=" + highest + " penultimate=" + penultimatePlaced, 1, Integer::sum);

        // rule 3: every floor needs its minimum number of treasure rooms
        if (ok && p.rooms().stream().anyMatch(RoomDef::hasTreasure)) {
            for (int f = 0; f < p.floors(); f++) {
                if (treasureOn(f) < TREASURE_MIN_PER_FLOOR) {
                    ok = false;
                    if (DEBUG) FAILURES.merge("treasure below minimum on floor " + f, 1, Integer::sum);
                    break;
                }
            }
        }

        // rule 5: every floor needs its minimum number of miniboss rooms (skipped while the minimum is 0)
        if (ok && MINIBOSS_MIN_PER_FLOOR > 0 && p.rooms().stream().anyMatch(RoomDef::miniboss)) {
            for (int f = 0; f < p.floors(); f++) {
                if (minibossOn(f) < MINIBOSS_MIN_PER_FLOOR) {
                    ok = false;
                    if (DEBUG) FAILURES.merge("miniboss below minimum on floor " + f, 1, Integer::sum);
                    break;
                }
            }
        }
        return ok;
    }

    /** Every placement that can fill cell n. With strict=true, candidates that would repeat a neighbour are dropped. */
    private List<Candidate> collect(Cell n, List<RoomDef> pool, boolean forced, Params p, boolean strict, int doomedNow) {
        List<Candidate> raw = new ArrayList<>();
        for (RoomDef def : pool) {
            boolean groundless = hasDown(def);
            if (groundless && !forced) continue;   // groundless rooms only above stairwells; they are exempt from rule 1
            if (def.miniboss() && minibossOn(n.y()) >= MINIBOSS_MAX_PER_FLOOR) continue;   // rule 5 maximum
            for (int turns = 0; turns < 4; turns++) {
                Shape s = def.shape(turns);
                for (int cx = 0; cx < s.w(); cx++) {
                    for (int cz = 0; cz < s.d(); cz++) {
                        int x = n.x() - cx, z = n.z() - cz;
                        if (!fits(def, turns, s, x, n.y(), z)) continue;
                        if (hasUp(def) && !stairsAllowed(s, x, n.y(), z)) continue;   // rule 4
                        if (!canFillAbove(s, x, n.y(), z, p.rooms())) continue;

                        int opens = newOpens(s, x, n.y(), z);
                        int sealed = SEAL_PENALTY < 1.0 ? Math.max(0, doomedAfter(s, x, n.y(), z) - doomedNow) : 0;
                        Set<Integer> used = neighbourVariants(def, s, x, n.y(), z);
                        boolean tooCloseToTreasure = def.hasTreasure() && treasureTooClose(s, x, n.y(), z);
                        for (int v = 0; v < def.templates().size(); v++) {
                            if (def.isTreasure(v) && (tooCloseToTreasure || treasureOn(n.y()) >= TREASURE_MAX_PER_FLOOR)) continue;   // rule 3
                            boolean clash = !groundless && switch (REPEAT_RULE) {
                                case OFF -> false;
                                case SAME_TYPE -> !used.isEmpty();
                                case SAME_ROOM -> used.contains(v);
                            };
                            if (clash && strict) continue;
                            raw.add(new Candidate(def, turns, x, z, v, 0, opens, clash, sealed));
                        }
                    }
                }
            }
        }
        return raw;
    }

    // ================================================================ coverage
    private double floorCoverage(Params p, int y) {
        int width = p.size() - 2 * y;
        return cellsOn(y) / (double) (width * width);
    }

    private double totalCoverage(Params p) {
        int coveredCells = 0, total = 0;
        for (int f = 0; f < p.floors(); f++) {
            int width = p.size() - 2 * f;
            coveredCells += cellsOn(f);
            total += width * width;
        }
        return coveredCells / (double) total;
    }

    private boolean coverageIsEnough(Params p) {
        if (totalCoverage(p) < MIN_COVERAGE_TOTAL) return false;
        for (int f = 0; f < p.floors(); f++)
            if (floorCoverage(p, f) < MIN_COVERAGE_FLOOR) return false;
        return true;
    }

    /** Free cells on floor y in a region that has no open doorway: nothing can ever be built there. */
    private int doomedCells(int y) {
        int lo = y, hi = size - 1 - y, width = hi - lo + 1;
        boolean[] seen = new boolean[width * width];
        int[] queue = new int[width * width];
        int doomed = 0;
        for (int x = lo; x <= hi; x++) {
            for (int z = lo; z <= hi; z++) {
                int start = (x - lo) * width + (z - lo);
                if (covered[y][x][z] || seen[start]) continue;

                int head = 0, tail = 0, count = 0;
                boolean hasOpen = false;
                queue[tail++] = start;
                seen[start] = true;
                while (head < tail) {
                    int cur = queue[head++];
                    int cx = lo + cur / width, cz = lo + cur % width;
                    count++;
                    if (isOpen[y][cx][cz]) hasOpen = true;
                    for (int k = 0; k < 4; k++) {
                        int nx = cx + DX[k], nz = cz + DZ[k];
                        if (nx < lo || nx > hi || nz < lo || nz > hi || covered[y][nx][nz]) continue;
                        int idx = (nx - lo) * width + (nz - lo);
                        if (seen[idx]) continue;
                        seen[idx] = true;
                        queue[tail++] = idx;
                    }
                }
                if (!hasOpen) doomed += count;
            }
        }
        return doomed;
    }

    /** Same as doomedCells, assuming a room with shape s were placed at (x, y, z) first. */
    private int doomedAfter(Shape s, int x, int y, int z) {
        int w = s.w(), d = s.d();
        boolean[] wasOpen = new boolean[w * d];
        for (int cx = 0; cx < w; cx++) {
            for (int cz = 0; cz < d; cz++) {
                wasOpen[cx * d + cz] = isOpen[y][x + cx][z + cz];
                covered[y][x + cx][z + cz] = true;
                isOpen[y][x + cx][z + cz] = false;
            }
        }
        List<int[]> opened = new ArrayList<>(4);
        for (Port q : s.ports()) {
            if (q.side().dy != 0) continue;
            int nx = x + q.cx() + q.side().dx, nz = z + q.cz() + q.side().dz;
            if (nx < 0 || nx >= size || nz < 0 || nz >= size) continue;
            if (covered[y][nx][nz] || isOpen[y][nx][nz] || !mask.contains(new Cell(nx, y, nz))) continue;
            isOpen[y][nx][nz] = true;
            opened.add(new int[]{nx, nz});
        }

        int doomed = doomedCells(y);

        for (int[] c : opened) isOpen[y][c[0]][c[1]] = false;
        for (int cx = 0; cx < w; cx++) {
            for (int cz = 0; cz < d; cz++) {
                covered[y][x + cx][z + cz] = false;
                isOpen[y][x + cx][z + cz] = wasOpen[cx * d + cz];
            }
        }
        return doomed;
    }

    // ================================================================ rules
    /** Doorways must agree across every edge between rooms, and no doorway may open into the void. */
    private boolean fits(RoomDef def, int turns, Shape s, int x, int y, int z) {
        for (int cx = 0; cx < s.w(); cx++) {
            for (int cz = 0; cz < s.d(); cz++) {
                Cell c = new Cell(x + cx, y, z + cz);
                if (!mask.contains(c) || cells.containsKey(c)) return false;
            }
        }
        for (int cx = 0; cx < s.w(); cx++) {
            for (int cz = 0; cz < s.d(); cz++) {
                Cell c = new Cell(x + cx, y, z + cz);
                for (Dir side : Dir.values()) {
                    int ix = cx + side.dx, iz = cz + side.dz;
                    if (side.dy == 0 && ix >= 0 && ix < s.w() && iz >= 0 && iz < s.d()) continue;   // inside the room
                    Cell n = c.offset(side);
                    boolean mine = s.ports().contains(new Port(cx, cz, side));
                    Placed other = cells.get(n);
                    if (other != null) {
                        if (mine != hasDoorway(other, n, side.opposite())) return false;
                    } else if (mine && !mask.contains(n)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Variants of this same type among the rooms connected to this placement through a doorway. */
    private Set<Integer> neighbourVariants(RoomDef def, Shape s, int x, int y, int z) {
        Set<Integer> used = new HashSet<>();
        for (Port q : s.ports()) {
            Placed other = cells.get(new Cell(x + q.cx(), y, z + q.cz()).offset(q.side()));
            if (other != null && other.def() == def) used.add(other.variant());
        }
        return used;
    }

    /** Spacing rules shrink on narrow floors: floor width / 3, at least 2, at most the configured distance. */
    public static int scaledDistance(int max, int size, int floor) {
        int width = size - 2 * floor;
        return Math.max(2, Math.min(max, width / 3));
    }

    /** Rule 3: would a room with this footprint be too close to a treasure room on the same floor? */
    private boolean treasureTooClose(Shape s, int x, int y, int z) {
        int required = scaledDistance(TREASURE_MIN_DISTANCE, size, y);
        for (Placed t : treasureRooms.getOrDefault(y, List.of()))
            if (distance(x, z, s.w(), s.d(), t.x(), t.z(), t.shape().w(), t.shape().d()) < required) return true;
        return false;
    }

    /** Rule 4: may a stairwell with this footprint be placed here? */
    private boolean stairsAllowed(Shape s, int x, int y, int z) {
        List<Placed> sameFloor = stairsRooms.getOrDefault(y, List.of());
        if (sameFloor.size() >= STAIRS_MAX_PER_FLOOR) return false;

        int apart = scaledDistance(STAIRS_MIN_DISTANCE, size, y);
        for (Placed t : sameFloor)
            if (distance(x, z, s.w(), s.d(), t.x(), t.z(), t.shape().w(), t.shape().d()) < apart) return false;

        int fromArrival = scaledDistance(STAIRS_MIN_ARRIVAL_DISTANCE, size, y);
        for (Placed a : arrivals.getOrDefault(y, List.of()))
            if (distance(x, z, s.w(), s.d(), a.x(), a.z(), a.shape().w(), a.shape().d()) < fromArrival) return false;

        // the arrival room this stairwell creates upstairs must not be close to a stairwell already on that floor
        int upstairs = scaledDistance(STAIRS_MIN_ARRIVAL_DISTANCE, size, y + 1);
        for (Port q : s.ports()) {
            if (q.side() != Dir.UP) continue;
            for (Placed t : stairsRooms.getOrDefault(y + 1, List.of()))
                if (distance(x + q.cx(), z + q.cz(), 1, 1, t.x(), t.z(), t.shape().w(), t.shape().d()) < upstairs) return false;
        }
        return true;
    }

    /** Manhattan distance between the nearest cells of two rectangles on the same floor (0 = overlapping, 1 = adjacent). */
    private static int distance(int ax, int az, int aw, int ad, int bx, int bz, int bw, int bd) {
        int dx = Math.max(0, Math.max(bx - (ax + aw - 1), ax - (bx + bw - 1)));
        int dz = Math.max(0, Math.max(bz - (az + ad - 1), az - (bz + bd - 1)));
        return dx + dz;
    }

    /** Distance between two placed rooms, as used by the spacing rules. */
    public static int gap(Placed a, Placed b) {
        return distance(a.x(), a.z(), a.shape().w(), a.shape().d(), b.x(), b.z(), b.shape().w(), b.shape().d());
    }

    private boolean hasDoorway(Placed p, Cell c, Dir side) {
        return p.shape().ports().contains(new Port(c.x() - p.x(), c.z() - p.z(), side));
    }

    private static boolean hasUp(RoomDef def) { return def.ports().stream().anyMatch(q -> q.side() == Dir.UP); }

    private static boolean hasDown(RoomDef def) { return def.ports().stream().anyMatch(q -> q.side() == Dir.DOWN); }

    private void put(Placed p) {
        placed.add(p);
        floorCells.merge(p.y(), p.shape().w() * p.shape().d(), Integer::sum);
        typeCounts.computeIfAbsent(p.y(), k -> new IdentityHashMap<>()).merge(p.def(), 1, Integer::sum);
        roomCounts.computeIfAbsent(p.y(), k -> new HashMap<>()).merge(p.template(), 1, Integer::sum);
        if (p.def().isTreasure(p.variant())) treasureRooms.computeIfAbsent(p.y(), k -> new ArrayList<>()).add(p);
        if (p.def().miniboss()) minibosses.merge(p.y(), 1, Integer::sum);
        if (hasUp(p.def())) stairsRooms.computeIfAbsent(p.y(), k -> new ArrayList<>()).add(p);
        if (hasDown(p.def())) arrivals.computeIfAbsent(p.y(), k -> new ArrayList<>()).add(p);
        for (int cx = 0; cx < p.shape().w(); cx++) {
            for (int cz = 0; cz < p.shape().d(); cz++) {
                cells.put(new Cell(p.x() + cx, p.y(), p.z() + cz), p);
                covered[p.y()][p.x() + cx][p.z() + cz] = true;
                isOpen[p.y()][p.x() + cx][p.z() + cz] = false;
            }
        }
        for (Port port : p.shape().ports()) {
            Cell n = new Cell(p.x() + port.cx(), p.y(), p.z() + port.cz()).offset(port.side());
            if (mask.contains(n) && !cells.containsKey(n)) {
                open.add(n);
                isOpen[n.y()][n.x()][n.z()] = true;
            }
        }
    }

    private int cellsOn(int y) { return floorCells.getOrDefault(y, 0); }

    private int usesOfType(RoomDef def, int y) {
        Map<RoomDef, Integer> m = typeCounts.get(y);
        return m == null ? 0 : m.getOrDefault(def, 0);
    }

    private int usesOfRoom(Identifier template, int y) {
        Map<Identifier, Integer> m = roomCounts.get(y);
        return m == null ? 0 : m.getOrDefault(template, 0);
    }

    private int treasureOn(int y) { return treasureRooms.getOrDefault(y, List.of()).size(); }

    private int minibossOn(int y) { return minibosses.getOrDefault(y, 0); }

    /** Doorways of this placement that lead to cells with no room yet. */
    private int newOpens(Shape s, int x, int y, int z) {
        int count = 0;
        for (Port q : s.ports())
            if (!cells.containsKey(new Cell(x + q.cx(), y, z + q.cz()).offset(q.side()))) count++;
        return count;
    }

    private boolean needsRoomAbove(Cell n) {
        Cell below = n.offset(Dir.DOWN);
        Placed b = cells.get(below);
        return b != null && hasDoorway(b, below, Dir.UP);
    }

    /** For every upward doorway of a candidate, checks that a room with a downward doorway could be placed above it. */
    private boolean canFillAbove(Shape s, int x, int y, int z, List<RoomDef> rooms) {
        for (Port q : s.ports()) {
            if (q.side() != Dir.UP) continue;
            Cell above = new Cell(x + q.cx(), y + 1, z + q.cz());
            if (cells.containsKey(above)) continue;

            boolean found = false;
            search:
            for (RoomDef def : rooms) {
                if (!hasDown(def)) continue;
                for (int turns = 0; turns < 4; turns++) {
                    Shape rs = def.shape(turns);
                    for (int cx = 0; cx < rs.w(); cx++)
                        for (int cz = 0; cz < rs.d(); cz++)
                            if (fits(def, turns, rs, above.x() - cx, above.y(), above.z() - cz)) { found = true; break search; }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** Immediately places a groundless room above every upward doorway of the room just placed. */
    private boolean fillAbove(Placed room, List<RoomDef> rooms, Random rng) {
        for (Port q : room.shape().ports()) {
            if (q.side() != Dir.UP) continue;
            Cell above = new Cell(room.x() + q.cx(), room.y() + 1, room.z() + q.cz());
            if (cells.containsKey(above)) continue;

            List<Candidate> cands = new ArrayList<>();
            for (RoomDef def : rooms) {
                if (!hasDown(def)) continue;
                for (int turns = 0; turns < 4; turns++) {
                    Shape s = def.shape(turns);
                    for (int cx = 0; cx < s.w(); cx++) {
                        for (int cz = 0; cz < s.d(); cz++) {
                            int x = above.x() - cx, z = above.z() - cz;
                            if (!fits(def, turns, s, x, above.y(), z)) continue;
                            boolean tooClose = def.hasTreasure() && treasureTooClose(s, x, above.y(), z);
                            for (int v = 0; v < def.templates().size(); v++) {
                                if (def.isTreasure(v) && (tooClose || treasureOn(above.y()) >= TREASURE_MAX_PER_FLOOR)) continue;
                                double w = def.weight()
                                        * Math.pow(SAME_ROOM_FLOOR_DECAY, usesOfRoom(def.templates().get(v), above.y()));
                                cands.add(new Candidate(def, turns, x, z, v, w, 0, false, 0));
                            }
                        }
                    }
                }
            }
            if (cands.isEmpty()) return false;

            Candidate c = pickWeighted(cands, rng);
            put(new Placed(c.def(), c.turns(), c.def().shape(c.turns()), c.x(), above.y(), c.z(), c.variant()));
        }
        return true;
    }

    /** 2x2 entrance, centred on a random face of the ground floor, with its entrance wall facing outwards. */
    private void putEntrance(Params p, Random rng) {
        int size = p.size(), mid = size / 2 - 1;
        int turns, x, z;
        switch (rng.nextInt(4)) {
            case 0 -> { turns = 2; x = mid;      z = 0; }          // outward = north
            case 1 -> { turns = 3; x = size - 2; z = mid; }        // outward = east
            case 2 -> { turns = 0; x = mid;      z = size - 2; }   // outward = south
            default -> { turns = 1; x = 0;       z = mid; }        // outward = west
        }
        Placed entrance = new Placed(p.entrance(), turns, p.entrance().shape(turns), x, 0, z,
                rng.nextInt(p.entrance().templates().size()));
        put(entrance);
        arrivals.computeIfAbsent(0, k -> new ArrayList<>()).add(entrance);   // players arrive here on floor 0
    }

    private static Candidate pickWeighted(List<Candidate> cands, Random rng) {
        double total = cands.stream().mapToDouble(Candidate::weight).sum();
        double roll = rng.nextDouble() * total;
        for (Candidate c : cands) { roll -= c.weight(); if (roll < 0) return c; }
        return cands.get(cands.size() - 1);
    }

    // ================================================================ debug helpers
    private void recordFailure(Cell n, boolean closing, boolean blockedByRepeatRule) {
        StringBuilder sb = new StringBuilder(blockedByRepeatRule ? "[repeat rule] " : "");
        sb.append("floor ").append(n.y()).append(closing ? " closing: " : " growing: ");
        for (Dir d : Dir.values()) {
            Cell nb = n.offset(d);
            Placed o = cells.get(nb);
            String s = o != null ? (hasDoorway(o, nb, d.opposite()) ? "DOOR" : "wall")
                    : !mask.contains(nb) ? "void" : "free";
            sb.append(d).append('=').append(s).append(' ');
        }
        FAILURES.merge(sb.toString(), 1, Integer::sum);
    }

    /** E = entrance, P = penultimate, M = a miniboss room, T = a treasure room, otherwise the room's index in TempleRooms.ALL. */
    public static String debug(List<Placed> layout, int size, int floors) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < floors; y++) {
            char[][] g = new char[size][size];
            for (char[] row : g) Arrays.fill(row, '.');
            for (Placed p : layout) {
                if (p.y() != y) continue;
                char ch = p.def() == TempleRooms.ENTRANCE ? 'E'
                        : p.def() == TempleRooms.PENULTIMATE ? 'P'
                        : p.def().miniboss() ? 'M'
                        : p.def().isTreasure(p.variant()) ? 'T'
                        : Character.forDigit(TempleRooms.ALL.indexOf(p.def()), 36);
                for (int cx = 0; cx < p.shape().w(); cx++)
                    for (int cz = 0; cz < p.shape().d(); cz++)
                        g[p.z() + cz][p.x() + cx] = ch;
            }
            sb.append("floor ").append(y).append('\n');
            for (char[] row : g) sb.append(new String(row)).append('\n');
        }
        return sb.toString();
    }
}