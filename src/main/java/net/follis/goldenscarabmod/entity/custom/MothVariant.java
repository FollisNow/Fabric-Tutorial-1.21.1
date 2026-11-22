package net.follis.goldenscarabmod.entity.custom;

import net.follis.goldenscarabmod.util.IVariant;
import net.minecraft.util.StringIdentifiable;

public enum MothVariant implements IVariant, StringIdentifiable {
    GOLD(0, "Gold"),
    OAK(1, "Oak"),
    OAK_CRACKED(2, "Cracked Oak"),
    BIRCH(3, "Birch"),
    BIRCH_RADIANT(4, "Radiant Birch"),
    BIRCH_NEGATIVE(5, "Anti Birch"),
    SPRUCE(6, "Spruce"),
    SPRUCE_RUBY(7, "Ruby Spruce"),
    DARK_OAK(8, "Dark Oak"),
    DARK_OAK_EMERALD(9, "Dark Oak Emerald"),
    CHERRY(10, "Cherry"),
    CHERRY_BLOOM(11, "Bloom Cherry"),
    MANGROVE(12, "Mangrove"),
    MANGROVE_TANGLED(13, "Tangled Mangrove"),
    JUNGLE(14, "Jungle"),
    JUNGLE_SPIDER(15, "Spider Jungle"),
    ACACIA(16, "Acacia"),
    ACACIA_SAPPHIRE(17, "Sapphire Acacia"),
    EYE(18, "Eye");


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

    public boolean isNotMesmerizing() {
        return !switch (this) {
            case GOLD, OAK_CRACKED, BIRCH_RADIANT, BIRCH_NEGATIVE, SPRUCE_RUBY, DARK_OAK_EMERALD,
                 CHERRY_BLOOM, MANGROVE_TANGLED, JUNGLE_SPIDER, ACACIA_SAPPHIRE -> true;
            default -> false;
        };
    }
}
