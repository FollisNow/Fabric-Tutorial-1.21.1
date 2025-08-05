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
                map.put(MothVariant.OMEN,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth.png"));
                map.put(MothVariant.SCARAB,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth.png"));
                map.put(MothVariant.LADYBUG,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/moth/moth.png"));
                map.put(MothVariant.BARK,
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
