package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldBreederEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GoldBreederModel<T extends GoldBreederEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer GOLD_BREEDER = new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "gold_breeder"), "main");

    /** The gun cuboid is modelled tilted by -0.48 rad, which points the barrel that much above the arm when it is raised. */
    private static final float GUN_TILT = 0.48F;
    private static final float MAX_ELEVATION_DEGREES = 60.0F;
    private static final float MAX_YAW_DEGREES = 150.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart bottle;
    private final ModelPart head;
    private final ModelPart head_grass;
    private final ModelPart flowers;
    private final ModelPart arms;
    private final ModelPart left_arm_root;
    private final ModelPart left_arm_joint;
    private final ModelPart left_arm_shoulder;
    private final ModelPart left_arm_end;
    private final ModelPart left_love_gun;
    private final ModelPart right_arm_root;
    private final ModelPart right_arm_shoulder;
    private final ModelPart right_arm_end;
    private final ModelPart right_arm_forearm;
    private final ModelPart right_arm_blade;
    private final ModelPart wheel;
    public GoldBreederModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.bottle = this.body.getChild("bottle");
        this.head = this.root.getChild("head");
        this.head_grass = this.head.getChild("head_grass");
        this.flowers = this.head_grass.getChild("flowers");
        this.arms = this.head.getChild("arms");
        this.left_arm_root = this.arms.getChild("left_arm_root");
        this.left_arm_joint = this.left_arm_root.getChild("left_arm_joint");
        this.left_arm_shoulder = this.left_arm_joint.getChild("left_arm_shoulder");
        this.left_arm_end = this.left_arm_shoulder.getChild("left_arm_end");
        this.left_love_gun = this.left_arm_end.getChild("left_love_gun");
        this.right_arm_root = this.arms.getChild("right_arm_root");
        this.right_arm_shoulder = this.right_arm_root.getChild("right_arm_shoulder");
        this.right_arm_end = this.right_arm_shoulder.getChild("right_arm_end");
        this.right_arm_forearm = this.right_arm_end.getChild("right_arm_forearm");
        this.right_arm_blade = this.right_arm_forearm.getChild("right_arm_blade");
        this.wheel = this.root.getChild("wheel");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 11.5F, 0.0F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(38, 31).cuboid(-2.0F, 1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 14).cuboid(-3.5F, -12.5F, -3.5F, 7.0F, 1.0F, 7.0F, new Dilation(0.0F))
                .uv(11, 42).cuboid(-3.5F, -3.5F, -3.5F, 7.0F, 1.0F, 7.0F, new Dilation(0.0F))
                .uv(0, 27).cuboid(-2.5F, -13.5F, -2.5F, 5.0F, 1.0F, 5.0F, new Dilation(0.0F))
                .uv(20, 27).cuboid(2.0F, -2.5F, -1.0F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(36, 36).cuboid(-3.0F, -2.5F, -1.0F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 8.0F, 0.0F));

        ModelPartData bottle = body.addChild("bottle", ModelPartBuilder.create().uv(10, 40).cuboid(0.0F, -8.0F, -1.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
                .uv(38, 22).cuboid(-7.0F, -8.0F, -1.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
                .uv(38, 22).cuboid(0.0F, -8.0F, -8.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
                .uv(10, 40).cuboid(-7.0F, -8.0F, -8.0F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F))
                .uv(32, 0).cuboid(-6.0F, -8.0F, -7.5F, 6.0F, 8.0F, 0.0F, new Dilation(0.01F))
                .uv(32, 0).cuboid(-6.0F, -8.0F, -0.5F, 6.0F, 8.0F, 0.0F, new Dilation(0.01F))
                .uv(26, 22).cuboid(0.5F, -8.0F, -7.0F, 0.0F, 8.0F, 6.0F, new Dilation(0.01F))
                .uv(26, 22).cuboid(-6.5F, -8.0F, -7.0F, 0.0F, 8.0F, 6.0F, new Dilation(0.01F)), ModelTransform.pivot(3.0F, -3.5F, 4.0F));

        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -8.5F, 0.0F));

        ModelPartData head_grass = head.addChild("head_grass", ModelPartBuilder.create().uv(0, 50).cuboid(-3.0F, -4.0F, -3.0F, 6.0F, 1.0F, 6.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.9F, 0.0F));

        ModelPartData flowers = head_grass.addChild("flowers", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -4.9F, 0.0F));

        ModelPartData cube_r1 = flowers.addChild("cube_r1", ModelPartBuilder.create().uv(0, 58).cuboid(-3.0F, -5.0F, 0.0F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 2.3562F, 0.0F));

        ModelPartData cube_r2 = flowers.addChild("cube_r2", ModelPartBuilder.create().uv(0, 58).cuboid(-3.0F, -5.0F, 0.0F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        ModelPartData arms = head.addChild("arms", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -2.0F, 0.5F));

        ModelPartData left_arm_root = arms.addChild("left_arm_root", ModelPartBuilder.create().uv(20, 36).cuboid(0.0F, -1.25F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(4.0F, 1.5F, -0.5F));

        ModelPartData left_arm_joint = left_arm_root.addChild("left_arm_joint", ModelPartBuilder.create().uv(0, 42).cuboid(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(1.5F, 0.25F, 0.0F));

        ModelPartData left_arm_shoulder = left_arm_joint.addChild("left_arm_shoulder", ModelPartBuilder.create().uv(0, 33).cuboid(-1.0F, -1.375F, -1.5F, 2.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(1.5F, 0.0F, 0.0F));

        ModelPartData left_arm_end = left_arm_shoulder.addChild("left_arm_end", ModelPartBuilder.create().uv(6, 42).cuboid(-0.5F, 0.375F, -1.25F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
                .uv(6, 42).cuboid(-0.5F, 0.375F, 0.25F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 4.25F, 0.0F));

        ModelPartData left_love_gun = left_arm_end.addChild("left_love_gun", ModelPartBuilder.create().uv(10, 33).cuboid(-1.0F, -0.6109F, -1.5577F, 2.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 4.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        ModelPartData right_arm_root = arms.addChild("right_arm_root", ModelPartBuilder.create().uv(28, 36).cuboid(-1.0F, -1.25F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-4.0F, 1.5F, -0.5F, -1.5708F, 0.0F, 0.0F));

        ModelPartData right_arm_shoulder = right_arm_root.addChild("right_arm_shoulder", ModelPartBuilder.create().uv(42, 8).cuboid(1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F))
                .uv(32, 8).cuboid(-1.0F, -1.375F, -1.5F, 2.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, 0.25F, 0.0F, 2.0508F, 0.0F, 0.0F));

        ModelPartData right_arm_end = right_arm_shoulder.addChild("right_arm_end", ModelPartBuilder.create().uv(14, 40).cuboid(-0.5F, -2.625F, -0.5F, 1.0F, 7.0F, 1.0F, new Dilation(0.0F))
                .uv(38, 33).cuboid(-1.0F, 4.375F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 4.25F, 0.0F));

        ModelPartData right_arm_forearm = right_arm_end.addChild("right_arm_forearm", ModelPartBuilder.create().uv(0, 22).cuboid(-1.25F, -1.0F, -1.5F, 10.0F, 2.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 6.375F, 0.0F, 0.0F, 1.5708F, 0.0F));

        ModelPartData right_arm_blade = right_arm_forearm.addChild("right_arm_blade", ModelPartBuilder.create().uv(23, 22).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(7.0F, -1.0F, 0.0F));

        ModelPartData cube_r3 = right_arm_blade.addChild("cube_r3", ModelPartBuilder.create().uv(23, 22).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.03F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 2.7925F, 0.0F));

        ModelPartData cube_r4 = right_arm_blade.addChild("cube_r4", ModelPartBuilder.create().uv(23, 22).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.02F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 1.9635F, 0.0F));

        ModelPartData cube_r5 = right_arm_blade.addChild("cube_r5", ModelPartBuilder.create().uv(23, 22).cuboid(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.01F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        ModelPartData wheel = root.addChild("wheel", ModelPartBuilder.create().uv(40, 14).cuboid(-0.5F, 1.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(40, 18).cuboid(-0.5F, -2.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(28, 14).cuboid(-0.5F, -1.5F, -2.5F, 1.0F, 3.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 10.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    /** Note: the parameter is T, not GoldBreederEntity, otherwise this does not override EntityModel#setAngles. */
    @Override
    public void setAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        // No head rotation on purpose: the golem aims with its gun instead of turning its head.
        this.animateMovement(GoldBreederAnimations.GOLD_BREEDER_WALK, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.idleAnimationState, GoldBreederAnimations.GOLD_BREEDER_IDLE, ageInTicks, 1f);
        this.updateAnimation(entity.harvestCropAnimationState, GoldBreederAnimations.GOLD_BREEDER_HARVEST_CROP, ageInTicks, 1f);
        this.updateAnimation(entity.harvestMeatAnimationState, GoldBreederAnimations.GOLD_BREEDER_HARVEST_MEAT, ageInTicks, 1f);
        this.updateAnimation(entity.loveGunAnimationState, GoldBreederAnimations.GOLD_BREEDER_LOVE_GUN, ageInTicks, 1f);

        // ageInTicks = entity.age + tickDelta
        this.aimLoveGun(entity, ageInTicks - entity.age);
    }

    /**
     * Raises the left arm and points the gun at the target.
     *
     * Model space: +Y is down, -Z is forward, +X is the golem's left. A part's rotation is applied pitch (X) first,
     * then yaw (Y). The arm hangs along +Y, so a pitch of -90 degrees points it forward; the elevation is added on top,
     * and the gun's own tilt is subtracted so the BARREL (not the arm) points at the target. A positive yaw turns the
     * arm toward -X, i.e. to the golem's right, which matches the relative yaw computed by the entity.
     */
    private void aimLoveGun(T entity, float tickDelta) {
        float blend = entity.getAimBlend(tickDelta);
        if (blend <= 0.001F) return;

        float elevation = (float) Math.toRadians(MathHelper.clamp(
                entity.getAimPitch(tickDelta), -MAX_ELEVATION_DEGREES, MAX_ELEVATION_DEGREES));
        float yaw = (float) Math.toRadians(MathHelper.clamp(
                entity.getAimYaw(tickDelta), -MAX_YAW_DEGREES, MAX_YAW_DEGREES));

        float raisedPitch = -((float) (Math.PI / 2.0) + elevation - GUN_TILT);
        this.left_arm_shoulder.pitch = MathHelper.lerp(blend, 0.0F, raisedPitch);
        this.left_arm_shoulder.yaw = yaw * blend;
    }

    /**
     * Applies, in order, the transform of every bone from the root down to the love gun (including the current
     * animation and aim rotation, since setAngles has already run). Afterwards the matrix stack is in the gun's
     * local model space, in 1/16 block units converted to blocks. Used to pin the mode icon on the gun.
     */
    public void transformToGun(MatrixStack matrices) {
        this.root.rotate(matrices);
        this.head.rotate(matrices);
        this.arms.rotate(matrices);
        this.left_arm_root.rotate(matrices);
        this.left_arm_joint.rotate(matrices);
        this.left_arm_shoulder.rotate(matrices);
        this.left_arm_end.rotate(matrices);
        this.left_love_gun.rotate(matrices);
    }

    /** Same idea as transformToGun, for the glass bottle on the chest (root -> body -> bottle). */
    public void transformToBottle(MatrixStack matrices) {
        this.root.rotate(matrices);
        this.body.rotate(matrices);
        this.bottle.rotate(matrices);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    public ModelPart getBodyPart() {return body;}
}