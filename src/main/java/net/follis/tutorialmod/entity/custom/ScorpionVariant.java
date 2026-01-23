package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum ScorpionVariant implements IVariant, StringIdentifiable {
    DESERT(0, "Desert"),
    DEF1(1, "Def1"),
    DEF2(2, "Def2"),
    DEF3(3, "Def3");

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
