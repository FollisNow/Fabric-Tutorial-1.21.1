package net.follis.tutorialmod.item.render;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.follis.tutorialmod.component.ModDataComponentTypes;
import net.follis.tutorialmod.item.custom.AbstractEntityJarItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public abstract class AbstractBugFigurineRenderer<M> implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private M model;

    protected abstract M createModel(MinecraftClient client);
    protected abstract Identifier getTexture(AbstractEntityJarItem.BugData data);
    protected abstract void renderModel(M model, MatrixStack matrices, VertexConsumer buffer, int light, int overlay, AbstractEntityJarItem.BugData data);

    private M getOrCreateModel() {
        if (this.model == null) {
            this.model = createModel(MinecraftClient.getInstance());
        }
        return this.model;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        AbstractEntityJarItem.BugData data = stack.get(ModDataComponentTypes.CAPTURED_BUG);
        Identifier texture = getTexture(data);
        int effectiveLight = (mode == ModelTransformationMode.GUI) ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;

        if (mode == ModelTransformationMode.GUI) DiffuseLighting.enableGuiDepthLighting();

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.scale(0.5F, 0.5F, 0.5F);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        renderModel(getOrCreateModel(), matrices, buffer, effectiveLight, overlay, data);

        matrices.pop();

        if (mode == ModelTransformationMode.GUI) DiffuseLighting.disableGuiDepthLighting();
    }
}
