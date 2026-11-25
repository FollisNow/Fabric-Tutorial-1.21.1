package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.BookskitoEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BookskitoModel<T extends BookskitoEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer BOOKSKITO = new EntityModelLayer(Identifier.of(GoldenScarabMod.MOD_ID, "bookskito"), "main");
    private final ModelPart root;
    private final ModelPart bookskito;
    private final ModelPart left_cover;
    private final ModelPart mark;
    private final ModelPart left_legs;
    private final ModelPart left_legs_tip;
    private final ModelPart right_cover;
    private final ModelPart right_legs;
    private final ModelPart right_legs_tip;
    private final ModelPart back;
    private final ModelPart left_eye;
    private final ModelPart right_eye;
    public BookskitoModel(ModelPart root) {
        this.root = root;
        this.bookskito = root.getChild("bookskito");
        this.left_cover = this.bookskito.getChild("left_cover");
        this.mark = this.left_cover.getChild("mark");
        this.left_legs = this.left_cover.getChild("left_legs");
        this.left_legs_tip = this.left_legs.getChild("left_legs_tip");
        this.right_cover = this.bookskito.getChild("right_cover");
        this.right_legs = this.right_cover.getChild("right_legs");
        this.right_legs_tip = this.right_legs.getChild("right_legs_tip");
        this.back = this.bookskito.getChild("back");
        this.left_eye = this.back.getChild("left_eye");
        this.right_eye = this.back.getChild("right_eye");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bookskito = modelPartData.addChild("bookskito", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 18.0F, 0.0F));

        ModelPartData left_cover = bookskito.addChild("left_cover", ModelPartBuilder.create().uv(0, 9).cuboid(0.0F, 0.0F, -4.5F, 7.0F, 0.0F, 9.0F, new Dilation(0.005F))
                .uv(0, 26).cuboid(0.0F, 0.0F, -3.5F, 6.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(1.0F, 0.0F, 0.0F));

        ModelPartData mark = left_cover.addChild("mark", ModelPartBuilder.create().uv(26, 27).cuboid(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.pivot(7.0F, 0.0F, -1.5F));

        ModelPartData left_legs = left_cover.addChild("left_legs", ModelPartBuilder.create().uv(30, 27).cuboid(0.0F, 0.0F, -2.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(26, 30).cuboid(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(28, 27).cuboid(0.0F, 0.0F, 1.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(5.25F, 1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));

        ModelPartData left_legs_tip = left_legs.addChild("left_legs_tip", ModelPartBuilder.create().uv(32, 11).cuboid(0.0F, 0.0F, -2.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 13).cuboid(0.0F, 0.0F, -0.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 15).cuboid(0.0F, 0.0F, 1.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, -1.0036F));

        ModelPartData right_cover = bookskito.addChild("right_cover", ModelPartBuilder.create().uv(0, 0).cuboid(-7.0F, 0.0F, -4.5F, 7.0F, 0.0F, 9.0F, new Dilation(0.005F))
                .uv(0, 18).cuboid(-6.0F, 0.0F, -3.5F, 6.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(-1.0F, 0.0F, 0.0F));

        ModelPartData right_legs = right_cover.addChild("right_legs", ModelPartBuilder.create().uv(28, 30).cuboid(0.0F, 0.0F, -2.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(30, 30).cuboid(0.0F, 0.0F, -0.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 0).cuboid(0.0F, 0.0F, 1.5F, 0.0F, 2.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(-5.25F, 1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));

        ModelPartData right_legs_tip = right_legs.addChild("right_legs_tip", ModelPartBuilder.create().uv(32, 27).cuboid(0.0F, 0.0F, -2.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 29).cuboid(0.0F, 0.0F, -0.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 31).cuboid(0.0F, 0.0F, 1.5F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 1.0036F));

        ModelPartData back = bookskito.addChild("back", ModelPartBuilder.create().uv(26, 18).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 9.0F, new Dilation(0.005F)), ModelTransform.pivot(0.0F, 0.0F, -3.5F));

        ModelPartData left_eye = back.addChild("left_eye", ModelPartBuilder.create(), ModelTransform.of(0.75F, 0.0F, 0.5F, 0.0F, 0.0F, 0.3054F));

        ModelPartData cube_r1 = left_eye.addChild("cube_r1", ModelPartBuilder.create().uv(32, 5).cuboid(0.5F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(32, 17).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, -2.0F, 0.0F, -0.8727F, 1.1781F, 0.0F));

        ModelPartData cube_r2 = left_eye.addChild("cube_r2", ModelPartBuilder.create().uv(32, 3).cuboid(-0.5F, -2.0F, 0.0F, 1.0F, 2.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 1.1781F, 0.0F));

        ModelPartData right_eye = back.addChild("right_eye", ModelPartBuilder.create(), ModelTransform.of(-0.75F, 0.0F, 0.5F, 0.0F, 0.0F, -0.3054F));

        ModelPartData cube_r3 = right_eye.addChild("cube_r3", ModelPartBuilder.create().uv(32, 9).cuboid(-0.5F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F, new Dilation(0.001F))
                .uv(26, 33).cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, -2.0F, 0.0F, -0.8727F, -1.1781F, 0.0F));

        ModelPartData cube_r4 = right_eye.addChild("cube_r4", ModelPartBuilder.create().uv(32, 7).cuboid(-0.5F, -2.0F, 0.0F, 1.0F, 2.0F, 0.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(BookskitoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        switch (entity.getRestState()) {
            case ROOSTING -> this.updateAnimation(entity.roostingAnimationState, BookskitoAnimations.ROOSTING_ANIMATION, ageInTicks, 1.0F);
            case GROUNDED -> this.updateAnimation(entity.groundedAnimationState, BookskitoAnimations.GROUNDED_ANIMATION, ageInTicks, 1.0F);
            default -> {
                this.bookskito.pitch = -0.5F; // constant tilt while flying
                this.updateAnimation(entity.flyingAnimationState, BookskitoAnimations.FLY_ANIMATION, ageInTicks, 1.0F);
            }
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
