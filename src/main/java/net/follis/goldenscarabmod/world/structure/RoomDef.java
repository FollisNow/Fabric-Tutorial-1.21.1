package net.follis.goldenscarabmod.world.structure;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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