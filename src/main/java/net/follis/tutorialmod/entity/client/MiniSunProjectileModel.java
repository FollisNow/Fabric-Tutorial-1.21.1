package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.MiniSunProjectileEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MiniSunProjectileModel extends EntityModel<MiniSunProjectileEntity> {
    public static final EntityModelLayer MINI_SUN = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "mini_sun"), "main");
    private final ModelPart mini_sun;

    public MiniSunProjectileModel(ModelPart root) {
        this.mini_sun = root.getChild("mini_sun");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData mini_sun = modelPartData.addChild("mini_sun", ModelPartBuilder.create().uv(0, 0).cuboid(-7.0F, -8.0F, -1.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(3.0F, 21.0F, -3.0F));
        return TexturedModelData.of(modelData, 32, 32);    }

    @Override
    public void setAngles(MiniSunProjectileEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        mini_sun.render(matrices, vertexConsumer, light, overlay, color);
    }
}
