package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldCarverEntity;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class GoldCarverRenderer extends MobEntityRenderer<GoldCarverEntity, GoldCarverModel<GoldCarverEntity>> {
    public static final Identifier TEXTURE = Identifier.of(TutorialMod.MOD_ID, "textures/entity/gold_carver/gold_carver.png");

    public GoldCarverRenderer(EntityRendererFactory.Context context) {
        super(context, new GoldCarverModel<>(context.getPart(GoldCarverModel.GOLD_CARVER)), 0.75f);

    }

    @Override
    public Identifier getTexture(GoldCarverEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GoldCarverEntity livingEntity, float f, float g, MatrixStack matrixStack,
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
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getBodyYaw()));
            matrices.translate(0.09375F, 1.03125F, 0.25F);
            matrices.scale(0.1875F, 0.1875F, 0.1875F);
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
