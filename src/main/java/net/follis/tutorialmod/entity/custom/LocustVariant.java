package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;

public enum LocustVariant implements IVariant {
    GOLD(0, "Gold"),
    DREAM(1, "Dream"),
    GRASSHOPPER(2, "Grasshopper"),
    RED(3, "Red");

    private final int id;
    private final String name; // Associated name

    LocustVariant(int id, String name) {
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

    public static LocustVariant byId(int id) {
        return IVariant.byId(LocustVariant.class, id); // Call the generic method
    }
}
