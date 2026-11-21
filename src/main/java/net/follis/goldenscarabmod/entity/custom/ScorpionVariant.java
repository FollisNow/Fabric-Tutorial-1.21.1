package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum ScorpionVariant implements IVariant, StringIdentifiable {
    GOLD(0, "Gold"),
    RED(1, "Red"),
    BLACK(2, "Black"),
    BLUE(3, "Blue");

    private final int id;
    private final String name; // Associated name

    ScorpionVariant(int id, String name) {
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

    public static ScorpionVariant byId(int id) {
        return IVariant.byId(ScorpionVariant.class, id); // Call the generic method
    }

    @Override
    public String asString() {
        return this.name;
    }
}
