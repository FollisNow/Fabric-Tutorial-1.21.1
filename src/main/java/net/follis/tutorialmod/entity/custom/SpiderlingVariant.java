package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;

public enum SpiderlingVariant implements IVariant {
    DEFAULT(0, "Default");

    private final int id;
    private final String name; // Associated name

    SpiderlingVariant(int id, String name) {
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

    public static SpiderlingVariant byId(int id) {
        return IVariant.byId(SpiderlingVariant.class, id); // Call the generic method
    }
}
