package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.BookskitoEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;

/** Draws the enchanted-book shimmer over the enchanted (gold) bookskito. */
public class BookskitoEnchantGlintFeatureRenderer extends FeatureRenderer<BookskitoEntity, BookskitoModel<BookskitoEntity>> {
    public BookskitoEnchantGlintFeatureRenderer(FeatureRendererContext<BookskitoEntity, BookskitoModel<BookskitoEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BookskitoEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        if (!entity.isEnchanted() || entity.isInvisible()) return;

        VertexConsumer glint = vertexConsumers.getBuffer(RenderLayer.getDirectEntityGlint());
        this.getContextModel().render(matrices, glint, light, OverlayTexture.DEFAULT_UV, -1);
    }
}