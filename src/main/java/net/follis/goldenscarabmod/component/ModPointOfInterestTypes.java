package net.follis.goldenscarabmod.component;

import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

public class ModPointOfInterestTypes {
    public static final PointOfInterestType AMETHYST_BEE_HOME_POI = registerPOI("amethyst_bee_home_poi", ModBlocks.AMETHYST_BEE_HIVE);
    public static final PointOfInterestType TEST_POI = registerPOI("test_poi", Blocks.DIAMOND_BLOCK);

    private static PointOfInterestType registerPOI(String name, Block block) {
        return PointOfInterestHelper.register(Identifier.of(GoldenScarabMod.MOD_ID, name),
                4, 16, block);
    }
    public static void registerPointOfInterestTypes() {
        GoldenScarabMod.LOGGER.info("Registering POI types for " + GoldenScarabMod.MOD_ID);
    }
}
