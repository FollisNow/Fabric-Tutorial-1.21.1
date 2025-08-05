package net.follis.tutorialmod.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.util.ModTags;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EntityTypeTags;

import java.util.concurrent.CompletableFuture;

public class ModEntityTypeTagProvider extends FabricTagProvider.EntityTypeTagProvider {
    public ModEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(EntityTypeTags.BEEHIVE_INHABITORS)
                .add(ModEntities.AMETHYST_BEE);

        getOrCreateTagBuilder(EntityTypeTags.ARTHROPOD)
                .add(ModEntities.AMETHYST_BEE)
                .add(ModEntities.BEETLE)
                .add(ModEntities.LOCUST)
                .add(ModEntities.MANTIS);
    }
}
