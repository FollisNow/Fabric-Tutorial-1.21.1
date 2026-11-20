package net.follis.goldenscarabmod.entity.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/** Draws a glowing square beam. */
public final class BeamDrawing {
    private BeamDrawing() {}

    /** From the origin along -Y (the way the model's blade points), length blocks long. Turn the matrices first to aim it. */
    public static void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double length, float radius,
                            int red, int green, int blue, int alpha) {
        if (radius <= 0.0F || length <= 0.0) return;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLightning());   // glows, and writes depth
        Matrix4f m = matrices.peek().getPositionMatrix();
        float l = (float) -length;
        side(consumer, m, -radius, -radius, radius, -radius, l, red, green, blue, alpha);
        side(consumer, m, radius, -radius, radius, radius, l, red, green, blue, alpha);
        side(consumer, m, radius, radius, -radius, radius, l, red, green, blue, alpha);
        side(consumer, m, -radius, radius, -radius, -radius, l, red, green, blue, alpha);
    }

    /** One face, drawn from both sides so culling never hides it. */
    private static void side(VertexConsumer c, Matrix4f m, float x0, float z0, float x1, float z1, float len,
                             int r, int g, int b, int a) {
        c.vertex(m, x0, 0.0F, z0).color(r, g, b, a);
        c.vertex(m, x1, 0.0F, z1).color(r, g, b, a);
        c.vertex(m, x1, len, z1).color(r, g, b, a);
        c.vertex(m, x0, len, z0).color(r, g, b, a);

        c.vertex(m, x0, len, z0).color(r, g, b, a);
        c.vertex(m, x1, len, z1).color(r, g, b, a);
        c.vertex(m, x1, 0.0F, z1).color(r, g, b, a);
        c.vertex(m, x0, 0.0F, z0).color(r, g, b, a);
    }
}