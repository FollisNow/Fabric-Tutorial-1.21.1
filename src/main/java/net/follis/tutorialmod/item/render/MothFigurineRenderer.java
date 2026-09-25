package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.MothModel;
import net.follis.tutorialmod.entity.client.MothRenderer;
import net.follis.tutorialmod.entity.custom.MothVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MothFigurineRenderer extends AbstractBugFigurineRenderer<MothModel<?>> {
    @Override
    protected MothModel<?> createModel(MinecraftClient client) {
        return new MothModel<>(client.getEntityModelLoader().getModelPart(MothModel.MOTH));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        MothVariant variant = data != null ? MothVariant.byId(data.getVariantId()) : MothVariant.OAK;
        return MothRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(MothModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}