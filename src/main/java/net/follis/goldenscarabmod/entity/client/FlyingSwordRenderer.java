package net.follis.goldenscarabmod.entity.client;

import net.follis.goldenscarabmod.entity.custom.FlyingSwordEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {
    private static final float SCALE = 1.0F;
    private static final float APPEAR_TICKS = 6.0F;
    private static final float BOB_HEIGHT = 0.12F;   // how far a hovering rain sword drifts up and down

    protected final GoldenSwordModel model;

    public FlyingSwordRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new GoldenSwordModel(ctx.getPart(GoldenSwordModel.GOLDEN_SWORD));
    }

    @Override
    public void render(FlyingSwordEntity sword, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float t = sword.getTicks() + tickDelta;
        float sinceAppear = t - sword.getAppearAt();
        if (sinceAppear < 0.0F) return;   // not summoned yet

        float grow = MathHelper.clamp(sinceAppear / APPEAR_TICKS, 0.0F, 1.0F);
        float fade = MathHelper.clamp((sword.getLifeEnd() - t) / FlyingSwordEntity.FADE_TICKS, 0.0F, 1.0F);
        float size = grow * fade;   // opaque layer: the sword grows and shrinks instead of fading
        if (size <= 0.0F) return;

        // gold is the texture as it is; a sword that changes colour fades from gold to its own
        int tr = 255, tg = 255, tb = 255;
        switch (sword.getTint()) {
            case 1 -> { tr = 70; tg = 45; tb = 95; }    // black sword
            case 2 -> { tr = 255; tg = 70; tb = 70; }   // red sword
            default -> {}
        }
        float mix = sword.getRecolorLength() <= 0 ? 1.0F
                : MathHelper.clamp((t - sword.getRecolorAt()) / sword.getRecolorLength(), 0.0F, 1.0F);
        int red = (int) MathHelper.lerp(mix, 255.0F, (float) tr);
        int green = (int) MathHelper.lerp(mix, 255.0F, (float) tg);
        int blue = (int) MathHelper.lerp(mix, 255.0F, (float) tb);

        Vec3d entityPosition = sword.getLerpedPos(tickDelta);
        FlyingSwordEntity.StarPose star = sword.getStarPose(tickDelta);

        matrices.push();
        if (star != null) {
            // part of a star: drawn at its place in the star, worked out here, so it is a perfect circle whatever the network does.
            // The blade stays flat in the plane of the star and does not turn on itself.
            matrices.translate(star.position().x - entityPosition.x, star.position().y - entityPosition.y,
                    star.position().z - entityPosition.z);
            SwordDrawing.draw(model, matrices, vertexConsumers, star.aim(), star.normal(), SwordDrawing.CENTER_HEIGHT,
                    SCALE * size, 0.0F, red, green, blue);
        } else if (sword.isRain()) {
            // a rain sword is drawn where its client-side hover puts it, which is not exactly where the server's entity is
            Vec3d drawnPosition = sword.getRenderPosition(tickDelta);
            double bob = sword.isHovering() ? BOB_HEIGHT * MathHelper.sin((t + sword.getAppearAt() * 3.0F) * 0.16F) : 0.0;
            matrices.translate(drawnPosition.x - entityPosition.x, drawnPosition.y - entityPosition.y + bob,
                    drawnPosition.z - entityPosition.z);
            // its position is the tip of the blade; the spin freezes once it falls
            SwordDrawing.draw(model, matrices, vertexConsumers, sword.getAim(tickDelta), SwordDrawing.TIP_HEIGHT, SCALE * size,
                    Math.min(t, sword.getFallAt()) * 8.0F, red, green, blue);
        } else {
            // centred on its position; a sword stuck in a block stops turning, every other one turns slowly on itself
            SwordDrawing.draw(model, matrices, vertexConsumers, sword.getAim(tickDelta), SwordDrawing.CENTER_HEIGHT,
                    SCALE * size, Math.min(t, sword.getSpinEnd()) * 10.0F, red, green, blue);
        }
        matrices.pop();

        super.render(sword, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(FlyingSwordEntity entity) {
        return SwordDrawing.TEXTURE;
    }
}