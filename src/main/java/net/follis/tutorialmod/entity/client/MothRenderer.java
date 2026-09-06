package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.MothEntity;
import net.follis.tutorialmod.entity.custom.MothVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class MothRenderer extends MobEntityRenderer<MothEntity, MothModel<MothEntity>> {
    private static final Map<MothVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(MothVariant.class), map -> {
                map.put(MothVariant.OAK,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_oak.png"));
                map.put(MothVariant.OAK_CRACKED,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_oak_cracked.png"));
                map.put(MothVariant.BIRCH,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_birch.png"));
                map.put(MothVariant.BIRCH_RADIANT,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_birch_radiant.png"));
                map.put(MothVariant.BIRCH_NEGATIVE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_birch_negative.png"));
                map.put(MothVariant.SPRUCE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_spruce.png"));
                map.put(MothVariant.SPRUCE_RUBY,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_spruce_ruby.png"));
                map.put(MothVariant.DARK_OAK,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_dark_oak.png"));
                map.put(MothVariant.DARK_OAK_EMERALD,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_dark_oak_emerald.png"));
                map.put(MothVariant.CHERRY,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_cherry.png"));
                map.put(MothVariant.CHERRY_BLOOM,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_cherry_bloom.png"));
                map.put(MothVariant.MANGROVE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_mangrove.png"));
                map.put(MothVariant.MANGROVE_TANGLED,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_mangrove_tangled.png"));
                map.put(MothVariant.JUNGLE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_jungle.png"));
                map.put(MothVariant.JUNGLE_SPIDER,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_jungle_spider.png"));
                map.put(MothVariant.ACACIA,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_acacia.png"));
                map.put(MothVariant.ACACIA_SAPPHIRE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth_acacia_sapphire.png"));
                map.put(MothVariant.EYE,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth.png"));
            });

    public MothRenderer(EntityRendererFactory.Context context) {
        super(context, new MothModel<>(context.getPart(MothModel.MOTH)), 0.4f);
    }

    @Override
    public Identifier getTexture(MothEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    public void render(MothEntity livingEntity, float yaw, float tickDelta, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
}
