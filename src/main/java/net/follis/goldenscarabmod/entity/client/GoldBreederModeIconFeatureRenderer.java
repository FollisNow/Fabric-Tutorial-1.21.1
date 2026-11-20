package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.GoldBreederEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the current breeding-mode icon on the outer side of the love gun.
 *
 * AbstractGoldWorkerRenderer can't be used for this: it draws the held item at a fixed offset from the body, while
 * the gun swings up and aims at bugs. Here the icon follows the gun bone itself (see GoldBreederModel#transformToGun),
 * so it moves with the arm, the aiming and the animations.
 */
public class GoldBreederModeIconFeatureRenderer
        extends FeatureRenderer<GoldBreederEntity, GoldBreederModel<GoldBreederEntity>> {

    // Position on the gun, in model pixels (1/16 block). The gun cuboid spans x -1..1, y -0.6..3.4, z -1.6..1.4;
    // +X is the golem's outer (left) side, so the icon sits just outside that face.
    private static final float ICON_X = 1.1F;
    private static final float ICON_Y = 1.4F;
    private static final float ICON_Z = -0.06F;
    /** Rotation that makes the icon face outward. If it looks backwards/mirrored, use +90 or add 180. */
    private static final float ICON_YAW_DEGREES = -90.0F;
    /** x/y scale, same as the channeler. FIXED mode already halves the item, so 0.1875 makes it about 1.5 px wide. */
    private static final float ICON_SIZE = 0.1875F;
    /** z scale. Blocks are cubes: squash them so they read as a flat picture instead of a box on the gun. */
    private static final float ICON_FLATTEN = 0.025F;

    private final HeldItemRenderer heldItemRenderer;
    private final Map<GoldBreederEntity.BreedMode, ItemStack> iconCache = new EnumMap<>(GoldBreederEntity.BreedMode.class);

    public GoldBreederModeIconFeatureRenderer(
            FeatureRendererContext<GoldBreederEntity, GoldBreederModel<GoldBreederEntity>> context,
            HeldItemRenderer heldItemRenderer) {
        super(context);
        this.heldItemRenderer = heldItemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       GoldBreederEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;

        // read-only cache of icon stacks (never mutated), so no allocation per frame
        ItemStack icon = this.iconCache.computeIfAbsent(entity.getBreedMode(), mode -> new ItemStack(mode.icon()));
        if (icon.isEmpty()) return;

        matrices.push();

        // 1) into the gun's local space (current animation + aim included)
        this.getContextModel().transformToGun(matrices);
        // 2) onto the outer face of the gun (model units are pixels, convert to blocks)
        matrices.translate(ICON_X / 16.0F, ICON_Y / 16.0F, ICON_Z / 16.0F);
        // 3) the entity renderer mirrors the model (scale -1,-1,1): undo it so the item is not drawn upside down
        matrices.scale(-1.0F, -1.0F, 1.0F);
        // 4) turn the item's front toward the gun's outer side, then shrink it
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ICON_YAW_DEGREES));
        matrices.scale(ICON_SIZE, ICON_SIZE, ICON_FLATTEN);

        this.heldItemRenderer.renderItem(entity, icon, ModelTransformationMode.FIXED, false,
                matrices, vertexConsumers, light);

        matrices.pop();
    }
}