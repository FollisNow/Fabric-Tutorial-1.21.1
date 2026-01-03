package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldCarverEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GoldCarverModel<T extends GoldCarverEntity> extends SinglePartEntityModel<T> implements ModelWithArms {
    public static final EntityModelLayer GOLD_CARVER = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "gold_carver"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart wheel;
    private final ModelPart arms;
    private final ModelPart left_arm;
    private final ModelPart left_bottom_claw;
    private final ModelPart left_upper_claw;
    private final ModelPart right_arm;
    private final ModelPart right_bottom_claw;
    private final ModelPart right_upper_claw;
    public GoldCarverModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.root.getChild("head");
        this.wheel = this.root.getChild("wheel");
        this.arms = this.root.getChild("arms");
        this.left_arm = this.arms.getChild("left_arm");
        this.left_bottom_claw = this.left_arm.getChild("left_bottom_claw");
        this.left_upper_claw = this.left_arm.getChild("left_upper_claw");
        this.right_arm = this.arms.getChild("right_arm");
        this.right_bottom_claw = this.right_arm.getChild("right_bottom_claw");
        this.right_upper_claw = this.right_arm.getChild("right_upper_claw");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 10.5F, 0.0F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(44, 16).cuboid(-2.0F, 1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(32, 0).cuboid(-4.0F, -16.5F, -3.0F, 8.0F, 3.0F, 6.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-4.0F, -13.5F, -4.0F, 8.0F, 11.0F, 8.0F, new Dilation(0.0F))
                .uv(10, 41).cuboid(2.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(44, 9).cuboid(-4.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 8.0F, 0.0F));

        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 19).cuboid(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -11.5F, 0.0F));

        ModelPartData wheel = root.addChild("wheel", ModelPartBuilder.create().uv(24, 33).cuboid(-0.5F, 1.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(44, 18).cuboid(-0.5F, -2.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 33).cuboid(-0.5F, -1.5F, -2.5F, 1.0F, 3.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 10.0F, 0.0F));

        ModelPartData arms = root.addChild("arms", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -6.0F, 0.0F));

        ModelPartData left_arm = arms.addChild("left_arm", ModelPartBuilder.create().uv(32, 9).cuboid(-1.0F, -1.5F, -1.5F, 3.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(5.0F, 0.0F, 0.0F));

        ModelPartData left_bottom_claw = left_arm.addChild("left_bottom_claw", ModelPartBuilder.create().uv(0, 41).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 9.5F, 0.0F));

        ModelPartData left_upper_claw = left_arm.addChild("left_upper_claw", ModelPartBuilder.create().uv(24, 37).cuboid(-1.5F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 9.5F, 0.0F));

        ModelPartData right_arm = arms.addChild("right_arm", ModelPartBuilder.create().uv(32, 23).cuboid(-2.0F, -1.5F, -1.5F, 3.0F, 11.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-5.0F, 0.0F, 0.0F));

        ModelPartData right_bottom_claw = right_arm.addChild("right_bottom_claw", ModelPartBuilder.create().uv(36, 37).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.5F, 9.5F, 0.0F));

        ModelPartData right_upper_claw = right_arm.addChild("right_upper_claw", ModelPartBuilder.create().uv(12, 33).cuboid(-1.5F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.5F, 9.5F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(GoldCarverEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(GoldCarverAnimations.GOLD_CARVER_WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.idleAnimationState, GoldCarverAnimations.GOLD_CARVER_IDLE, ageInTicks, 1f);
        this.updateAnimation(entity.pickupAnimationState, GoldCarverAnimations.GOLD_CARVER_PICKUP, ageInTicks, 1f);
        this.updateAnimation(entity.craftAnimationState, GoldCarverAnimations.GOLD_CARVER_CRAFT, ageInTicks, 1f);
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
