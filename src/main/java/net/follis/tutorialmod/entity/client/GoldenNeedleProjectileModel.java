package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldenNeedleProjectileEntity;
import net.follis.tutorialmod.entity.custom.TomahawkProjectileEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GoldenNeedleProjectileModel extends EntityModel<GoldenNeedleProjectileEntity> {
    public static final EntityModelLayer GOLDEN_NEEDLE = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "golden_needle"), "main");
    private final ModelPart golden_needle;

    public GoldenNeedleProjectileModel(ModelPart root) {
        this.golden_needle = root.getChild("golden_needle");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData golden_needle = modelPartData.addChild("golden_needle", ModelPartBuilder.create().uv(0, 10).cuboid(0.0F, -1.0F, 2.0F, 1.0F, 1.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-0.5F, -1.5F, 10.0F, 2.0F, 2.0F, 8.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, -4.5F, -0.5F, -1.5708F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }
    @Override
    public void setAngles(GoldenNeedleProjectileEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        golden_needle.render(matrices, vertexConsumer, light, overlay, color);
    }
}
