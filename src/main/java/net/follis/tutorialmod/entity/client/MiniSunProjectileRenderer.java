package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.MiniSunProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class MiniSunProjectileRenderer extends EntityRenderer<MiniSunProjectileEntity> {
    protected MiniSunProjectileModel model;

    public MiniSunProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new MiniSunProjectileModel(ctx.getPart(MiniSunProjectileModel.MINI_SUN));
    }

    @Override
    public void render(MiniSunProjectileEntity miniSunProjectile, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {

        matrixStack.push();
        matrixStack.translate(0, 0, 0);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(miniSunProjectile.getRenderingRotation() * 5f));
        matrixStack.translate(0, -1.0f, 0);


        VertexConsumer vertexConsumer = ItemRenderer.getDirectItemGlintConsumer(vertexConsumerProvider, this.model.getLayer(this.getTexture(miniSunProjectile)), false, false);
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
        super.render(miniSunProjectile, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(MiniSunProjectileEntity entity) {
        return Identifier.of(TutorialMod.MOD_ID, "textures/entity/mini_sun/mini_sun.png");
    }
}
