package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.follis.goldenscarabmod.entity.custom.GoldenSwordProjectileEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GoldenSwordProjectileRenderer extends EntityRenderer<GoldenSwordProjectileEntity> {
    private static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/golden_sword/golden_sword.png");

    private static final float SCALE = 1.5F;
    private static final float TIP_HEIGHT = 3.0F / 16.0F;   // in the model, the tip is this far above the origin
    private static final float APPEAR_TICKS = 6.0F;         // how long the sword takes to grow in
    private static final float FADE_TICKS = 10.0F;          // how long it takes to fade once stuck in the ground
    private static final Vector3f MODEL_TIP_DIRECTION = new Vector3f(0.0F, -1.0F, 0.0F);   // the model's blade points down

    protected final GoldenSwordProjectileModel model;

    public GoldenSwordProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new GoldenSwordProjectileModel(ctx.getPart(GoldenSwordProjectileModel.GOLDEN_SWORD));
    }

    @Override
    public void render(GoldenSwordProjectileEntity sword, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float t = sword.getTicks() + tickDelta;
        float sinceAppear = t - sword.getAppearAt();
        if (sinceAppear < 0.0F) return;   // not summoned yet

        float grow = MathHelper.clamp(sinceAppear / APPEAR_TICKS, 0.0F, 1.0F);
        float fade = sword.isStuck()
                ? MathHelper.clamp((GoldenSwordProjectileEntity.STUCK_TICKS - (sword.getStuckTicks() + tickDelta)) / FADE_TICKS, 0.0F, 1.0F)
                : 1.0F;

        Vec3d aim = sword.getAim(tickDelta);

        matrices.push();
        // point the blade along its aim, turning around the tip (which is the entity's position)
        matrices.multiply(new Quaternionf().rotationTo(MODEL_TIP_DIRECTION,
                new Vector3f((float) aim.x, (float) aim.y, (float) aim.z)));
        // then slide the model so that its tip sits exactly on that pivot
        matrices.translate(0.0F, -TIP_HEIGHT * SCALE * grow, 0.0F);
        // a slow spin along the blade while hovering, frozen once it falls
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Math.min(t, sword.getFallAt()) * 8.0F));
        matrices.scale(SCALE * grow, SCALE * grow, SCALE * grow);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
        int color = ColorHelper.Argb.getArgb((int) (255.0F * grow * fade), 255, 255, 255);
        model.render(matrices, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, color);
        matrices.pop();

        super.render(sword, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(GoldenSwordProjectileEntity entity) {
        return TEXTURE;
    }
}