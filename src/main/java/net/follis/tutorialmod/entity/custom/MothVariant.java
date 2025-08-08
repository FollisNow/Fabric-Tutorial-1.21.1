package net.follis.tutorialmod.entity.custom;

import java.util.Arrays;
import java.util.Comparator;

public enum MothVariant {
    VERY_RARE(0), // Channels effects

    RARE1(1), // hypnotic
    RARE2(2), // hypnotic
    RARE3(3), // hypnotic

    OAK(4),
    BIRCH(5),
    SPRUCE(6),
    DARK_OAK(7),
    CHERRY(8),
    JUNGLE(9);

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
