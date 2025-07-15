package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LocustRenderer extends MobEntityRenderer<LocustEntity, LocustModel<LocustEntity>> {
    public LocustRenderer(EntityRendererFactory.Context context) {
        super(context, new LocustModel<>(context.getPart(LocustModel.LOCUST)), 0.4f);
    }

    @Override
    public Identifier getTexture(LocustEntity entity) {
        return Identifier.of(TutorialMod.MOD_ID, "textures/entity/locust/locust.png");
    }

    @Override
    public void render(LocustEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }

        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
