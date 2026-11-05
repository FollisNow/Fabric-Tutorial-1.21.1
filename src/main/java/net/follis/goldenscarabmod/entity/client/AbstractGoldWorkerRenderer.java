package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.AbstractGoldWorkerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders the template item the golem holds.
 *
 * The old version stored "cachedBodyYaw" in a renderer field. A renderer is shared by EVERY entity of
 * its type, so several golems overwrote each other's value, and the lerp factor depended on the frame
 * rate. Here the yaw comes from the entity itself and is interpolated with the partial tick: no state.
 */
public abstract class AbstractGoldWorkerRenderer<T extends AbstractGoldWorkerEntity, M extends EntityModel<T>>
        extends MobEntityRenderer<T, M> {

    private final HeldItemRenderer heldItemRenderer;

    protected AbstractGoldWorkerRenderer(EntityRendererFactory.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
        this.heldItemRenderer = context.getHeldItemRenderer();
    }

    /** Position/rotate/scale the held item relative to the golem (already rotated to face the body yaw). */
    protected abstract void applyItemTransform(MatrixStack matrices);

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        ItemStack stack = entity.getMainHandStack();
        if (stack.isEmpty()) return;

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        this.applyItemTransform(matrices);
        this.heldItemRenderer.renderItem(entity, stack, ModelTransformationMode.HEAD, false,
                matrices, vertexConsumers, light);
        matrices.pop();
    }
}