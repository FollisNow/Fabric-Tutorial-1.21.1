package net.follis.tutorialmod.block.entity.renderer;

import net.follis.tutorialmod.block.entity.custom.TrappedSkullBlockEntity;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

import java.util.Map;

public class TrappedSkullBlockEntityRenderer implements BlockEntityRenderer<TrappedSkullBlockEntity> {
    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> models;

    public TrappedSkullBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.models = SkullBlockEntityRenderer.getModels(ctx.getLayerRenderDispatcher());
    }

    @Override
    public void render(TrappedSkullBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        BlockState state = be.getCachedState();
        SkullBlock.SkullType type = ((AbstractSkullBlock) state.getBlock()).getSkullType();
        Direction facing = state.getBlock() instanceof WallSkullBlock ? state.get(WallSkullBlock.FACING) : null;

        SkullBlockEntityRenderer.renderSkull(facing, be.getRenderYaw(tickDelta), 0f, matrices, vcp, light,
                models.get(type), SkullBlockEntityRenderer.getRenderLayer(type, null));
    }
}
