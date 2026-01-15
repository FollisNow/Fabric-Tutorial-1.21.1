package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum BeetleVariant implements IVariant, StringIdentifiable {
    OMEN(0, "Omen"),
    ROSE_CHAFER(1, "Rose Chafer"),
    LADYBUG(2, "Ladybug"),
    BARK(3, "Bark"),
    RHINOCEROS(4, "Rhinoceros");

    private final int id;
    private final String name; // Associated name

    BeetleVariant(int id, String name) {
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

    public static BeetleVariant byId(int id) {
        return IVariant.byId(BeetleVariant.class, id); // Call the generic method
    }

    @Override
    public String asString() {
        return this.name;
    }
}
