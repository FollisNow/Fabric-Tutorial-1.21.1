package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.entity.custom.CocoonMaterial;
import net.follis.tutorialmod.entity.custom.LarvaeEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

public class LarvaeCocoonFeatureRenderer extends FeatureRenderer<LarvaeEntity, LarvaeModel<LarvaeEntity>> {
    private final LarvaeRenderer parentRenderer;

    public LarvaeCocoonFeatureRenderer(LarvaeRenderer parentRenderer) {
        super(parentRenderer);
        this.parentRenderer = parentRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LarvaeEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        List<CocoonMaterial> segments = entity.getCocoonSegments();
        LarvaeModel<LarvaeEntity> model = this.getContextModel();
        VertexConsumer buffer = vertexConsumers.getBuffer(parentRenderer.getCocoonRenderLayer(entity));

        List<ModelPart> materialParts = model.getMaterialParts();
        for (int i = 0; i < materialParts.size() && i < segments.size(); i++) {
            CocoonMaterial material = segments.get(i);
            if (material == null) continue;

            int argb = material.getColor() | 0xFF000000;
            model.renderMaterialPart(matrices, buffer, light, OverlayTexture.DEFAULT_UV, i, argb);
        }
    }
}