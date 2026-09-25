package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.BeetleModel;
import net.follis.tutorialmod.entity.client.BeetleRenderer;
import net.follis.tutorialmod.entity.custom.BeetleVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BeetleFigurineRenderer extends AbstractBugFigurineRenderer<BeetleModel<?>> {
    @Override
    protected BeetleModel<?> createModel(MinecraftClient client) {
        return new BeetleModel<>(client.getEntityModelLoader().getModelPart(BeetleModel.BEETLE));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        BeetleVariant variant = data != null ? BeetleVariant.byId(data.getVariantId()) : BeetleVariant.LADYBUG;
        return BeetleRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(BeetleModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}