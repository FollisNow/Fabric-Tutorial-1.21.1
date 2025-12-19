package net.follis.tutorialmod.util;

public interface IVariant {
    int getId(); // Method to get the ID of the variant
    String getName(); // Method to get the name of the variant

    static <T extends Enum<T> & IVariant> T byId(Class<? extends Enum<? extends IVariant>> enumClass, int id) {
        // Default implementation to retrieve enum by ID
        T[] variants = (T[]) enumClass.getEnumConstants();
        return variants[id % variants.length]; // Ensure valid access
    }

}
