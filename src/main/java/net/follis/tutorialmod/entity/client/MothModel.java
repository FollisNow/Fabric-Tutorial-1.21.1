package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.MothEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MothModel<T extends MothEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer MOTH = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "moth"), "main");
    private final ModelPart root;
    private final ModelPart moth;
    private final ModelPart right_antenna;
    private final ModelPart left_antenna;
    private final ModelPart left_wing_tip;
    private final ModelPart right_wing_tip;
    private final ModelPart left_wing;
    private final ModelPart right_wing;
    private final ModelPart left_wing_inner;
    private final ModelPart right_wing_inner;
    public MothModel(ModelPart root) {
        this.root = root.getChild("root");
        this.moth = this.root.getChild("moth");
        this.right_antenna = this.moth.getChild("right_antenna");
        this.left_antenna = this.moth.getChild("left_antenna");
        this.left_wing_tip = this.moth.getChild("left_wing_tip");
        this.right_wing_tip = this.moth.getChild("right_wing_tip");
        this.left_wing = this.moth.getChild("left_wing");
        this.right_wing = this.moth.getChild("right_wing");
        this.left_wing_inner = this.moth.getChild("left_wing_inner");
        this.right_wing_inner = this.moth.getChild("right_wing_inner");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData moth = root.addChild("moth", ModelPartBuilder.create().uv(14, 12).cuboid(-3.0F, -4.0F, -6.0F, 6.0F, 4.0F, 12.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_antenna = moth.addChild("right_antenna", ModelPartBuilder.create().uv(8, 16).cuboid(0.0F, 0.0F, -7.0F, 2.0F, 0.0F, 7.0F, new Dilation(0.001F))
                .uv(25, -7).cuboid(0.0F, 0.0F, -7.0F, 0.0F, 2.0F, 7.0F, new Dilation(0.001F)), ModelTransform.pivot(-3.0F, -2.5F, -6.0F));

        ModelPartData left_antenna = moth.addChild("left_antenna", ModelPartBuilder.create().uv(38, 16).cuboid(-2.0F, 0.0F, -7.0F, 2.0F, 0.0F, 7.0F, new Dilation(0.001F))
                .uv(25, -4).cuboid(0.0F, 0.0F, -7.0F, 0.0F, 2.0F, 7.0F, new Dilation(0.001F)), ModelTransform.pivot(3.0F, -2.5F, -6.0F));

        ModelPartData left_wing_tip = moth.addChild("left_wing_tip", ModelPartBuilder.create().uv(20, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 0.0F, 5.0F, new Dilation(0.001F)), ModelTransform.pivot(2.5F, -2.25F, 6.0F));

        ModelPartData right_wing_tip = moth.addChild("right_wing_tip", ModelPartBuilder.create().uv(28, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 0.0F, 5.0F, new Dilation(0.001F)), ModelTransform.pivot(-2.5F, -2.25F, 6.0F));

        ModelPartData left_wing = moth.addChild("left_wing", ModelPartBuilder.create().uv(-15, 0).cuboid(0.0F, -0.0114F, -6.7385F, 12.0F, 0.0F, 15.0F, new Dilation(0.001F)), ModelTransform.of(3.0F, -3.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        ModelPartData right_wing = moth.addChild("right_wing", ModelPartBuilder.create().uv(25, 0).cuboid(-12.0F, -0.0114F, -6.7385F, 12.0F, 0.0F, 15.0F, new Dilation(0.001F)), ModelTransform.of(-3.0F, -3.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        ModelPartData left_wing_inner = moth.addChild("left_wing_inner", ModelPartBuilder.create().uv(-14, 16).cuboid(0.0F, -0.0019F, -8.0121F, 7.0F, 0.0F, 14.0F, new Dilation(0.001F)), ModelTransform.of(3.0F, -1.55F, 5.25F, -0.0873F, 0.0F, 0.0F));

        ModelPartData right_wing_inner = moth.addChild("right_wing_inner", ModelPartBuilder.create().uv(36, 16).cuboid(-7.0F, -0.0019F, -8.0121F, 7.0F, 0.0F, 14.0F, new Dilation(0.001F)), ModelTransform.of(-3.0F, -1.55F, 5.25F, -0.0873F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(MothEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        if (entity.isRoosting()) {
            this.updateAnimation(entity.roostingAnimationState, MothAnimations.ROOSTING_ANIMATION, ageInTicks, 1.0F);
        } else {
            this.moth.pitch = -0.5F; // Set a constant tilt value
            this.updateAnimation(entity.flyingAnimationState, MothAnimations.FLY_ANIMATION, ageInTicks, 1.0F);
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        root.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }
}
