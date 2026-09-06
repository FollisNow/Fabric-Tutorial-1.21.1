package net.follis.tutorialmod.entity.custom;

import net.follis.tutorialmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum MothVariant implements IVariant, StringIdentifiable {
    OAK(0, "Oak"),
    OAK_CRACKED(1, "Cracked Oak"),
    BIRCH(2, "Birch"),
    BIRCH_RADIANT(3, "Radiant Birch"),
    BIRCH_NEGATIVE(4, "Anti Birch"),
    SPRUCE(5, "Spruce"),
    SPRUCE_RUBY(6, "Ruby Spruce"),
    DARK_OAK(7, "Dark Oak"),
    DARK_OAK_EMERALD(8, "Dark Oak Emerald"),
    CHERRY(9, "Cherry"),
    CHERRY_BLOOM(10, "Bloom Cherry"),
    MANGROVE(11, "Mangrove"),
    MANGROVE_TANGLED(12, "Tangled Mangrove"),
    JUNGLE(13, "Jungle"),
    JUNGLE_SPIDER(14, "Spider Jungle"),
    ACACIA(15, "Acacia"),
    ACACIA_SAPPHIRE(16, "Sapphire Acacia"),
    EYE(17, "Eye");

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

    @Override
    public String asString() {
        return this.name;
    }
}
