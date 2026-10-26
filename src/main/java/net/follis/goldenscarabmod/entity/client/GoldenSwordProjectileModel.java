package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldenSwordProjectileEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GoldenSwordProjectileModel extends EntityModel<GoldenSwordProjectileEntity> {
    public static final EntityModelLayer GOLDEN_SWORD = new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "golden_sword"), "main");
    private final ModelPart golden_sword;

    public GoldenSwordProjectileModel(ModelPart root) {
        this.golden_sword = root.getChild("golden_sword");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData golden_sword = modelPartData.addChild("golden_sword", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, -10.1667F, -0.5F, 3.0F, 11.0F, 1.0F, new Dilation(0.0F))
                .uv(9, 0).cuboid(-3.5F, 0.8333F, -0.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(9, 3).cuboid(-1.5F, 1.8333F, -0.5F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 13).cuboid(-0.5F, 2.8333F, -0.5F, 1.0F, 3.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 16.1667F, -0.5F));

        ModelPartData cube_r1 = golden_sword.addChild("cube_r1", ModelPartBuilder.create().uv(9, 10).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.05F)), ModelTransform.of(0.0F, 6.8333F, 0.0F, 0.0F, 0.0F, -0.7854F));

        ModelPartData cube_r2 = golden_sword.addChild("cube_r2", ModelPartBuilder.create().uv(9, 6).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.05F)), ModelTransform.of(0.0F, -10.1667F, 0.0F, 0.0F, 0.0F, -0.7854F));
        return TexturedModelData.of(modelData, 32, 32);
    }
    @Override
    public void setAngles(GoldenSwordProjectileEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        golden_sword.render(matrices, vertexConsumer, light, overlay, color);
    }
}
