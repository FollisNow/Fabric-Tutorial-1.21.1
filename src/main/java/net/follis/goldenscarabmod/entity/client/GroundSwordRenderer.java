package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GroundSwordEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GroundSwordRenderer extends EntityRenderer<GroundSwordEntity> {
    private static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/golden_sword/golden_sword.png");

    private static final float SCALE = 2.2F;
    private static final float TIP_HEIGHT = 3.0F / 16.0F;   // in the model, the tip is this far above the origin
    private static final float VISIBLE_HEIGHT = 2.2F;       // how high the tip gets above the floor
    private static final Vector3f MODEL_TIP_DIRECTION = new Vector3f(0.0F, -1.0F, 0.0F);
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

    protected final GoldenSwordModel model;

    public GroundSwordRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new GoldenSwordModel(ctx.getPart(GoldenSwordModel.GOLDEN_SWORD));
    }

    @Override
    public void render(GroundSwordEntity sword, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float since = sword.getTicks() + tickDelta - sword.getEruptAt();
        float total = GroundSwordEntity.RISE_TICKS + GroundSwordEntity.UP_TICKS + GroundSwordEntity.SINK_TICKS;
        if (since < 0.0F || since > total) return;   // still hidden underground, or already gone

        float rise;
        if (since < GroundSwordEntity.RISE_TICKS) {
            rise = since / GroundSwordEntity.RISE_TICKS;
        } else if (since < GroundSwordEntity.RISE_TICKS + GroundSwordEntity.UP_TICKS) {
            rise = 1.0F;
        } else {
            rise = 1.0F - (since - GroundSwordEntity.RISE_TICKS - GroundSwordEntity.UP_TICKS) / GroundSwordEntity.SINK_TICKS;
        }
        rise = MathHelper.clamp(rise, 0.0F, 1.0F);

        matrices.push();
        matrices.translate(0.0F, VISIBLE_HEIGHT * rise, 0.0F);   // the tip's height above the floor
        matrices.multiply(new Quaternionf().rotationTo(MODEL_TIP_DIRECTION, UP));   // tip up
        matrices.translate(0.0F, -TIP_HEIGHT * SCALE, 0.0F);                         // the tip sits on that point
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sword.getId() * 47) % 360));   // each one faces a different way
        matrices.scale(SCALE, SCALE, SCALE);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
        model.render(matrices, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
        matrices.pop();

        super.render(sword, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(GroundSwordEntity entity) {
        return TEXTURE;
    }
}