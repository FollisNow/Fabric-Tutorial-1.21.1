package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.BambooTrapEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BambooTrapModel extends EntityModel<BambooTrapEntity> {
    public static final EntityModelLayer BAMBOO_TRAP = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "bamboo_trap"), "main");
    private final ModelPart root;
    private final ModelPart outer;
    private final ModelPart right_maw;
    private final ModelPart left_maw;
    private final ModelPart pivot;
    public BambooTrapModel(ModelPart root) {
        this.root = root.getChild("root");
        this.outer = this.root.getChild("outer");
        this.right_maw = this.outer.getChild("right_maw");
        this.left_maw = this.outer.getChild("left_maw");
        this.pivot = this.root.getChild("pivot");
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, -7.0F));

        ModelPartData outer = root.addChild("outer", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData right_maw = outer.addChild("right_maw", ModelPartBuilder.create().uv(18, 15).cuboid(-3.5F, -1.5F, -3.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.005F))
                .uv(18, 11).cuboid(-3.0F, -0.5F, -4.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.005F))
                .uv(4, 4).cuboid(-4.0F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.005F))
                .uv(4, 13).cuboid(3.0F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.005F)), ModelTransform.pivot(0.0F, -0.5F, 7.0F));

        ModelPartData left_maw = outer.addChild("left_maw", ModelPartBuilder.create().uv(18, 17).cuboid(-3.5F, -1.5F, 2.5F, 7.0F, 1.0F, 1.0F, new Dilation(0.0F))
                .uv(4, 4).cuboid(-4.0F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(4, 13).cuboid(3.0F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(18, 9).cuboid(-3.0F, -0.5F, 3.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -0.5F, 7.0F));

        ModelPartData pivot = root.addChild("pivot", ModelPartBuilder.create().uv(18, 13).cuboid(-3.0F, -1.5F, 2.0F, 6.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 4.5F));
        return TexturedModelData.of(modelData, 64, 64);
    }
    @Override
    public void setAngles(BambooTrapEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        root.render(matrices, vertexConsumer, light, overlay, color);
    }
}
