package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldChannelerEntity;
import net.follis.goldenscarabmod.entity.custom.GoldStalkerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GoldStalkerModel<T extends GoldStalkerEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer GOLD_STALKER =
            new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "gold_stalker"), "main");

    public ModelPart getHead() { return this.head; }

    private final ModelPart gold_stalker;
    private final ModelPart chest;
    private final ModelPart arms;
    private final ModelPart right_arm_root;
    private final ModelPart right_arm_joint;
    private final ModelPart right_arm_shoulder;
    private final ModelPart right_arm_end;
    private final ModelPart right_hand;
    private final ModelPart left_arm_root;
    private final ModelPart left_arm_joint;
    private final ModelPart left_arm_shoulder;
    private final ModelPart left_arm_end;
    private final ModelPart left_hand;
    private final ModelPart head;
    private final ModelPart hopper;
    private final ModelPart wheel_root;
    private final ModelPart wheel;
    public GoldStalkerModel(ModelPart root) {
        this.gold_stalker = root.getChild("gold_stalker");
        this.chest = this.gold_stalker.getChild("chest");
        this.arms = this.gold_stalker.getChild("arms");
        this.right_arm_root = this.arms.getChild("right_arm_root");
        this.right_arm_joint = this.right_arm_root.getChild("right_arm_joint");
        this.right_arm_shoulder = this.right_arm_joint.getChild("right_arm_shoulder");
        this.right_arm_end = this.right_arm_shoulder.getChild("right_arm_end");
        this.right_hand = this.right_arm_end.getChild("right_hand");
        this.left_arm_root = this.arms.getChild("left_arm_root");
        this.left_arm_joint = this.left_arm_root.getChild("left_arm_joint");
        this.left_arm_shoulder = this.left_arm_joint.getChild("left_arm_shoulder");
        this.left_arm_end = this.left_arm_shoulder.getChild("left_arm_end");
        this.left_hand = this.left_arm_end.getChild("left_hand");
        this.head = this.gold_stalker.getChild("head");
        this.hopper = this.head.getChild("hopper");
        this.wheel_root = this.gold_stalker.getChild("wheel_root");
        this.wheel = this.wheel_root.getChild("wheel");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData gold_stalker = modelPartData.addChild("gold_stalker", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 1.0F, 0.5F));

        ModelPartData chest = gold_stalker.addChild("chest", ModelPartBuilder.create().uv(0, 59).cuboid(-2.0F, 2.1F, -4.0F, 6.0F, 1.0F, 6.0F, new Dilation(0.0F))
                .uv(32, 37).cuboid(-3.0F, 3.1F, -5.0F, 8.0F, 4.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(-1.0F, 7.9F, 0.5F));

        ModelPartData arms = gold_stalker.addChild("arms", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 11.25F, 0.0F));

        ModelPartData right_arm_root = arms.addChild("right_arm_root", ModelPartBuilder.create().uv(64, 49).cuboid(-1.0F, -1.25F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-4.0F, 1.5F, -0.5F));

        ModelPartData right_arm_joint = right_arm_root.addChild("right_arm_joint", ModelPartBuilder.create().uv(16, 66).cuboid(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(-1.5F, 0.25F, 0.0F));

        ModelPartData right_arm_shoulder = right_arm_joint.addChild("right_arm_shoulder", ModelPartBuilder.create().uv(64, 8).cuboid(-1.0F, -1.375F, -1.5F, 2.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-1.5F, 0.0F, 0.0F));

        ModelPartData right_arm_end = right_arm_shoulder.addChild("right_arm_end", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.65F, 0.0F));

        ModelPartData cube_r1 = right_arm_end.addChild("cube_r1", ModelPartBuilder.create().uv(4, 66).cuboid(-0.5F, -5.5F, 3.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 66).cuboid(-0.5F, -5.5F, -1.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -0.025F, -1.5F, 0.0F, 0.0F, -0.4451F));

        ModelPartData right_hand = right_arm_end.addChild("right_hand", ModelPartBuilder.create().uv(24, 59).cuboid(-0.5F, -0.5F, -1.5F, 1.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, -4.525F, 0.0F));

        ModelPartData cube_r2 = right_hand.addChild("cube_r2", ModelPartBuilder.create().uv(64, 42).cuboid(-1.5F, -0.5F, -1.5F, 2.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.5F, 5.0141F, -0.0577F, -0.48F, 0.0F, 0.0F));

        ModelPartData left_arm_root = arms.addChild("left_arm_root", ModelPartBuilder.create().uv(32, 65).cuboid(0.0F, -1.25F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(4.0F, 1.5F, -0.5F));

        ModelPartData left_arm_joint = left_arm_root.addChild("left_arm_joint", ModelPartBuilder.create().uv(22, 68).cuboid(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(1.5F, 0.25F, 0.0F));

        ModelPartData left_arm_shoulder = left_arm_joint.addChild("left_arm_shoulder", ModelPartBuilder.create().uv(64, 17).cuboid(-1.0F, -1.375F, -1.5F, 2.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(1.5F, 0.0F, 0.0F));

        ModelPartData left_arm_end = left_arm_shoulder.addChild("left_arm_end", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.65F, 0.0F));

        ModelPartData cube_r3 = left_arm_end.addChild("cube_r3", ModelPartBuilder.create().uv(12, 66).cuboid(-0.5F, -5.5F, 3.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F))
                .uv(8, 66).cuboid(-0.5F, -5.5F, -1.0F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -0.025F, -1.5F, 0.0F, 0.0F, 0.4451F));

        ModelPartData left_hand = left_arm_end.addChild("left_hand", ModelPartBuilder.create().uv(64, 26).cuboid(-0.5F, -0.5141F, -1.4423F, 1.0F, 6.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(2.0F, -4.5109F, -0.0577F));

        ModelPartData cube_r4 = left_hand.addChild("cube_r4", ModelPartBuilder.create().uv(64, 35).cuboid(-0.5F, -0.5F, -1.5F, 2.0F, 4.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, 5.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        ModelPartData head = gold_stalker.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.0F, -0.5F));

        ModelPartData hopper = head.addChild("hopper", ModelPartBuilder.create().uv(60, 57).cuboid(-3.0F, -4.0F, -1.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-9.0F, -11.0F, -7.0F, 16.0F, 1.0F, 16.0F, new Dilation(0.0F))
                .uv(0, 17).cuboid(5.0F, -17.0F, -7.0F, 2.0F, 6.0F, 14.0F, new Dilation(0.0F))
                .uv(32, 17).cuboid(-9.0F, -17.0F, -5.0F, 2.0F, 6.0F, 14.0F, new Dilation(0.0F))
                .uv(32, 49).cuboid(-9.0F, -17.0F, -7.0F, 14.0F, 6.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 51).cuboid(-7.0F, -17.0F, 7.0F, 14.0F, 6.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 37).cuboid(-5.0F, -10.0F, -3.0F, 8.0F, 6.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(1.0F, 7.0F, -1.0F));

        ModelPartData wheel_root = gold_stalker.addChild("wheel_root", ModelPartBuilder.create().uv(64, 55).cuboid(-5.0F, -0.5F, 0.0F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(32, 57).cuboid(-6.5F, -5.5F, -3.0F, 7.0F, 1.0F, 7.0F, new Dilation(0.0F))
                .uv(46, 65).cuboid(-1.0F, -4.5F, -0.5F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(40, 65).cuboid(-6.0F, -4.5F, -0.5F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(3.0F, 20.5F, -1.0F));

        ModelPartData wheel = wheel_root.addChild("wheel", ModelPartBuilder.create().uv(52, 65).cuboid(-0.5F, 1.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(60, 65).cuboid(-0.5F, -2.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(64, 0).cuboid(-0.5F, -1.5F, -2.5F, 1.0F, 3.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-3.0F, 0.0F, 0.5F));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(GoldStalkerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(GoldStalkerAnimations.WALK_ANIMATION, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.pickupAnimationState, GoldStalkerAnimations.PICKUP_ANIMATION, ageInTicks, 1f);
        this.updateAnimation(entity.depositAnimationState, GoldStalkerAnimations.DEPOSIT_ANIMATION, ageInTicks, 1f);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        this.gold_stalker.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return this.gold_stalker;
    }
}