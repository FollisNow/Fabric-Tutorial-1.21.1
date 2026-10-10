package net.follis.tutorialmod.block.entity.renderer;

import net.follis.tutorialmod.block.entity.custom.GoldRepulsorBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class GoldRepulsorBlockEntityRenderer implements BlockEntityRenderer<GoldRepulsorBlockEntity> {

    public GoldRepulsorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    @Override
    public void render(GoldRepulsorBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
    }
}
