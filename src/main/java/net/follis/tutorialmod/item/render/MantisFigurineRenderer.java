package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.MantisModel;
import net.follis.tutorialmod.entity.client.MantisRenderer;
import net.follis.tutorialmod.entity.custom.MantisVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MantisFigurineRenderer extends AbstractBugFigurineRenderer<MantisModel<?>> {
    @Override
    protected MantisModel<?> createModel(MinecraftClient client) {
        return new MantisModel<>(client.getEntityModelLoader().getModelPart(MantisModel.MANTIS));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        MantisVariant variant = data != null ? MantisVariant.byId(data.getVariantId()) : MantisVariant.DEFAULT;
        return MantisRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(MantisModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}