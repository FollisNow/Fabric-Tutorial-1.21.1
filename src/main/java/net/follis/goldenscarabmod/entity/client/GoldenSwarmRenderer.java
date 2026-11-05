package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.GoldenSwarmEntity;
import net.follis.goldenscarabmod.entity.swarm.SwarmFx;
import net.follis.goldenscarabmod.entity.swarm.SwarmState;
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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws the swarm directly as quads: no ModelPart, no model layer.
 * Gold sheets sample little 4x4 patches of the vanilla gold block texture (so the palette
 * varies from sheet to sheet) and are darkened per corner for a fake ambient-occlusion look.
 * Eyes are emissive redstone block patches.
 */
public class GoldenSwarmRenderer extends EntityRenderer<GoldenSwarmEntity> {

    private static final Identifier GOLD_TEXTURE = Identifier.of("minecraft", "textures/block/gold_block.png");
    private static final Identifier EYE_TEXTURE = Identifier.of("minecraft", "textures/block/redstone_block.png");

    private static final float PX = 1f / 16f;
    private static final float PATCH = 4f; // pixels sampled per sheet

    public GoldenSwarmRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.7f;
        // lets the swarm code (which knows nothing about client classes) shake the screen when the arm slams
        SwarmFx.slamHandler = ScreenShake::trigger;
    }

    @Override
    public Identifier getTexture(GoldenSwarmEntity entity) {
        return GOLD_TEXTURE;
    }

    @Override
    public void render(GoldenSwarmEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        SwarmState swarm = entity.swarm;
        if (swarm != null && swarm.isReady()) {
            // The matrix is already translated to the entity's interpolated position,
            // so sheets are emitted relative to that position.
            double ex = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double ey = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double ez = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
            Vec3d cam = this.dispatcher.camera.getPos();
            float camX = (float) (cam.x - ex), camY = (float) (cam.y - ey), camZ = (float) (cam.z - ez);

            MatrixStack.Entry entry = matrices.peek();

            // Two passes on purpose: asking the provider for a second RenderLayer ends the first
            // one's buffer, so each consumer must be fully used before the next is requested.
            VertexConsumer gold = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(GOLD_TEXTURE));
            for (SwarmState.Sheet s : swarm.sheets) {
                if (!s.eye) drawSheet(gold, entry, s, tickDelta, ex, ey, ez, camX, camY, camZ, light);
            }
            for (SwarmState.Sheet s : swarm.fxSheets) {   // the phase-change flurry (drawSheet skips the idle ones)
                drawSheet(gold, entry, s, tickDelta, ex, ey, ez, camX, camY, camZ, light);
            }

            VertexConsumer eyes = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(EYE_TEXTURE));
            for (SwarmState.Sheet s : swarm.sheets) {
                if (s.eye) drawSheet(eyes, entry, s, tickDelta, ex, ey, ez, camX, camY, camZ,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);
            }
            for (SwarmState.Sheet s : swarm.telegraphSheets) {   // red markers on the floor: where the next attack lands
                drawSheet(eyes, entry, s, tickDelta, ex, ey, ez, camX, camY, camZ,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE);
            }
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void drawSheet(VertexConsumer vc, MatrixStack.Entry entry, SwarmState.Sheet s, float td,
                                  double ex, double ey, double ez, float camX, float camY, float camZ, int light) {
        // shed sheets shrink to nothing; skip them once they are gone
        float sc = MathHelper.lerp(td, s.pscale, s.scale);
        if (sc <= 0.02f) return;
        float hw = s.halfW * sc, hh = s.halfH * sc;

        // interpolated centre, relative to the entity
        float cx = (float) (MathHelper.lerp(td, s.px, s.x) - ex);
        float cy = (float) (MathHelper.lerp(td, s.py, s.y) - ey);
        float cz = (float) (MathHelper.lerp(td, s.pz, s.z) - ez);

        // interpolated, renormalised normal
        float nx = MathHelper.lerp(td, s.pnx, s.nx);
        float ny = MathHelper.lerp(td, s.pny, s.ny);
        float nz = MathHelper.lerp(td, s.pnz, s.nz);
        float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-4f) {
            nx = 0f;
            ny = 1f;
            nz = 0f;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
        }

        // tangent basis (u, v) of the sheet plane
        float rx = 0f, ry = 1f, rz = 0f;
        if (Math.abs(ny) > 0.95f) {
            rx = 1f;
            ry = 0f;
        }
        float ux = ry * nz - rz * ny, uy = rz * nx - rx * nz, uz = rx * ny - ry * nx;
        float ul = MathHelper.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;

        // spin around the normal, then scale by the half sizes
        float spin = MathHelper.lerp(td, s.pspin, s.spin);
        float cs = MathHelper.cos(spin), sn = MathHelper.sin(spin);
        float ax = (ux * cs + vx * sn) * hw, ay = (uy * cs + vy * sn) * hw, az = (uz * cs + vz * sn) * hw;
        float bx = (-ux * sn + vx * cs) * hh, by = (-uy * sn + vy * cs) * hh, bz = (-uz * sn + vz * cs) * hh;

        // sheets are two-sided: light them with the normal that faces the camera
        float side = (nx * (camX - cx) + ny * (camY - cy) + nz * (camZ - cz)) < 0f ? -1f : 1f;
        float lnx = nx * side, lny = ny * side, lnz = nz * side;

        float u0 = s.texU * PX, u1 = (s.texU + PATCH) * PX;
        float v0 = s.texV * PX, v1 = (s.texV + PATCH) * PX;

        Matrix4f pos = entry.getPositionMatrix();
        vert(vc, pos, entry, cx - ax - bx, cy - ay - by, cz - az - bz, u0, v1, shade(s, 0), light, lnx, lny, lnz);
        vert(vc, pos, entry, cx + ax - bx, cy + ay - by, cz + az - bz, u1, v1, shade(s, 1), light, lnx, lny, lnz);
        vert(vc, pos, entry, cx + ax + bx, cy + ay + by, cz + az + bz, u1, v0, shade(s, 2), light, lnx, lny, lnz);
        vert(vc, pos, entry, cx - ax + bx, cy - ay + by, cz - az + bz, u0, v0, shade(s, 3), light, lnx, lny, lnz);
    }

    private static int shade(SwarmState.Sheet s, int corner) {
        return MathHelper.clamp((int) (255f * s.ao * s.corner[corner]), 0, 255);
    }

    private static void vert(VertexConsumer vc, Matrix4f pos, MatrixStack.Entry entry, float x, float y, float z,
                             float u, float v, int c, int light, float nx, float ny, float nz) {
        vc.vertex(pos, x, y, z)
                .color(c, c, c, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}