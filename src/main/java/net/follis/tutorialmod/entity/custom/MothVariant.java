package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;

public enum MothVariant implements IVariant {
    VERY_RARE(0, "Very Rare"), // Channels effects
    OAK_HYPNO(1, "Hypnotic Oak"),       // Hypnotic
    RARE2(2, "Rare 2"),       // Hypnotic
    RARE3(3, "Rare 3"),       // Hypnotic
    OAK(4, "Oak"),
    BIRCH(5, "Birch"),
    SPRUCE(6, "Spruce"),
    DARK_OAK(7, "Dark Oak"),
    CHERRY(8, "Cherry"),
    JUNGLE(9, "Jungle");

    private final int id;
    private final String name; // Associated name

    MothVariant(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name; // Return the associated name
    }

    public static MothVariant byId(int id) {
        return IVariant.byId(MothVariant.class, id); // Call the generic method
    }
}
