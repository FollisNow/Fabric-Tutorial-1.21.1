package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.ScorpionEntity;
import net.follis.goldenscarabmod.entity.custom.ScorpionShard;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Draws the assimilated pottery sherd lying on top of the scorpion's body. */
public class ScorpionShardFeatureRenderer extends FeatureRenderer<ScorpionEntity, ScorpionModel<ScorpionEntity>> {
    /** 1.0 = a full 16 px sprite; 0.3 is about 5 px, the width of the body. */
    private static final float SCALE = 0.3F;

    private final ItemRenderer itemRenderer;

    public ScorpionShardFeatureRenderer(FeatureRendererContext<ScorpionEntity, ScorpionModel<ScorpionEntity>> context,
                                        ItemRenderer itemRenderer) {
        super(context);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ScorpionEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        ScorpionShard shard = entity.getShard();
        if (shard == ScorpionShard.NONE || entity.isInvisible()) return;

        matrices.push();
        // move into the body part's space (pivot + any animation transform), so the sherd follows the body
        this.getContextModel().getPart().rotate(matrices);
        // model space is y-down: -3 px is the top of the body cuboid; the extra -0.02 stops z-fighting
        // z = -1 px puts it slightly forward of centre, well away from the tail base at z = 5
        matrices.translate(0.0F, -(3.0F / 16.0F) - 0.02F, -1.0F / 16.0F);
        // undo the renderer's (-1, -1, 1) flip so the item is upright, then lay it flat
        // (sprite top toward the front of the scorpion, readable from behind)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.scale(SCALE, SCALE, SCALE);

        // NONE = no item-model transform, so only the transforms above apply
        this.itemRenderer.renderItem(entity, shard.getSherd().getDefaultStack(), ModelTransformationMode.NONE, false,
                matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId());
        matrices.pop();
    }
}