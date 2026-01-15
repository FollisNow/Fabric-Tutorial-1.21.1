package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum MantisVariant implements IVariant, StringIdentifiable {
    DEFAULT(0, "Default"),
    ORCHID(1, "Orchid");

    private final int id;
    private final String name; // Associated name

    MantisVariant(int id, String name) {
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

    public static MantisVariant byId(int id) {
        return IVariant.byId(MantisVariant.class, id); // Call the generic method
    }

    @Override
    public String asString() {
        return this.name;
    }
}
