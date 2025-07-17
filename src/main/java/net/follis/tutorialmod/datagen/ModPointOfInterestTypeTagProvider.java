package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.concurrent.CompletableFuture;

public class ModPointOfInterestTypeTagProvider extends FabricTagProvider<PointOfInterestType> {
    public ModPointOfInterestTypeTagProvider(FabricDataOutput output, RegistryKey<? extends Registry<PointOfInterestType>> registryKey, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registryKey, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(ModTags.PointOfInterestTypes.AMETHYST_BEE_HOME)
                .add(ModPointOfInterestTypes.AMETHYST_BEE_HOME_POI)
                .add(ModPointOfInterestTypes.TEST_POI)
                .add(PointOfInterestTypes.BEEHIVE);
    }
}
