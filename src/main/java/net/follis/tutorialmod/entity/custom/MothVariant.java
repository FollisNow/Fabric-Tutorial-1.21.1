package net.follis.tutorialmod.entity.custom;

import java.util.Arrays;
import java.util.Comparator;

public enum MothVariant {
    OMEN(0),
    SCARAB(1),
    LADYBUG(2),
    BARK(3);

    private static final MothVariant[] BY_ID = Arrays.stream(values()).sorted(Comparator.
            comparingInt(MothVariant::getId)).toArray(MothVariant[]::new);
    private final int id;

    MothVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static MothVariant byId(int id) {
        return BY_ID[id % BY_ID.length];
    }
}
