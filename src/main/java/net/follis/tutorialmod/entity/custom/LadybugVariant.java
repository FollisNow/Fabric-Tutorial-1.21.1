package net.follis.tutorialmod.entity.custom;

import java.util.Arrays;
import java.util.Comparator;

public enum LadybugVariant {
    DEFAULT(0),
    OMEN(1);

    private static final LadybugVariant[] BY_ID = Arrays.stream(values()).sorted(Comparator.
            comparingInt(LadybugVariant::getId)).toArray(LadybugVariant[]::new);
    private final int id;

    LadybugVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static LadybugVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}
