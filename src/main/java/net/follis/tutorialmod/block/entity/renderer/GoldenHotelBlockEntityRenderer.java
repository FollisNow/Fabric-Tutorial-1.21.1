package net.follis.tutorialmod.block.entity.renderer;

import net.follis.tutorialmod.block.ModBlocks;
import net.follis.tutorialmod.block.custom.GoldenHotelBlock;
import net.follis.tutorialmod.block.entity.custom.GoldenHotelBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.List;

public class GoldenHotelBlockEntityRenderer implements BlockEntityRenderer<GoldenHotelBlockEntity> {
    List<Integer> offsets = List.of(-2, 2);



    public GoldenHotelBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    @Override
    public void render(GoldenHotelBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        ItemStack stack = entity.getStack(0);

        matrices.push();
        matrices.translate(0.5f, 0.35f, 0.5f);
        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getRenderingRotation()));

        itemRenderer.renderItem(stack, ModelTransformationMode.GUI, getLightLevel(entity.getWorld(),
                entity.getPos()), OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 1);
        matrices.pop();

        if (entity.getWorld().isClient && !entity.getWorld().getBlockState(entity.getPos()).isAir()) {
            Direction facing = entity.getWorld().getBlockState(entity.getPos()).get(GoldenHotelBlock.FACING);
            offsets.forEach(offset -> {
                if (entity.getWorld().getBlockState(entity.getPos().offset(facing.rotateYClockwise(), offset)).isAir()) {
                    if (facing == Direction.NORTH){
                        renderSidePedestal(matrices, vertexConsumers, light, overlay, offset, 0);
                    }
                    if (facing == Direction.SOUTH) {
                        renderSidePedestal(matrices, vertexConsumers, light, overlay, offset * -1, 0);
                    }
                    if (facing == Direction.EAST){
                        renderSidePedestal(matrices, vertexConsumers, light, overlay, 0, offset);
                    }
                    if (facing == Direction.WEST) {
                        renderSidePedestal(matrices, vertexConsumers, light, overlay, 0, offset * -1);
                    }
                }
            });
        }
    }

    private static void renderSidePedestal(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int pPackedLight, int pPackedOverlay,
                                           float xOffset, float zOffset) {
        BlockRenderManager blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
        BlockState state = ModBlocks.GOLDEN_PEDESTAL.getDefaultState();
        BakedModel model = blockRenderer.getModel(state);

        matrices.push();
        matrices.translate(xOffset,0, zOffset);

        RenderLayer translucentType = ModRenderTypes.GHOST_RENDER_LAYER;
        VertexConsumer consumer = vertexConsumers.getBuffer(translucentType).color(1f, 1f, 1f, 1f);

        blockRenderer.getModelRenderer().render(matrices.peek(),
                consumer,
                state,
                model, 1f, 1f, 1f,
                pPackedLight,
                pPackedOverlay);
        matrices.pop();
    }

    private int getLightLevel(World world, BlockPos pos) {
        int bLight = world.getLightLevel(LightType.BLOCK, pos);
        int sLight = world.getLightLevel(LightType.SKY, pos);
        return LightmapTextureManager.pack(bLight, sLight);
    }
}
