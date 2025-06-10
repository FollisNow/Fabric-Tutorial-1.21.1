package net.follis.tutorialmod.world.gen;

public class ModWorldGeneration {
    public static void generateModWorldGen() { // Should follow GenerationStep order
        ModOreGeneration.generateOres();

        ModTreeGeneration.generateTrees();
        ModBushGeneration.generateBushes();
    }
}
