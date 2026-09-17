package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum LarvaeVariant implements IVariant, StringIdentifiable {
    REGULAR(0, "Regular"),
    CADDISFLY(1, "Caddisfly");

    private final int id;
    private final String name; // Associated name

    LarvaeVariant(int id, String name) {
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

    public static LarvaeVariant byId(int id) {
        return IVariant.byId(LarvaeVariant.class, id); // Call the generic method
    }

    @Override
    public String asString() {
        return this.name;
    }
}
