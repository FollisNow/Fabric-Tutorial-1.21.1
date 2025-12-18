package net.follis.tutorialmod.entity.custom;

import java.util.Arrays;
import java.util.Comparator;

public enum SpiderlingVariant {
    DEFAULT(0);

    private static final SpiderlingVariant[] BY_ID = Arrays.stream(values()).sorted(Comparator.
            comparingInt(SpiderlingVariant::getId)).toArray(SpiderlingVariant[]::new);
    private final int id;

    SpiderlingVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static SpiderlingVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}
