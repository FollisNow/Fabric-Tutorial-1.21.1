package net.follis.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.entity.ModBlockEntities;
import net.follis.tutorialmod.block.entity.renderer.GoldenHotelBlockEntityRenderer;
import net.follis.tutorialmod.block.entity.renderer.GoldenPedestalBlockEntityRenderer;
import net.follis.tutorialmod.client.MesmerizeClientState;
import net.follis.tutorialmod.entity.ModEntities;
import net.follis.tutorialmod.entity.client.*;
import net.follis.tutorialmod.network.MesmerizePayload;
import net.follis.tutorialmod.particle.GoldenChainParticle;
import net.follis.tutorialmod.particle.GoldenLeavesParticle;
import net.follis.tutorialmod.particle.ModParticles;
import net.follis.tutorialmod.particle.PinkGarnetParticle;
import net.follis.tutorialmod.screen.ModScreenHandlers;
import net.follis.tutorialmod.screen.custom.GoldenHotelScreen;
import net.follis.tutorialmod.screen.custom.GrowthChamberScreen;
import net.follis.tutorialmod.screen.custom.GoldenPedestalScreen;
import net.follis.tutorialmod.util.ModModelPredicates;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class TutorialModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PINK_GARNET_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PINK_GARNET_TRAPDOOR, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLD_CHAIN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLD_LANTERN, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CAULIFLOWER_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HONEY_BERRY_BUSH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CRYSTAL_MUSHROOM, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLDEN_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_GOLDEN_SAPLING, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.YELLOW_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_YELLOW_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.AURORA_FIRE_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_AURORA_FIRE_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLUE_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_BLUE_MARIGOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FOLLY_DIANTHUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_FOLLY_DIANTHUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PINK_DIANTHUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_PINK_DIANTHUS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOLD_ROSE_BUSH, RenderLayer.getCutout());


        ModModelPredicates.registerModelPredicates();

        EntityModelLayerRegistry.registerModelLayer(MantisModel.MANTIS, MantisModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.MANTIS, MantisRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(BeetleModel.BEETLE, BeetleModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.BEETLE, BeetleRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(SpiderlingModel.SPIDERLING, SpiderlingModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.SPIDERLING, SpiderlingRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MothModel.MOTH, MothModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.MOTH, MothRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(LocustModel.LOCUST, LocustModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.LOCUST, LocustRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(ScorpionModel.SCORPION, ScorpionModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.SCORPION, ScorpionRenderer::new);


        EntityModelLayerRegistry.registerModelLayer(AmethystBeeModel.AMETHYST_BEE, AmethystBeeModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.AMETHYST_BEE, AmethystBeeRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(GoldenSilverfishModel.GOLDEN_SILVERFISH, GoldenSilverfishModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GOLDEN_SILVERFISH, GoldenSilverfishRenderer::new);

        //GOLD
        EntityModelLayerRegistry.registerModelLayer(GoldCarverModel.GOLD_CARVER, GoldCarverModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GOLD_CARVER, GoldCarverRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(GoldPannerModel.GOLD_PANNER, GoldPannerModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GOLD_PANNER, GoldPannerRenderer::new);


        EntityModelLayerRegistry.registerModelLayer(TomahawkProjectileModel.TOMAHAWK, TomahawkProjectileModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.TOMAHAWK, TomahawkProjectileRenderer::new);

        EntityRendererRegistry.register(ModEntities.DART, DartEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(BambooTrapModel.BAMBOO_TRAP, BambooTrapModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.BAMBOO_TRAP, BambooTrapRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(GoldenNeedleProjectileModel.GOLDEN_NEEDLE, GoldenNeedleProjectileModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.GOLDEN_NEEDLE, GoldenNeedleProjectileRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MiniSunProjectileModel.MINI_SUN, MiniSunProjectileModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.MINI_SUN, MiniSunProjectileRenderer::new);

        EntityRendererRegistry.register(ModEntities.CHAIR, ChairRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.PINK_GARNET_PARTICLE, PinkGarnetParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.GOLDEN_LEAVES_PARTICLE, GoldenLeavesParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.GOLDEN_CHAIN_PARTICLE, GoldenChainParticle.Factory::new);

        BlockEntityRendererFactories.register(ModBlockEntities.GOLDEN_PEDESTAL_BE, GoldenPedestalBlockEntityRenderer::new);
        HandledScreens.register(ModScreenHandlers.GOLDEN_PEDESTAL_SCREEN_HANDLER, GoldenPedestalScreen::new);

        BlockEntityRendererFactories.register(ModBlockEntities.GOLDEN_HOTEL_BE, GoldenHotelBlockEntityRenderer::new);
        HandledScreens.register(ModScreenHandlers.GOLDEN_HOTEL_SCREEN_HANDLER, GoldenHotelScreen::new);

        HandledScreens.register(ModScreenHandlers.GROWTH_CHAMBER_SCREEN_HANDLER, GrowthChamberScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(MesmerizePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MesmerizeClientState.active = payload.active();
                MesmerizeClientState.targetEntityId = payload.targetEntityId();
                MesmerizeClientState.degreesPerSecond = payload.degreesPerSecond();
            });
        });
    }
}
