package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.ScorpionEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class ScorpionModel<T extends ScorpionEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer SCORPION = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "scorpion"), "main");
    private int internalTimer = 0;
    private final ModelPart scorpion;
    private final ModelPart legs;
    private final ModelPart L_legs;
    private final ModelPart L_backest_leg;
    private final ModelPart L_back_leg;
    private final ModelPart L_mid_leg;
    private final ModelPart L_front_leg;
    private final ModelPart R_legs;
    private final ModelPart R_backest_leg;
    private final ModelPart R_back_leg;
    private final ModelPart R_mid_leg;
    private final ModelPart R_front_leg;
    private final ModelPart mandibule;
    private final ModelPart tail_root;
    private final ModelPart tail_mid;
    private final ModelPart tail_tip;
    private final ModelPart sting_root;
    public ScorpionModel(ModelPart root) {
        this.scorpion = root.getChild("scorpion");
        this.legs = this.scorpion.getChild("legs");
        this.L_legs = this.legs.getChild("L_legs");
        this.L_backest_leg = this.L_legs.getChild("L_backest_leg");
        this.L_back_leg = this.L_legs.getChild("L_back_leg");
        this.L_mid_leg = this.L_legs.getChild("L_mid_leg");
        this.L_front_leg = this.L_legs.getChild("L_front_leg");
        this.R_legs = this.legs.getChild("R_legs");
        this.R_backest_leg = this.R_legs.getChild("R_backest_leg");
        this.R_back_leg = this.R_legs.getChild("R_back_leg");
        this.R_mid_leg = this.R_legs.getChild("R_mid_leg");
        this.R_front_leg = this.R_legs.getChild("R_front_leg");
        this.mandibule = this.scorpion.getChild("mandibule");
        this.tail_root = this.scorpion.getChild("tail_root");
        this.tail_mid = this.tail_root.getChild("tail_mid");
        this.tail_tip = this.tail_mid.getChild("tail_tip");
        this.sting_root = this.tail_tip.getChild("sting_root");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData scorpion = modelPartData.addChild("scorpion", ModelPartBuilder.create().uv(0, 0).cuboid(-2.5F, -3.0F, -5.0F, 5.0F, 3.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 22.0F, 0.0F));

        ModelPartData legs = scorpion.addChild("legs", ModelPartBuilder.create(), ModelTransform.pivot(2.5F, -1.5F, 1.0F));

        ModelPartData L_legs = legs.addChild("L_legs", ModelPartBuilder.create(), ModelTransform.of(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

        ModelPartData L_backest_leg = L_legs.addChild("L_backest_leg", ModelPartBuilder.create(), ModelTransform.of(1.1F, -0.7F, -0.2F, 0.0F, -0.48F, 0.0F));

        ModelPartData cube_r1 = L_backest_leg.addChild("cube_r1", ModelPartBuilder.create().uv(22, 31).cuboid(-1.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.9F, 0.7F, 0.2F, 0.0F, -0.2618F, 0.1745F));

        ModelPartData L_back_leg = L_legs.addChild("L_back_leg", ModelPartBuilder.create(), ModelTransform.pivot(1.1F, -0.9F, -0.6F));

        ModelPartData cube_r2 = L_back_leg.addChild("cube_r2", ModelPartBuilder.create().uv(30, 0).cuboid(-1.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.9F, 0.9F, 0.1F, 0.0F, -0.2618F, 0.1745F));

        ModelPartData L_mid_leg = L_legs.addChild("L_mid_leg", ModelPartBuilder.create(), ModelTransform.pivot(1.1F, -0.7F, -1.5F));

        ModelPartData cube_r3 = L_mid_leg.addChild("cube_r3", ModelPartBuilder.create().uv(30, 8).cuboid(-1.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.9F, 0.7F, 0.0F, 0.0F, 0.0F, 0.1745F));

        ModelPartData L_front_leg = L_legs.addChild("L_front_leg", ModelPartBuilder.create(), ModelTransform.pivot(1.0F, -0.2F, -2.3F));

        ModelPartData cube_r4 = L_front_leg.addChild("cube_r4", ModelPartBuilder.create().uv(30, 4).cuboid(-1.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, 0.2F, -0.2F, 0.0F, 0.2618F, 0.1745F));

        ModelPartData R_legs = legs.addChild("R_legs", ModelPartBuilder.create(), ModelTransform.of(-3.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

        ModelPartData R_backest_leg = R_legs.addChild("R_backest_leg", ModelPartBuilder.create(), ModelTransform.of(-1.1F, -0.7F, -0.2F, 0.0F, 0.48F, 0.0F));

        ModelPartData cube_r5 = R_backest_leg.addChild("cube_r5", ModelPartBuilder.create().uv(30, 10).cuboid(-10.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-0.9F, 0.7F, 0.2F, 0.0F, 0.2618F, -0.1745F));

        ModelPartData R_back_leg = R_legs.addChild("R_back_leg", ModelPartBuilder.create(), ModelTransform.pivot(-1.1F, -0.9F, -0.6F));

        ModelPartData cube_r6 = R_back_leg.addChild("cube_r6", ModelPartBuilder.create().uv(30, 2).cuboid(-10.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-0.9F, 0.9F, 0.1F, 0.0F, 0.2618F, -0.1745F));

        ModelPartData R_mid_leg = R_legs.addChild("R_mid_leg", ModelPartBuilder.create(), ModelTransform.pivot(-1.1F, -0.7F, -1.5F));

        ModelPartData cube_r7 = R_mid_leg.addChild("cube_r7", ModelPartBuilder.create().uv(22, 29).cuboid(-10.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(-0.9F, 0.7F, 0.0F, 0.0F, 0.0F, -0.1745F));

        ModelPartData R_front_leg = R_legs.addChild("R_front_leg", ModelPartBuilder.create(), ModelTransform.pivot(-1.0F, -0.2F, -2.3F));

        ModelPartData cube_r8 = R_front_leg.addChild("cube_r8", ModelPartBuilder.create().uv(30, 6).cuboid(-10.0F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.2F, -0.2F, 0.0F, -0.2618F, -0.1745F));

        ModelPartData mandibule = scorpion.addChild("mandibule", ModelPartBuilder.create().uv(24, 13).cuboid(-2.0F, -2.0F, -5.0F, 3.0F, 2.0F, 6.0F, new Dilation(0.001F))
                .uv(24, 21).cuboid(4.0F, -2.0F, -5.0F, 3.0F, 2.0F, 6.0F, new Dilation(0.001F)), ModelTransform.pivot(-2.5F, 0.0F, -6.0F));

        ModelPartData tail_root = scorpion.addChild("tail_root", ModelPartBuilder.create().uv(0, 13).cuboid(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 9.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.25F, 5.0F, 0.8727F, 0.0F, 0.0F));

        ModelPartData tail_mid = tail_root.addChild("tail_mid", ModelPartBuilder.create().uv(0, 24).cuboid(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 9.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 8.0F, 1.5272F, 0.0F, 0.0F));

        ModelPartData tail_tip = tail_mid.addChild("tail_tip", ModelPartBuilder.create().uv(22, 33).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 5.0F, new Dilation(-0.01F)), ModelTransform.of(0.0F, -0.75F, 9.0F, 1.4399F, 0.0F, 0.0F));

        ModelPartData sting_root = tail_tip.addChild("sting_root", ModelPartBuilder.create().uv(0, 35).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 5.0F, 0.0873F, 0.0F, 0.0F));

        ModelPartData cube_r9 = sting_root.addChild("cube_r9", ModelPartBuilder.create().uv(18, 35).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(-0.04F)), ModelTransform.of(0.0F, -0.65F, 2.45F, -0.9163F, 0.0F, 0.0F));

        ModelPartData cube_r10 = sting_root.addChild("cube_r10", ModelPartBuilder.create().uv(14, 35).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(-0.03F)), ModelTransform.of(0.0F, -0.65F, 2.05F, -0.3229F, 0.0F, 0.0F));

        ModelPartData cube_r11 = sting_root.addChild("cube_r11", ModelPartBuilder.create().uv(10, 35).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(-0.02F)), ModelTransform.of(0.0F, -0.35F, 1.8F, 0.3491F, 0.0F, 0.0F));

        ModelPartData cube_r12 = sting_root.addChild("cube_r12", ModelPartBuilder.create().uv(6, 35).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new Dilation(-0.01F)), ModelTransform.of(0.0F, 0.0F, 2.0F, 1.1781F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(ScorpionEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        this.updateAnimation(entity.attackAnimationState, ScorpionAnimations.attack_animation, animationProgress, 1f);

        this.R_backest_leg.roll = -0.174533F;
        this.L_backest_leg.roll = 0.174533F;
        this.R_back_leg.roll = -0.128954F;
        this.L_back_leg.roll = 0.128954F;
        this.R_mid_leg.roll = -0.128954F;
        this.L_mid_leg.roll = 0.128954F;
        this.R_front_leg.roll = -0.174533F;
        this.L_front_leg.roll = 0.174533F;

        this.R_backest_leg.yaw = ((float)Math.PI / 4F);
        this.L_backest_leg.yaw = (-(float)Math.PI / 4F);
        this.R_back_leg.yaw = ((float)Math.PI / 8F);
        this.L_back_leg.yaw = (-(float)Math.PI / 8F);
        this.R_mid_leg.yaw = (-(float)Math.PI / 8F);
        this.L_mid_leg.yaw = ((float)Math.PI / 8F);
        this.R_front_leg.yaw = (-(float)Math.PI / 4F);
        this.L_front_leg.yaw = ((float)Math.PI / 4F);

        float i = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbDistance;
        float j = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + (float) Math.PI) * 0.4F) * limbDistance;
        float k = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + ((float) Math.PI / 2F)) * 0.4F) * limbDistance;
        float l = -(MathHelper.cos(limbAngle * 0.6662F * 2.0F + ((float) Math.PI * 1.5F)) * 0.4F) * limbDistance;
        float m = Math.abs(MathHelper.sin(limbAngle * 0.6662F + 0.0F) * 0.4F) * limbDistance;
        float n = Math.abs(MathHelper.sin(limbAngle * 0.6662F + (float) Math.PI) * 0.4F) * limbDistance;
        float o = Math.abs(MathHelper.sin(limbAngle * 0.6662F + ((float) Math.PI / 2F)) * 0.4F) * limbDistance;
        float p = Math.abs(MathHelper.sin(limbAngle * 0.6662F + ((float) Math.PI * 1.5F)) * 0.4F) * limbDistance;

        ModelPart var10000 = this.R_backest_leg;
        var10000.yaw += i;
        var10000 = this.L_backest_leg;
        var10000.yaw += -i;
        var10000 = this.R_back_leg;
        var10000.yaw += j;
        var10000 = this.L_back_leg;
        var10000.yaw += -j;
        var10000 = this.R_mid_leg;
        var10000.yaw += k;
        var10000 = this.L_mid_leg;
        var10000.yaw += -k;
        var10000 = this.R_front_leg;
        var10000.yaw += l;
        var10000 = this.L_front_leg;
        var10000.yaw += -l;
        var10000 = this.R_backest_leg;
        var10000.roll += m;
        var10000 = this.L_backest_leg;
        var10000.roll += -m;
        var10000 = this.R_back_leg;
        var10000.roll += n;
        var10000 = this.L_back_leg;
        var10000.roll += -n;
        var10000 = this.R_mid_leg;
        var10000.roll += o;
        var10000 = this.L_mid_leg;
        var10000.roll += -o;
        var10000 = this.R_front_leg;
        var10000.roll += p;
        var10000 = this.L_front_leg;
        var10000.roll += -p;

