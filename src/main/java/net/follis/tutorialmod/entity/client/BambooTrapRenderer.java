package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.BambooTrapEntity;
import net.follis.tutorialmod.entity.custom.ChairEntity;
import net.follis.tutorialmod.entity.custom.GoldenNeedleProjectileEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

public class BambooTrapRenderer extends MobEntityRenderer<BambooTrapEntity, BambooTrapModel> {
    protected BambooTrapModel model;

    public BambooTrapRenderer(EntityRendererFactory.Context context) {
        super(context,new BambooTrapModel(context.getPart(BambooTrapModel.BAMBOO_TRAP)) , 0.1f);
        this.model = new BambooTrapModel(context.getPart(BambooTrapModel.BAMBOO_TRAP));
    }

    @Override
    public Identifier getTexture(BambooTrapEntity entity) {
        return Identifier.of(TutorialMod.MOD_ID, "textures/entity/bamboo_trap/bamboo_trap.png");
    }

    @Nullable
    protected RenderLayer getRenderLayer(BambooTrapEntity bambooTrapEntity, boolean bl, boolean bl2, boolean bl3) {
        Identifier identifier = this.getTexture(bambooTrapEntity);
        return this.model.getLayer(identifier);
    }
}
