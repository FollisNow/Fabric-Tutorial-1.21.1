package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.GoldChannelerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the item of the selected filter (sculk sensor, player head, ...) as a small flat icon on the front of
 * the crest. It is attached to the root > head > crest bones, so it follows the head when it turns and the
 * body when it leans while walking.
 *
 * A feature renderer receives the matrix stack already in MODEL space (the vanilla flip of x and y included),
 * which is why the bones are applied first and the flip is undone afterwards.
 */
public class GoldChannelerFilterIconFeature
        extends FeatureRenderer<GoldChannelerEntity, GoldChannelerModel<GoldChannelerEntity>> {

    // Placement in model pixels, relative to the crest pivot (y points DOWN in model space, -z is the front).
    // The crest spans x -12..1 (13 wide) and y -9..0 (9 tall): its middle is (-5.5, -4.5).
    private static final float ICON_X = -5.5F;
    private static final float ICON_Y = -7.5F;
    // At this size the icon only faces the crest layers around its middle, whose fronts are at z = -1.5 or -2.0
    // (relative to the crest pivot). A plane at -2.0 sits just in front of them; go to about -2.2 if you ever
    // see flickering on the row below it.
    private static final float ICON_Z = -1.0F;

    /** x/y scale. FIXED mode already halves the item, so 0.1875 makes it about 1.5 px wide (a quarter of before). */
    private static final float ICON_SIZE = 0.1875F;
    /** z scale. Blocks are cubes: squash them so they read as a flat picture instead of a box on the crest. */
    private static final float ICON_FLATTEN = 0.025F;

    private final HeldItemRenderer heldItemRenderer;
    private final Map<GoldChannelerEntity.Filter, ItemStack> icons = new EnumMap<>(GoldChannelerEntity.Filter.class);

    public GoldChannelerFilterIconFeature(
            FeatureRendererContext<GoldChannelerEntity, GoldChannelerModel<GoldChannelerEntity>> context,
            HeldItemRenderer heldItemRenderer) {
        super(context);
        this.heldItemRenderer = heldItemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       GoldChannelerEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;

        ItemStack icon = this.icons.computeIfAbsent(entity.getFilter(), filter -> new ItemStack(filter.getIcon()));
        GoldChannelerModel<GoldChannelerEntity> model = this.getContextModel();

        matrices.push();
        // same chain of bones as the model: root > head > crest (each call applies pivot + current rotation)
        model.getPart().rotate(matrices);
        model.getHead().rotate(matrices);
        model.getCrest().rotate(matrices);
        matrices.translate(ICON_X / 16.0F, ICON_Y / 16.0F, ICON_Z / 16.0F);

        // back to an upright frame (undo the vanilla x/y flip). The golem's front is now -z, and FIXED
        // (item-frame style) display faces -z by default, so the icon looks forward with no extra rotation.
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.scale(ICON_SIZE, ICON_SIZE, ICON_FLATTEN);

        this.heldItemRenderer.renderItem(entity, icon, ModelTransformationMode.FIXED, false,
                matrices, vertexConsumers, light);
        matrices.pop();
    }
}