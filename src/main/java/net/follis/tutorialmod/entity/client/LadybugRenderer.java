package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.LadybugEntity;
import net.follis.tutorialmod.entity.custom.LadybugVariant;
import net.follis.tutorialmod.entity.custom.MantisEntity;
import net.follis.tutorialmod.entity.custom.MantisVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class LadybugRenderer extends MobEntityRenderer<LadybugEntity, LadybugModel<LadybugEntity>> {
    private static final Map<LadybugVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(LadybugVariant.class), map -> {
                map.put(LadybugVariant.DEFAULT,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/ladybug/ladybug.png"));
                map.put(LadybugVariant.OMEN,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/ladybug/ladybug_omen.png"));
            });

    public LadybugRenderer(EntityRendererFactory.Context context) {
        super(context, new LadybugModel<>(context.getPart(LadybugModel.LADYBUG)), 0.4f);
    }

    @Override
    public Identifier getTexture(LadybugEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    public void render(LadybugEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
