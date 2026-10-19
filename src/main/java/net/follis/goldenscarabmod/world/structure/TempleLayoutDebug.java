package net.follis.goldenscarabmod.world.structure;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;   // not java.util.Random

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TempleLayoutDebug {
    private static final int SAMPLES = 1000;
    private static final int SHIPPED_SAMPLES = 100;
    private static final int EXAMPLE_SEED = 42;

    public static void main(String[] args) {
        for (int size : new int[]{6, 8, 10, 12}) {
            int floors = size / 2 - 2;
            TempleLayout.Params params = new TempleLayout.Params(
                    TempleRooms.ALL, TempleRooms.ENTRANCE, TempleRooms.PENULTIMATE, TempleRooms.BOSS,
                    size, floors, 0);

            System.out.println("==================== size " + size + " (" + floors + " floor" + (floors == 1 ? "" : "s") + ") ====================");

            // 1. single-attempt layouts, with failure bookkeeping on
            TempleLayout.DEBUG = true;
            TempleLayout.FAILURES.clear();
            List<List<TempleLayout.Placed>> layouts = new ArrayList<>();
            for (int seed = 0; seed < SAMPLES; seed++) {
                List<TempleLayout.Placed> layout = TempleLayout.generate(params, Random.create(seed), 1);
                if (layout != null) layouts.add(layout);
            }
            TempleLayout.DEBUG = false;
            System.out.println((SAMPLES - layouts.size()) + "/" + SAMPLES + " single attempts failed. Top causes:");
            TempleLayout.FAILURES.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue()).limit(8)
                    .forEach(e -> System.out.println("  " + e.getValue() + "x  " + e.getKey()));

            // 2. one example, generated the way the game does it (retries and coverage check included)
            List<TempleLayout.Placed> example = TempleLayout.generate(params, Random.create(EXAMPLE_SEED), 200);
            System.out.println(example == null ? "no layout found" : TempleLayout.debug(example, size, floors));

            // 3. indicators on the raw single attempts
            coverage("single attempts", params, layouts);
            holes(params, layouts);
            shapes(layouts);
            usage(layouts);
            rules(params, layouts);
            miniboss("single attempts", params, layouts);
            // 4. what the game actually gets: up to 200 attempts, first layout that meets the coverage targets
            long t0 = System.nanoTime();
            int failed = 0;
            List<List<TempleLayout.Placed>> shipped = new ArrayList<>();
            for (int seed = 0; seed < SHIPPED_SAMPLES; seed++) {
                List<TempleLayout.Placed> layout = TempleLayout.generate(params, Random.create(seed), 200);
                if (layout == null) failed++; else shipped.add(layout);
            }
            double ms = (System.nanoTime() - t0) / 1e6 / SHIPPED_SAMPLES;
            coverage("as generated in game", params, shipped);
            miniboss("as generated in game", params, shipped);
            System.out.printf("timing: %d/%d temples failed all 200 attempts, %.1f ms per temple%n", failed, SHIPPED_SAMPLES, ms);
            System.out.println();
        }
    }

    // ---------------------------------------------------------------- helpers
    private static String fmt(int v) { return v == Integer.MAX_VALUE ? "n/a" : String.valueOf(v); }

    private static int floorCells(int size, int floor) {
        int w = size - 2 * floor;
        return w * w;
    }

    /** "temple/corner_2" -> "corner" */
    private static String nameOf(RoomDef def) {
        return def.templates().get(0).getPath().replaceFirst("^temple/", "").replaceFirst("_\\d+$", "");
    }

    // ---------------------------------------------------------------- indicators
    /** How much of each floor's grid is covered by rooms. */
    private static void coverage(String label, TempleLayout.Params params, List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty()) return;
        int floors = params.floors();
        double[] sum = new double[floors], worst = new double[floors];
        Arrays.fill(worst, 1.0);
        double whole = 0, wholeWorst = 1.0;

        for (List<TempleLayout.Placed> layout : layouts) {
            int[] covered = new int[floors];
            for (TempleLayout.Placed p : layout) {
                if (p.y() >= floors) continue;   // the boss room is outside the grid
                covered[p.y()] += p.shape().w() * p.shape().d();
            }
            int coveredAll = 0, cellsAll = 0;
            for (int f = 0; f < floors; f++) {
                double c = covered[f] / (double) floorCells(params.size(), f);
                sum[f] += c;
                worst[f] = Math.min(worst[f], c);
                coveredAll += covered[f];
                cellsAll += floorCells(params.size(), f);
            }
            double w = coveredAll / (double) cellsAll;
            whole += w;
            wholeWorst = Math.min(wholeWorst, w);
        }

        StringBuilder sb = new StringBuilder("coverage per floor, " + label + " (average / worst):");
        for (int f = 0; f < floors; f++)
            sb.append(String.format("  floor %d: %.0f%% / %.0f%%", f, 100 * sum[f] / layouts.size(), 100 * worst[f]));
        sb.append(String.format("   | whole temple: %.0f%% / %.0f%%", 100 * whole / layouts.size(), 100 * wholeWorst));
        System.out.println(sb);
    }

    /** What the empty cells look like: scattered leftovers, or big unvisited blocks? */
    private static void holes(TempleLayout.Params params, List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty()) return;
        int floors = params.floors(), size = params.size();
        double[] holeCells = new double[floors], edgeCells = new double[floors];
        double[] gaps = new double[floors], largestSum = new double[floors];
        int[] largestWorst = new int[floors];
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (List<TempleLayout.Placed> layout : layouts) {
            boolean[][][] covered = new boolean[floors][size][size];
            for (TempleLayout.Placed p : layout) {
                if (p.y() >= floors) continue;
                for (int cx = 0; cx < p.shape().w(); cx++)
                    for (int cz = 0; cz < p.shape().d(); cz++)
                        covered[p.y()][p.x() + cx][p.z() + cz] = true;
            }
            for (int f = 0; f < floors; f++) {
                int lo = f, hi = size - 1 - f, largest = 0;
                boolean[][] seen = new boolean[size][size];
                for (int x = lo; x <= hi; x++) {
                    for (int z = lo; z <= hi; z++) {
                        if (covered[f][x][z]) continue;
                        holeCells[f]++;
                        if (x == lo || x == hi || z == lo || z == hi) edgeCells[f]++;   // touches the floor's outer boundary
                        if (seen[x][z]) continue;

                        // flood fill: one "gap" = a group of empty cells touching each other
                        gaps[f]++;
                        int cluster = 0;
                        ArrayDeque<int[]> queue = new ArrayDeque<>();
                        queue.add(new int[]{x, z});
                        seen[x][z] = true;
                        while (!queue.isEmpty()) {
                            int[] c = queue.poll();
                            cluster++;
                            for (int[] s : steps) {
                                int nx = c[0] + s[0], nz = c[1] + s[1];
                                if (nx < lo || nx > hi || nz < lo || nz > hi || covered[f][nx][nz] || seen[nx][nz]) continue;
                                seen[nx][nz] = true;
                                queue.add(new int[]{nx, nz});
                            }
                        }
                        largest = Math.max(largest, cluster);
                    }
                }
                largestSum[f] += largest;
                largestWorst[f] = Math.max(largestWorst[f], largest);
            }
        }

        double n = layouts.size();
        System.out.println("empty cells per floor (average per temple, single attempts):");
        for (int f = 0; f < floors; f++) {
            System.out.printf("    floor %d: %.1f empty cells (%.0f%% on the outer ring), %.1f separate gaps, largest gap %.1f avg / %d worst%n",
                    f, holeCells[f] / n, holeCells[f] == 0 ? 0 : 100 * edgeCells[f] / holeCells[f],
                    gaps[f] / n, largestSum[f] / n, largestWorst[f]);
        }
    }

    /** Which room shapes make up the covered area, and how often a 2x2 room appears. */
    private static void shapes(List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty()) return;
        long cells1 = 0, cells2 = 0, cells4 = 0, big = 0;
        int templesWithBig = 0;
        for (List<TempleLayout.Placed> layout : layouts) {
            boolean any = false;
            for (TempleLayout.Placed p : layout) {
                if (p.def() == TempleRooms.ENTRANCE || p.def() == TempleRooms.BOSS) continue;
                int area = p.shape().w() * p.shape().d();
                if (area == 1) cells1++;
                else if (area == 2) cells2 += 2;
                else { cells4 += 4; big++; any = true; }
            }
            if (any) templesWithBig++;
        }
        double total = cells1 + cells2 + cells4;
        System.out.printf("shape mix (share of covered area, entrance excluded): 1x1 %.0f%% | 1x2 %.0f%% | 2x2 %.0f%%   "
                        + "| temples with a 2x2 room besides the entrance: %.0f%% (%.1f per temple)%n",
                100 * cells1 / total, 100 * cells2 / total, 100 * cells4 / total,
                100.0 * templesWithBig / layouts.size(), big / (double) layouts.size());
    }

    /** The least-used room types, to spot ones that are too rare to matter. */
    private static void usage(List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty()) return;
        Map<RoomDef, int[]> stats = new IdentityHashMap<>();
        for (RoomDef d : TempleRooms.ALL) stats.put(d, new int[2]);   // [rooms in total, temples containing one]
        for (List<TempleLayout.Placed> layout : layouts) {
            Set<RoomDef> seen = Collections.newSetFromMap(new IdentityHashMap<RoomDef, Boolean>());
            for (TempleLayout.Placed p : layout) {
                int[] s = stats.get(p.def());
                if (s == null) continue;   // entrance, penultimate, boss
                s[0]++;
                if (seen.add(p.def())) s[1]++;
            }
        }
        List<RoomDef> sorted = new ArrayList<>(TempleRooms.ALL);
        sorted.sort((a, b) -> stats.get(a)[0] - stats.get(b)[0]);

        StringBuilder sb = new StringBuilder("rarest room types (average per temple, share of temples that have one):");
        for (int i = 0; i < Math.min(8, sorted.size()); i++) {
            RoomDef d = sorted.get(i);
            sb.append(String.format("%n    %-18s %5.2f   %3.0f%%", nameOf(d),
                    stats.get(d)[0] / (double) layouts.size(), 100.0 * stats.get(d)[1] / layouts.size()));
        }
        System.out.println(sb);
    }

    /** Checks that every rule holds on every generated layout. */
    private static void rules(TempleLayout.Params params, List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty()) return;
        int floors = params.floors(), size = params.size();
        int sameType = 0, sameRoom = 0, treasureViolations = 0, stairViolations = 0;
        int minTreasure = Integer.MAX_VALUE, maxTreasure = 0, minStairs = Integer.MAX_VALUE, maxStairs = 0;
        double topShare = 0;

        for (List<TempleLayout.Placed> layout : layouts) {
            Map<TempleLayout.Cell, TempleLayout.Placed> cells = new HashMap<>();
            for (TempleLayout.Placed p : layout)
                for (int cx = 0; cx < p.shape().w(); cx++)
                    for (int cz = 0; cz < p.shape().d(); cz++)
                        cells.put(new TempleLayout.Cell(p.x() + cx, p.y(), p.z() + cz), p);

            // rule 1: connected rooms of the same type / template (each pair is seen from both sides)
            for (TempleLayout.Placed p : layout) {
                if (p.def().ports().stream().anyMatch(q -> q.side() == Dir.DOWN)) continue;   // groundless rooms are exempt
                for (Port q : p.shape().ports()) {
                    TempleLayout.Placed other = cells.get(
                            new TempleLayout.Cell(p.x() + q.cx(), p.y(), p.z() + q.cz()).offset(q.side()));
                    if (other != null && other != p && other.def() == p.def()) {
                        sameType++;
                        if (other.variant() == p.variant()) sameRoom++;
                    }
                }
            }

            // rule 3: treasure rooms per floor and the distance between them
            for (int f = 0; f < floors; f++) {
                final int floor = f;
                List<TempleLayout.Placed> ts = layout.stream()
                        .filter(p -> p.y() == floor && p.def().isTreasure(p.variant())).toList();
                minTreasure = Math.min(minTreasure, ts.size());
                maxTreasure = Math.max(maxTreasure, ts.size());
                int required = TempleLayout.scaledDistance(TempleLayout.TREASURE_MIN_DISTANCE, size, f);
                for (int i = 0; i < ts.size(); i++)
                    for (int j = i + 1; j < ts.size(); j++)
                        if (TempleLayout.gap(ts.get(i), ts.get(j)) < required) treasureViolations++;
            }

            // rule 4: stairs per floor, distance between them and to the arrival point
            for (int f = 0; f < floors - 1; f++) {   // the top regular floor has no stairs
                final int floor = f;
                List<TempleLayout.Placed> st = layout.stream()
                        .filter(p -> p.y() == floor && p.def().ports().stream().anyMatch(q -> q.side() == Dir.UP)).toList();
                List<TempleLayout.Placed> arrivals = layout.stream()
                        .filter(p -> p.y() == floor && (p.def() == TempleRooms.ENTRANCE
                                || p.def().ports().stream().anyMatch(q -> q.side() == Dir.DOWN))).toList();
                minStairs = Math.min(minStairs, st.size());
                maxStairs = Math.max(maxStairs, st.size());
                int apart = TempleLayout.scaledDistance(TempleLayout.STAIRS_MIN_DISTANCE, size, f);
                int fromArrival = TempleLayout.scaledDistance(TempleLayout.STAIRS_MIN_ARRIVAL_DISTANCE, size, f);
                for (int i = 0; i < st.size(); i++) {
                    for (int j = i + 1; j < st.size(); j++)
                        if (TempleLayout.gap(st.get(i), st.get(j)) < apart) stairViolations++;
                    for (TempleLayout.Placed a : arrivals)
                        if (TempleLayout.gap(st.get(i), a) < fromArrival) stairViolations++;
                }
            }

            // rule 2: how dominant is the most common single room (template) on the ground floor?
            Map<Identifier, Integer> counts = new HashMap<>();
            int total = 0;
            for (TempleLayout.Placed p : layout) {
                if (p.y() != 0 || p.def() == TempleRooms.ENTRANCE) continue;
                counts.merge(p.template(), 1, Integer::sum);
                total++;
            }
            topShare += counts.values().stream().max(Integer::compare).orElse(0) / (double) Math.max(1, total);
        }

        StringBuilder spacing = new StringBuilder("spacing required per floor:");
        for (int f = 0; f < Math.max(1, floors); f++)
            spacing.append(' ').append(TempleLayout.scaledDistance(TempleLayout.STAIRS_MIN_DISTANCE, size, f));

        double n = layouts.size();
        System.out.printf("rules: same-type pairs %.2f per temple (same-room %.2f)  | treasure per floor %s..%s, spacing violations %d%n",
                sameType / 2.0 / n, sameRoom / 2.0 / n, fmt(minTreasure), fmt(maxTreasure), treasureViolations);
        System.out.printf("       stairs per floor %s..%s, spacing violations %d  | most common single room on floor 0: %.0f%%  | %s%n",
                fmt(minStairs), fmt(maxStairs == 0 ? Integer.MAX_VALUE : maxStairs), stairViolations, 100 * topShare / n, spacing);
    }
    /** How often a floor gets a miniboss room, and the most any single floor ever gets. */
    private static void miniboss(String label, TempleLayout.Params params, List<List<TempleLayout.Placed>> layouts) {
        if (layouts.isEmpty() || TempleRooms.ALL.stream().noneMatch(RoomDef::miniboss)) return;
        int floors = params.floors();
        int[] withOne = new int[floors];
        int most = 0;
        for (List<TempleLayout.Placed> layout : layouts) {
            for (int f = 0; f < floors; f++) {
                final int floor = f;
                int count = (int) layout.stream().filter(p -> p.y() == floor && p.def().miniboss()).count();
                if (count > 0) withOne[f]++;
                most = Math.max(most, count);
            }
        }
        StringBuilder sb = new StringBuilder("miniboss rooms, " + label + " (share of floors that have one):");
        for (int f = 0; f < floors; f++)
            sb.append(String.format("  floor %d: %.0f%%", f, 100.0 * withOne[f] / layouts.size()));
        sb.append("   | most on any one floor: ").append(most).append(" (must be <= 1)");
        System.out.println(sb);
    }
}