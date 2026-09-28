package net.follis.tutorialmod.entity.client;

import net.follis.tutorialmod.TutorialMod;
import net.follis.tutorialmod.entity.custom.GoldenNeedleProjectileEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class GoldenNeedleProjectileRenderer extends EntityRenderer<GoldenNeedleProjectileEntity> {
    private static final float ROPE_R = 1.0F;
    private static final float ROPE_G = 0.82F;
    private static final float ROPE_B = 0.30F;

    protected GoldenNeedleProjectileModel model;

    public GoldenNeedleProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new GoldenNeedleProjectileModel(ctx.getPart(GoldenNeedleProjectileModel.GOLDEN_NEEDLE));
    }

    // keep drawing the rope even when the needle itself is off-screen
    @Override
    public boolean shouldRender(GoldenNeedleProjectileEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.getTargetId() != 0 || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(GoldenNeedleProjectileEntity goldenNeedleProjectile, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.translate(0, 0, 0);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(g, goldenNeedleProjectile.prevYaw, goldenNeedleProjectile.getYaw()) - 90.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(g, goldenNeedleProjectile.prevPitch, goldenNeedleProjectile.getPitch()) + 90.0F));

        VertexConsumer vertexConsumer = ItemRenderer.getDirectItemGlintConsumer(vertexConsumerProvider, this.model.getLayer(this.getTexture(goldenNeedleProjectile)), false, false);
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();

        renderRope(goldenNeedleProjectile, g, matrixStack, vertexConsumerProvider);
        super.render(goldenNeedleProjectile, f, g, matrixStack, vertexConsumerProvider, i);
    }
    private static final int SEGMENTS = 24;
    private void renderRope(GoldenNeedleProjectileEntity needle, float tickDelta, MatrixStack matrices, VertexConsumerProvider providers) {
        int duration = needle.getDuration();
        if (needle.getTargetId() == 0 || duration <= 0) return;

        Entity target = needle.getWorld().getEntityById(needle.getTargetId());
        if (target == null || !target.isAlive()) return;

        // the matrix stack origin is the needle's interpolated position, so the rope runs from (0,0,0) to the target
        Vec3d end = target.getLerpedPos(tickDelta)
                .add(0, target.getHeight() / 2.0, 0)
                .subtract(needle.getLerpedPos(tickDelta));
        double length = end.length();
        if (length < 0.05) return;

        float time = needle.age + tickDelta;
        float fade = MathHelper.clamp((duration - tickDelta) / 40.0F, 0.3F, 1.0F);
        float pulse = 0.85F + 0.15F * MathHelper.sin(time * 0.4F);

        Vec3d dir = end.normalize();
        Vec3d up = Math.abs(dir.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d side1 = dir.crossProduct(up).normalize();
        Vec3d side2 = dir.crossProduct(side1).normalize();

        int segments = MathHelper.clamp((int) (length * 4), 6, 32);
        double sag = Math.min(length * 0.06, 0.4) * fade;
        Vec3d[] points = new Vec3d[SEGMENTS + 1];
        for (int s = 0; s <= SEGMENTS; s++) {
            float t = (float) s / SEGMENTS;
            float envelope = 4.0F * t * (1.0F - t);
            Vec3d p = end.multiply(t).add(0, -sag * envelope, 0);
            p = p.add(side1.multiply(MathHelper.sin(time * 0.35F + t * 6.0F) * 0.03 * envelope));
            points[s] = p;
        }

        VertexConsumer buffer = providers.getBuffer(RenderLayer.getLightning());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        drawRibbon(buffer, matrix, points, side1, side2, 0.11F * fade, 0.16F * pulse); // halo
        drawRibbon(buffer, matrix, points, side1, side2, 0.035F * fade, 0.9F * pulse);  // core
    }

    private static void drawRibbon(VertexConsumer buffer, Matrix4f matrix, Vec3d[] points, Vec3d side1, Vec3d side2, float halfWidth, float alpha) {
        for (int s = 0; s < points.length - 1; s++) {
            drawQuad(buffer, matrix, points[s], points[s + 1], side1, halfWidth, alpha);
            drawQuad(buffer, matrix, points[s], points[s + 1], side2, halfWidth, alpha);
        }
    }

    private static void drawQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d a, Vec3d b, Vec3d side, float halfWidth, float alpha) {
        float ox = (float) (side.x * halfWidth);
        float oy = (float) (side.y * halfWidth);
        float oz = (float) (side.z * halfWidth);

        // front face
        vertex(buffer, matrix, a.x - ox, a.y - oy, a.z - oz, alpha);
        vertex(buffer, matrix, a.x + ox, a.y + oy, a.z + oz, alpha);
        vertex(buffer, matrix, b.x + ox, b.y + oy, b.z + oz, alpha);
        vertex(buffer, matrix, b.x - ox, b.y - oy, b.z - oz, alpha);
        // back face (reversed winding so backface culling never hides it)
        vertex(buffer, matrix, b.x - ox, b.y - oy, b.z - oz, alpha);
        vertex(buffer, matrix, b.x + ox, b.y + oy, b.z + oz, alpha);
        vertex(buffer, matrix, a.x + ox, a.y + oy, a.z + oz, alpha);
        vertex(buffer, matrix, a.x - ox, a.y - oy, a.z - oz, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, double x, double y, double z, float alpha) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z).color(ROPE_R, ROPE_G, ROPE_B, alpha);
    }

    @Override
    public Identifier getTexture(GoldenNeedleProjectileEntity entity) {
        return Identifier.of(TutorialMod.MOD_ID, "textures/entity/golden_needle/golden_needle.png");
    }
}