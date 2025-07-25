package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.LocustEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LocustModel<T extends LocustEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LOCUST = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "locust"), "main");

    private final ModelPart locust;
    public LocustModel(ModelPart root) {
        this.locust = root.getChild("Locust");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData Locust = modelPartData.addChild("Locust", ModelPartBuilder.create(), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        ModelPartData body = Locust.addChild("body", ModelPartBuilder.create().uv(18, 16).cuboid(-1.0F, -1.5F, 1.45F, 2.0F, 3.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-1.0F, -1.5F, -5.55F, 2.0F, 3.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -1.5F, 0.55F));

        ModelPartData antennae = body.addChild("antennae", ModelPartBuilder.create().uv(28, 29).cuboid(1.0F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(12, 30).cuboid(-1.0F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(24, 29).cuboid(1.0F, -3.0F, 1.0F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(26, 29).cuboid(-1.0F, -3.0F, 1.0F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.pivot(0.0F, -1.5F, 5.45F));

        ModelPartData backLegs = body.addChild("backLegs", ModelPartBuilder.create().uv(18, 23).cuboid(1.0F, -0.4812F, -4.5108F, 1.0F, 1.0F, 5.0F, new Dilation(0.0F))
                .uv(0, 26).cuboid(-2.0F, -0.4812F, -4.5108F, 1.0F, 1.0F, 5.0F, new Dilation(0.0F))
                .uv(12, 26).cuboid(1.0F, 0.5188F, -4.5108F, 1.0F, 4.0F, 0.0F, new Dilation(0.001F))
                .uv(14, 26).cuboid(-2.0F, 0.5188F, -4.5108F, 1.0F, 4.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.7F, -0.35F, -0.5149F, 0.0F, 0.0F));

        ModelPartData frontLegs = body.addChild("frontLegs", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.8488F, 1.95F));

        ModelPartData right_frontLegs = frontLegs.addChild("right_frontLegs", ModelPartBuilder.create(), ModelTransform.pivot(1.4737F, 0.0F, 0.0F));

        ModelPartData cube_r1 = right_frontLegs.addChild("cube_r1", ModelPartBuilder.create().uv(16, 26).cuboid(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 1.0F, new Dilation(0.001F))
                .uv(20, 29).cuboid(0.0F, -1.5F, -2.5F, 0.0F, 3.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.1432F));

        ModelPartData left_frontLegs = frontLegs.addChild("left_frontLegs", ModelPartBuilder.create(), ModelTransform.pivot(-1.4737F, 0.0F, 0.0F));

        ModelPartData cube_r2 = left_frontLegs.addChild("cube_r2", ModelPartBuilder.create().uv(22, 29).cuboid(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 1.0F, new Dilation(0.001F))
                .uv(18, 29).cuboid(0.0F, -1.5F, -2.5F, 0.0F, 3.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.1432F));

        ModelPartData wings = body.addChild("wings", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -1.5F, 1.45F));

        ModelPartData left_wing = wings.addChild("left_wing", ModelPartBuilder.create(), ModelTransform.of(-1.0F, 0.0F, 0.0F, -0.1309F, 0.017F, -0.0061F));

        ModelPartData bone = left_wing.addChild("bone", ModelPartBuilder.create().uv(18, 0).cuboid(-0.144F, -0.0718F, -6.8826F, 0.0F, 1.0F, 7.0F, new Dilation(0.0F))
                .uv(0, 10).cuboid(-0.144F, -0.0718F, -7.8826F, 1.0F, 0.0F, 8.0F, new Dilation(0.001F))
                .uv(14, 30).cuboid(-0.144F, -0.0718F, 0.1174F, 1.0F, 1.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.087F, 0.0351F, -0.0046F));

        ModelPartData right_wing = wings.addChild("right_wing", ModelPartBuilder.create(), ModelTransform.of(1.0F, 0.0F, 0.0F, -0.1309F, -0.017F, 0.0061F));

        ModelPartData bone2 = right_wing.addChild("bone2", ModelPartBuilder.create().uv(18, 8).cuboid(0.1417F, -0.0725F, -6.9846F, 0.0F, 1.0F, 7.0F, new Dilation(0.001F))
                .uv(0, 18).cuboid(-0.8583F, -0.0725F, -7.9846F, 1.0F, 0.0F, 8.0F, new Dilation(0.001F))
                .uv(16, 30).cuboid(-0.8583F, -0.0725F, 0.0154F, 1.0F, 1.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.087F, -0.0351F, 0.0046F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(LocustEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(LocustAnimations.ANIM_LOCUST_JUMP, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.idleAnimationState, LocustAnimations.ANIM_LOCUST_IDLE, ageInTicks, 1f);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        locust.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return locust;
    }
}
