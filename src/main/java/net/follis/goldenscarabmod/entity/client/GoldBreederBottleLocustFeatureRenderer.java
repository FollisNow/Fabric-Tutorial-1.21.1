package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.GoldBreederEntity;
import net.follis.goldenscarabmod.entity.custom.LocustEntity;
import net.follis.goldenscarabmod.entity.custom.LocustVariant;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Draws a small locust living inside the golem's glass bottle. It uses the real LocustModel (idle animation, and the
 * jump animation while the golem walks), pinned to the "bottle" bone so it follows the golem's movement.
 */
public class GoldBreederBottleLocustFeatureRenderer
        extends FeatureRenderer<GoldBreederEntity, GoldBreederModel<GoldBreederEntity>> {

    /** Variant shown in the bottle (the golem's meat item is the grasshopper locust). */
    private static final LocustVariant VARIANT = LocustVariant.GRASSHOPPER;

    // Where the locust stands, in bottle-local model pixels. The glass interior is about
    // x -6..0, y -8..0 (0 = floor, -8 = top), z -7..-1, so this is the middle of the floor.
    private static final float FLOOR_X = -3.0F;
    private static final float FLOOR_Y = -0.2F;
    private static final float FLOOR_Z = -4.0F;
    /** Turns the locust inside the bottle (degrees). */
    private static final float YAW_DEGREES = 35.0F;
    /** The locust is ~13 px long with its wings and the glass interior is 6 px wide: 0.4 fits it. */
    private static final float SCALE = 0.4F;

    private final LocustModel<LocustEntity> locustModel;

    public GoldBreederBottleLocustFeatureRenderer(
            FeatureRendererContext<GoldBreederEntity, GoldBreederModel<GoldBreederEntity>> context,
            LocustModel<LocustEntity> locustModel) {
        super(context);
        this.locustModel = locustModel;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       GoldBreederEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;

        matrices.push();

        // into the bottle bone, then onto the middle of its floor
        this.getContextModel().transformToBottle(matrices);
        matrices.translate(FLOOR_X / 16.0F, FLOOR_Y / 16.0F, FLOOR_Z / 16.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(YAW_DEGREES));
        matrices.scale(SCALE, SCALE, SCALE);
        // The locust model is built with its feet at y = 24 px (its root pivot). Lift it by 24 px (1.5 blocks in its own
        // units, model Y points down) so those feet land exactly on the bottle floor.
        matrices.translate(0.0F, -1.5F, 0.0F);

        this.locustModel.setDecorativeAngles(entity.bottleLocustAnimationState, limbAngle, limbDistance, animationProgress);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(LocustRenderer.getTexture(VARIANT)));
        this.locustModel.render(matrices, consumer, light, LivingEntityRenderer.getOverlay(entity, 0.0F), -1);

        matrices.pop();
    }
}