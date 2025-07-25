package net.follis.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.renderer.PedestalBlockEntityRenderer;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.client.*;
import net.follis.tutorialmod.particle.GoldenLeavesParticle;
import net.follis.tutorialmod.particle.ModParticles;
import net.follis.tutorialmod.particle.PinkGarnetParticle;
import net.follis.tutorialmod.screen.ModScreenHandlers;
import net.follis.tutorialmod.screen.custom.GrowthChamberScreen;
import net.follis.tutorialmod.screen.custom.PedestalScreen;
import net.follis.tutorialmod.util.ModModelPredicates;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class TutorialModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PINK_GARNET_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PINK_GARNET_TRAPDOOR, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CAULIFLOWER_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HONEY_BERRY_BUSH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CRYSTAL_MUSHROOM, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLDEN_SAPLING, RenderLayer.getCutout());

        ModModelPredicates.registerModelPredicates();

        EntityModelLayerRegistry.registerModelLayer(MantisModel.MANTIS, MantisModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.MANTIS, MantisRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(LocustModel.LOCUST, LocustModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.LOCUST, LocustRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(AmethystBeeModel.AMETHYST_BEE, AmethystBeeModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.AMETHYST_BEE, AmethystBeeRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(TomahawkProjectileModel.TOMAHAWK, TomahawkProjectileModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.TOMAHAWK, TomahawkProjectileRenderer::new);

        EntityRendererRegistry.register(ModEntities.CHAIR, ChairRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.PINK_GARNET_PARTICLE, PinkGarnetParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.GOLDEN_LEAVES_PARTICLE, GoldenLeavesParticle.Factory::new);

        BlockEntityRendererFactories.register(ModBlockEntities.PEDESTAL_BE, PedestalBlockEntityRenderer::new);
        HandledScreens.register(ModScreenHandlers.PEDESTAL_SCREEN_HANDLER, PedestalScreen::new);

        HandledScreens.register(ModScreenHandlers.GROWTH_CHAMBER_SCREEN_HANDLER, GrowthChamberScreen::new);
    }
}