//        if (entity.getScorpionFlag(2)) {
//            TutorialMod.LOGGER.info("true");
//            internalTimer++;
//            AngleInterpolator pitchTailRootInterpolator = new AngleInterpolator(50F)
//                    .addStep(0.0F, 0.125F, 0, -15)
//                    .addStep(0.125F, 0.2073F, -15, -15)
//                    .addStep(0.2073F, 0.2083F, -15, 0)
//                    .addStep(0.2083F, 0.333F, 0, 65);
//
//            AngleInterpolator pitchTailMidInterpolator = new AngleInterpolator(87.5F)
//                    .addStep(0.0F, 0.125F, 0, 0)
//                    .addStep(0.125F, 0.2073F, 0, 0)
//                    .addStep(0.2073F, 0.2083F, 0, 0)
//                    .addStep(0.2083F, 0.333F, 0, -30);
//
//
//            AngleInterpolator pitchTailTipInterpolator = new AngleInterpolator(82.5F)
//                    .addStep(0.0F, 0.125F, 0, 7.5F)
//                    .addStep(0.125F, 0.2073F, 7.5F, 7.5F)
//                    .addStep(0.2073F, 0.2083F, 7.5F, 0)
//                    .addStep(0.2083F, 0.333F, 0, -57.5F);
//
//
//            float normalizedAge = (float) (internalTimer % 20) /20;
//            this.tail_root.pitch = pitchTailRootInterpolator.interpolate(normalizedAge);
//            this.tail_mid.pitch = pitchTailMidInterpolator.interpolate(normalizedAge);
//            this.tail_tip.pitch = pitchTailTipInterpolator.interpolate(normalizedAge);
//        } else {
//            internalTimer =0;
//        }
    }
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        scorpion.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return this.scorpion;
    }

}
class Step {
    private final float startTime;
    private final float endTime;
    private final float startValue;
    private final float endValue;

    public Step(float startTime, float endTime, float startValue, float endValue) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public float interpolate(float normalizedAge) {
        if (normalizedAge > endTime || normalizedAge < startTime) {
            return Float.NaN; // Out of range
        }
        float t = (normalizedAge - startTime) / (endTime - startTime);
        return (float) MathHelper.lerp(t, Math.toRadians(startValue), Math.toRadians(endValue));
    }
}

class AngleInterpolator {
    private final List<Step> steps = new ArrayList<>();
    private final float baseAngle;

    AngleInterpolator(float baseAngle) {
        this.baseAngle = baseAngle;
    }

    public AngleInterpolator addStep(float startTime, float endTime, float startValue, float endValue) {
        steps.add(new Step(startTime, endTime, startValue, endValue));
        return this;
    }

    public float interpolate(float normalizedAge) {
        for (Step step : steps) {
            float result = step.interpolate(normalizedAge);
            if (!Float.isNaN(result)) {
                return (float) (result + Math.toRadians(baseAngle));
            }
        }
        return (float) Math.toRadians(baseAngle); // No valid step found
    }
}