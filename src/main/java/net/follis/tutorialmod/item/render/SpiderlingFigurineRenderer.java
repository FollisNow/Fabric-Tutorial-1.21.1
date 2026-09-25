package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.SpiderlingModel;
import net.follis.tutorialmod.entity.client.SpiderlingRenderer;
import net.follis.tutorialmod.entity.custom.SpiderlingVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SpiderlingFigurineRenderer extends AbstractBugFigurineRenderer<SpiderlingModel<?>> {
    @Override
    protected SpiderlingModel<?> createModel(MinecraftClient client) {
        return new SpiderlingModel<>(client.getEntityModelLoader().getModelPart(SpiderlingModel.SPIDERLING));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        SpiderlingVariant variant = data != null ? SpiderlingVariant.byId(data.getVariantId()) : SpiderlingVariant.DEFAULT;
        return SpiderlingRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(SpiderlingModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}