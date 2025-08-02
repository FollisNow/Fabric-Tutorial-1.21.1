package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.BeetleEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BeetleModel<T extends BeetleEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer BEETLE = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "beetle"), "main");
    private final ModelPart beetle;
    private final ModelPart head;
    private final ModelPart wings;
    private final ModelPart left_wing;
    private final ModelPart right_wing;
    private final ModelPart central_wing;
    private final ModelPart legs;
    private final ModelPart right_legs;
    private final ModelPart left_legs;
    private final ModelPart mainBody;
    public BeetleModel(ModelPart root) {
        this.beetle = root.getChild("beetle");
        this.head = this.beetle.getChild("head");
        this.wings = this.beetle.getChild("wings");
        this.left_wing = this.wings.getChild("left_wing");
        this.right_wing = this.wings.getChild("right_wing");
        this.central_wing = this.wings.getChild("central_wing");
        this.legs = this.beetle.getChild("legs");
        this.right_legs = this.legs.getChild("right_legs");
        this.left_legs = this.legs.getChild("left_legs");
        this.mainBody = this.beetle.getChild("mainBody");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData beetle = modelPartData.addChild("beetle", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 25.2F, 0.0F));

        ModelPartData head = beetle.addChild("head", ModelPartBuilder.create().uv(22, 0).cuboid(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 3.0F, new Dilation(-0.001F)), ModelTransform.pivot(0.0F, -2.0F, -3.0F));

        ModelPartData wings = beetle.addChild("wings", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -5.0F, -2.0F));

        ModelPartData left_wing = wings.addChild("left_wing", ModelPartBuilder.create().uv(0, 8).cuboid(-2.0F, -1.0F, 1.0F, 2.0F, 2.0F, 6.0F, new Dilation(0.01F)), ModelTransform.pivot(2.0F, 1.0F, -1.0F));

        ModelPartData right_wing = wings.addChild("right_wing", ModelPartBuilder.create().uv(0, 16).cuboid(0.0F, -1.0F, 1.0F, 2.0F, 2.0F, 6.0F, new Dilation(0.01F)), ModelTransform.pivot(-2.0F, 1.0F, -1.0F));

        ModelPartData central_wing = wings.addChild("central_wing", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 1.0F));

        ModelPartData central_wing_r1 = central_wing.addChild("central_wing_r1", ModelPartBuilder.create().uv(16, 8).cuboid(-2.0F, 0.0F, 0.0F, 4.0F, 0.0F, 5.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));

        ModelPartData legs = beetle.addChild("legs", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -1.7609F, 0.5F));

        ModelPartData right_legs = legs.addChild("right_legs", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0873F));

        ModelPartData cube_r1 = right_legs.addChild("cube_r1", ModelPartBuilder.create().uv(22, 5).cuboid(-2.0602F, -0.7288F, -2.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(22, 22).cuboid(-2.0602F, -0.7288F, 1.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(18, 22).cuboid(-2.0602F, -0.7288F, -0.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F));

        ModelPartData left_legs = legs.addChild("left_legs", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0873F));

        ModelPartData cube_r2 = left_legs.addChild("cube_r2", ModelPartBuilder.create().uv(16, 22).cuboid(2.0602F, -0.7288F, 1.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(0, 24).cuboid(2.0602F, -0.7288F, -0.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(20, 22).cuboid(2.0602F, -0.7288F, -2.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1309F));

        ModelPartData mainBody = beetle.addChild("mainBody", ModelPartBuilder.create().uv(16, 13).cuboid(-1.0F, -0.6667F, -0.8333F, 2.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-2.0F, 0.3333F, -2.8333F, 4.0F, 1.0F, 7.0F, new Dilation(0.0F))
                .uv(16, 18).cuboid(-2.0F, -1.6667F, -2.8333F, 4.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -3.3333F, -0.1667F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(BeetleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(BeetleAnimations.FLY_ANIMATION, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.idleAnimationState, BeetleAnimations.IDLE_ANIMATION, ageInTicks, 1f);
    }
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        beetle.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return beetle;
    }
}
