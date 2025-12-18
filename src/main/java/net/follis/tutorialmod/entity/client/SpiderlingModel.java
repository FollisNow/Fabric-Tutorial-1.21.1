package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.SpiderlingEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class SpiderlingModel<T extends SpiderlingEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer SPIDERLING = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "spiderling"), "main");
    private static final String BODY0 = "body0";
    private static final String BODY1 = "body1";
    private static final String RIGHT_MIDDLE_FRONT_LEG = "right_middle_front_leg";
    private static final String LEFT_MIDDLE_FRONT_LEG = "left_middle_front_leg";
    private static final String RIGHT_MIDDLE_HIND_LEG = "right_middle_hind_leg";
    private static final String LEFT_MIDDLE_HIND_LEG = "left_middle_hind_leg";
    private final ModelPart spiderling;
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleLeg;
    private final ModelPart leftMiddleLeg;
    private final ModelPart rightMiddleFrontLeg;
    private final ModelPart leftMiddleFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    public SpiderlingModel(ModelPart spiderling) {
        this.spiderling = spiderling;
        this.head = spiderling.getChild("head");
        this.rightHindLeg = spiderling.getChild("right_hind_leg");
        this.leftHindLeg = spiderling.getChild("left_hind_leg");
        this.rightMiddleLeg = spiderling.getChild("right_middle_hind_leg");
        this.leftMiddleLeg = spiderling.getChild("left_middle_hind_leg");
        this.rightMiddleFrontLeg = spiderling.getChild("right_middle_front_leg");
        this.leftMiddleFrontLeg = spiderling.getChild("left_middle_front_leg");
        this.rightFrontLeg = spiderling.getChild("right_front_leg");
        this.leftFrontLeg = spiderling.getChild("left_front_leg");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("head", ModelPartBuilder.create().uv(32, 4).cuboid(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), ModelTransform.pivot(0.0F, 15.0F, -3.0F));
        modelPartData.addChild("body0", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), ModelTransform.pivot(0.0F, 15.0F, 0.0F));
        modelPartData.addChild("body1", ModelPartBuilder.create().uv(0, 12).cuboid(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), ModelTransform.pivot(0.0F, 15.0F, 9.0F));
        ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(18, 0).cuboid(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
        ModelPartBuilder modelPartBuilder2 = ModelPartBuilder.create().uv(18, 0).mirrored().cuboid(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
        modelPartData.addChild("right_hind_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 2.0F));
        modelPartData.addChild("left_hind_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 2.0F));
        modelPartData.addChild("right_middle_hind_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 1.0F));
        modelPartData.addChild("left_middle_hind_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 1.0F));
        modelPartData.addChild("right_middle_front_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, 0.0F));
        modelPartData.addChild("left_middle_front_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, 0.0F));
        modelPartData.addChild("right_front_leg", modelPartBuilder, ModelTransform.pivot(-4.0F, 15.0F, -1.0F));
        modelPartData.addChild("left_front_leg", modelPartBuilder2, ModelTransform.pivot(4.0F, 15.0F, -1.0F));
        return TexturedModelData.of(modelData, 64, 32);
    }
    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yaw = headYaw * ((float)Math.PI / 180F);

        this.rightHindLeg.roll = (-(float)Math.PI / 4F);
        this.leftHindLeg.roll = ((float)Math.PI / 4F);
        this.rightMiddleLeg.roll = -0.58119464F;
        this.leftMiddleLeg.roll = 0.58119464F;
        this.rightMiddleFrontLeg.roll = -0.58119464F;
        this.leftMiddleFrontLeg.roll = 0.58119464F;
        this.rightFrontLeg.roll = (-(float)Math.PI / 4F);
        this.leftFrontLeg.roll = ((float)Math.PI / 4F);

        this.rightHindLeg.yaw = ((float)Math.PI / 4F);
        this.leftHindLeg.yaw = (-(float)Math.PI / 4F);
        this.rightMiddleLeg.yaw = ((float)Math.PI / 8F);
        this.leftMiddleLeg.yaw = (-(float)Math.PI / 8F);
        this.rightMiddleFrontLeg.yaw = (-(float)Math.PI / 8F);
        this.leftMiddleFrontLeg.yaw = ((float)Math.PI / 8F);
        this.rightFrontLeg.yaw = (-(float)Math.PI / 4F);
        this.leftFrontLeg.yaw = ((float)Math.PI / 4F);

        if (entity.isInSittingPose()) {
            this.head.pitch = 25f / 180f * 3.14f; // lower the head if sitted (just for debugging)
        } else {
            this.head.pitch = headPitch * ((float) Math.PI / 180F);

            float i = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbDistance;
            float j = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + (float) Math.PI) * 0.4F) * limbDistance;
            float k = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + ((float) Math.PI / 2F)) * 0.4F) * limbDistance;
            float l = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + ((float) Math.PI * 1.5F)) * 0.4F) * limbDistance;
            float m = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 0.0F) * 0.4F) * limbDistance;
            float n = Math.abs(MathHelper.sin(limbAngle * 0.6662F + (float) Math.PI) * 0.4F) * limbDistance;
            float o = Math.abs(MathHelper.sin(limbAngle * 0.6662F + ((float) Math.PI / 2F)) * 0.4F) * limbDistance;
            float p = Math.abs(MathHelper.sin(limbAngle * 0.6662F + ((float) Math.PI * 1.5F)) * 0.4F) * limbDistance;
            ModelPart var10000 = this.rightHindLeg;
            var10000.yaw += i;
            var10000 = this.leftHindLeg;
            var10000.yaw += -i;
            var10000 = this.rightMiddleLeg;
            var10000.yaw += j;
            var10000 = this.leftMiddleLeg;
            var10000.yaw += -j;
            var10000 = this.rightMiddleFrontLeg;
            var10000.yaw += k;
            var10000 = this.leftMiddleFrontLeg;
            var10000.yaw += -k;
            var10000 = this.rightFrontLeg;
            var10000.yaw += l;
            var10000 = this.leftFrontLeg;
            var10000.yaw += -l;
            var10000 = this.rightHindLeg;
            var10000.roll += m;
            var10000 = this.leftHindLeg;
            var10000.roll += -m;
            var10000 = this.rightMiddleLeg;
            var10000.roll += n;
            var10000 = this.leftMiddleLeg;
            var10000.roll += -n;
            var10000 = this.rightMiddleFrontLeg;
            var10000.roll += o;
            var10000 = this.leftMiddleFrontLeg;
            var10000.roll += -o;
            var10000 = this.rightFrontLeg;
            var10000.roll += p;
            var10000 = this.leftFrontLeg;
            var10000.roll += -p;
        }
    }
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        spiderling.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return this.spiderling;
    }
}
