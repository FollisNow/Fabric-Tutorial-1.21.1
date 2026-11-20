package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.DominionEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class DominionModel<T extends DominionEntity> extends EntityModel<T> {
    public static final EntityModelLayer DOMINION = new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "dominion"), "main");
    private final ModelPart dominion;

    public DominionModel(ModelPart root) {
        this.dominion = root.getChild("dominion");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData dominion = modelPartData.addChild("dominion", ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -24.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(DominionEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.dominion.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
        this.dominion.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        dominion.render(matrices, vertexConsumer, light, overlay, color);
    }
}
