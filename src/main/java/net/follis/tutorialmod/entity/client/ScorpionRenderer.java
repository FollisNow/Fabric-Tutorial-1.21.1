package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.ScorpionEntity;
import net.follis.tutorialmod.entity.custom.ScorpionVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class ScorpionRenderer extends MobEntityRenderer<ScorpionEntity, ScorpionModel<ScorpionEntity>> {
    private static final Map<ScorpionVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(ScorpionVariant.class), map -> {
                map.put(ScorpionVariant.DESERT,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/scorpion/scorpion_desert.png"));
                map.put(ScorpionVariant.DEF1,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/scorpion/scorpion_desert.png"));
                map.put(ScorpionVariant.DEF2,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/scorpion/scorpion_desert.png"));
                map.put(ScorpionVariant.DEF3,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/scorpion/scorpion_desert.png"));
            });

    public ScorpionRenderer(EntityRendererFactory.Context context) {
        super(context, new ScorpionModel<>(context.getPart(ScorpionModel.SCORPION)), 0.4f);
    }

    @Override
    public Identifier getTexture(ScorpionEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    public void render(ScorpionEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
