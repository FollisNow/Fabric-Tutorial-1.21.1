package net.follis.tutorialmod.item.render;

import net.follis.tutorialmod.entity.client.ScorpionModel;
import net.follis.tutorialmod.entity.client.ScorpionRenderer;
import net.follis.tutorialmod.entity.custom.ScorpionVariant;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem.BugData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ScorpionFigurineRenderer extends AbstractBugFigurineRenderer<ScorpionModel<?>> {
    
    @Override
    protected ScorpionModel<?> createModel(MinecraftClient client) {
        return new ScorpionModel<>(client.getEntityModelLoader().getModelPart(ScorpionModel.SCORPION));
    }

    @Override
    protected Identifier getTexture(BugData data) {
        ScorpionVariant variant = data != null ? ScorpionVariant.byId(data.getVariantId()) : ScorpionVariant.DESERT;
        return ScorpionRenderer.getTexture(variant);
    }

    @Override
    protected void renderModel(ScorpionModel<?> model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, BugData data) {
        model.render(matrices, buffer, light, overlay, -1);
    }
}