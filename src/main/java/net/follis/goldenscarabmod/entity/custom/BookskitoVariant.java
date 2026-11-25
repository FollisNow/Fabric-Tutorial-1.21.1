package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum BookskitoVariant implements IVariant, StringIdentifiable {
    GOLD(0, "Gold"),
    REGULAR(1, "Regular");


    private final int id;
    private final String name; // Associated name

    BookskitoVariant(int id, String name) {
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

    public static BookskitoVariant byId(int id) {
        return IVariant.byId(BookskitoVariant.class, id); // Call the generic method
    }

    @Override
    public String asString() {
        return this.name;
    }

}
