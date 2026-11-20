package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.GoldenScarabMod;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The one place that draws a sword: every renderer that shows one goes through here. */
public final class SwordDrawing {
    public static final Identifier TEXTURE =
            Identifier.of(GoldenScarabMod.MOD_ID, "textures/entity/golden_sword/golden_sword.png");

    /** In the model, the middle of the sword is this far above the origin. */
    public static final float CENTER_HEIGHT = 11.0F / 16.0F;
    /** In the model, the tip is this far above the origin. */
    public static final float TIP_HEIGHT = 3.0F / 16.0F;

    private static final Vector3f MODEL_TIP_DIRECTION = new Vector3f(0.0F, -1.0F, 0.0F);   // the model's blade points down

    private SwordDrawing() {}

    /**
     * Draws one sword at the current origin of the matrices. The blade points along aim and the sword turns around
     * pivotHeight (CENTER_HEIGHT or TIP_HEIGHT): that point of the model sits on the origin. scale already includes any growing in.
     * The layer is opaque, so the sword takes part in depth testing like any mob.
     */
    /** The same, for a sword that turns freely on itself. */
    public static void draw(GoldenSwordModel model, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                            Vec3d aim, float pivotHeight, float scale, float spinDegrees, int red, int green, int blue) {
        draw(model, matrices, vertexConsumers, aim, null, pivotHeight, scale, spinDegrees, red, green, blue);
    }

    /**
     * Draws one sword at the current origin of the matrices. The blade points along aim and the sword turns around
     * pivotHeight (CENTER_HEIGHT or TIP_HEIGHT): that point of the model sits on the origin. scale already includes any growing in.
     * With a faceNormal the sword does not spin: the broad face of its blade looks along that direction, so a flat formation stays flat.
     * Without one it spins by spinDegrees around its blade.
     */
    public static void draw(GoldenSwordModel model, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                            Vec3d aim, @Nullable Vec3d faceNormal, float pivotHeight, float scale, float spinDegrees,
                            int red, int green, int blue) {
        matrices.push();
        boolean oriented = faceNormal != null && orient(matrices, aim, faceNormal);
        if (!oriented) {
            matrices.multiply(new Quaternionf().rotationTo(MODEL_TIP_DIRECTION,
                    new Vector3f((float) aim.x, (float) aim.y, (float) aim.z)));
        }
        matrices.translate(0.0F, -pivotHeight * scale, 0.0F);
        if (!oriented) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spinDegrees));
        matrices.scale(scale, scale, scale);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        int color = ColorHelper.Argb.getArgb(255, red, green, blue);
        model.render(matrices, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, color);
        matrices.pop();
    }

    /**
     * Turns the model so its blade points along aim and the broad face of the blade (the model's Z axis) looks along faceNormal.
     * Returns false when that is not possible, for example when the blade points along the normal.
     */
    private static boolean orient(MatrixStack matrices, Vec3d aim, Vec3d faceNormal) {
        Vector3f y = new Vector3f((float) -aim.x, (float) -aim.y, (float) -aim.z);   // the model's +Y runs from the tip to the handle
        if (y.lengthSquared() < 1.0E-6F) return false;
        y.normalize();

        Vector3f z = new Vector3f((float) faceNormal.x, (float) faceNormal.y, (float) faceNormal.z);
        z.sub(new Vector3f(y).mul(z.dot(y)));   // the normal made perpendicular to the blade
        if (z.lengthSquared() < 1.0E-4F) return false;
        z.normalize();

        Vector3f x = new Vector3f(y).cross(z);
        matrices.multiply(new Quaternionf().setFromNormalized(new Matrix3f(x, y, z)));
        return true;
    }
}