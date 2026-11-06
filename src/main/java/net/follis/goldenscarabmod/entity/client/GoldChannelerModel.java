package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldChannelerEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Placeholder model, 64x64 texture. Ground is y = 0 under the "root" pivot (24 in model space), so
 * everything is expressed with negative y going up.
 *
 * UV layout (box unwrap = width 2*(dx+dz), height dy+dz):
 *   body (0,0) 28x15 | wheel main (30,0) 12x8 | wheel tips (44,0) and (44,5) 8x4
 *   ears (30,9) and (39,9) 8x6 | head (0,16) 28x12 | arms (0,29) and (13,29) 12x13
 */
public class GoldChannelerModel<T extends GoldChannelerEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer GOLD_CHANNELER =
            new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "gold_channeler"), "main");

    public ModelPart getHead() { return this.head; }
    public ModelPart getCrest() { return this.crest; }

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart wheel;
    private final ModelPart head;
    private final ModelPart crest;
    private final ModelPart arms;
    private final ModelPart right_arm;
    private final ModelPart right_piston;
    private final ModelPart right_upper_piston;
    private final ModelPart right_bottom_piston;
    private final ModelPart left_arm;
    private final ModelPart left_piston;
    private final ModelPart left_bottom_piston;
    private final ModelPart left_upper_piston;
    public GoldChannelerModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.wheel = this.root.getChild("wheel");
        this.head = this.root.getChild("head");
        this.crest = this.head.getChild("crest");
        this.arms = this.root.getChild("arms");
        this.right_arm = this.arms.getChild("right_arm");
        this.right_piston = this.right_arm.getChild("right_piston");
        this.right_upper_piston = this.right_piston.getChild("right_upper_piston");
        this.right_bottom_piston = this.right_piston.getChild("right_bottom_piston");
        this.left_arm = this.arms.getChild("left_arm");
        this.left_piston = this.left_arm.getChild("left_piston");
        this.left_bottom_piston = this.left_piston.getChild("left_bottom_piston");
        this.left_upper_piston = this.left_piston.getChild("left_upper_piston");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 18.5F, 0.0F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(47, 44).cuboid(-2.0F, 1.5F, -0.5F, 4.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 11).cuboid(-3.0F, -12.5F, -3.0F, 6.0F, 10.0F, 6.0F, new Dilation(0.0F))
                .uv(11, 42).cuboid(2.0F, -2.5F, -1.0F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(18, 42).cuboid(-3.0F, -2.5F, -1.0F, 1.0F, 5.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 1.0F, 0.0F));

        ModelPartData wheel = root.addChild("wheel", ModelPartBuilder.create().uv(38, 44).cuboid(-0.5F, 1.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(25, 45).cuboid(-0.5F, -2.5F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(11, 33).cuboid(-0.5F, -1.5F, -2.5F, 1.0F, 3.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 3.0F, 0.0F));

        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-5.25F, -2.0F, -3.25F, 10.0F, 4.0F, 6.0F, new Dilation(0.0F)), ModelTransform.pivot(0.25F, -13.5F, 0.25F));

        ModelPartData crest = head.addChild("crest", ModelPartBuilder.create().uv(25, 11).cuboid(-12.0F, -1.0F, -2.5F, 13.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(25, 17).cuboid(-11.5F, -2.0F, -2.5F, 12.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(25, 23).cuboid(-11.0F, -3.0F, -2.0F, 11.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(0, 28).cuboid(-10.5F, -4.0F, -2.0F, 10.0F, 1.0F, 3.0F, new Dilation(0.0F))
                .uv(33, 0).cuboid(-10.0F, -5.0F, -1.5F, 9.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(33, 4).cuboid(-9.5F, -6.0F, -1.5F, 8.0F, 1.0F, 2.0F, new Dilation(0.0F))
                .uv(33, 8).cuboid(-9.0F, -7.0F, -1.0F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(38, 28).cuboid(-8.5F, -8.0F, -1.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(38, 31).cuboid(-8.0F, -9.0F, -1.0F, 5.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(5.25F, -2.0F, 0.25F));

        ModelPartData arms = root.addChild("arms", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -9.0F, 0.0F));

        ModelPartData right_arm = arms.addChild("right_arm", ModelPartBuilder.create().uv(27, 28).cuboid(-1.0F, -1.5F, -1.5F, 2.0F, 13.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(-4.0F, 0.0F, 0.0F));

        ModelPartData right_piston = right_arm.addChild("right_piston", ModelPartBuilder.create(), ModelTransform.pivot(-0.5F, 9.5F, 0.0F));

        ModelPartData right_upper_piston = right_piston.addChild("right_upper_piston", ModelPartBuilder.create().uv(38, 34).cuboid(-0.5F, 2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_bottom_piston = right_piston.addChild("right_bottom_piston", ModelPartBuilder.create().uv(47, 47).cuboid(0.0F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData left_arm = arms.addChild("left_arm", ModelPartBuilder.create().uv(0, 33).cuboid(-1.0F, -1.5F, -1.5F, 2.0F, 13.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(4.0F, 0.0F, 0.0F));

        ModelPartData left_piston = left_arm.addChild("left_piston", ModelPartBuilder.create(), ModelTransform.pivot(-0.5F, 9.5F, 0.0F));

        ModelPartData left_bottom_piston = left_piston.addChild("left_bottom_piston", ModelPartBuilder.create().uv(34, 49).cuboid(0.0F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData left_upper_piston = left_piston.addChild("left_upper_piston", ModelPartBuilder.create().uv(38, 39).cuboid(-0.5F, 2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(GoldChannelerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        // head follows the look direction (animations are additive on top of this)
        this.head.yaw = netHeadYaw * MathHelper.RADIANS_PER_DEGREE;
        this.head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;

        this.animateMovement(GoldChannelerAnimations.GOLD_CHANNELER_WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        this.updateAnimation(entity.idleAnimationState, GoldChannelerAnimations.GOLD_CHANNELER_IDLE, ageInTicks, 1.0F);
        this.updateAnimation(entity.pressLeftAnimationState, GoldChannelerAnimations.GOLD_CHANNELER_PRESS_LEFT, ageInTicks, 1.0F);
        this.updateAnimation(entity.pressRightAnimationState, GoldChannelerAnimations.GOLD_CHANNELER_PRESS_RIGHT, ageInTicks, 1.0F);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        this.root.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}