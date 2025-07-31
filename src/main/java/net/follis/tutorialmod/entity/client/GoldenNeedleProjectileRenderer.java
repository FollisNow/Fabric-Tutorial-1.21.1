package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldenNeedleProjectileEntity;
import net.follis.tutorialmod.entity.custom.TomahawkProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class GoldenNeedleProjectileRenderer extends EntityRenderer<GoldenNeedleProjectileEntity> {
    protected GoldenNeedleProjectileModel model;

    public GoldenNeedleProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new GoldenNeedleProjectileModel(ctx.getPart(GoldenNeedleProjectileModel.GOLDEN_NEEDLE));
    }

    @Override
    public void render(GoldenNeedleProjectileEntity goldenNeedleProjectile, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {

        matrixStack.push();
        matrixStack.translate(0, 0, 0);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(g, goldenNeedleProjectile.prevYaw, goldenNeedleProjectile.getYaw()) -90.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(g, goldenNeedleProjectile.prevPitch, goldenNeedleProjectile.getPitch()) + 90.0F));

        VertexConsumer vertexConsumer = ItemRenderer.getDirectItemGlintConsumer(vertexConsumerProvider, this.model.getLayer(this.getTexture(goldenNeedleProjectile)), false, false);
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
        super.render(goldenNeedleProjectile, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(GoldenNeedleProjectileEntity entity) {
        return Identifier.of(TutorialMod.MOD_ID, "textures/entity/golden_needle/golden_needle.png");
    }
}
