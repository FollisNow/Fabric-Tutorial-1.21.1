package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

public class ModPointOfInterestTypes {
    public static final PointOfInterestType AMETHYST_BEE_HOME_POI = registerPOI("amethyst_bee_home_poi", ModBlocks.AMETHYST_BEE_HIVE);
    public static final PointOfInterestType TEST_POI = registerPOI("test_poi", Blocks.DIAMOND_BLOCK);

    private static PointOfInterestType registerPOI(String name, Block block) {
        return PointOfInterestHelper.register(Identifier.of(TutorialMod.MOD_ID, name),
                3, 16, block);
    }
    public static void register() {
        // This method will be called during the initialization phase
        // Ensure this method is called before the registry is frozen
        // Registration logic here
    }

}
