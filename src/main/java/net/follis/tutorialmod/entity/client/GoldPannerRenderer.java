package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldPannerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class GoldPannerRenderer extends MobEntityRenderer<GoldPannerEntity, GoldPannerModel<GoldPannerEntity>> {
    public static final Identifier TEXTURE = Identifier.of(TutorialMod.MOD_ID, "textures/entity/gold_panner/gold_panner.png");
    private float cachedBodyYaw;

    public GoldPannerRenderer(EntityRendererFactory.Context context) {
        super(context, new GoldPannerModel<>(context.getPart(GoldPannerModel.GOLD_PANNER)), 0.75f);

    }

    @Override
    public Identifier getTexture(GoldPannerEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GoldPannerEntity livingEntity, float f, float g, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i) {
        if(livingEntity.isBaby()) {
            matrixStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            matrixStack.scale(1f, 1f, 1f);
        }
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);

        ItemStack itemStack = livingEntity.getMainHandStack();
        if (!itemStack.isEmpty()) {
            matrixStack.push();
            this.renderItem(livingEntity, itemStack, matrixStack, vertexConsumerProvider, i);
            matrixStack.pop();
        }

    }

    protected void renderItem(LivingEntity entity, ItemStack stack, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        HeldItemRenderer heldItemRenderer = this.dispatcher.getHeldItemRenderer();;
        if (!stack.isEmpty()) {
            matrices.push();
            cachedBodyYaw = MathHelper.lerpAngleDegrees(0.3F, cachedBodyYaw ,-entity.getBodyYaw());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cachedBodyYaw));
            matrices.translate(0.0F, 0.9375F, 0.3125F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            matrices.scale(0.375F, 0.375F, 0.375F);
            heldItemRenderer.renderItem(entity, stack, ModelTransformationMode.HEAD, false, matrices, vertexConsumers, light);
            matrices.pop();
        }
    }

    private int getLightLevel(World world, BlockPos pos) {
        int bLight = world.getLightLevel(LightType.BLOCK, pos);
        int sLight = world.getLightLevel(LightType.SKY, pos);
        return LightmapTextureManager.pack(bLight, sLight);
    }
}
