package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldPannerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GoldPannerModel<T extends GoldPannerEntity> extends SinglePartEntityModel<T> implements ModelWithArms {
    public static final EntityModelLayer GOLD_PANNER = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "gold_panner"), "main");
    private final ModelPart root;
    private final ModelPart pan;
    private final ModelPart body;
    private final ModelPart wheel;
    private final ModelPart arms;
    private final ModelPart left_arm;
    private final ModelPart right_arm;
    public GoldPannerModel(ModelPart root) {
        this.root = root.getChild("root");
        this.pan = this.root.getChild("pan");
        this.body = this.root.getChild("body");
        this.wheel = this.root.getChild("wheel");
        this.arms = this.root.getChild("arms");
        this.left_arm = this.arms.getChild("left_arm");
        this.right_arm = this.arms.getChild("right_arm");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 10.5F, 0.0F));

        ModelPartData pan = root.addChild("pan", ModelPartBuilder.create().uv(32, 7).cuboid(-4.0F, -2.0F, -3.0F, 1.0F, 1.0F, 6.0F, new Dilation(0.0F))
                .uv(12, 30).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 12).cuboid(-4.0F, -1.0F, -4.0F, 8.0F, 1.0F, 8.0F, new Dilation(0.0F))
                .uv(32, 14).cuboid(-4.0F, -2.0F, -4.0F, 8.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(32, 16).cuboid(-4.0F, -2.0F, 3.0F, 8.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(32, 0).cuboid(3.0F, -2.0F, -3.0F, 1.0F, 1.0F, 6.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -2.5F, 0.0F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(32, 18).cuboid(-2.0F, 1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 21).cuboid(-4.0F, -5.5F, -3.0F, 8.0F, 3.0F, 6.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-4.0F, -9.5F, -4.0F, 8.0F, 4.0F, 8.0F, new Dilation(0.0F))
                .uv(24, 35).cuboid(2.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(32, 35).cuboid(-4.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 8.0F, 0.0F));

        ModelPartData wheel = root.addChild("wheel", ModelPartBuilder.create().uv(40, 20).cuboid(-0.5F, 1.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(40, 24).cuboid(-0.5F, -2.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(12, 35).cuboid(-0.5F, -1.5F, -2.5F, 1.0F, 3.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 10.0F, 0.0F));

        ModelPartData arms = root.addChild("arms", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.0F, 0.0F));

        ModelPartData left_arm = arms.addChild("left_arm", ModelPartBuilder.create().uv(28, 21).cuboid(-1.0F, -3.5F, -1.5F, 3.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(5.0F, 2.0F, 0.0F));

        ModelPartData right_arm = arms.addChild("right_arm", ModelPartBuilder.create().uv(0, 30).cuboid(-2.0F, -3.5F, -1.5F, 3.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(GoldPannerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(GoldPannerAnimations.GOLD_PANNER_WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.pickupAnimationState, GoldPannerAnimations.GOLD_PANNER_PICKUP, ageInTicks, 1f);
        this.updateAnimation(entity.craftAnimationState, GoldPannerAnimations.GOLD_PANNER_CRAFT, ageInTicks, 1f);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        root.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setArmAngle(Arm arm, MatrixStack matrices) {
        float f = 1.0F;
        float g = 3.0F;
        this.root.rotate(matrices);
        this.body.rotate(matrices);
        matrices.translate(0.0F, 0.0625F, 0.1875F);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotation(this.right_arm.pitch));
        matrices.scale(0.7F, 0.7F, 0.7F);
        matrices.translate(0.0625F, 0.0F, 0.0F);
    }
}
