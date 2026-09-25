package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.LocustModel;
import net.follis.tutorialmod.entity.client.LocustRenderer;
import net.follis.tutorialmod.entity.custom.LocustVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LocustFigurineRenderer extends AbstractBugFigurineRenderer<LocustModel<?>> {
    @Override
    protected LocustModel<?> createModel(MinecraftClient client) {
        return new LocustModel<>(client.getEntityModelLoader().getModelPart(LocustModel.LOCUST));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        LocustVariant variant = data != null ? LocustVariant.byId(data.getVariantId()) : LocustVariant.GRASSHOPPER;
        return LocustRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(LocustModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}