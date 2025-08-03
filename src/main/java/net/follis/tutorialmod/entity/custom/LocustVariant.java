package net.follis.tutorialmod.entity.custom;

import java.util.Arrays;
import java.util.Comparator;

public enum LocustVariant {
    GOLD(0),
    DREAM(1),
    GRASSHOPPER(2),
    RED(3);

    private static final LocustVariant[] BY_ID = Arrays.stream(values()).sorted(Comparator.
            comparingInt(LocustVariant::getId)).toArray(LocustVariant[]::new);
    private final int id;

    LocustVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static LocustVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}
