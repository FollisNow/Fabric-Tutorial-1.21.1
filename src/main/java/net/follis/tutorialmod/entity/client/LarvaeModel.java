package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.LarvaeEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.List;

public class LarvaeModel<T extends LarvaeEntity> extends SinglePartEntityModel<T> {
    public static final EntityModelLayer LARVAE = new EntityModelLayer(Identifier.of(TutorialMod.MOD_ID, "larvae"), "main");
    private final ModelPart larvae;
    private final ModelPart body;
    private final ModelPart mandibule_L;
    private final ModelPart mandibule_R;
    private final ModelPart cocoon;
    private final ModelPart gems;
    private final ModelPart material0;
    private final ModelPart gem20;
    private final ModelPart gem16;
    private final ModelPart gem10;
    private final ModelPart gem7;
    private final ModelPart gem3;
    private final ModelPart gem13;
    private final ModelPart gem14;
    private final ModelPart material1;
    private final ModelPart gem5;
    private final ModelPart gem9;
    private final ModelPart gem11;
    private final ModelPart gem17;
    private final ModelPart gem19;
    private final ModelPart gem1;
    private final ModelPart material2;
    private final ModelPart gem4;
    private final ModelPart gem6;
    private final ModelPart gem15;
    private final ModelPart gem0;
    private final ModelPart material3;
    private final ModelPart gem8;
    private final ModelPart gem12;
    private final ModelPart gem18;
    private final ModelPart gem2;
    public LarvaeModel(ModelPart root) {
        this.larvae = root.getChild("Larvae");
        this.body = this.larvae.getChild("body");
        this.mandibule_L = this.body.getChild("mandibule_L");
        this.mandibule_R = this.body.getChild("mandibule_R");
        this.cocoon = this.larvae.getChild("cocoon");
        this.gems = this.cocoon.getChild("gems");
        this.material0 = this.gems.getChild("material0");
        this.gem20 = this.material0.getChild("gem20");
        this.gem16 = this.material0.getChild("gem16");
        this.gem10 = this.material0.getChild("gem10");
        this.gem7 = this.material0.getChild("gem7");
        this.gem3 = this.material0.getChild("gem3");
        this.gem13 = this.material0.getChild("gem13");
        this.gem14 = this.material0.getChild("gem14");
        this.material1 = this.gems.getChild("material1");
        this.gem5 = this.material1.getChild("gem5");
        this.gem9 = this.material1.getChild("gem9");
        this.gem11 = this.material1.getChild("gem11");
        this.gem17 = this.material1.getChild("gem17");
        this.gem19 = this.material1.getChild("gem19");
        this.gem1 = this.material1.getChild("gem1");
        this.material2 = this.gems.getChild("material2");
        this.gem4 = this.material2.getChild("gem4");
        this.gem6 = this.material2.getChild("gem6");
        this.gem15 = this.material2.getChild("gem15");
        this.gem0 = this.material2.getChild("gem0");
        this.material3 = this.gems.getChild("material3");
        this.gem8 = this.material3.getChild("gem8");
        this.gem12 = this.material3.getChild("gem12");
        this.gem18 = this.material3.getChild("gem18");
        this.gem2 = this.material3.getChild("gem2");
        material0.visible = false;
        material1.visible = false;
        material2.visible = false;
        material3.visible = false;
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData Larvae = modelPartData.addChild("Larvae", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        ModelPartData body = Larvae.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -2.0F, -5.0F, 2.0F, 2.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData mandibule_L = body.addChild("mandibule_L", ModelPartBuilder.create(), ModelTransform.pivot(1.0F, -0.6F, -4.15F));

        ModelPartData cube_r1 = mandibule_L.addChild("cube_r1", ModelPartBuilder.create().uv(18, 9).cuboid(0.0F, -1.0F, -1.5F, 0.0F, 2.0F, 3.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, -1.75F, -0.5672F, 0.0F, 0.0F));

        ModelPartData mandibule_R = body.addChild("mandibule_R", ModelPartBuilder.create(), ModelTransform.pivot(-1.0F, -0.6F, -4.1F));

        ModelPartData cube_r2 = mandibule_R.addChild("cube_r2", ModelPartBuilder.create().uv(18, 4).cuboid(0.0F, -1.0F, -1.5F, 0.0F, 2.0F, 3.0F, new Dilation(0.001F)), ModelTransform.of(0.0F, 0.0F, -1.8F, -0.5672F, 0.0F, 0.0F));

        ModelPartData cocoon = Larvae.addChild("cocoon", ModelPartBuilder.create().uv(0, 18).cuboid(-2.0F, 0.0F, 3.0F, 3.0F, 1.0F, 4.0F, new Dilation(0.002F))
                .uv(18, 0).cuboid(-2.0F, -1.2F, 3.0F, 3.0F, 2.0F, 2.0F, new Dilation(0.001F))
                .uv(0, 9).cuboid(-2.0F, -2.0F, -3.0F, 3.0F, 3.0F, 6.0F, new Dilation(0.003F)), ModelTransform.pivot(0.5F, -0.8F, -1.0F));

        ModelPartData gems = cocoon.addChild("gems", ModelPartBuilder.create(), ModelTransform.pivot(-1.55F, -1.8F, -1.85F));

        ModelPartData material0 = gems.addChild("material0", ModelPartBuilder.create(), ModelTransform.pivot(1.2308F, 1.5756F, 3.4143F));

        ModelPartData gem20 = material0.addChild("gem20", ModelPartBuilder.create(), ModelTransform.of(-1.0143F, 0.9857F, -3.0143F, 0.0F, 0.7505F, 0.0F));

        ModelPartData cube_r3 = gem20.addChild("cube_r3", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.3F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem16 = material0.addChild("gem16", ModelPartBuilder.create(), ModelTransform.of(0.3857F, 0.9857F, 2.9857F, 0.0F, -0.8727F, 0.0F));

        ModelPartData cube_r4 = gem16.addChild("cube_r4", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem10 = material0.addChild("gem10", ModelPartBuilder.create(), ModelTransform.pivot(-0.3893F, 0.5357F, 4.5357F));

        ModelPartData cube_r5 = gem10.addChild("cube_r5", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem7 = material0.addChild("gem7", ModelPartBuilder.create(), ModelTransform.of(0.2428F, -1.5143F, -3.7152F, 0.0F, 1.1694F, 0.0F));

        ModelPartData cube_r6 = gem7.addChild("cube_r6", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem3 = material0.addChild("gem3", ModelPartBuilder.create(), ModelTransform.of(-1.2143F, 0.3857F, 1.2857F, 0.9195F, -0.33F, 0.2634F));

        ModelPartData cube_r7 = gem3.addChild("cube_r7", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem13 = material0.addChild("gem13", ModelPartBuilder.create(), ModelTransform.pivot(0.4857F, -1.5143F, 0.2857F));

        ModelPartData cube_r8 = gem13.addChild("cube_r8", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.025F, 0.4F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem14 = material0.addChild("gem14", ModelPartBuilder.create(), ModelTransform.of(1.0857F, 0.2857F, -2.2143F, 0.0F, 0.0F, 0.3142F));

        ModelPartData cube_r9 = gem14.addChild("cube_r9", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData material1 = gems.addChild("material1", ModelPartBuilder.create(), ModelTransform.pivot(0.7498F, 1.278F, 3.3833F));

        ModelPartData gem5 = material1.addChild("gem5", ModelPartBuilder.create(), ModelTransform.of(-0.9333F, 0.2833F, 2.7167F, 0.0F, 0.0F, 0.2967F));

        ModelPartData cube_r10 = gem5.addChild("cube_r10", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem9 = material1.addChild("gem9", ModelPartBuilder.create(), ModelTransform.of(-0.4333F, -1.2167F, 0.2167F, 0.0F, 0.733F, 0.0F));

        ModelPartData cube_r11 = gem9.addChild("cube_r11", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.375F, 0.05F, -0.4F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem11 = material1.addChild("gem11", ModelPartBuilder.create(), ModelTransform.of(1.5667F, -0.7167F, -3.0833F, -0.5465F, 0.2123F, 0.2087F));

        ModelPartData cube_r12 = gem11.addChild("cube_r12", ModelPartBuilder.create().uv(0, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem17 = material1.addChild("gem17", ModelPartBuilder.create(), ModelTransform.pivot(-0.2333F, 1.2833F, 3.8167F));

        ModelPartData cube_r13 = gem17.addChild("cube_r13", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, -0.5F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem19 = material1.addChild("gem19", ModelPartBuilder.create(), ModelTransform.pivot(0.8667F, 1.2833F, -1.8833F));

        ModelPartData cube_r14 = gem19.addChild("cube_r14", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem1 = material1.addChild("gem1", ModelPartBuilder.create(), ModelTransform.of(-0.9198F, -0.1261F, -3.1833F, 0.6226F, -0.2355F, 0.2895F));

        ModelPartData cube_r15 = gem1.addChild("cube_r15", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData material2 = gems.addChild("material2", ModelPartBuilder.create(), ModelTransform.pivot(0.5415F, 1.0363F, 3.1F));

        ModelPartData gem4 = material2.addChild("gem4", ModelPartBuilder.create(), ModelTransform.of(1.225F, -0.075F, 2.9F, 0.0F, -0.6545F, 0.0F));

        ModelPartData cube_r16 = gem4.addChild("cube_r16", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem6 = material2.addChild("gem6", ModelPartBuilder.create(), ModelTransform.of(0.925F, -0.875F, -0.6F, 0.0F, 0.6981F, 0.0F));

        ModelPartData cube_r17 = gem6.addChild("cube_r17", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.175F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem15 = material2.addChild("gem15", ModelPartBuilder.create(), ModelTransform.of(1.675F, 0.225F, 0.0F, 0.6135F, -0.2674F, 0.1888F));

        ModelPartData cube_r18 = gem15.addChild("cube_r18", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem0 = material2.addChild("gem0", ModelPartBuilder.create(), ModelTransform.of(-0.725F, 0.825F, -1.4F, 0.0F, 0.0F, 0.3316F));

        ModelPartData cube_r19 = gem0.addChild("cube_r19", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData material3 = gems.addChild("material3", ModelPartBuilder.create(), ModelTransform.pivot(1.1915F, 0.9863F, 3.825F));

        ModelPartData gem8 = material3.addChild("gem8", ModelPartBuilder.create(), ModelTransform.of(-0.475F, -0.9F, -2.75F, 0.0F, 1.7017F, 0.0F));

        ModelPartData cube_r20 = gem8.addChild("cube_r20", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem12 = material3.addChild("gem12", ModelPartBuilder.create(), ModelTransform.pivot(0.925F, 0.575F, 2.275F));

        ModelPartData cube_r21 = gem12.addChild("cube_r21", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem18 = material3.addChild("gem18", ModelPartBuilder.create(), ModelTransform.pivot(0.125F, 1.575F, 0.175F));

        ModelPartData cube_r22 = gem18.addChild("cube_r22", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.6155F));

        ModelPartData gem2 = material3.addChild("gem2", ModelPartBuilder.create(), ModelTransform.of(-1.475F, -0.125F, -0.225F, 1.2741F, 0.0F, 0.0F));

        ModelPartData cube_r23 = gem2.addChild("cube_r23", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.1F, 0.0F, 0.0F, -1.0872F, 0.3563F, 0.935F));

        ModelPartData gem21 = material3.addChild("gem21", ModelPartBuilder.create(), ModelTransform.of(-0.675F, -0.025F, 1.775F, 0.0F, -2.0682F, 0.0F));

        ModelPartData cube_r24 = gem21.addChild("cube_r24", ModelPartBuilder.create().uv(1, 3).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.175F, 0.0F, -0.7854F, 0.0F, 0.6155F));
        return TexturedModelData.of(modelData, 32, 32);
    }
    @Override
    public void setAngles(LarvaeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);

        this.animateMovement(LarvaeAnimations.ANIM_LARVAE_CRAWL, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.updateAnimation(entity.idleAnimationState, LarvaeAnimations.ANIM_LARVAE_IDLE, ageInTicks, 1f);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        larvae.render(matrices, vertexConsumer, light, overlay, color);
    }

    @Override
    public ModelPart getPart() {
        return larvae;
    }

    public List<ModelPart> getMaterialParts() {
        return List.of(material0, material1, material2, material3);
    }

    public void renderMaterialPart(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int index, int color) {
        ModelPart part = getMaterialParts().get(index);

        matrices.push();
        larvae.rotate(matrices);
        cocoon.rotate(matrices);
        gems.rotate(matrices);

        part.visible = true;
        part.render(matrices, vertexConsumer, light, overlay, color);
        part.visible = false;

        matrices.pop();
    }
}
