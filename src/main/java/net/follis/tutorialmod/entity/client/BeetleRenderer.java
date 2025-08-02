package net.follis.tutorialmod.entity.client;

import com.google.common.collect.Maps;
import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.BeetleEntity;
import net.follis.tutorialmod.entity.custom.BeetleVariant;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Map;

public class BeetleRenderer extends MobEntityRenderer<BeetleEntity, BeetleModel<BeetleEntity>> {
    private static final Map<BeetleVariant, Identifier> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(BeetleVariant.class), map -> {
                map.put(BeetleVariant.DEFAULT,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/beetle/beetle.png"));
                map.put(BeetleVariant.OMEN,
                        Identifier.of(TutorialMod.MOD_ID, "textures/entity/beetle/beetle_omen.png"));
            });

    public BeetleRenderer(EntityRendererFactory.Context context) {
        super(context, new BeetleModel<>(context.getPart(BeetleModel.BEETLE)), 0.4f);
    }

    @Override
    public Identifier getTexture(BeetleEntity entity) {
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    public void render(BeetleEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
